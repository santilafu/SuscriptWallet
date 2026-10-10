package com.subia.service

import com.subia.repository.GmailScanTicketRepository
import com.subia.repository.PasswordResetTokenRepository
import com.subia.repository.RefreshTokenRepository
import com.subia.repository.SecurityEventRepository
import com.subia.security.AuthRateLimitFilter
import com.subia.security.RateLimitFilter
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * Tareas de mantenimiento periódicas (M-09).
 *
 * Antes no existía ninguna: los tickets de Gmail, los refresh tokens y los tokens de
 * restablecimiento caducados y los eventos de seguridad crecían sin límite en la BD gratuita de
 * Aiven, y los mapas en memoria del rate limiting no se vaciaban nunca.
 *
 * En Render free la instancia se duerme sin tráfico: estas tareas solo corren mientras está
 * despierta, lo cual basta para una purga (no hay nada que dependa de una hora exacta).
 */
@Component
class MaintenanceJobs(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val gmailScanTicketRepository: GmailScanTicketRepository,
    private val securityEventRepository: SecurityEventRepository,
    private val rateLimitFilter: RateLimitFilter,
    private val authRateLimitFilter: AuthRateLimitFilter
) {
    private val log = LoggerFactory.getLogger(MaintenanceJobs::class.java)

    /** Purga de BD cada 6 horas (primera ejecución 5 min después del arranque). */
    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT6H")
    @Transactional
    fun purgeExpiredData() {
        val now = OffsetDateTime.now()
        val refresh = refreshTokenRepository.deleteExpiredBefore(now)
        val resets = passwordResetTokenRepository.deleteExpiredBefore(now)
        val tickets = gmailScanTicketRepository.deleteExpiredBefore(now)
        val events = securityEventRepository.deleteOlderThan(now.minusDays(SECURITY_EVENT_RETENTION_DAYS))
        if (refresh + resets + tickets + events > 0) {
            log.info(
                "Purga: {} refresh tokens, {} tokens de reset, {} tickets Gmail, {} eventos de seguridad",
                refresh, resets, tickets, events
            )
        }
    }

    /** Vacía cada 5 minutos las entradas inactivas de los rate limiters en memoria. */
    @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT5M")
    fun purgeRateLimiters() {
        rateLimitFilter.purgeIdle()
        authRateLimitFilter.purgeIdle()
    }

    companion object {
        const val SECURITY_EVENT_RETENTION_DAYS = 180L
    }
}
