package com.subia.service

import com.subia.model.GmailScanTicket
import com.subia.repository.GmailScanResultRepository
import com.subia.repository.GmailScanTicketRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
    fun `consume marca el ticket usado con un UPDATE condicional y lo devuelve`() {
        val t = GmailScanTicket("abc", 7L, 12, OffsetDateTime.now().plusMinutes(5), used = true)
        every { ticketRepo.markUsedIfValid("abc", any()) } returns 1
        every { ticketRepo.findById("abc") } returns Optional.of(t)

        val result = service.consume("abc")

        assertEquals(7L, result?.userId)
        assertEquals(12, result?.months)
        verify(exactly = 1) { ticketRepo.markUsedIfValid("abc", any()) }
        verify(exactly = 0) { ticketRepo.save(any()) }
    }

    @Test
    fun `consume devuelve null si el UPDATE no afecta a ninguna fila (usado, expirado o inexistente)`() {
        every { ticketRepo.markUsedIfValid("abc", any()) } returns 0

        assertNull(service.consume("abc"))
        verify(exactly = 0) { ticketRepo.findById(any()) }
    }

    @Test
    fun `findResults no consulta el repositorio con una lista vacia`() {
        assertEquals(emptyList<Any>(), service.findResults(7L, emptyList()))
        verify(exactly = 0) { resultRepo.findByIdInAndUserId(any(), any()) }
    }

    @Test
    fun `deleteResults solo borra cuando hay ids`() {
        service.deleteResults(7L, emptyList())
        verify(exactly = 0) { resultRepo.deleteByIdInAndUserId(any(), any()) }

        service.deleteResults(7L, listOf(1L, 2L))
        verify(exactly = 1) { resultRepo.deleteByIdInAndUserId(listOf(1L, 2L), 7L) }
    }
}
