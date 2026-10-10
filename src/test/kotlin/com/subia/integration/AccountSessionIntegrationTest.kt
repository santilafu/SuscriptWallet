package com.subia.integration

import com.subia.exception.VerificationTokenException
import com.subia.model.User
import com.subia.repository.PasswordResetTokenRepository
import com.subia.repository.UserRepository
import com.subia.service.UserService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Auditoría ronda 6: pre-hijacking por el reenvío del registro, cierre de sesiones WEB al
 * restablecer la contraseña o borrar la cuenta, y usos simultáneos del mismo enlace.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf("com.subia.integration.TestDatabase#available")
class AccountSessionIntegrationTest {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) = TestDatabase.register(registry)

        private const val PASSWORD = "Correcta#2026"
        private val PASSWORD_HASH: String = BCryptPasswordEncoder(4).encode(PASSWORD)
        private val ipCounter = AtomicInteger(1)
    }

    @Autowired lateinit var userService: UserService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    @Autowired lateinit var mockMvc: MockMvc

    /** IP distinta por petición para no tropezar con el rate limit por IP. */
    private fun ip() = "10.60.${ipCounter.get() / 250}.${ipCounter.getAndIncrement() % 250 + 1}"

    private fun uniqueEmail(prefix: String) = "$prefix-${UUID.randomUUID().toString().take(8)}@example.com"

    private fun createUser(email: String = uniqueEmail("user"), verified: Boolean = true): User =
        userRepository.save(User(email = email, passwordHash = PASSWORD_HASH, emailVerified = verified))

    /** Login web con formulario; devuelve la cookie de sesión (Spring Session JDBC: `SESSION`). */
    private fun webLogin(email: String, password: String): Cookie {
        val result = mockMvc.post("/login") {
            with(csrf())
            header("X-Forwarded-For", ip())
            param("username", email)
            param("password", password)
        }.andExpect { redirectedUrl("/dashboard") }.andReturn()
        val setCookie = result.response.getHeaders("Set-Cookie").first { it.startsWith("SESSION=") }
        return Cookie("SESSION", setCookie.substringAfter("SESSION=").substringBefore(";"))
    }

    private fun dashboardWith(cookie: Cookie) = mockMvc.get("/dashboard") {
        cookie(cookie)
        header("X-Forwarded-For", ip())
    }

    private fun webRegister(email: String, password: String) {
        mockMvc.post("/register") {
            with(csrf())
            header("X-Forwarded-For", ip())
            param("email", email)
            param("password", password)
            param("confirmPassword", password)
        }.andExpect { redirectedUrl("/verify-pending") }
    }

    // ── 1. Pre-hijacking por el reenvío del registro ─────────────────────

    @Test
    fun `pre-hijacking - el registro de la victima no verifica la cuenta con la contrasena del atacante`() {
        val email = uniqueEmail("victima")
        val attackerPassword = "Atacante#2026"
        val victimPassword = "Victima#20266"

        // El atacante registra el email de la víctima con SU contraseña (queda sin verificar)
        webRegister(email, attackerPassword)
        val pending = userRepository.findByEmail(email)!!
        val attackerLink = pending.emailVerificationToken!!

        // La víctima se registra después: misma respuesta que un alta nueva (anti-enumeración)
        webRegister(email, victimPassword)

        // Ningún enlace de verificación vale ya: el del atacante (que llegó al buzón de la
        // víctima) verificaría la cuenta con la contraseña del atacante
        val afterVictim = userRepository.findById(pending.id).orElseThrow()
        assertNull(afterVictim.emailVerificationToken, "el enlace de verificación previo debe quedar invalidado")
        assertFalse(afterVictim.emailVerified)
        assertThrows(VerificationTokenException::class.java) { userService.verifyEmail(attackerLink) }

        // En su lugar, la víctima recibe un enlace para FIJAR su contraseña
        val resetToken = passwordResetTokenRepository.findAll().single { it.userId == pending.id }.token
        mockMvc.post("/reset-password") {
            with(csrf())
            header("X-Forwarded-For", ip())
            param("token", resetToken)
            param("newPassword", victimPassword)
            param("confirmPassword", victimPassword)
        }.andExpect { redirectedUrl("/login?passwordReset=true") }

        assertTrue(userRepository.findById(pending.id).orElseThrow().emailVerified)
        // El atacante ya no entra; la víctima sí
        mockMvc.post("/login") {
            with(csrf())
            header("X-Forwarded-For", ip())
            param("username", email)
            param("password", attackerPassword)
        }.andExpect { redirectedUrl("/login?error") }
        webLogin(email, victimPassword)
    }

    // ── 2. Restablecer la contraseña / borrar la cuenta cierra las sesiones web ──

    @Test
    fun `resetPassword cierra las sesiones web abiertas`() {
        val user = createUser()
        val cookie = webLogin(user.email, PASSWORD)
        dashboardWith(cookie).andExpect { status { isOk() } }

        userService.initiatePasswordReset(user.email)
        val token = passwordResetTokenRepository.findAll().single { it.userId == user.id }.token
        userService.resetPassword(token, "Nueva#Clave2026")

        // La cookie anterior ya no da acceso
        dashboardWith(cookie).andExpect { redirectedUrl("/login?expired") }
    }

    @Test
    fun `deleteAccount cierra las sesiones web abiertas`() {
        val user = createUser()
        val cookie = webLogin(user.email, PASSWORD)
        dashboardWith(cookie).andExpect { status { isOk() } }

        userService.deleteAccount(user.id)

        dashboardWith(cookie).andExpect { redirectedUrl("/login?expired") }
    }

    // ── 6. Usos simultáneos del mismo enlace ─────────────────────────────

    private fun <T> concurrently(n: Int, action: (Int) -> T): Pair<List<T>, List<Throwable>> {
        val pool = Executors.newFixedThreadPool(n)
        val start = CountDownLatch(1)
        val ok = ConcurrentLinkedQueue<T>()
        val errors = ConcurrentLinkedQueue<Throwable>()
        repeat(n) { i ->
            pool.submit {
                start.await()
                try { ok.add(action(i)) } catch (t: Throwable) { errors.add(t) }
            }
        }
        start.countDown()
        pool.shutdown()
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))
        return ok.toList() to errors.toList()
    }

    @Test
    fun `dos envios simultaneos del mismo enlace de reset - solo uno fija la contrasena`() {
        val user = createUser()
        userService.initiatePasswordReset(user.email)
        val token = passwordResetTokenRepository.findAll().single { it.userId == user.id }.token
        val passwords = listOf("Primera#Clave26", "Segunda#Clave26")

        val (ok, errors) = concurrently(2) { i -> userService.resetPassword(token, passwords[i]); passwords[i] }

        assertEquals(1, ok.size, "solo un envío debe tener éxito (errores: $errors)")
        assertEquals(1, errors.size)
        assertTrue(errors.single() is VerificationTokenException, "el segundo falla con VerificationTokenException: ${errors.single()}")
        val hash = userRepository.findById(user.id).orElseThrow().passwordHash
        assertTrue(BCryptPasswordEncoder().matches(ok.single(), hash), "queda la contraseña del envío que ganó")
    }

    @Test
    fun `dos usos simultaneos del mismo enlace de verificacion - el segundo falla`() {
        val email = uniqueEmail("verif")
        userService.register(email, PASSWORD, PASSWORD)
        val token = userRepository.findByEmail(email)!!.emailVerificationToken!!

        val (ok, errors) = concurrently(2) { userService.verifyEmail(token) }

        assertEquals(1, ok.size, "errores: $errors")
        assertEquals(1, errors.size)
        assertTrue(errors.single() is VerificationTokenException)
        assertTrue(userRepository.findByEmail(email)!!.emailVerified)
    }
}
