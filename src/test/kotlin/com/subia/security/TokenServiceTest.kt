package com.subia.security

import com.subia.model.RefreshToken
import com.subia.repository.RefreshTokenRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.BadCredentialsException
import java.time.OffsetDateTime
import java.util.UUID

class TokenServiceTest {

    private lateinit var jwtService: JwtService
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var userDetailsService: UserDetailsServiceImpl
    private lateinit var userService: com.subia.service.UserService
    private lateinit var tokenService: TokenService

    private val familyId = UUID.randomUUID()

    private fun revokedToken() = RefreshToken(
        id = UUID.randomUUID(),
        token = "revoked-token",
        email = "user@example.com",
        familyId = familyId,
        issuedAt = OffsetDateTime.now().minusDays(1),
        expiresAt = OffsetDateTime.now().plusDays(29),
        revoked = true,
        revokedAt = OffsetDateTime.now().minusHours(1)
    )

    private fun activeToken() = RefreshToken(
        id = UUID.randomUUID(),
        token = "active-token",
        email = "user@example.com",
        familyId = familyId,
        issuedAt = OffsetDateTime.now().minusHours(1),
        expiresAt = OffsetDateTime.now().plusDays(29),
        revoked = false
    )

    @BeforeEach
    fun setUp() {
        jwtService = mockk()
        refreshTokenRepository = mockk()
        userDetailsService = mockk()
        userService = mockk()
        tokenService = TokenService(
            jwtService = jwtService,
            refreshTokenRepository = refreshTokenRepository,
            passwordEncoder = mockk(),
            userDetailsService = userDetailsService,
            userService = userService,
            accessTtlMinutes = 15L,
            refreshTtlDays = 30L
        )
    }

    @Test
    fun `refresh con token revocado revoca toda la familia y lanza excepcion`() {
        val revoked = revokedToken()
        val otherActive = activeToken().copy(token = "other-active-token", revoked = false)
        val savedSlot = mutableListOf<RefreshToken>()

        every { refreshTokenRepository.findByToken("revoked-token") } returns revoked
        every { refreshTokenRepository.findAllByFamilyId(familyId) } returns listOf(revoked, otherActive)
        every { refreshTokenRepository.save(capture(savedSlot)) } answers { savedSlot.last() }

        val ex = assertThrows(BadCredentialsException::class.java) {
            tokenService.refresh("revoked-token")
        }

        assertEquals("TOKEN_REUSE_DETECTED", ex.message)
        // El token activo de la familia debe haber sido revocado
        val revokedOtherActive = savedSlot.find { it.token == "other-active-token" }
        assertNotNull(revokedOtherActive)
        assertTrue(revokedOtherActive!!.revoked)
        assertNotNull(revokedOtherActive.revokedAt)
    }

    @Test
    fun `refresh exitoso emite nuevo par y revoca el anterior`() {
        val active = activeToken()
        val savedSlot = mutableListOf<RefreshToken>()

        every { refreshTokenRepository.findByToken("active-token") } returns active
        every { refreshTokenRepository.save(capture(savedSlot)) } answers { savedSlot.last() }
        every { jwtService.generateAccessToken("user@example.com") } returns "new-access-token"

        val result = tokenService.refresh("active-token")

        assertNotNull(result)
        assertEquals("new-access-token", result.accessToken)
        assertNotNull(result.refreshToken)
        assertNotEquals("active-token", result.refreshToken)

        // El token anterior debe haberse revocado
        val revokedPrev = savedSlot.find { it.token == "active-token" }
        assertNotNull(revokedPrev)
        assertTrue(revokedPrev!!.revoked)

        // Se debe haber guardado el nuevo token con el mismo familyId
        val newToken = savedSlot.find { it.token == result.refreshToken }
        assertNotNull(newToken)
        assertEquals(familyId, newToken!!.familyId)
    }
}
