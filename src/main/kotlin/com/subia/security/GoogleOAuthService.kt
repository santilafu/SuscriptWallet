package com.subia.security

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.subia.exception.InvalidGoogleTokenException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class GoogleOAuthService(
    @Value("\${app.google.client-id:}") private val clientId: String
) {
    fun verifyAndGetPayload(idToken: String): GoogleIdToken.Payload {
        if (clientId.isBlank()) {
            throw InvalidGoogleTokenException("Google client ID no configurado")
        }

        val verifier = GoogleIdTokenVerifier.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance()
        )
            .setAudience(listOf(clientId))
            .build()

        val googleIdToken = verifier.verify(idToken)
            ?: throw InvalidGoogleTokenException("Token de Google inválido o expirado")

        val issuer = googleIdToken.payload.issuer
        if (issuer != "accounts.google.com" && issuer != "https://accounts.google.com") {
            throw InvalidGoogleTokenException("Emisor del token de Google no válido: $issuer")
        }

        return googleIdToken.payload
    }
}
