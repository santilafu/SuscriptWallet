package com.subia.service

import com.subia.exception.InvalidGoogleTokenException
import com.subia.exception.InvalidRegistrationException
import com.subia.exception.VerificationTokenException
import com.subia.exception.WeakPasswordException
import com.subia.model.PasswordResetToken
import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.GmailScanResultRepository
import com.subia.repository.GmailScanTicketRepository
import com.subia.repository.PasswordResetTokenRepository
import com.subia.repository.RefreshTokenRepository
import com.subia.repository.SecurityEventRepository
import com.subia.repository.SubscriptionRepository
import com.subia.repository.UserRepository
import com.subia.repository.findByEmailAnyCase
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
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
    private val gmailScanResultRepository: GmailScanResultRepository,
    private val gmailScanTicketRepository: GmailScanTicketRepository,
    private val emailService: EmailService,
    /** Sesiones web (Spring Session JDBC), para cerrarlas al restablecer o borrar (ronda 6). */
    private val sessionRegistry: SessionRegistry,
    transactionManager: PlatformTransactionManager
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

    /**
     * Transacción programática (REQUIRED) para `resetPassword` y `deleteAccount` (ronda 6): así las
     * sesiones web se cierran DESPUÉS del commit y fuera de la transacción. Spring Session JDBC usa
     * su propia transacción REQUIRES_NEW: llamada dentro de la nuestra, cada petición ocuparía DOS
     * conexiones del pool (en producción solo hay 3) y, si el restablecimiento fallara después, se
     * habrían cerrado sesiones sin cambiar la contraseña.
     */
    private val inTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED
    }
    private val passwordEncoder = BCryptPasswordEncoder(12)
    private val secureRandom = SecureRandom()

    /**
     * Registro con email y contraseña.
     *
     * - El email se normaliza (trim + minúsculas) y se valida su formato; antes se aceptaba
     *   cualquier texto y `confirmPassword` se ignoraba (M-10).
     * - Anti-enumeración: si el email ya existe se responde igual que en un alta nueva.
     * - Cuenta existente SIN verificar y con contraseña (ronda 6, pre-hijacking): antes se reenviaba
     *   el enlace de VERIFICACIÓN, y al pulsarlo la cuenta quedaba verificada con la contraseña de
     *   quien la registró primero, que pudo ser un atacante. Ahora se anula el enlace de
     *   verificación pendiente y se envía uno para FIJAR la contraseña (mismo flujo que
     *   restablecer): solo quien controla el buzón elige la contraseña, y eso verifica la cuenta.
     *   La contraseña escrita en este registro no se guarda (permitiría a un tercero fijar la de
     *   una cuenta ajena pendiente).
     */
    @Transactional
    fun register(email: String, password: String, confirmPassword: String) {
        val normalized = normalizeEmail(email)
        if (normalized.length > 254 || !EMAIL_REGEX.matches(normalized)) {
            throw InvalidRegistrationException("invalidEmail", "Email no válido")
        }
        if (password != confirmPassword) {
            throw InvalidRegistrationException("passwordMismatch", "Las contraseñas no coinciden")
        }
        validatePasswordStrength(password)

        val existing = findByEmail(normalized)
        if (existing != null) {
            if (!existing.emailVerified && existing.passwordHash != null) {
                // UPDATE por columna: solo anula el enlace de verificación previo
                userRepository.clearVerificationToken(existing.id)
                val token = createPasswordResetToken(existing.id)
                emailService.sendPendingAccountEmail(existing.email, token)
                log.info("Registro repetido de una cuenta pendiente: enviado enlace para fijar la contraseña")
            } else {
                log.debug("Intento de registro con email existente (silenciado)")
            }
            return
        }

        val passwordHash = passwordEncoder.encode(password)
        val verificationToken = generateSecureToken()

        val user = User(
            email = normalized,
            passwordHash = passwordHash,
            emailVerified = false,
            role = UserRole.USER,
            emailVerificationToken = verificationToken,
            emailVerificationExpiresAt = OffsetDateTime.now().plusHours(24)
        )

        userRepository.save(user)
        emailService.sendVerificationEmail(normalized, verificationToken)
        log.info("Usuario registrado: {}", normalized)
    }

    @Transactional
    fun verifyEmail(token: String) {
        val user = userRepository.findByEmailVerificationToken(token)
            ?: throw VerificationTokenException("Token de verificación inválido o ya utilizado")

        val expiresAt = user.emailVerificationExpiresAt
        if (expiresAt == null || expiresAt.isBefore(OffsetDateTime.now())) {
            throw VerificationTokenException("Token de verificación expirado")
        }

        // UPDATE por columna que consume el token (ronda 5): antes `save(user.copy(...))` reescribía
        // la fila entera y podía pisar el contador de intentos o una contraseña cambiada a la vez.
        if (userRepository.markEmailVerified(user.id, token) == 0) {
            throw VerificationTokenException("Token de verificación inválido o ya utilizado")
        }
        log.info("Email verificado para usuario: {}", user.email)
    }

    /**
     * Cuenta un login fallido y, desde el 4º, bloquea con espera exponencial (5 min · 2^(n-4),
     * tope 24 h).
     *
     * Ronda 5: el contador se incrementa con un UPDATE atómico (`failed_attempts + 1`) y se relee
     * en la misma transacción (la fila queda bloqueada hasta el commit, así que el valor leído es
     * el de este intento). Antes era leer → `save(copy)`: con N fallos concurrentes se perdían
     * incrementos (prueba: 20 en paralelo dejaban 3) y se reescribía la fila entera.
     *
     * `@Transactional` (REQUIRED): se une a la de `TokenService.login` (que tiene `noRollbackFor`)
     * y abre la suya cuando la llama el listener del login web, que no tiene transacción.
     */
    @Transactional
    fun handleFailedLogin(email: String) {
        val user = findByEmail(email) ?: return
        if (userRepository.incrementFailedAttempts(user.id) == 0) return
        val attempts = userRepository.findFailedAttempts(user.id) ?: return

        if (attempts >= LOCK_THRESHOLD) {
            val lockedUntil = OffsetDateTime.now().plusMinutes(lockMinutes(attempts))
            userRepository.extendLock(user.id, lockedUntil)
            log.warn("Cuenta bloqueada para {} — intentos: {}, bloqueada hasta: {}", email, attempts, lockedUntil)
        }
    }

    /** Reinicia contador y bloqueo con un UPDATE de esas dos columnas (ronda 5). */
    @Transactional
    fun resetFailedAttempts(email: String) {
        val user = findByEmail(email) ?: return
        userRepository.resetFailedAttempts(user.id)
    }

    /**
     * Login con Google (API y web, ronda 5). El bloqueo por intentos protege contra adivinar
     * CONTRASEÑAS: un login con Google verificado no lo sufre (si no, cualquiera bloqueaba una
     * cuenta ajena con 4 contraseñas malas y la dejaba también sin Google) y reinicia el contador.
     * Sí se exige el email verificado, igual en web y API (tras vincular con Google siempre lo está).
     */
    @Transactional
    fun loginWithGoogle(email: String, googleId: String): User {
        val user = findOrCreateByGoogle(email, googleId)
        userRepository.resetFailedAttempts(user.id)
        if (!user.emailVerified) throw DisabledException("EMAIL_NOT_VERIFIED")
        return user
    }

    /**
     * Login con Google: busca por googleId, si no por email (vinculando la cuenta) y si no crea
     * una cuenta nueva. Google ya ha verificado el email (`email_verified`, ver GoogleOAuthService).
     *
     * Al vincular una cuenta que estaba SIN verificar:
     * - se marca como verificada (antes quedaba sin verificar y el login web con Google daba 500
     *   por DisabledException);
     * - se borra su contraseña, porque nadie ha demostrado ser dueño del email al ponerla: si no,
     *   quien registró antes ese email con una contraseña suya (pre-hijacking) entraría en la
     *   cuenta en cuanto el dueño real usara Google. El dueño puede fijar una con "olvidé mi contraseña".
     */
    @Transactional
    fun findOrCreateByGoogle(email: String, googleId: String): User {
        // Buscar por googleId
        userRepository.findByGoogleId(googleId)?.let { return it }

        // Buscar por email y vincular googleId
        findByEmail(email)?.let { return linkGoogleAccount(it, googleId) }

        // Crear nuevo usuario Google (ronda 6). Con dos primeros logins simultáneos ambos llegaban
        // aquí y el segundo INSERT chocaba con el índice único de email / google_id → 500. Además, en
        // PostgreSQL una sentencia fallida aborta la transacción y ya no se podía releer nada.
        // `ON CONFLICT DO NOTHING` espera a que el otro alta haga commit y no inserta nada, sin abortar
        // la transacción; después se lee la cuenta (la nuestra o la del otro). Sin REQUIRES_NEW: no
        // ocupa una segunda conexión del pool (en producción solo hay 3).
        val normalized = normalizeEmail(email)
        if (userRepository.insertGoogleUserIfAbsent(normalized, googleId) == 1) {
            log.info("Usuario creado via Google OAuth: {}", normalized)
        } else {
            log.info("Alta con Google simultánea o email ya registrado: se usa la cuenta existente")
        }
        return userRepository.findByGoogleId(googleId)
            // Choque por email con una cuenta creada a la vez por otra vía: se vincula como arriba
            ?: findByEmail(normalized)?.let { linkGoogleAccount(it, googleId) }
            ?: throw IllegalStateException("No se pudo crear ni encontrar la cuenta de Google")
    }

    /**
     * Vincula Google a una cuenta existente encontrada por email. UPDATE por columna (ronda 5): no
     * reescribe contador, bloqueo ni (si está verificada) contraseña.
     */
    private fun linkGoogleAccount(existing: User, googleId: String): User {
        // Ronda 6: no sobrescribir otra cuenta de Google ya vinculada (antes se reemplazaba el
        // googleId en silencio y la cuenta Google original perdía el acceso).
        if (existing.googleId != null && existing.googleId != googleId) {
            log.warn("Login con Google rechazado: la cuenta ya está vinculada a otra cuenta de Google")
            throw InvalidGoogleTokenException("La cuenta ya está vinculada a otra cuenta de Google")
        }
        if (existing.googleId == googleId) return existing
        if (existing.emailVerified) {
            userRepository.linkGoogle(existing.id, googleId)
            return existing.copy(googleId = googleId)
        }
        log.info("Cuenta sin verificar vinculada a Google: se verifica y se descarta su contraseña previa")
        userRepository.linkGoogleAndVerify(existing.id, googleId)
        return existing.copy(
            googleId = googleId,
            emailVerified = true,
            emailVerificationToken = null,
            emailVerificationExpiresAt = null,
            passwordHash = null
        )
    }

    @Transactional
    fun initiatePasswordReset(email: String) {
        // Anti-enumeración: siempre retornar sin revelar si el email existe
        val user = findByEmail(email) ?: run {
            log.debug("Solicitud de reset para email inexistente (silenciada): {}", email)
            return
        }

        val token = createPasswordResetToken(user.id)
        // A la dirección guardada de la cuenta, no a la escrita (puede variar en mayúsculas/espacios)
        emailService.sendPasswordResetEmail(user.email, token)
        log.info("Reset de contraseña iniciado para: {}", user.email)
    }

    /** Invalida los enlaces de restablecimiento anteriores y crea uno nuevo (1 h). */
    private fun createPasswordResetToken(userId: Long): String {
        passwordResetTokenRepository.deleteByUserId(userId)
        val token = generateSecureToken()
        passwordResetTokenRepository.save(
            PasswordResetToken(userId = userId, token = token, expiresAt = OffsetDateTime.now().plusHours(1))
        )
        return token
    }

    /**
     * true si el enlace de restablecimiento sigue sirviendo (existe, no usado, no caducado).
     * La página de /reset-password lo comprueba al abrirse (R3-04) para no pedir una contraseña
     * nueva que luego se rechazaría al enviar.
     */
    @Transactional(readOnly = true)
    fun isResetTokenValid(token: String): Boolean =
        token.isNotBlank() && runCatching { usableResetToken(token) }.isSuccess

    private fun usableResetToken(token: String): PasswordResetToken {
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw VerificationTokenException("Token de restablecimiento inválido")

        if (resetToken.used) {
            throw VerificationTokenException("Token de restablecimiento ya utilizado")
        }

        if (resetToken.expiresAt.isBefore(OffsetDateTime.now())) {
            throw VerificationTokenException("Token de restablecimiento expirado")
        }
        return resetToken
    }

    fun resetPassword(token: String, newPassword: String) {
        val email = inTransaction.execute { resetPasswordInTransaction(token, newPassword) }!!
        // ...y las de la WEB (ronda 6), ya tras el commit: antes una sesión abierta con la
        // contraseña antigua seguía viva.
        expireWebSessions(email)
    }

    /** Devuelve el email guardado de la cuenta para cerrar después sus sesiones web. */
    private fun resetPasswordInTransaction(token: String, newPassword: String): String {
        val resetToken = usableResetToken(token)

        validatePasswordStrength(newPassword)

        val user = userRepository.findById(resetToken.userId)
            .orElseThrow { VerificationTokenException("Usuario no encontrado") }

        // Consumir el enlace de forma atómica (ronda 5): dos envíos simultáneos del mismo enlace ya
        // no pueden fijar cada uno su contraseña.
        val resetTokenId = resetToken.id ?: throw VerificationTokenException("Token de restablecimiento inválido")
        if (passwordResetTokenRepository.markUsedIfUnused(resetTokenId) == 0) {
            throw VerificationTokenException("Token de restablecimiento ya utilizado")
        }

        val newHash = passwordEncoder.encode(newPassword)
        // El enlace llegó al buzón del usuario: eso demuestra que el email es suyo, así que se
        // verifica la cuenta y se levanta el bloqueo por intentos (antes una cuenta sin verificar
        // seguía sin poder entrar tras restablecer la contraseña). UPDATE por columna (ronda 5):
        // `save(user.copy(...))` reescribía también googleId, rol, etc. con valores leídos antes.
        userRepository.resetPasswordAndUnlock(user.id, newHash)
        // Cerrar las sesiones de la app: quien tuviera la contraseña antigua deja de tener acceso.
        val now = OffsetDateTime.now()
        refreshTokenRepository.findByEmail(user.email)
            .filter { !it.revoked }
            .forEach { refreshTokenRepository.save(it.copy(revoked = true, revokedAt = now)) }
        log.info("Contraseña restablecida para usuario ID: {}", resetToken.userId)
        return user.email
    }

    fun deleteAccount(userId: Long) {
        val email = inTransaction.execute { deleteAccountInTransaction(userId) }!!
        // Cerrar también las sesiones web abiertas en otros navegadores (ronda 6), tras el commit
        expireWebSessions(email)
    }

    private fun deleteAccountInTransaction(userId: Long): String {
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
        // Eliminar resultados y tickets del escaneo de Gmail (contienen remitente, servicio y precio)
        gmailScanResultRepository.deleteByUserId(userId)
        gmailScanTicketRepository.deleteByUserId(userId)
        // Eliminar usuario
        userRepository.deleteById(userId)
        log.info("Cuenta eliminada para usuario ID: {}", userId)
        return user.email
    }

    /**
     * Marca como caducadas todas las sesiones web del usuario (ronda 6). El registro es
     * `SpringSessionBackedSessionRegistry` (ver SessionConfig): busca en la tabla de Spring Session
     * por el nombre del principal, que es el email GUARDADO de la cuenta (UserDetailsServiceImpl usa
     * `user.email`), así que se pasa ese mismo valor. En la siguiente petición, el
     * ConcurrentSessionFilter cierra la sesión y redirige a /login?expired.
     */
    private fun expireWebSessions(email: String) {
        try {
            val sessions = sessionRegistry.getAllSessions(email, false)
            sessions.forEach { it.expireNow() }
            if (sessions.isNotEmpty()) log.info("Cerradas {} sesiones web del usuario", sessions.size)
        } catch (ex: Exception) {
            log.warn("No se pudieron cerrar las sesiones web del usuario: {}", ex.message)
        }
    }

    fun checkPassword(userId: Long, rawPassword: String): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        val hash = user.passwordHash ?: return false
        return passwordEncoder.matches(rawPassword, hash)
    }

    fun findById(userId: Long): User? = userRepository.findById(userId).orElse(null)

    /**
     * Busca por email tolerando espacios y mayúsculas, también en cuentas antiguas guardadas con
     * mayúsculas (ver [findByEmailAnyCase]). La usan registro, Google, olvido de contraseña y login.
     */
    fun findByEmail(email: String): User? = userRepository.findByEmailAnyCase(email)

    companion object {
        /** Intentos fallidos a partir de los cuales se bloquea la cuenta. */
        const val LOCK_THRESHOLD = 4

        /** Minutos de bloqueo para el intento n (n >= 4): 5 · 2^(n-4), con tope de 24 h. */
        fun lockMinutes(attempts: Int): Long =
            min(5.0 * 2.0.pow((attempts - LOCK_THRESHOLD).toDouble()), 1440.0).toLong()

        /** Formato de email razonable (no RFC completo): algo@dominio.tld sin espacios. */
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun normalizeEmail(email: String): String = email.trim().lowercase()
    }

    private fun validatePasswordStrength(password: String) {
        if (password.length > 72) {
            // BCrypt solo usa los primeros 72 bytes: lo que vaya detrás se ignoraría en silencio.
            throw WeakPasswordException("La contraseña no puede superar 72 caracteres")
        }
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
