package com.subia.controller.api

import com.subia.dto.api.GmailAddRequestDto
import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.GmailScanResultRow
import com.subia.model.Subscription
import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.UserRepository
import com.subia.service.CategoryMappingService
import com.subia.service.CategoryService
import com.subia.service.GmailScanService
import com.subia.service.GmailScanTicketService
import com.subia.service.SubscriptionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.LocalDate

class ApiGmailControllerTest {

    private val gmailScanService = mockk<GmailScanService>(relaxed = true)
    private val ticketService = mockk<GmailScanTicketService>(relaxed = true)
    private val subscriptionService = mockk<SubscriptionService>(relaxed = true)
    private val categoryService = mockk<CategoryService>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    // El mapeo nombre→clave es real (CategoryMappingService) sobre el CategoryService mockeado,
    // así los stubs de findAll() siguen alimentando el mapeo igual que antes de unificarlo (B-01).
    private val controller = ApiGmailController(
        gmailScanService, ticketService, subscriptionService, categoryService,
        CategoryMappingService(categoryService), userRepository
    )

    private fun jwt(email: String) = mockk<Jwt> { every { subject } returns email }

    private fun row(id: Long, key: String) = GmailScanResultRow(
        id = id, scanId = "s", userId = 7L, serviceName = "Netflix", description = "",
        domain = "netflix.com", senderEmail = "billing@netflix.com", lastSeen = "2026-05-01",
        price = BigDecimal("14.99"), currency = "EUR", billingCycle = BillingCycle.MONTHLY,
        priceFromEmail = true, categoryKey = key
    )

    private fun streamingCategory() = Category(id = 30L, name = "Streaming", color = "#000", icon = "📺")

    private fun givenUserAndStreaming() {
        every { userRepository.findByEmail("a@b.com") } returns
            User(id = 7L, email = "a@b.com", passwordHash = "x", emailVerified = true, role = UserRole.USER)
        every { categoryService.findAll() } returns listOf(streamingCategory())
        every { categoryService.findById(30L) } returns streamingCategory()
    }

    @Test
    fun `add da de alta solo las detecciones con categoria mapeable y cuenta las omitidas`() {
        givenUserAndStreaming()
        every { ticketService.findResults(7L, listOf(1L, 2L)) } returns
            listOf(row(1L, "streaming"), row(2L, "desconocida"))

        val resp = controller.add(jwt("a@b.com"), GmailAddRequestDto(ids = listOf(1L, 2L)))

        assertEquals(1, resp.data?.added)
        assertEquals(1, resp.data?.skipped)
        verify(exactly = 1) { subscriptionService.save(any(), 7L) }
    }

    @Test
    fun `add purga solo las detecciones insertadas y conserva las sin categoria`() {
        givenUserAndStreaming()
        every { ticketService.findResults(7L, listOf(1L, 2L)) } returns
            listOf(row(1L, "streaming"), row(2L, "desconocida"))

        controller.add(jwt("a@b.com"), GmailAddRequestDto(ids = listOf(1L, 2L)))

        verify(exactly = 1) { ticketService.deleteResults(7L, listOf(1L)) }
    }

    @Test
    fun `add calcula la renovacion desde lastSeen segun el ciclo`() {
        givenUserAndStreaming()
        val anual = row(1L, "streaming").copy(lastSeen = "2026-03-15", billingCycle = BillingCycle.YEARLY)
        every { ticketService.findResults(7L, listOf(1L)) } returns listOf(anual)
        val saved = slot<Subscription>()
        every { subscriptionService.save(capture(saved), 7L) } answers { saved.captured }

        controller.add(jwt("a@b.com"), GmailAddRequestDto(ids = listOf(1L)))

        assertEquals(BillingCycle.YEARLY, saved.captured.billingCycle)
        // 2026-03-15 + 1 año = 2027-03-15 (y nunca una fecha anterior a hoy)
        assertEquals(LocalDate.of(2027, 3, 15), saved.captured.renewalDate)
    }
}
