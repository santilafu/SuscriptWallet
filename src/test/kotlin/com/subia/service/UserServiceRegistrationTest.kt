package com.subia.service

import com.subia.exception.InvalidRegistrationException
import com.subia.exception.WeakPasswordException
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

class UserServiceRegistrationTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var emailService: EmailService
    private lateinit var userService: UserService

    // Contraseña fuerte válida para tests que no prueban validación
    private val strongPassword = "ValidP@ssw0rd!"

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
        userService = UserService(
            userRepository, passwordResetTokenRepository, subscriptionRepository, refreshTokenRepository,
            securityEventRepository, mockk(relaxed = true), mockk(relaxed = true), emailService,
            mockk(relaxed = true), mockk(relaxed = true)
        )
    }

    @Test
    fun `registro con email existente y verificado retorna sin excepcion (anti-enumeracion)`() {
        every { userRepository.findByEmail("existing@example.com") } returns
            User(id = 1, email = "existing@example.com", passwordHash = "h", emailVerified = true)

        // No debe lanzar excepción — retorna silenciosamente
        assertDoesNotThrow {
            userService.register("existing@example.com", strongPassword, strongPassword)
        }

        // No debe llamar a save ni enviar email
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { emailService.sendVerificationEmail(any(), any()) }
    }

    @Test
    fun `registro repetido de una cuenta sin verificar invalida la verificacion y envia un enlace para fijar la contrasena`() {
        // Ronda 6 (pre-hijacking): reenviar la VERIFICACIÓN dejaba la cuenta verificada con la
        // contraseña de quien la registró primero (quizá un atacante). Ahora se anula el enlace de
        // verificación y se envía uno para fijar la contraseña a quien controla el buzón.
        val pending = User(
            id = 2, email = "pending@example.com", passwordHash = "hash-original", emailVerified = false,
            emailVerificationToken = "viejo"
        )
        every { userRepository.findByEmail("pending@example.com") } returns pending
        every { userRepository.clearVerificationToken(2) } returns 1
        every { passwordResetTokenRepository.deleteByUserId(2) } returns Unit
        val saved = slot<com.subia.model.PasswordResetToken>()
        every { passwordResetTokenRepository.save(capture(saved)) } answers { saved.captured }

        userService.register("pending@example.com", strongPassword, strongPassword)

        verify(exactly = 1) { userRepository.clearVerificationToken(2) }
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { emailService.sendVerificationEmail(any(), any()) }
        assertEquals(2L, saved.captured.userId)
        verify(exactly = 1) { emailService.sendPendingAccountEmail("pending@example.com", saved.captured.token) }
    }

    @Test
    fun `registro normaliza el email a minusculas y sin espacios`() {
        every { userRepository.findByEmail(any()) } returns null
        every { userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc(any()) } returns null
        val saved = slot<User>()
        every { userRepository.save(capture(saved)) } answers { saved.captured }

        userService.register("  Nuevo.Usuario@Example.COM ", strongPassword, strongPassword)

        assertEquals("nuevo.usuario@example.com", saved.captured.email)
        assertEquals(UserRole.USER, saved.captured.role)
        assertFalse(saved.captured.emailVerified)
    }

    @Test
    fun `registro con email mal formado lanza invalidEmail`() {
        val ex = assertThrows(InvalidRegistrationException::class.java) {
            userService.register("no-es-un-email", strongPassword, strongPassword)
        }
        assertEquals("invalidEmail", ex.code)
    }

    @Test
    fun `registro con contrasenas distintas lanza passwordMismatch`() {
        val ex = assertThrows(InvalidRegistrationException::class.java) {
            userService.register("a@example.com", strongPassword, strongPassword + "x")
        }
        assertEquals("passwordMismatch", ex.code)
    }

    @Test
    fun `registro con contrasena debil lanza WeakPasswordException`() {
        // Contraseña sin mayúscula, sin símbolo y menos de 10 chars
        assertThrows(WeakPasswordException::class.java) {
            userService.register("new@example.com", "short", "short")
        }
    }

    @Test
    fun `registro con contrasena de mas de 72 caracteres lanza WeakPasswordException`() {
        // BCrypt ignora lo que pasa de 72 bytes
        val larga = "Aa1!" + "x".repeat(70)
        assertThrows(WeakPasswordException::class.java) {
            userService.register("new@example.com", larga, larga)
        }
    }

    @Test
    fun `registro con contrasena del top-1000 lanza WeakPasswordException`() {
        // "password123" no cumple: falta mayúscula y símbolo especial → WeakPasswordException
        assertThrows(WeakPasswordException::class.java) {
            userService.register("new@example.com", "password123", "password123")
        }
    }
}
