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

        return validatePayload(googleIdToken.payload)
    }

    /**
     * Comprobaciones sobre el payload ya verificado criptográficamente.
     * Se separa de [verifyAndGetPayload] para poder testearlo sin red.
     *
     * Exigimos `email_verified == true`: [com.subia.service.UserService.findOrCreateByGoogle]
     * vincula el googleId a una cuenta existente solo por coincidencia de email, así que un
     * email sin verificar permitiría entrar en la cuenta de otro usuario.
     */
    internal fun validatePayload(payload: GoogleIdToken.Payload): GoogleIdToken.Payload {
        val issuer = payload.issuer
        if (issuer != "accounts.google.com" && issuer != "https://accounts.google.com") {
            throw InvalidGoogleTokenException("Emisor del token de Google no válido: $issuer")
        }
        if (payload.emailVerified != true) {
            throw InvalidGoogleTokenException("El email de la cuenta de Google no está verificado")
        }
        return payload
    }
}
