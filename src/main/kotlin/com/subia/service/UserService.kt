package com.subia.service

import com.subia.exception.VerificationTokenException
import com.subia.exception.WeakPasswordException
import com.subia.model.PasswordResetToken
import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.PasswordResetTokenRepository
import com.subia.repository.RefreshTokenRepository
import com.subia.repository.SecurityEventRepository
import com.subia.repository.SubscriptionRepository
import com.subia.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.OffsetDateTime
import kotlin.math.min
import kotlin.math.pow

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val securityEventRepository: SecurityEventRepository,
    private val emailService: EmailService
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)
    private val passwordEncoder = BCryptPasswordEncoder(12)
    private val secureRandom = SecureRandom()

    @Transactional
    fun register(email: String, password: String, confirmPassword: String) {
        validatePasswordStrength(password)

        // Anti-enumeración: si el email ya existe retornar sin revelar
        if (userRepository.existsByEmail(email)) {
            log.debug("Intento de registro con email existente (silenciado): {}", email)
            return
        }

        val passwordHash = passwordEncoder.encode(password)
        val verificationToken = generateSecureToken()

        val user = User(
            email = email,
            passwordHash = passwordHash,
            emailVerified = false,
            role = UserRole.USER,
            emailVerificationToken = verificationToken,
            emailVerificationExpiresAt = OffsetDateTime.now().plusHours(24)
        )

        userRepository.save(user)
        emailService.sendVerificationEmail(email, verificationToken)
        log.info("Usuario registrado: {}", email)
    }

    @Transactional
    fun verifyEmail(token: String) {
        val user = userRepository.findByEmailVerificationToken(token)
            ?: throw VerificationTokenException("Token de verificación inválido o ya utilizado")

        val expiresAt = user.emailVerificationExpiresAt
        if (expiresAt == null || expiresAt.isBefore(OffsetDateTime.now())) {
            throw VerificationTokenException("Token de verificación expirado")
        }

        val verified = user.copy(
            emailVerified = true,
            emailVerificationToken = null,
            emailVerificationExpiresAt = null
        )
        userRepository.save(verified)
        log.info("Email verificado para usuario: {}", user.email)
    }

    @Transactional
    fun handleFailedLogin(email: String) {
        val user = userRepository.findByEmail(email) ?: return

        val newAttempts = user.failedAttempts + 1
        val lockedUntil = if (newAttempts >= 4) {
            val minutesLocked = min(5.0 * 2.0.pow((newAttempts - 4).toDouble()), 1440.0).toLong()
            OffsetDateTime.now().plusMinutes(minutesLocked)
        } else {
            user.lockedUntil
        }

        userRepository.save(user.copy(
            failedAttempts = newAttempts,
            lockedUntil = lockedUntil
        ))

        if (newAttempts >= 4) {
            log.warn("Cuenta bloqueada para {} — intentos: {}, bloqueada hasta: {}", email, newAttempts, lockedUntil)
        }
    }

    @Transactional
    fun resetFailedAttempts(email: String) {
        val user = userRepository.findByEmail(email) ?: return
        userRepository.save(user.copy(failedAttempts = 0, lockedUntil = null))
    }

    @Transactional
    fun findOrCreateByGoogle(email: String, googleId: String): User {
        // Buscar por googleId
        userRepository.findByGoogleId(googleId)?.let { return it }

        // Buscar por email y vincular googleId
        userRepository.findByEmail(email)?.let { existing ->
            val updated = existing.copy(googleId = googleId)
            return userRepository.save(updated)
        }

        // Crear nuevo usuario Google
        val newUser = User(
            email = email,
            googleId = googleId,
            emailVerified = true,
            role = UserRole.USER
        )
        return userRepository.save(newUser).also {
            log.info("Usuario creado via Google OAuth: {}", email)
        }
    }

    @Transactional
    fun initiatePasswordReset(email: String) {
        // Anti-enumeración: siempre retornar sin revelar si el email existe
        val user = userRepository.findByEmail(email) ?: run {
            log.debug("Solicitud de reset para email inexistente (silenciada): {}", email)
            return
        }

        // Invalidar tokens anteriores
        passwordResetTokenRepository.deleteByUserId(user.id)

        val token = generateSecureToken()
        val resetToken = PasswordResetToken(
            userId = user.id,
            token = token,
            expiresAt = OffsetDateTime.now().plusHours(1)
        )
        passwordResetTokenRepository.save(resetToken)
        emailService.sendPasswordResetEmail(email, token)
        log.info("Reset de contraseña iniciado para: {}", email)
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw VerificationTokenException("Token de restablecimiento inválido")

        if (resetToken.used) {
            throw VerificationTokenException("Token de restablecimiento ya utilizado")
        }

        if (resetToken.expiresAt.isBefore(OffsetDateTime.now())) {
            throw VerificationTokenException("Token de restablecimiento expirado")
        }

        validatePasswordStrength(newPassword)

        val user = userRepository.findById(resetToken.userId)
            .orElseThrow { VerificationTokenException("Usuario no encontrado") }

        val newHash = passwordEncoder.encode(newPassword)
        userRepository.save(user.copy(passwordHash = newHash))
        passwordResetTokenRepository.save(resetToken.copy(used = true))
        log.info("Contraseña restablecida para usuario ID: {}", resetToken.userId)
    }

    @Transactional
    fun deleteAccount(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("Usuario no encontrado: $userId")
        }
        // Eliminar refresh tokens por email
        val refreshTokens = refreshTokenRepository.findByEmail(user.email)
        refreshTokenRepository.deleteAll(refreshTokens)
        // Eliminar suscripciones
        subscriptionRepository.deleteByUserId(userId)
        // Eliminar password reset tokens
        passwordResetTokenRepository.deleteByUserId(userId)
        // Eliminar security events
        securityEventRepository.deleteByUserId(userId)
        // Eliminar usuario
        userRepository.deleteById(userId)
        log.info("Cuenta eliminada para usuario ID: {}", userId)
    }

    fun checkPassword(userId: Long, rawPassword: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        val hash = user.passwordHash ?: return false
        return passwordEncoder.matches(rawPassword, hash)
    }

    fun findById(userId: Long): User? = userRepository.findById(userId).orElse(null)

    fun findByEmail(email: String): User? = userRepository.findByEmail(email)

    private fun validatePasswordStrength(password: String) {
        if (password.length < 10) {
            throw WeakPasswordException("La contraseña debe tener al menos 10 caracteres")
        }
        if (!password.any { it.isUpperCase() }) {
            throw WeakPasswordException("La contraseña debe contener al menos una letra mayúscula")
        }
        if (!password.any { it.isDigit() }) {
            throw WeakPasswordException("La contraseña debe contener al menos un dígito")
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            throw WeakPasswordException("La contraseña debe contener al menos un símbolo especial")
        }
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
