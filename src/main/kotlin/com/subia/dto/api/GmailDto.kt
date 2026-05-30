package com.subia.dto.api

import java.math.BigDecimal

/** Respuesta a la petición de escaneo: URL de consentimiento que la app abre en Custom Tab. */
data class GmailScanTicketDto(val connectUrl: String)

/** Una suscripción detectada, tal como la revisa la app. `id` identifica la fila para el alta. */
data class GmailDetectedDto(
    val id: Long,
    val serviceName: String,
    val description: String,
    val domain: String,
    val senderEmail: String,
    val lastSeen: String,
    val price: BigDecimal,
    val currency: String,
    val billingCycle: String,
    val priceFromEmail: Boolean
)

/** Petición de alta: ids de las detecciones que el usuario marcó. */
data class GmailAddRequestDto(val ids: List<Long>)

/** Resultado del alta. */
data class GmailAddResultDto(val added: Int)
