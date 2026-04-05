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
    fun `loadUserByUsername con cuenta bloqueada lanza LockedException`() {
        val lockedUser = verifiedUser().copy(
            lockedUntil = OffsetDateTime.now().plusMinutes(10)
        )
        every { userRepository.findByEmail("user@example.com") } returns lockedUser

        assertThrows(LockedException::class.java) {
            userDetailsService.loadUserByUsername("user@example.com")
        }
    }

    @Test
    fun `loadUserByUsername con email no verificado lanza DisabledException`() {
        val unverifiedUser = verifiedUser().copy(emailVerified = false)
        every { userRepository.findByEmail("user@example.com") } returns unverifiedUser

        val ex = assertThrows(DisabledException::class.java) {
            userDetailsService.loadUserByUsername("user@example.com")
        }
        assertEquals("EMAIL_NOT_VERIFIED", ex.message)
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
}
