package com.subia.repository

import com.subia.model.GmailScanTicket
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface GmailScanTicketRepository : JpaRepository<GmailScanTicket, String> {
    fun deleteByExpiresAtBefore(cutoff: OffsetDateTime)
}
