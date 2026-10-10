package com.subia.security

import jakarta.servlet.http.HttpServletRequest

/**
 * IP del cliente para rate limiting y auditoría.
 *
 * Render (y cualquier proxy) añade la IP real en `X-Forwarded-For`; sin esa cabecera todas las
 * peticiones vendrían de la IP del proxy. Se toma el primer valor, que el cliente PUEDE falsificar
 * (pendiente M-03: confirmar la topología de proxies de Render para usar el valor fiable).
 * Al menos se acota la longitud: el valor se usa como clave de mapas en memoria y se guarda en
 * `security_events.ip_address VARCHAR(45)`, así que una cabecera enorme no debe colarse entera.
 */
object ClientIp {
    private const val MAX_LEN = 45

    fun of(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        val ip = if (!forwarded.isNullOrBlank()) {
            forwarded.substringBefore(",").trim()
        } else {
            request.remoteAddr ?: "unknown"
        }
        return ip.ifBlank { "unknown" }.take(MAX_LEN)
    }
}
