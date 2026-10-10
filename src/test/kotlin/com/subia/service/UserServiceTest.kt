package com.subia.service

import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.PasswordResetTokenRepository
import com.subia.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var emailService: EmailService
    private lateinit var userService: UserService

    private fun baseUser(failedAttempts: Int = 0) = User(
        id = 1L,
        email = "test@example.com",
        passwordHash = "\$2a\$12\$placeholder",
        emailVerified = true,
        role = UserRole.USER,
        failedAttempts = failedAttempts
    )

    private lateinit var subscriptionRepository: com.subia.repository.SubscriptionRepository
    private lateinit var refreshTokenRepository: com.subia.repository.RefreshTokenRepository
    private lateinit var securityEventRepository: com.subia.repository.SecurityEventRepository
    private lateinit var gmailScanResultRepository: com.subia.repository.GmailScanResultRepository
    private lateinit var gmailScanTicketRepository: com.subia.repository.GmailScanTicketRepository

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordResetTokenRepository = mockk()
        subscriptionRepository = mockk(relaxed = true)
        refreshTokenRepository = mockk(relaxed = true)
        securityEventRepository = mockk(relaxed = true)
        gmailScanResultRepository = mockk(relaxed = true)
        gmailScanTicketRepository = mockk(relaxed = true)
        emailService = mockk(relaxed = true)
        userService = UserService(
            userRepository, passwordResetTokenRepository, subscriptionRepository, refreshTokenRepository,
            securityEventRepository, gmailScanResultRepository, gmailScanTicketRepository, emailService,
            mockk(relaxed = true), mockk(relaxed = true)
        )
    }

    @Test
    fun `deleteAccount purga tambien los tickets y resultados de Gmail del usuario`() {
        every { userRepository.findById(1L) } returns java.util.Optional.of(baseUser())
        every { userRepository.deleteById(1L) } returns Unit
        every { passwordResetTokenRepository.deleteByUserId(1L) } returns Unit

        userService.deleteAccount(1L)

        verify(exactly = 1) { gmailScanResultRepository.deleteByUserId(1L) }
        verify(exactly = 1) { gmailScanTicketRepository.deleteByUserId(1L) }
        verify(exactly = 1) { userRepository.deleteById(1L) }
    }

    /**
     * Simula `handleFailedLogin` cuando el UPDATE atómico deja el contador en [attemptsAfter] y
     * devuelve el `lockedUntil` que se pidió fijar (null si no se bloqueó).
     */
    private fun failedLoginLocking(attemptsAfter: Int): java.time.OffsetDateTime? {
        every { userRepository.findByEmail("test@example.com") } returns baseUser(failedAttempts = attemptsAfter - 1)
        every { userRepository.incrementFailedAttempts(1L) } returns 1
        every { userRepository.findFailedAttempts(1L) } returns attemptsAfter
        val until = slot<OffsetDateTime>()
        every { userRepository.extendLock(1L, capture(until)) } returns 1

        userService.handleFailedLogin("test@example.com")

        verify(exactly = 1) { userRepository.incrementFailedAttempts(1L) }
        // Nunca se reescribe la fila entera (ronda 5)
        verify(exactly = 0) { userRepository.save(any()) }
        return if (until.isCaptured) until.captured else null
    }

    private fun assertLockedAbout(minutes: Long, lockedUntil: OffsetDateTime?, before: OffsetDateTime, after: OffsetDateTime) {
        assertNotNull(lockedUntil)
        assertTrue(lockedUntil!!.isAfter(before.plusMinutes(minutes).minusSeconds(5)))
        assertTrue(lockedUntil.isBefore(after.plusMinutes(minutes).plusSeconds(5)))
    }

    @Test
    fun `intento 3 suma el fallo sin bloquear`() {
        assertNull(failedLoginLocking(3))
        verify(exactly = 0) { userRepository.extendLock(any(), any()) }
    }

    @Test
    fun `lockout exponencial - intento 4 bloquea 5min`() {
        val before = OffsetDateTime.now()
        val until = failedLoginLocking(4)
        assertLockedAbout(5, until, before, OffsetDateTime.now())
    }

    @Test
    fun `lockout exponencial - intento 5 bloquea 10min`() {
        // Fórmula: min(5 * 2^(n-4), 1440) → n=5: 5 * 2^1 = 10 min
        val before = OffsetDateTime.now()
        val until = failedLoginLocking(5)
        assertLockedAbout(10, until, before, OffsetDateTime.now())
    }

    @Test
    fun `lockout exponencial - intento 6 bloquea 20min`() {
        val before = OffsetDateTime.now()
        val until = failedLoginLocking(6)
        assertLockedAbout(20, until, before, OffsetDateTime.now())
    }

    @Test
    fun `lockout exponencial - intento 7 bloquea 40min y el tope es 1440min`() {
        val before = OffsetDateTime.now()
        val until = failedLoginLocking(7)
        assertLockedAbout(40, until, before, OffsetDateTime.now())
        assertEquals(1440L, UserService.lockMinutes(15))
        assertEquals(1440L, UserService.lockMinutes(200))
    }

    @Test
    fun `handleFailedLogin con email desconocido no escribe nada`() {
        every { userRepository.findByEmail("nadie@example.com") } returns null
        every { userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc("nadie@example.com") } returns null

        userService.handleFailedLogin("nadie@example.com")

        verify(exactly = 0) { userRepository.incrementFailedAttempts(any()) }
    }

    @Test
    fun `resetFailedAttempts limpia con un UPDATE de columnas, sin save`() {
        every { userRepository.findByEmail("test@example.com") } returns baseUser(failedAttempts = 5)
        every { userRepository.resetFailedAttempts(1L) } returns 1

        userService.resetFailedAttempts("test@example.com")

        verify(exactly = 1) { userRepository.resetFailedAttempts(1L) }
        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `findByEmail tolera mayusculas y espacios`() {
        every { userRepository.findByEmail("Test@Example.com") } returns null
        every { userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc("Test@Example.com") } returns baseUser()

        assertEquals(1L, userService.findByEmail(" Test@Example.com ")?.id)
    }

    @Test
    fun `findByEmail encuentra una cuenta antigua guardada con mayusculas (ronda 4)`() {
        // Cuenta antigua `Juan@Gmail.com`; el usuario escribe (o el registro normaliza a) minúsculas
        val legacy = baseUser().copy(id = 7L, email = "Juan@Gmail.com")
        every { userRepository.findByEmail("juan@gmail.com") } returns null
        every { userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc("juan@gmail.com") } returns legacy

        assertEquals(7L, userService.findByEmail("juan@gmail.com")?.id)
    }

    @Test
    fun `findByEmail prefiere la coincidencia exacta`() {
        every { userRepository.findByEmail("test@example.com") } returns baseUser()

        assertEquals(1L, userService.findByEmail("test@example.com")?.id)
        verify(exactly = 0) { userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(any()) }
    }

    @Test
    fun `Google vincula una cuenta sin verificar - la verifica y descarta la contrasena previa`() {
        // Pre-hijacking: alguien registró este email con su contraseña y nunca lo verificó
        val pending = baseUser().copy(emailVerified = false, emailVerificationToken = "tok")
        every { userRepository.findByGoogleId("g-1") } returns null
        every { userRepository.findByEmail("test@example.com") } returns pending
        every { userRepository.linkGoogleAndVerify(1L, "g-1") } returns 1

        val result = userService.findOrCreateByGoogle("test@example.com", "g-1")

        verify(exactly = 1) { userRepository.linkGoogleAndVerify(1L, "g-1") }
        verify(exactly = 0) { userRepository.save(any()) }
        assertEquals("g-1", result.googleId)
        assertTrue(result.emailVerified)
        assertNull(result.passwordHash)
        assertNull(result.emailVerificationToken)
    }

    @Test
    fun `Google vincula una cuenta verificada sin tocar su contrasena`() {
        every { userRepository.findByGoogleId("g-1") } returns null
        every { userRepository.findByEmail("test@example.com") } returns baseUser()
        every { userRepository.linkGoogle(1L, "g-1") } returns 1

        val result = userService.findOrCreateByGoogle("test@example.com", "g-1")

        // Solo se escribe google_id: la contraseña (y el contador) no se tocan
        verify(exactly = 1) { userRepository.linkGoogle(1L, "g-1") }
        verify(exactly = 0) { userRepository.linkGoogleAndVerify(any(), any()) }
        verify(exactly = 0) { userRepository.save(any()) }
        assertEquals("g-1", result.googleId)
        assertEquals(baseUser().passwordHash, result.passwordHash)
    }

    @Test
    fun `Google con otro googleId no sobrescribe la vinculacion existente (ronda 6)`() {
        every { userRepository.findByGoogleId("g-nuevo") } returns null
        every { userRepository.findByEmail("test@example.com") } returns baseUser().copy(googleId = "g-original")

        assertThrows(com.subia.exception.InvalidGoogleTokenException::class.java) {
            userService.findOrCreateByGoogle("test@example.com", "g-nuevo")
        }
        verify(exactly = 0) { userRepository.linkGoogle(any(), any()) }
        verify(exactly = 0) { userRepository.linkGoogleAndVerify(any(), any()) }
    }

    @Test
    fun `loginWithGoogle reinicia el contador de intentos aunque la cuenta este bloqueada`() {
        val locked = baseUser(failedAttempts = 6).copy(googleId = "g-1", lockedUntil = OffsetDateTime.now().plusHours(1))
        every { userRepository.findByGoogleId("g-1") } returns locked
        every { userRepository.resetFailedAttempts(1L) } returns 1

        val result = userService.loginWithGoogle("test@example.com", "g-1")

        assertEquals(1L, result.id)
        verify(exactly = 1) { userRepository.resetFailedAttempts(1L) }
    }

    @Test
    fun `resetPassword verifica el email, desbloquea y revoca los refresh tokens`() {
        val tokenId = java.util.UUID.randomUUID()
        val token = com.subia.model.PasswordResetToken(
            id = tokenId, userId = 1L, token = "reset-tok", expiresAt = OffsetDateTime.now().plusMinutes(30)
        )
        val locked = baseUser(failedAttempts = 6).copy(
            emailVerified = false, lockedUntil = OffsetDateTime.now().plusHours(1)
        )
        val activeRefresh = com.subia.model.RefreshToken(
            id = java.util.UUID.randomUUID(), token = "h", email = "test@example.com",
            expiresAt = OffsetDateTime.now().plusDays(10)
        )
        every { passwordResetTokenRepository.findByToken("reset-tok") } returns token
        every { passwordResetTokenRepository.markUsedIfUnused(tokenId) } returns 1
        every { userRepository.findById(1L) } returns java.util.Optional.of(locked)
        val newHash = slot<String>()
        every { userRepository.resetPasswordAndUnlock(1L, capture(newHash)) } returns 1
        every { refreshTokenRepository.findByEmail("test@example.com") } returns listOf(activeRefresh)
        val savedRefresh = slot<com.subia.model.RefreshToken>()
        every { refreshTokenRepository.save(capture(savedRefresh)) } answers { savedRefresh.captured }

        userService.resetPassword("reset-tok", "NuevaP@ssw0rd!")

        // Un único UPDATE por columnas (contraseña + verificado + desbloqueo), sin reescribir la fila
        assertTrue(newHash.captured.startsWith("\$2"))
        verify(exactly = 0) { userRepository.save(any()) }
        assertTrue(savedRefresh.captured.revoked)
    }

    @Test
    fun `resetPassword rechaza un enlace que otro envio concurrente ya consumio`() {
        val tokenId = java.util.UUID.randomUUID()
        val token = com.subia.model.PasswordResetToken(
            id = tokenId, userId = 1L, token = "reset-tok", expiresAt = OffsetDateTime.now().plusMinutes(30)
        )
        every { passwordResetTokenRepository.findByToken("reset-tok") } returns token
        every { userRepository.findById(1L) } returns java.util.Optional.of(baseUser())
        every { passwordResetTokenRepository.markUsedIfUnused(tokenId) } returns 0

        assertThrows(com.subia.exception.VerificationTokenException::class.java) {
            userService.resetPassword("reset-tok", "NuevaP@ssw0rd!")
        }
        verify(exactly = 0) { userRepository.resetPasswordAndUnlock(any(), any()) }
    }

    @Test
    fun `isResetTokenValid rechaza enlaces desconocidos, usados o caducados (R3-04)`() {
        fun tok(t: String, used: Boolean = false, mins: Long = 30) = com.subia.model.PasswordResetToken(
            userId = 1L, token = t, used = used, expiresAt = OffsetDateTime.now().plusMinutes(mins)
        )
        every { passwordResetTokenRepository.findByToken("ok") } returns tok("ok")
        every { passwordResetTokenRepository.findByToken("usado") } returns tok("usado", used = true)
        every { passwordResetTokenRepository.findByToken("caducado") } returns tok("caducado", mins = -5)
        every { passwordResetTokenRepository.findByToken("nada") } returns null

        assertTrue(userService.isResetTokenValid("ok"))
        assertFalse(userService.isResetTokenValid("usado"))
        assertFalse(userService.isResetTokenValid("caducado"))
        assertFalse(userService.isResetTokenValid("nada"))
        assertFalse(userService.isResetTokenValid(""))
    }
}
