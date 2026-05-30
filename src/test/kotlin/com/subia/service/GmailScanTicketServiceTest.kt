package com.subia.service

import com.subia.model.GmailScanTicket
import com.subia.repository.GmailScanResultRepository
import com.subia.repository.GmailScanTicketRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional

class GmailScanTicketServiceTest {

    private val ticketRepo = mockk<GmailScanTicketRepository>(relaxed = true)
    private val resultRepo = mockk<GmailScanResultRepository>(relaxed = true)
    private val service = GmailScanTicketService(ticketRepo, resultRepo)

    @Test
    fun `issue crea un ticket con el userId y meses dados y lo persiste`() {
        val saved = slot<GmailScanTicket>()
        every { ticketRepo.save(capture(saved)) } answers { saved.captured }

        val ticket = service.issue(userId = 7L, months = 12)

        assertEquals(7L, ticket.userId)
        assertEquals(12, ticket.months)
        assertEquals(false, ticket.used)
    }

    @Test
    fun `consume devuelve el ticket valido y lo marca usado`() {
        val t = GmailScanTicket("abc", 7L, 12, OffsetDateTime.now().plusMinutes(5), used = false)
        every { ticketRepo.findById("abc") } returns Optional.of(t)
        val saved = slot<GmailScanTicket>()
        every { ticketRepo.save(capture(saved)) } answers { saved.captured }

        val result = service.consume("abc")

        assertEquals(7L, result?.userId)
        assertEquals(true, saved.captured.used)
    }

    @Test
    fun `consume devuelve null si el ticket ya fue usado`() {
        val t = GmailScanTicket("abc", 7L, 12, OffsetDateTime.now().plusMinutes(5), used = true)
        every { ticketRepo.findById("abc") } returns Optional.of(t)
        assertNull(service.consume("abc"))
    }

    @Test
    fun `consume devuelve null si el ticket expiro`() {
        val t = GmailScanTicket("abc", 7L, 12, OffsetDateTime.now().minusMinutes(1), used = false)
        every { ticketRepo.findById("abc") } returns Optional.of(t)
        assertNull(service.consume("abc"))
    }

    @Test
    fun `consume devuelve null si el ticket no existe`() {
        every { ticketRepo.findById("nope") } returns Optional.empty()
        assertNull(service.consume("nope"))
    }
}
