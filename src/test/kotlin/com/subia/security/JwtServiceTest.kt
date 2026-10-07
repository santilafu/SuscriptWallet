package com.subia.security

import com.subia.model.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Comprueba que el access token lleva el rol del usuario como claim: el chain de la API
 * lo necesita para aplicar hasRole("ADMIN") (las categorías son globales, ver A-02).
 */
class JwtServiceTest {

    private val service = JwtService(secret = "clave-de-prueba-con-al-menos-32-bytes!!", ttlMinutes = 15).apply { init() }

    @Test
    fun `el access token incluye el subject y el claim role`() {
        val token = service.generateAccessToken("admin@example.com", UserRole.ADMIN)

        val jwt = service.decoder.decode(token)
        assertEquals("admin@example.com", jwt.subject)
        assertEquals("ADMIN", jwt.getClaimAsString(JwtService.ROLE_CLAIM))
    }

    @Test
    fun `sin rol el token no lleva el claim role`() {
        val token = service.generateAccessToken("user@example.com", null)

        assertNull(service.decoder.decode(token).getClaimAsString(JwtService.ROLE_CLAIM))
    }
}
