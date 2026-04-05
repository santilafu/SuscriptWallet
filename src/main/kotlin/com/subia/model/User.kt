package com.subia.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.OffsetDateTime

enum class UserRole {
    USER, ADMIN
}

@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(nullable = false, unique = true, length = 254)
    val email: String,

    @JsonIgnore
    @Column(name = "password_hash", length = 72)
    val passwordHash: String? = null,

    @Column(name = "google_id", unique = true, length = 128)
    val googleId: String? = null,

    @Column(name = "email_verified", nullable = false)
    val emailVerified: Boolean = false,

    @Column(name = "email_verification_token", length = 64)
    val emailVerificationToken: String? = null,

    @Column(name = "email_verification_expires_at")
    val emailVerificationExpiresAt: OffsetDateTime? = null,

    @Column(name = "failed_attempts", nullable = false)
    val failedAttempts: Int = 0,

    @Column(name = "locked_until")
    val lockedUntil: OffsetDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: UserRole = UserRole.USER,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
