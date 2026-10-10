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

    // En BD solo se guarda el hash SHA-256 del token (M-02)
    private fun revokedToken() = RefreshToken(
        id = UUID.randomUUID(),
        token = TokenService.hash("revoked-token"),
        email = "user@example.com",
        familyId = familyId,
        issuedAt = OffsetDateTime.now().minusDays(1),
        expiresAt = OffsetDateTime.now().plusDays(29),
        revoked = true,
        revokedAt = OffsetDateTime.now().minusHours(1)
    )

    private fun activeToken() = RefreshToken(
        id = UUID.randomUUID(),
        token = TokenService.hash("active-token"),
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
    fun `hash es SHA-256 hexadecimal estable`() {
        // Mismo valor que produce la migración V14 en PostgreSQL: encode(sha256('abc'), 'hex')
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            TokenService.hash("abc")
        )
    }

    @Test
    fun `refresh con token revocado revoca toda la familia y lanza excepcion`() {
        val revoked = revokedToken()
        val otherActive = activeToken().copy(token = TokenService.hash("other-active-token"), revoked = false)
        val savedSlot = mutableListOf<RefreshToken>()

        every { refreshTokenRepository.findByToken(TokenService.hash("revoked-token")) } returns revoked
        every { refreshTokenRepository.findAllByFamilyId(familyId) } returns listOf(revoked, otherActive)
        every { refreshTokenRepository.save(capture(savedSlot)) } answers { savedSlot.last() }

        val ex = assertThrows(BadCredentialsException::class.java) {
            tokenService.refresh("revoked-token")
        }

        assertEquals("TOKEN_REUSE_DETECTED", ex.message)
        // El token activo de la familia debe haber sido revocado
        val revokedOtherActive = savedSlot.find { it.token == TokenService.hash("other-active-token") }
        assertNotNull(revokedOtherActive)
        assertTrue(revokedOtherActive!!.revoked)
        assertNotNull(revokedOtherActive.revokedAt)
    }

    @Test
    fun `refresh exitoso emite nuevo par, revoca el anterior y guarda solo el hash`() {
        val active = activeToken()
        val savedSlot = mutableListOf<RefreshToken>()

        every { refreshTokenRepository.findByToken(TokenService.hash("active-token")) } returns active
        every { refreshTokenRepository.revokeIfActive(active.id!!, any()) } returns 1
        every { refreshTokenRepository.save(capture(savedSlot)) } answers { savedSlot.last() }
        every { userService.findByEmail("user@example.com") } returns
            com.subia.model.User(id = 1L, email = "user@example.com", passwordHash = "x", emailVerified = true, role = com.subia.model.UserRole.USER)
        every { jwtService.generateAccessToken("user@example.com", com.subia.model.UserRole.USER) } returns "new-access-token"

        val result = tokenService.refresh("active-token")

        assertEquals("new-access-token", result.accessToken)
        assertEquals("USER", result.role)
        assertNotEquals("active-token", result.refreshToken)

        // El token anterior se revoca con el UPDATE atómico
        verify(exactly = 1) { refreshTokenRepository.revokeIfActive(active.id!!, any()) }

        // El nuevo token se guarda hasheado (nunca en claro) y con el mismo familyId
        val newToken = savedSlot.single()
        assertEquals(TokenService.hash(result.refreshToken), newToken.token)
        assertNotEquals(result.refreshToken, newToken.token)
        assertEquals(familyId, newToken.familyId)
    }

    @Test
    fun `dos refresh concurrentes con el mismo token - el segundo es rechazado`() {
        val active = activeToken()
        every { refreshTokenRepository.findByToken(TokenService.hash("active-token")) } returns active
        // Otra petición ya revocó la fila: el UPDATE condicional no afecta a ninguna
        every { refreshTokenRepository.revokeIfActive(active.id!!, any()) } returns 0

        assertThrows(BadCredentialsException::class.java) { tokenService.refresh("active-token") }
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `refresh de un admin devuelve role ADMIN`() {
        val active = activeToken()
        every { refreshTokenRepository.findByToken(TokenService.hash("active-token")) } returns active
        every { refreshTokenRepository.revokeIfActive(active.id!!, any()) } returns 1
        every { refreshTokenRepository.save(any()) } answers { firstArg() }
        every { userService.findByEmail("user@example.com") } returns
            com.subia.model.User(id = 1L, email = "user@example.com", passwordHash = "x", emailVerified = true, role = com.subia.model.UserRole.ADMIN)
        every { jwtService.generateAccessToken("user@example.com", com.subia.model.UserRole.ADMIN) } returns "admin-access"

        val result = tokenService.refresh("active-token")

        assertEquals("ADMIN", result.role)
    }

    @Test
    fun `TokenResponse serializa el campo role junto a los de siempre`() {
        val json = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(
            com.subia.controller.TokenResponse.from(TokenPair("a", "r", 900L, "ADMIN"))
        )
        val tree = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(json)
        assertEquals("a", tree["accessToken"].asText())
        assertEquals("r", tree["refreshToken"].asText())
        assertEquals(900L, tree["expiresInSeconds"].asLong())
        assertEquals("Bearer", tree["tokenType"].asText())
        assertEquals("ADMIN", tree["role"].asText())
    }
}
