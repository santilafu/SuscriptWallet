package com.subia.controller.api

import com.subia.dto.api.ApiResponse
import com.subia.dto.api.GmailAddRequestDto
import com.subia.dto.api.GmailAddResultDto
import com.subia.dto.api.GmailDetectedDto
import com.subia.dto.api.GmailScanTicketDto
import com.subia.model.GmailScanResultRow
import com.subia.model.Subscription
import com.subia.repository.UserRepository
import com.subia.service.CategoryService
import com.subia.service.GmailScanService
import com.subia.service.GmailScanTicketService
import com.subia.service.SubscriptionService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

/**
 * API REST del escaneo de Gmail para la app móvil. Reutiliza la lógica de [GmailScanService];
 * la identidad entre el JWT de la app y el callback OAuth (que llega por navegador sin JWT)
 * se resuelve con un ticket de un solo uso (ver [GmailScanTicketService]).
 */
@RestController
@RequestMapping("/api/gmail")
class ApiGmailController(
    private val gmailScanService: GmailScanService,
    private val gmailScanTicketService: GmailScanTicketService,
    private val subscriptionService: SubscriptionService,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
) {
    private fun resolveUserId(jwt: Jwt): Long =
        userRepository.findByEmail(jwt.subject)?.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")

    /** Emite el ticket y devuelve la URL de consentimiento (state = ticketId). */
    @PostMapping("/scan/ticket")
    fun ticket(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false, defaultValue = "12") months: Int
    ): ApiResponse<GmailScanTicketDto> {
        val userId = resolveUserId(jwt)
        val ticket = gmailScanTicketService.issue(userId, months.coerceIn(1, GmailScanService.MAX_MONTHS))
        val url = gmailScanService.buildAuthorizationUrl(ticket.id)
        return ApiResponse(data = GmailScanTicketDto(connectUrl = url))
    }

    /** Devuelve las detecciones guardadas para el usuario. */
    @GetMapping("/scan/results")
    fun results(@AuthenticationPrincipal jwt: Jwt): ApiResponse<List<GmailDetectedDto>> {
        val userId = resolveUserId(jwt)
        return ApiResponse(data = gmailScanTicketService.resultsFor(userId).map { it.toDto() })
    }

    /** Da de alta las detecciones marcadas y las purga. */
    @PostMapping("/scan/add")
    fun add(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody req: GmailAddRequestDto
    ): ApiResponse<GmailAddResultDto> {
        val userId = resolveUserId(jwt)
        val rows = gmailScanTicketService.consumeResults(userId, req.ids)
        val keyToId = categoryKeyToId()
        var added = 0
        for (r in rows) {
            val categoryId = keyToId[r.categoryKey] ?: continue
            subscriptionService.save(
                Subscription(
                    name = r.serviceName,
                    description = r.description,
                    price = r.price,
                    currency = r.currency,
                    billingCycle = r.billingCycle,
                    renewalDate = LocalDate.now().plusMonths(1),
                    category = categoryService.findById(categoryId),
                    active = true,
                    notes = ""
                ),
                userId
            )
            added++
        }
        return ApiResponse(data = GmailAddResultDto(added = added))
    }

    /** Mapea la categoryKey del catálogo al id real de la categoría (mismo criterio que GmailController). */
    private fun categoryKeyToId(): Map<String, Long> =
        categoryService.findAll().mapNotNull { cat ->
            val key = when (cat.name) {
                "IA" -> "ia"
                "Streaming" -> "streaming"
                "Música" -> "musica"
                "Software" -> "software"
                "Cloud" -> "cloud"
                "Gaming" -> "gaming"
                "Seguridad" -> "seguridad"
                "Noticias y Lectura" -> "noticias"
                "Salud y Deporte" -> "salud"
                "Desarrollo" -> "desarrollo"
                "Prueba gratuita" -> "prueba"
                "Finanzas" -> "finanzas"
                "Educación" -> "educacion"
                "Creatividad y foto" -> "creatividad"
                "Citas y social" -> "citas"
                else -> null
            }
            if (key != null) key to cat.id else null
        }.toMap()

    private fun GmailScanResultRow.toDto() = GmailDetectedDto(
        id = id, serviceName = serviceName, description = description, domain = domain,
        senderEmail = senderEmail, lastSeen = lastSeen, price = price, currency = currency,
        billingCycle = billingCycle.name, priceFromEmail = priceFromEmail
    )
}
