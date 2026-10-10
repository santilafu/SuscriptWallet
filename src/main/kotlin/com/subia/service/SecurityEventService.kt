package com.subia.service

import com.subia.model.SecurityEvent
import com.subia.model.SecurityEventType
import com.subia.repository.SecurityEventRepository
import com.subia.security.ClientIp
import jakarta.servlet.http.HttpServletRequest
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class SecurityEventService(
    private val securityEventRepository: SecurityEventRepository
) {

    @Async
    fun logEvent(
        userId: Long?,
        type: SecurityEventType,
        ipAddress: String,
        userAgent: String?
    ) {
        val event = SecurityEvent(
            userId = userId,
            eventType = type,
            ipAddress = ipAddress,
            userAgent = userAgent
        )
        securityEventRepository.save(event)
    }

    /** IP del cliente acotada a 45 caracteres (tamaño de `security_events.ip_address`). */
    fun extractIp(request: HttpServletRequest): String = ClientIp.of(request)
}
