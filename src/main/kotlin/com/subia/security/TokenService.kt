package com.subia.security

import com.subia.model.RefreshToken
import com.subia.model.UserRole
import com.subia.repository.RefreshTokenRepository
import com.subia.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Par de tokens emitido al cliente. [role] es el rol de la cuenta ("USER" o "ADMIN"): la app móvil
 * lo usa para mostrar u ocultar la gestión de categorías (que son globales y solo crea el admin).
 */
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val role: String = UserRole.USER.name
)

@Service
class TokenService(
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userDetailsService: UserDetailsServiceImpl,
    private val userService: UserService,
    @Value("\${jwt.access-token-ttl-minutes:15}") private val accessTtlMinutes: Long,
    @Value("\${jwt.refresh-token-ttl-days:30}") private val refreshTtlDays: Long
) {

    /**
     * Login de la API.
     *
     * `noRollbackFor`: el contador de intentos fallidos (`handleFailedLogin`) se escribe en ESTA
     * transacción y justo después se lanza BadCredentialsException. Con el rollback por defecto de
     * las RuntimeException esa escritura se deshacía y el bloqueo por intentos nunca llegaba a
     * aplicarse en la API (auditoría ronda 4; lo demuestra AuthIntegrationTest).
     *
     * Orden de comprobaciones (ronda 4): cuenta bloqueada → contraseña → email verificado.
     * - Bloqueada: se rechaza SIN mirar la contraseña. Si la respuesta dependiera de la contraseña,
     *   durante el bloqueo se podría seguir probando claves y la de "cuenta bloqueada" delataría la
     *   correcta: el bloqueo dejaría de frenar la fuerza bruta.
     * - Sin verificar: el motivo (EMAIL_NOT_VERIFIED) solo se revela con la contraseña correcta;
     *   con una incorrecta, error genérico (y cuenta como intento fallido).
     */
    @Transactional(noRollbackFor = [AuthenticationException::class])
    fun login(email: String, rawPassword: String): TokenPair {
        val userDetails = try {
            userDetailsService.loadUserByUsername(email)
        } catch (ex: UsernameNotFoundException) {
            throw BadCredentialsException("Credenciales inválidas")
        }
        if (!userDetails.isAccountNonLocked) throw LockedException("Cuenta bloqueada")

        // Email canónico de la cuenta (el que llega puede traer espacios o mayúsculas)
        val canonicalEmail = userDetails.username
        if (!passwordEncoder.matches(rawPassword, userDetails.password)) {
            userService.handleFailedLogin(canonicalEmail)
            throw BadCredentialsException("Credenciales inválidas")
        }
        if (!userDetails.isEnabled) throw DisabledException("EMAIL_NOT_VERIFIED")

        userService.resetFailedAttempts(canonicalEmail)
        return issueTokenPair(canonicalEmail)
    }

    /**
     * `noRollbackFor`: al detectar la reutilización de un token se revoca toda la familia y después
     * se lanza TOKEN_REUSE_DETECTED. Sin esto el rollback deshacía la revocación y el token robado
     * (o el emitido en la rotación) seguía sirviendo (auditoría ronda 4).
     */
    @Transactional(noRollbackFor = [AuthenticationException::class])
    fun refresh(rawRefreshToken: String): TokenPair {
        val entity = findStoredToken(rawRefreshToken)
            ?: throw BadCredentialsException("Refresh token no encontrado")

        if (entity.revoked) {
            // Token reuse detected — revoke entire family
            val family = refreshTokenRepository.findAllByFamilyId(entity.familyId)
            family.forEach { token ->
                if (!token.revoked) {
                    refreshTokenRepository.save(token.copy(revoked = true, revokedAt = OffsetDateTime.now()))
                }
            }
            throw BadCredentialsException("TOKEN_REUSE_DETECTED")
        }

        if (entity.expiresAt.isBefore(OffsetDateTime.now())) {
            throw BadCredentialsException("Refresh token expirado")
        }

        // Revocar el token actual de forma atómica: si dos peticiones llegan a la vez con el mismo
        // token, solo una obtiene un par nuevo (antes las dos lo conseguían).
        val id = entity.id
        if (id != null && refreshTokenRepository.revokeIfActive(id, OffsetDateTime.now()) != 1) {
            throw BadCredentialsException("Refresh token ya utilizado")
        }

        // Issue new pair with same familyId (el nuevo siempre se guarda hasheado)
        return issueTokenPair(entity.email, entity.familyId)
    }

    @Transactional
    fun logout(rawRefreshToken: String) {
        val entity = findStoredToken(rawRefreshToken) ?: return
        if (!entity.revoked) {
            refreshTokenRepository.save(entity.copy(revoked = true, revokedAt = OffsetDateTime.now()))
        }
    }

    /**
     * Busca el token por su hash y, si no aparece, en claro.
     *
     * Transición V14; quitar en la próxima versión. En un despliegue sin corte (Render), la
     * instancia antigua sigue emitiendo tokens en claro después de que la nueva haya migrado la
     * BD; sin este respaldo, esas sesiones se cerraban al pasar a la instancia nueva. Al rotar, el
     * token nuevo ya se guarda hasheado.
     *
     * El respaldo NO se aplica a valores con forma de hash (64 hex): si no, quien leyera la BD
     * podría usar el hash guardado directamente como refresh token (anularía M-02). Los tokens en
     * claro antiguos son UUID (36 caracteres) y nunca tienen esa forma.
     */
    private fun findStoredToken(rawRefreshToken: String): RefreshToken? =
        refreshTokenRepository.findByToken(hash(rawRefreshToken))
            ?: rawRefreshToken.takeUnless { looksLikeHash(it) }?.let { refreshTokenRepository.findByToken(it) }

    @Transactional
    fun issueTokenPairForEmail(email: String): TokenPair = issueTokenPair(email)

    private fun issueTokenPair(email: String, familyId: UUID = UUID.randomUUID()): TokenPair {
        // El rol viaja en el JWT para que /api/** pueda exigir ROLE_ADMIN (categorías globales).
        val role = userService.findByEmail(email)?.role
        val accessToken = jwtService.generateAccessToken(email, role)
        val rawRefreshToken = UUID.randomUUID().toString()
        val now = OffsetDateTime.now()

        refreshTokenRepository.save(
            RefreshToken(
                // Solo se guarda el hash: el valor en claro viaja una vez al cliente (M-02)
                token = hash(rawRefreshToken),
                email = email,
                familyId = familyId,
                issuedAt = now,
                expiresAt = now.plusDays(refreshTtlDays)
            )
        )

        return TokenPair(accessToken, rawRefreshToken, accessTtlMinutes * 60, (role ?: UserRole.USER).name)
    }

    companion object {
        private val HASH_REGEX = Regex("^[0-9a-fA-F]{64}$")

        private fun looksLikeHash(value: String) = HASH_REGEX.matches(value)

        /**
         * SHA-256 en hexadecimal del refresh token. Es el valor que se guarda y se busca en BD.
         * Debe coincidir con la migración V14 (`encode(sha256(convert_to(token,'UTF8')),'hex')`).
         */
        fun hash(rawToken: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(rawToken.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
