package com.subia.security

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.subia.exception.InvalidGoogleTokenException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests de las comprobaciones que hacemos sobre el payload del ID token de Google
 * (la verificación criptográfica la hace la librería de Google y no se testea aquí).
 */
class GoogleOAuthServiceTest {

    private val service = GoogleOAuthService(clientId = "client-id-de-prueba")

    private fun payload(issuer: String = "https://accounts.google.com", emailVerified: Boolean? = true) =
        GoogleIdToken.Payload().apply {
            this.issuer = issuer
            email = "user@example.com"
            subject = "google-sub-123"
            this.emailVerified = emailVerified
        }

    @Test
    fun `acepta un payload con emisor de Google y email verificado`() {
        val result = service.validatePayload(payload())
        assertEquals("user@example.com", result.email)
    }

    @Test
    fun `acepta el emisor sin esquema https`() {
        service.validatePayload(payload(issuer = "accounts.google.com"))
    }

    @Test
    fun `rechaza un emisor que no sea Google`() {
        val ex = assertThrows(InvalidGoogleTokenException::class.java) {
            service.validatePayload(payload(issuer = "https://evil.example.com"))
        }
        assertTrue(ex.message!!.contains("Emisor"))
    }

    @Test
    fun `rechaza un email no verificado por Google`() {
        // Sin esta comprobación, una cuenta Google con email sin verificar podría vincularse
        // (solo por coincidencia de email) a la cuenta de otro usuario ya registrado.
        val ex = assertThrows(InvalidGoogleTokenException::class.java) {
            service.validatePayload(payload(emailVerified = false))
        }
        assertTrue(ex.message!!.contains("verificado"))
    }

    @Test
    fun `rechaza un payload sin el claim email_verified`() {
        assertThrows(InvalidGoogleTokenException::class.java) {
            service.validatePayload(payload(emailVerified = null))
        }
    }

    @Test
    fun `verifyAndGetPayload falla si no hay client id configurado`() {
        val sinClientId = GoogleOAuthService(clientId = "")
        assertThrows(InvalidGoogleTokenException::class.java) {
            sinClientId.verifyAndGetPayload("cualquier-token")
        }
    }
}
