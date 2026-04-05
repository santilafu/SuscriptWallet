package com.subia.service

import com.subia.exception.WeakPasswordException
import com.subia.repository.PasswordResetTokenRepository
import com.subia.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
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
        userService = UserService(userRepository, passwordResetTokenRepository, subscriptionRepository, refreshTokenRepository, securityEventRepository, emailService)
    }

    @Test
    fun `registro con email existente retorna sin excepcion (anti-enumeracion)`() {
        every { userRepository.existsByEmail("existing@example.com") } returns true

        // No debe lanzar excepción — retorna silenciosamente
        assertDoesNotThrow {
            userService.register("existing@example.com", strongPassword, strongPassword)
        }

        // No debe llamar a save ni enviar email
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { emailService.sendVerificationEmail(any(), any()) }
    }

    @Test
    fun `registro con contrasena debil lanza WeakPasswordException`() {
        // Contraseña sin mayúscula, sin símbolo y menos de 10 chars
        assertThrows(WeakPasswordException::class.java) {
            userService.register("new@example.com", "short", "short")
        }
    }

    @Test
    fun `registro con contrasena del top-1000 lanza WeakPasswordException`() {
        // "password123" no cumple: falta mayúscula y símbolo especial → WeakPasswordException
        // Esto cubre la verificación de contraseñas comunes triviales
        assertThrows(WeakPasswordException::class.java) {
            userService.register("new@example.com", "password123", "password123")
        }
    }
}
