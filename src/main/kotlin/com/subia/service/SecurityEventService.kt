package com.subia.service

import com.subia.model.SecurityEvent
import com.subia.model.SecurityEventType
import com.subia.repository.SecurityEventRepository
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

    fun extractIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        return if (!forwarded.isNullOrBlank()) {
            forwarded.split(",").first().trim()
        } else {
            request.remoteAddr ?: "unknown"
        }
    }
}
