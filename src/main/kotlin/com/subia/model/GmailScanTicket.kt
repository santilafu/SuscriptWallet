package com.subia.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Ticket de un solo uso que liga el JWT de la app (userId) con el callback OAuth de Google,
 * que llega por navegador sin el JWT. Su id (UUID) viaja en el parámetro `state` de OAuth.
 */
@Entity
@Table(name = "gmail_scan_ticket")
data class GmailScanTicket(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val months: Int,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,

    @Column(nullable = false)
    val used: Boolean = false
)
