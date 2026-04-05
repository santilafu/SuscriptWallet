package com.subia.model

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "password_reset_tokens")
data class PasswordResetToken(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false, unique = true, length = 128)
    val token: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,

    @Column(nullable = false)
    val used: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)