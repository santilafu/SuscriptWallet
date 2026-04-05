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

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordResetTokenRepository = mockk()
        subscriptionRepository = mockk(relaxed = true)
        refreshTokenRepository = mockk(relaxed = true)
        securityEventRepository = mockk(relaxed = true)
        emailService = mockk(relaxed = true)
        userService = UserService(userRepository, passwordResetTokenRepository, subscriptionRepository, refreshTokenRepository, securityEventRepository, emailService)
    }

    @Test
    fun `lockout exponencial - intento 4 bloquea 5min`() {
        val user = baseUser(failedAttempts = 3)
        val savedSlot = slot<User>()
        every { userRepository.findByEmail("test@example.com") } returns user
        every { userRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val before = OffsetDateTime.now()
        userService.handleFailedLogin("test@example.com")
        val after = OffsetDateTime.now()

        val saved = savedSlot.captured
        assertEquals(4, saved.failedAttempts)
        assertNotNull(saved.lockedUntil)
        val lockedUntil = saved.lockedUntil!!
        assertTrue(lockedUntil.isAfter(before.plusMinutes(4).plusSeconds(55)))
        assertTrue(lockedUntil.isBefore(after.plusMinutes(5).plusSeconds(5)))
    }

    @Test
    fun `lockout exponencial - intento 5 bloquea 10min`() {
        // Fórmula: min(5 * 2^(n-4), 1440) → n=5: 5 * 2^1 = 10 min
        val user = baseUser(failedAttempts = 4)
        val savedSlot = slot<User>()
        every { userRepository.findByEmail("test@example.com") } returns user
        every { userRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val before = OffsetDateTime.now()
        userService.handleFailedLogin("test@example.com")
        val after = OffsetDateTime.now()

        val saved = savedSlot.captured
        assertEquals(5, saved.failedAttempts)
        val lockedUntil = saved.lockedUntil!!
        assertTrue(lockedUntil.isAfter(before.plusMinutes(9).plusSeconds(55)))
        assertTrue(lockedUntil.isBefore(after.plusMinutes(10).plusSeconds(5)))
    }

    @Test
    fun `lockout exponencial - intento 6 bloquea 20min`() {
        // Fórmula: min(5 * 2^(n-4), 1440) → n=6: 5 * 2^2 = 20 min
        val user = baseUser(failedAttempts = 5)
        val savedSlot = slot<User>()
        every { userRepository.findByEmail("test@example.com") } returns user
        every { userRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val before = OffsetDateTime.now()
        userService.handleFailedLogin("test@example.com")
        val after = OffsetDateTime.now()

        val saved = savedSlot.captured
        assertEquals(6, saved.failedAttempts)
        val lockedUntil = saved.lockedUntil!!
        assertTrue(lockedUntil.isAfter(before.plusMinutes(19).plusSeconds(55)))
        assertTrue(lockedUntil.isBefore(after.plusMinutes(20).plusSeconds(5)))
    }

    @Test
    fun `lockout exponencial - intento 7+ bloquea hasta 1440min`() {
        // Fórmula: min(5 * 2^(n-4), 1440) — para n=11: 5 * 2^7 = 640, para n=15: 5*2^11 = 10240 → tope 1440
        // Probamos con n=7 (intento 7): 5 * 2^3 = 40 min
        val user = baseUser(failedAttempts = 6)
        val savedSlot = slot<User>()
        every { userRepository.findByEmail("test@example.com") } returns user
        every { userRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val before = OffsetDateTime.now()
        userService.handleFailedLogin("test@example.com")
        val after = OffsetDateTime.now()

        val saved = savedSlot.captured
        assertEquals(7, saved.failedAttempts)
        val lockedUntil = saved.lockedUntil!!
        // n=7: 5 * 2^3 = 40 min
        assertTrue(lockedUntil.isAfter(before.plusMinutes(39).plusSeconds(55)))
        assertTrue(lockedUntil.isBefore(after.plusMinutes(40).plusSeconds(5)))
    }

    @Test
    fun `resetFailedAttempts limpia contador y lockedUntil`() {
        val user = baseUser(failedAttempts = 5).copy(
            lockedUntil = OffsetDateTime.now().plusMinutes(30)
        )
        val savedSlot = slot<User>()
        every { userRepository.findByEmail("test@example.com") } returns user
        every { userRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        userService.resetFailedAttempts("test@example.com")

        val saved = savedSlot.captured
        assertEquals(0, saved.failedAttempts)
        assertNull(saved.lockedUntil)
    }
}
