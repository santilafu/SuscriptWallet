package com.subia.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Una suscripción detectada en el escaneo, guardada temporalmente para que la app la revise.
 * Se purga al darla de alta o por expiración. `scanId` == id del ticket que originó el escaneo.
 */
@Entity
@Table(name = "gmail_scan_result")
data class GmailScanResultRow(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "scan_id", length = 64, nullable = false)
    val scanId: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "service_name", nullable = false)
    val serviceName: String,

    @Column(nullable = false)
    val description: String = "",

    @Column(nullable = false)
    val domain: String,

    @Column(name = "sender_email", nullable = false)
    val senderEmail: String,

    @Column(name = "last_seen", nullable = false)
    val lastSeen: String,

    @Column(nullable = false)
    val price: BigDecimal,

    @Column(nullable = false)
    val currency: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    val billingCycle: BillingCycle,

    @Column(name = "price_from_email", nullable = false)
    val priceFromEmail: Boolean,

    @Column(name = "category_key", nullable = false)
    val categoryKey: String
)
