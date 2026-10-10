package com.subia.integration

import com.subia.model.RefreshToken
import com.subia.model.User
import com.subia.repository.PasswordResetTokenRepository
import com.subia.repository.RefreshTokenRepository
import com.subia.repository.UserRepository
import com.subia.security.TokenService
import com.subia.security.UserDetailsServiceImpl
import com.subia.service.UserService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests de INTEGRACIÓN de autenticación contra PostgreSQL real (auditoría ronda 4).
 *
 * Usan el bean real de [TokenService] (con su proxy transaccional) y comprueban lo que queda
 * guardado en la BD: justo lo que los tests con mocks no podían ver (rollback de escrituras al
 * lanzar BadCredentialsException dentro de un método @Transactional).
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.subia.integration.TestDatabase#available")
class AuthIntegrationTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) = TestDatabase.register(registry)

        private const val PASSWORD = "Correcta#2026"
        // BCrypt(12) es lento: se calcula una vez para todos los tests
        private val PASSWORD_HASH: String = BCryptPasswordEncoder(12).encode(PASSWORD)
    }

    @Autowired lateinit var tokenService: TokenService
    @Autowired lateinit var userService: UserService
    @Autowired lateinit var userDetailsService: UserDetailsServiceImpl
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    @Autowired lateinit var mockMvc: MockMvc

    private fun uniqueEmail(prefix: String = "user") =
        "$prefix-${UUID.randomUUID().toString().take(8)}@example.com"

    private fun createUser(
        email: String = uniqueEmail(),
        verified: Boolean = true,
        lockedUntil: OffsetDateTime? = null,
        failedAttempts: Int = 0
    ): User = userRepository.save(
        User(
            email = email,
            passwordHash = PASSWORD_HASH,
            emailVerified = verified,
            lockedUntil = lockedUntil,
            failedAttempts = failedAttempts
        )
    )

    // ── 1. Reutilización de refresh token ────────────────────────────────

    @Test
    fun `reutilizar un refresh token revoca la familia EN BD`() {
        val user = createUser()
        val first = tokenService.issueTokenPairForEmail(user.email)
        val second = tokenService.refresh(first.refreshToken)

        val ex = assertThrows(BadCredentialsException::class.java) { tokenService.refresh(first.refreshToken) }
        assertEquals("TOKEN_REUSE_DETECTED", ex.message)

        val stored = refreshTokenRepository.findByToken(TokenService.hash(second.refreshToken))!!
        assertTrue(stored.revoked, "el token emitido en la rotación debe quedar revocado en BD")
        assertNotNull(stored.revokedAt)
        assertThrows(BadCredentialsException::class.java) { tokenService.refresh(second.refreshToken) }
    }

    // ── 2. Bloqueo por intentos fallidos en la API ───────────────────────

    @Test
    fun `4 logins fallidos por la API bloquean la cuenta EN BD`() {
        val user = createUser()
        repeat(4) {
            assertThrows(BadCredentialsException::class.java) { tokenService.login(user.email, "Mala#0000000") }
        }

        val stored = userRepository.findById(user.id).orElseThrow()
        assertEquals(4, stored.failedAttempts)
        assertNotNull(stored.lockedUntil)
        assertTrue(stored.lockedUntil!!.isAfter(OffsetDateTime.now()))

        // Bloqueada: ni con la contraseña correcta
        assertThrows(LockedException::class.java) { tokenService.login(user.email, PASSWORD) }
    }

    @Test
    fun `login por HTTP con contrasena mala cuenta el fallo en BD`() {
        val user = createUser()
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Forwarded-For", "10.4.0.1")
            content = """{"email":"${user.email}","password":"Mala#0000000"}"""
        }.andExpect { status { isUnauthorized() } }

        assertEquals(1, userRepository.findById(user.id).orElseThrow().failedAttempts)
    }

    // ── 3. Transición V14: refresh tokens antiguos guardados en claro ────

    @Test
    fun `refresh acepta un token antiguo guardado en claro y guarda el nuevo hasheado`() {
        val user = createUser()
        val legacyRaw = UUID.randomUUID().toString()
        val legacy = refreshTokenRepository.save(
            RefreshToken(token = legacyRaw, email = user.email, expiresAt = OffsetDateTime.now().plusDays(10))
        )

        val pair = tokenService.refresh(legacyRaw)

        assertTrue(refreshTokenRepository.findById(legacy.id!!).orElseThrow().revoked)
        val rotated = refreshTokenRepository.findByToken(TokenService.hash(pair.refreshToken))
        assertNotNull(rotated, "el token nuevo se guarda hasheado")
        assertEquals(legacy.familyId, rotated!!.familyId)
        assertNull(refreshTokenRepository.findByToken(pair.refreshToken), "nunca en claro")
    }

    @Test
    fun `logout revoca un token antiguo guardado en claro`() {
        val user = createUser()
        val legacyRaw = UUID.randomUUID().toString()
        val legacy = refreshTokenRepository.save(
            RefreshToken(token = legacyRaw, email = user.email, expiresAt = OffsetDateTime.now().plusDays(10))
        )
        tokenService.logout(legacyRaw)
        assertTrue(refreshTokenRepository.findById(legacy.id!!).orElseThrow().revoked)
    }

    @Test
    fun `el hash guardado en BD no sirve como refresh token`() {
        val user = createUser()
        val pair = tokenService.issueTokenPairForEmail(user.email)
        // Quien lea la BD solo ve el hash: no debe poder usarlo como si fuera el token
        assertThrows(BadCredentialsException::class.java) { tokenService.refresh(TokenService.hash(pair.refreshToken)) }
        assertFalse(refreshTokenRepository.findByToken(TokenService.hash(pair.refreshToken))!!.revoked)
    }

    // ── 4. Cuentas antiguas con mayúsculas en el email ───────────────────

    @Test
    fun `cuenta antigua con mayusculas se encuentra en login, registro, Google y olvido`() {
        val tag = UUID.randomUUID().toString().take(8)
        val legacyEmail = "Juan.$tag@Gmail.com"
        val typed = legacyEmail.lowercase()
        val user = createUser(email = legacyEmail)
        fun sameAccounts() = userRepository.findAll().count { it.email.equals(legacyEmail, ignoreCase = true) }

        // Login (API y carga de UserDetails de la web)
        val pair = tokenService.login(typed, PASSWORD)
        assertNotNull(pair.accessToken)
        assertEquals(legacyEmail, userDetailsService.loadUserByUsername(typed.uppercase()).username)

        // Registro con el email en minúsculas: no crea otra cuenta
        userService.register(typed, "Otra#Clave2026", "Otra#Clave2026")
        assertEquals(1, sameAccounts())

        // Olvidé mi contraseña: genera el token para la cuenta existente
        userService.initiatePasswordReset(typed)
        assertTrue(passwordResetTokenRepository.findAll().any { it.userId == user.id })

        // Google: vincula la cuenta existente en vez de crear otra
        val google = userService.findOrCreateByGoogle(typed, "google-$tag")
        assertEquals(user.id, google.id)
        assertEquals(1, sameAccounts())
    }

    // ── 5. El motivo (sin verificar) solo con la contraseña correcta ─────

    @Test
    fun `API - cuenta sin verificar con contrasena mala da error generico`() {
        val user = createUser(verified = false)
        assertThrows(BadCredentialsException::class.java) { tokenService.login(user.email, "Mala#0000000") }
        assertThrows(DisabledException::class.java) { tokenService.login(user.email, PASSWORD) }
    }

    @Test
    fun `API - cuenta bloqueada no entra ni con la contrasena correcta`() {
        val user = createUser(lockedUntil = OffsetDateTime.now().plusMinutes(10), failedAttempts = 4)
        assertThrows(LockedException::class.java) { tokenService.login(user.email, PASSWORD) }
        assertTrue(refreshTokenRepository.findByEmail(user.email).isEmpty())
    }

    @Test
    fun `web - cuenta sin verificar solo revela el motivo con la contrasena correcta`() {
        val user = createUser(verified = false)
        mockMvc.post("/login") {
            with(csrf())
            header("X-Forwarded-For", "10.5.0.1")
            param("username", user.email)
            param("password", "Mala#0000000")
        }.andExpect { redirectedUrl("/login?error") }
        // Ronda 6: con contraseña mala la cuenta pendiente cuenta el intento (antes de mirar si está verificada)
        assertEquals(1, userRepository.findById(user.id).orElseThrow().failedAttempts)

        mockMvc.post("/login") {
            with(csrf())
            header("X-Forwarded-For", "10.5.0.2")
            param("username", user.email)
            param("password", PASSWORD)
        }.andExpect { redirectedUrl("/login?error=unverified") }
    }

    @Test
    fun `web - cuenta bloqueada no entra ni con la contrasena correcta`() {
        val user = createUser(lockedUntil = OffsetDateTime.now().plusMinutes(10), failedAttempts = 4)
        mockMvc.post("/login") {
            with(csrf())
            header("X-Forwarded-For", "10.5.0.3")
            param("username", user.email)
            param("password", PASSWORD)
        }.andExpect { redirectedUrl("/login?error=locked") }
    }

    @Test
    fun `web - 4 logins fallidos bloquean la cuenta EN BD`() {
        val user = createUser()
        repeat(4) { i ->
            mockMvc.post("/login") {
                with(csrf())
                header("X-Forwarded-For", "10.6.0.$i")
                param("username", user.email)
                param("password", "Mala#0000000")
            }.andExpect { redirectedUrl("/login?error") }
        }
        val stored = userRepository.findById(user.id).orElseThrow()
        assertEquals(4, stored.failedAttempts)
        assertNotNull(stored.lockedUntil)
    }

    // ── 6. Concurrencia del contador de intentos (ronda 5) ───────────────

    /**
     * Antes `handleFailedLogin` leía la fila y guardaba `user.copy(failedAttempts = n + 1)`: con
     * varios fallos a la vez todos leían el mismo n y se perdían incrementos (con ráfagas el
     * bloqueo no llegaba). Ahora es un UPDATE atómico `failed_attempts = failed_attempts + 1`.
     */
    @Test
    fun `20 logins fallidos concurrentes cuentan los 20 intentos EN BD`() {
        val user = createUser()
        val n = 20
        val pool = Executors.newFixedThreadPool(n)
        val start = CountDownLatch(1)
        val errors = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
        repeat(n) {
            pool.submit {
                start.await()
                try {
                    userService.handleFailedLogin(user.email)
                } catch (t: Throwable) {
                    errors.add(t)
                }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))

        assertTrue(errors.isEmpty(), "errores: $errors")
        val stored = userRepository.findById(user.id).orElseThrow()
        assertEquals(n, stored.failedAttempts)
        assertNotNull(stored.lockedUntil)
        // 20 intentos → tope de 24 h (min(5·2^16, 1440) minutos)
        assertTrue(stored.lockedUntil!!.isAfter(OffsetDateTime.now().plusHours(23)))
    }

    @Test
    fun `resetPassword desbloquea, verifica y el enlace solo sirve una vez`() {
        val user = createUser(verified = false, lockedUntil = OffsetDateTime.now().plusHours(1), failedAttempts = 7)
        userService.initiatePasswordReset(user.email)
        val token = passwordResetTokenRepository.findAll().single { it.userId == user.id }.token

        userService.resetPassword(token, "Nueva#Clave2026")

        val stored = userRepository.findById(user.id).orElseThrow()
        assertEquals(0, stored.failedAttempts)
        assertNull(stored.lockedUntil)
        assertTrue(stored.emailVerified)
        assertNotNull(tokenService.login(user.email, "Nueva#Clave2026").accessToken)
        assertThrows(com.subia.exception.VerificationTokenException::class.java) {
            userService.resetPassword(token, "Otra#Clave20266")
        }
    }
}
