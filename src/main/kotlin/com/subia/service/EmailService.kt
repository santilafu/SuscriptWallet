package com.subia.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class EmailService(
    @Value("\${resend.api-key:}") private val apiKey: String,
    @Value("\${resend.from-email:noreply@suscriptwallet.com}") private val fromEmail: String,
    @Value("\${app.base-url:http://localhost:8081}") private val baseUrl: String
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    private val restClient: RestClient = RestClient.builder()
        .baseUrl("https://api.resend.com")
        .build()

    fun sendVerificationEmail(to: String, token: String) {
        val link = "$baseUrl/verify-email?token=$token"
        val html = """
            <h2>Verifica tu cuenta en SuscriptWallet</h2>
            <p>Haz clic en el siguiente enlace para verificar tu correo electrónico:</p>
            <p><a href="$link">Verificar cuenta</a></p>
            <p>Este enlace expira en 24 horas.</p>
            <p>Si no creaste esta cuenta, puedes ignorar este mensaje.</p>
        """.trimIndent()

        sendEmail(
            to = to,
            subject = "Verifica tu cuenta en SuscriptWallet",
            html = html
        )
    }

    fun sendPasswordResetEmail(to: String, token: String) {
        val link = "$baseUrl/reset-password?token=$token"
        val html = """
            <h2>Restablece tu contraseña en SuscriptWallet</h2>
            <p>Haz clic en el siguiente enlace para restablecer tu contraseña:</p>
            <p><a href="$link">Restablecer contraseña</a></p>
            <p>Este enlace expira en 1 hora.</p>
            <p>Si no solicitaste este cambio, puedes ignorar este mensaje.</p>
        """.trimIndent()

        sendEmail(
            to = to,
            subject = "Restablece tu contraseña en SuscriptWallet",
            html = html
        )
    }

    private fun sendEmail(to: String, subject: String, html: String) {
        if (apiKey.isBlank()) {
            log.warn("RESEND_API_KEY no configurada — email a {} no enviado. Subject: {}", to, subject)
            return
        }

        try {
            val body = mapOf(
                "from" to fromEmail,
                "to" to listOf(to),
                "subject" to subject,
                "html" to html
            )

            restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .toBodilessEntity()

            log.info("Email enviado a {} — Subject: {}", to, subject)
        } catch (ex: Exception) {
            log.error("Error enviando email a {} — {}", to, ex.message, ex)
        }
    }
}
