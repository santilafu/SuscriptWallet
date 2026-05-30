package com.subia.controller.api

import com.subia.dto.api.GmailAddRequestDto
import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.GmailScanResultRow
import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.UserRepository
import com.subia.service.CategoryService
import com.subia.service.GmailScanService
import com.subia.service.GmailScanTicketService
import com.subia.service.SubscriptionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal

class ApiGmailControllerTest {

    private val gmailScanService = mockk<GmailScanService>(relaxed = true)
    private val ticketService = mockk<GmailScanTicketService>(relaxed = true)
    private val subscriptionService = mockk<SubscriptionService>(relaxed = true)
    private val categoryService = mockk<CategoryService>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val controller = ApiGmailController(
        gmailScanService, ticketService, subscriptionService, categoryService, userRepository
    )

    private fun jwt(email: String) = mockk<Jwt> { every { subject } returns email }

    private fun row(id: Long, key: String) = GmailScanResultRow(
        id = id, scanId = "s", userId = 7L, serviceName = "Netflix", description = "",
        domain = "netflix.com", senderEmail = "billing@netflix.com", lastSeen = "2026-05-01",
        price = BigDecimal("14.99"), currency = "EUR", billingCycle = BillingCycle.MONTHLY,
        priceFromEmail = true, categoryKey = key
    )

    @Test
    fun `add da de alta solo las detecciones con categoria mapeable`() {
        every { userRepository.findByEmail("a@b.com") } returns
            User(id = 7L, email = "a@b.com", passwordHash = "x", emailVerified = true, role = UserRole.USER)
        every { ticketService.consumeResults(7L, listOf(1L, 2L)) } returns
            listOf(row(1L, "streaming"), row(2L, "desconocida"))
        every { categoryService.findAll() } returns
            listOf(Category(id = 30L, name = "Streaming", color = "#000", icon = "📺"))
        every { categoryService.findById(30L) } returns
            Category(id = 30L, name = "Streaming", color = "#000", icon = "📺")

        val resp = controller.add(jwt("a@b.com"), GmailAddRequestDto(ids = listOf(1L, 2L)))

        assertEquals(1, resp.data?.added)
        verify(exactly = 1) { subscriptionService.save(any(), 7L) }
    }
}
