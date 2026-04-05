package com.subia.model

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    val token: String,
    @Column(nullable = false, length = 64)
    val email: String,
    @Column(name = "user_id")
    val userId: Long? = null,
    @Column(name = "family_id", nullable = false)
    val familyId: UUID = UUID.randomUUID(),
    @Column(name = "issued_at", nullable = false)
    val issuedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,
    @Column(nullable = false)
    val revoked: Boolean = false,
    @Column(name = "revoked_at")
    val revokedAt: OffsetDateTime? = null
)