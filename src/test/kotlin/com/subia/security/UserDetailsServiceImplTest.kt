package com.subia.security

import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import java.time.OffsetDateTime

class UserDetailsServiceImplTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userDetailsService: UserDetailsServiceImpl

    private fun verifiedUser() = User(
        id = 1L,
        email = "user@example.com",
        passwordHash = "\$2a\$12\$hashedpassword",
        emailVerified = true,
        role = UserRole.USER
    )

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userDetailsService = UserDetailsServiceImpl(userRepository)
    }

    @Test
    fun `loadUserByUsername con cuenta bloqueada lanza LockedException en loadActiveUser`() {
        val lockedUser = verifiedUser().copy(
            lockedUntil = OffsetDateTime.now().plusMinutes(10)
        )
        every { userRepository.findByEmail("user@example.com") } returns lockedUser

        assertThrows(LockedException::class.java) {
            userDetailsService.loadActiveUser("user@example.com")
        }
        // Para el formLogin se devuelve marcada como bloqueada (DaoAuthenticationProvider lanza la excepción)
        assertFalse(userDetailsService.loadUserByUsername("user@example.com").isAccountNonLocked)
    }

    @Test
    fun `loadUserByUsername con email no verificado lanza DisabledException en loadActiveUser`() {
        val unverifiedUser = verifiedUser().copy(emailVerified = false)
        every { userRepository.findByEmail("user@example.com") } returns unverifiedUser

        val ex = assertThrows(DisabledException::class.java) {
            userDetailsService.loadActiveUser("user@example.com")
        }
        assertEquals("EMAIL_NOT_VERIFIED", ex.message)
        assertFalse(userDetailsService.loadUserByUsername("user@example.com").isEnabled)
    }

    @Test
    fun `loadUserForGoogle ignora el bloqueo por intentos pero exige email verificado (ronda 5)`() {
        every { userRepository.findByEmail("user@example.com") } returns
            verifiedUser().copy(lockedUntil = OffsetDateTime.now().plusMinutes(10), failedAttempts = 5)
        assertTrue(userDetailsService.loadUserForGoogle("user@example.com").isAccountNonLocked)

        every { userRepository.findByEmail("user@example.com") } returns verifiedUser().copy(emailVerified = false)
        assertThrows(DisabledException::class.java) { userDetailsService.loadUserForGoogle("user@example.com") }
    }

    @Test
    fun `loadUserByUsername con cuenta valida retorna UserDetails`() {
        val user = verifiedUser()
        every { userRepository.findByEmail("user@example.com") } returns user

        val userDetails = userDetailsService.loadUserByUsername("user@example.com")

        assertNotNull(userDetails)
        assertEquals("user@example.com", userDetails.username)
        assertEquals(user.passwordHash, userDetails.password)
        assertTrue(userDetails.authorities.any { it.authority == "ROLE_USER" })
    }

    @Test
    fun `formLogin con cuenta sin verificar da DisabledException y no InternalAuthenticationServiceException`() {
        every { userRepository.findByEmail("user@example.com") } returns verifiedUser().copy(emailVerified = false)
        val provider = org.springframework.security.authentication.dao.DaoAuthenticationProvider(userDetailsService)
        provider.setPasswordEncoder(object : org.springframework.security.crypto.password.PasswordEncoder {
            override fun encode(raw: CharSequence) = raw.toString()
            override fun matches(raw: CharSequence, encoded: String) = raw.toString() == encoded
        })

        assertThrows(DisabledException::class.java) {
            provider.authenticate(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@example.com", "x")
            )
        }
    }

    @Test
    fun `loadUserByUsername encuentra una cuenta antigua con mayusculas (ronda 4)`() {
        every { userRepository.findByEmail("juan@gmail.com") } returns null
        every { userRepository.findFirstByEmailIgnoreCaseOrderByIdAsc("juan@gmail.com") } returns
            verifiedUser().copy(email = "Juan@Gmail.com")

        // El username devuelto es el email canónico guardado (el que se usa en JWT y sesiones)
        assertEquals("Juan@Gmail.com", userDetailsService.loadUserByUsername(" juan@gmail.com ").username)
    }
}
