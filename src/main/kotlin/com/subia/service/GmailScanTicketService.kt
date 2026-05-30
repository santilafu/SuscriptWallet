package com.subia.service

import com.subia.model.GmailScanResultRow
import com.subia.model.GmailScanTicket
import com.subia.repository.GmailScanResultRepository
import com.subia.repository.GmailScanTicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/** TTL del ticket: tiempo máximo entre que la app lo pide y Google devuelve el callback. */
private const val TICKET_TTL_MINUTES = 10L

@Service
class GmailScanTicketService(
    private val ticketRepo: GmailScanTicketRepository,
    private val resultRepo: GmailScanResultRepository
) {
    /** Emite un ticket de un solo uso para el usuario y lo persiste. */
    fun issue(userId: Long, months: Int): GmailScanTicket {
        val ticket = GmailScanTicket(
            id = UUID.randomUUID().toString(),
            userId = userId,
            months = months,
            expiresAt = OffsetDateTime.now().plusMinutes(TICKET_TTL_MINUTES),
            used = false
        )
        return ticketRepo.save(ticket)
    }

    /**
     * Valida y consume el ticket: devuelve el ticket si es válido (existe, no usado, no expirado)
     * marcándolo como usado; en cualquier otro caso devuelve null.
     */
    @Transactional
    fun consume(ticketId: String): GmailScanTicket? {
        val ticket = ticketRepo.findById(ticketId).orElse(null) ?: return null
        if (ticket.used || ticket.expiresAt.isBefore(OffsetDateTime.now())) return null
        ticketRepo.save(ticket.copy(used = true))
        return ticket
    }

    /** Reemplaza los resultados del usuario por el nuevo lote del escaneo. */
    @Transactional
    fun replaceResults(userId: Long, rows: List<GmailScanResultRow>) {
        resultRepo.deleteByUserId(userId)
        resultRepo.saveAll(rows)
    }

    fun resultsFor(userId: Long): List<GmailScanResultRow> = resultRepo.findByUserId(userId)

    @Transactional
    fun consumeResults(userId: Long, ids: List<Long>): List<GmailScanResultRow> {
        val rows = resultRepo.findByIdInAndUserId(ids, userId)
        if (rows.isNotEmpty()) resultRepo.deleteByIdInAndUserId(rows.map { it.id }, userId)
        return rows
    }
}
