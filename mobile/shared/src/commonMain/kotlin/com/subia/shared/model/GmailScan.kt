package com.subia.shared.model

import kotlinx.serialization.Serializable

/** Respuesta del backend al pedir el escaneo: URL de consentimiento a abrir en Custom Tab. */
@Serializable
data class GmailScanTicketResponse(val connectUrl: String)

/** Una suscripción detectada en el correo, tal como la revisa la app. */
@Serializable
data class GmailDetected(
    val id: Long,
    val serviceName: String,
    val description: String = "",
    val domain: String,
    val senderEmail: String,
    val lastSeen: String,
    val price: Double,
    val currency: String,
    val billingCycle: String,
    val priceFromEmail: Boolean
)

/** Petición de alta: ids de las detecciones marcadas por el usuario. */
@Serializable
data class GmailAddRequest(val ids: List<Long>)

/** Resultado del alta. */
@Serializable
data class GmailAddResult(val added: Int)

/** Cuerpo vacío para el POST del ticket (los parámetros van en el query string). */
@Serializable
object EmptyBody
