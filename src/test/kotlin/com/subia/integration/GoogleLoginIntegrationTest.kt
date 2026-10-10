package com.subia.integration

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.subia.model.User
import com.subia.repository.UserRepository
import com.subia.service.UserService
import com.subia.security.GoogleOAuthService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Login con Google frente al bloqueo por intentos fallidos (auditoría ronda 5).
 *
 * El bloqueo existe contra adivinar CONTRASEÑAS. Antes cualquiera podía bloquear una cuenta ajena
 * con 4 contraseñas malas y la web rechazaba también el login con Google (la API no): incoherente
 * y una vía de denegación de servicio. Ahora, en web y API, un login con Google verificado entra
 * aunque haya bloqueo por intentos y reinicia el contador.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.subia.integration.TestDatabase#available")
class GoogleLoginIntegrationTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) = TestDatabase.register(registry)

        private val PASSWORD_HASH: String = BCryptPasswordEncoder(4).encode("Correcta#2026")
    }

    @MockitoBean lateinit var googleOAuthService: GoogleOAuthService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userService: UserService

    /** Cuenta con contraseña bloqueada por intentos, y el id token de Google que la representa. */
    private fun lockedAccountWithGoogleToken(): Pair<User, String> {
        val tag = UUID.randomUUID().toString().take(8)
        val user = userRepository.save(
            User(
                email = "locked-$tag@example.com",
                passwordHash = PASSWORD_HASH,
                emailVerified = true,
                failedAttempts = 6,
                lockedUntil = OffsetDateTime.now().plusHours(2)
            )
        )
        val idToken = "id-token-$tag"
        val payload = GoogleIdToken.Payload().setEmail(user.email).setSubject("google-$tag")
        Mockito.`when`(googleOAuthService.verifyAndGetPayload(idToken)).thenReturn(payload)
        return user to idToken
    }

    private fun assertUnlocked(user: User, googleId: String) {
        val stored = userRepository.findById(user.id).orElseThrow()
        assertEquals(0, stored.failedAttempts)
        assertNull(stored.lockedUntil)
        assertEquals(googleId, stored.googleId)
        assertEquals(PASSWORD_HASH, stored.passwordHash, "vincular Google no toca la contraseña de una cuenta verificada")
    }

    @Test
    fun `API - Google entra en una cuenta bloqueada por intentos y reinicia el contador`() {
        val (user, idToken) = lockedAccountWithGoogleToken()
        mockMvc.post("/api/auth/google") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Forwarded-For", "10.7.0.1")
            content = """{"idToken":"$idToken"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { exists() }
        }
        assertUnlocked(user, idToken.replace("id-token-", "google-"))
    }

    @Test
    fun `web - Google entra en una cuenta bloqueada por intentos y reinicia el contador`() {
        val (user, idToken) = lockedAccountWithGoogleToken()
        mockMvc.post("/auth/google/callback") {
            with(csrf())
            header("X-Forwarded-For", "10.7.0.2")
            param("credential", idToken)
        }.andExpect { redirectedUrl("/dashboard") }
        assertUnlocked(user, idToken.replace("id-token-", "google-"))
    }

    @Test
    fun `Google ya vinculado tambien desbloquea (busqueda por googleId)`() {
        val (user, idToken) = lockedAccountWithGoogleToken()
        val googleId = idToken.replace("id-token-", "google-")
        userRepository.save(user.copy(googleId = googleId))
        mockMvc.post("/api/auth/google") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Forwarded-For", "10.7.0.3")
            content = """{"idToken":"$idToken"}"""
        }.andExpect { status { isOk() } }
        assertUnlocked(user, googleId)
    }
    // ── Ronda 6 ──────────────────────────────────────────────────────────

    @Test
    fun `web - Google vincula una cuenta SIN verificar - la verifica y borra su contrasena EN BD`() {
        val tag = UUID.randomUUID().toString().take(8)
        val user = userRepository.save(
            User(email = "pending-$tag@example.com", passwordHash = PASSWORD_HASH, emailVerified = false,
                emailVerificationToken = "tok-$tag", emailVerificationExpiresAt = OffsetDateTime.now().plusHours(1))
        )
        val idToken = "id-token-$tag"
        Mockito.`when`(googleOAuthService.verifyAndGetPayload(idToken))
            .thenReturn(GoogleIdToken.Payload().setEmail(user.email).setSubject("google-$tag"))

        mockMvc.post("/auth/google/callback") {
            with(csrf())
            header("X-Forwarded-For", "10.7.0.4")
            param("credential", idToken)
        }.andExpect { redirectedUrl("/dashboard") }

        val stored = userRepository.findById(user.id).orElseThrow()
        assertNull(stored.passwordHash, "la contraseña puesta sin demostrar el buzón se descarta")
        assertTrue(stored.emailVerified)
        assertNull(stored.emailVerificationToken)
        assertEquals("google-$tag", stored.googleId)
    }

    @Test
    fun `API - Google con otro googleId no sobrescribe la vinculacion existente`() {
        val tag = UUID.randomUUID().toString().take(8)
        val user = userRepository.save(
            User(email = "linked-$tag@example.com", passwordHash = PASSWORD_HASH, emailVerified = true, googleId = "google-orig-$tag")
        )
        val idToken = "id-token-other-$tag"
        Mockito.`when`(googleOAuthService.verifyAndGetPayload(idToken))
            .thenReturn(GoogleIdToken.Payload().setEmail(user.email).setSubject("google-otro-$tag"))

        mockMvc.post("/api/auth/google") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Forwarded-For", "10.7.0.5")
            content = """{"idToken":"$idToken"}"""
        }.andExpect { status { isUnauthorized() } }

        assertEquals("google-orig-$tag", userRepository.findById(user.id).orElseThrow().googleId)
    }

    @Test
    fun `primer login con Google simultaneo del mismo usuario nuevo - una sola cuenta y sin errores`() {
        repeat(5) {
            val tag = UUID.randomUUID().toString().take(8)
            val email = "nuevo-$tag@example.com"
            val n = 6
            val pool = java.util.concurrent.Executors.newFixedThreadPool(n)
            val start = java.util.concurrent.CountDownLatch(1)
            val ids = java.util.concurrent.ConcurrentLinkedQueue<Long>()
            val errors = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
            repeat(n) {
                pool.submit {
                    start.await()
                    try { ids.add(userService.loginWithGoogle(email, "google-$tag").id) } catch (t: Throwable) { errors.add(t) }
                }
            }
            start.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS))

            assertTrue(errors.isEmpty(), "errores: $errors")
            assertEquals(1, ids.toSet().size, "todos los logins devuelven la misma cuenta")
            assertEquals(1, userRepository.findAll().count { it.email == email })
        }
    }
}
