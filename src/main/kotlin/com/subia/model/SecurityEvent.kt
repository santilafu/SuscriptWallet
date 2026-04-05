package com.subia.model

import jakarta.persistence.*
import java.time.OffsetDateTime

enum class SecurityEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    REGISTER,
    LOGOUT,
    ACCOUNT_LOCKED,
    PASSWORD_RESET_REQUEST,
    PASSWORD_RESET_SUCCESS,
    ACCOUNT_DELETED
}

@Entity
@Table(name = "security_events")
data class SecurityEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id")
    val userId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    val eventType: SecurityEventType,

    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    val userAgent: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
