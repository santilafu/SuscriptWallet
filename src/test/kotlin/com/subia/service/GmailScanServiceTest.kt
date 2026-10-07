package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.CatalogItem
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests de la lógica pura del escaneo de Gmail: extracción del remitente, normalización de
 * dominios, matching contra el catálogo y extracción de precio/ciclo del cuerpo del correo.
 *
 * No tocan la red ni la base de datos: el [CatalogService] va mockeado y solo se ejercitan
 * las funciones deterministas.
 */
class GmailScanServiceTest {

    private val service = GmailScanService(
        catalogService = mockk(relaxed = true),
        clientId = "",
        clientSecret = "",
        baseUrl = "http://localhost:8081"
    )

    private fun item(name: String, domain: String) = CatalogItem(
        name = name,
        price = BigDecimal("9.99"),
        currency = "EUR",
        billingCycle = BillingCycle.MONTHLY,
        description = "",
        categoryKey = "streaming",
        domain = domain
    )

    private fun plan(name: String, price: String, annual: String? = null) = CatalogItem(
        name = name,
        price = BigDecimal(price),
        currency = "EUR",
        billingCycle = BillingCycle.MONTHLY,
        description = "",
        categoryKey = "streaming",
        domain = "netflix.com",
        priceAnnual = annual?.let { BigDecimal(it) }
    )

    // ── extractEmail ────────────────────────────────────────────────────────
    @Test
    fun `extractEmail saca la direccion de un From con nombre`() {
        assertEquals("info@netflix.com", service.extractEmail("Netflix <info@netflix.com>"))
    }

    @Test
    fun `extractEmail acepta un From que ya es solo la direccion`() {
        assertEquals("billing@spotify.com", service.extractEmail("  billing@spotify.com  "))
    }

    // ── registrableDomain ───────────────────────────────────────────────────
    @Test
    fun `registrableDomain recorta los subdominios a las dos ultimas etiquetas`() {
        assertEquals("netflix.com", service.registrableDomain("mg.mail.netflix.com"))
        assertEquals("example.com", service.registrableDomain("a.b.c.example.com"))
        assertEquals("netflix.com", service.registrableDomain("netflix.com"))
    }

    // ── matchDomain ─────────────────────────────────────────────────────────
    @Test
    fun `matchDomain casa por dominio exacto y por subdominio`() {
        val byDomain = mapOf("netflix.com" to listOf(item("Netflix", "netflix.com")))
        assertEquals("Netflix", service.matchDomain("netflix.com", byDomain)?.value?.first()?.name)
        assertEquals("Netflix", service.matchDomain("email.account.netflix.com", byDomain)?.value?.first()?.name)
    }

    @Test
    fun `matchDomain devuelve null si el remitente no esta en el catalogo`() {
        val byDomain = mapOf("netflix.com" to listOf(item("Netflix", "netflix.com")))
        assertNull(service.matchDomain("desconocido.com", byDomain))
    }

    // ── chooseBestPlan ──────────────────────────────────────────────────────
    @Test
    fun `chooseBestPlan elige el tier mas cercano al precio detectado`() {
        val planes = listOf(
            plan("Netflix Básico", "6.99"),
            plan("Netflix Estándar", "13.99"),
            plan("Netflix Premium", "19.99")
        )
        assertEquals("Netflix Estándar", service.chooseBestPlan(planes, BigDecimal("13.99")).name)
        assertEquals("Netflix Premium", service.chooseBestPlan(planes, BigDecimal("18.50")).name)
        assertEquals("Netflix Básico", service.chooseBestPlan(planes, BigDecimal("7.20")).name)
    }

    @Test
    fun `chooseBestPlan usa el primer plan si no hay precio detectado`() {
        val planes = listOf(plan("Netflix Básico", "6.99"), plan("Netflix Premium", "19.99"))
        assertEquals("Netflix Básico", service.chooseBestPlan(planes, null).name)
    }

    @Test
    fun `chooseBestPlan compara tambien contra el precio anual`() {
        val planes = listOf(
            plan("Mensual", "9.99", annual = "99.99"),
            plan("Otro", "4.99")
        )
        // El importe detectado coincide con el precio anual del primer plan.
        assertEquals("Mensual", service.chooseBestPlan(planes, BigDecimal("99.99")).name)
    }

    // ── inferCycleFromPlan ──────────────────────────────────────────────────
    @Test
    fun `inferCycleFromPlan asume anual cuando el importe encaja con el precio anual`() {
        val p = plan("Plan", "9.99", annual = "99.99")
        assertEquals(BillingCycle.YEARLY, service.inferCycleFromPlan(p, BigDecimal("99.99")))
    }

    @Test
    fun `inferCycleFromPlan no asume nada cuando el importe es el mensual`() {
        val p = plan("Plan", "9.99", annual = "99.99")
        assertNull(service.inferCycleFromPlan(p, BigDecimal("9.99")))
    }

    @Test
    fun `inferCycleFromPlan no asume nada si el plan no tiene precio anual`() {
        val p = plan("Plan", "9.99")
        assertNull(service.inferCycleFromPlan(p, BigDecimal("9.99")))
    }

    // ── findPrice ───────────────────────────────────────────────────────────
    @Test
    fun `findPrice detecta importe en euros con simbolo despues y coma decimal`() {
        val p = service.findPrice("Tu suscripción cuesta 12,99 € al mes — gracias")
        assertEquals(BigDecimal("12.99"), p?.amount)
        assertEquals("EUR", p?.currency)
        assertEquals(BillingCycle.MONTHLY, p?.cycle)
    }

    @Test
    fun `findPrice ignora un -este mes- ambiguo y no infiere ciclo`() {
        // "este mes" se refiere al periodo facturado, no a la periodicidad: no debe asumir MONTHLY.
        val p = service.findPrice("Tu factura de este mes es de 8,50 €")
        assertEquals(BigDecimal("8.50"), p?.amount)
        assertNull(p?.cycle)
    }

    @Test
    fun `findPrice detecta importe con simbolo delante`() {
        val p = service.findPrice("Total: €9.99 al mes")
        assertEquals(BigDecimal("9.99"), p?.amount)
        assertEquals("EUR", p?.currency)
        assertEquals(BillingCycle.MONTHLY, p?.cycle)
    }

    @Test
    fun `findPrice detecta dolares y ciclo anual en ingles`() {
        val p = service.findPrice("You were charged USD 15.00 per year")
        assertEquals(BigDecimal("15.00"), p?.amount)
        assertEquals("USD", p?.currency)
        assertEquals(BillingCycle.YEARLY, p?.cycle)
    }

    @Test
    fun `findPrice detecta libras y ciclo semanal`() {
        val p = service.findPrice("Just £7.99 weekly for your plan")
        assertEquals(BigDecimal("7.99"), p?.amount)
        assertEquals("GBP", p?.currency)
        assertEquals(BillingCycle.WEEKLY, p?.cycle)
    }

    @Test
    fun `findPrice deja el ciclo en null cuando el texto no lo indica`() {
        val p = service.findPrice("Importe del recibo: 19,99 EUR")
        assertEquals(BigDecimal("19.99"), p?.amount)
        assertEquals("EUR", p?.currency)
        assertNull(p?.cycle)
    }

    @Test
    fun `findPrice devuelve null si no hay ninguna moneda`() {
        assertNull(service.findPrice("Gracias por tu compra, pedido numero 12345"))
    }

    @Test
    fun `findPrice descarta un importe de cero`() {
        assertNull(service.findPrice("Tu plan gratuito: 0,00 € al mes"))
    }

    @Test
    fun `findPrice devuelve null para texto vacio`() {
        assertNull(service.findPrice("   "))
    }

    // ── Recibos recurrentes: alias de remitente ─────────────────────────────
    private fun bill(name: String, domain: String, key: String, price: String, cycle: BillingCycle = BillingCycle.MONTHLY) =
        CatalogItem(
            name = name, price = BigDecimal(price), currency = "EUR", billingCycle = cycle,
            description = "", categoryKey = key, domain = domain, variablePrice = true
        )

    private val billsByDomain = mapOf(
        "iberdrola.es" to listOf(bill("Iberdrola", "iberdrola.es", "hogar", "70.00")),
        "endesa.com" to listOf(bill("Endesa", "endesa.com", "hogar", "70.00")),
        "movistar.es" to listOf(bill("Movistar", "movistar.es", "telecos", "60.00")),
        "mapfre.es" to listOf(bill("Mapfre", "mapfre.es", "seguros", "450.00", BillingCycle.YEARLY)),
        "sanitas.es" to listOf(bill("Sanitas", "sanitas.es", "seguros", "60.00")),
        "securitasdirect.es" to listOf(bill("Securitas Direct", "securitasdirect.es", "hogar", "45.00"))
    )

    @Test
    fun `matchDomain casa un remitente que factura desde un dominio alias (telefonica → movistar)`() {
        val match = service.matchDomain("telefonica.com", billsByDomain)
        assertEquals("movistar.es", match?.key)
        assertEquals("Movistar", match?.value?.first()?.name)
    }

    @Test
    fun `matchDomain casa un subdominio de un alias (comunicaciones endesaclientes)`() {
        assertEquals("Endesa", service.matchDomain("comunicaciones.endesaclientes.com", billsByDomain)?.value?.first()?.name)
        assertEquals("Securitas Direct", service.matchDomain("info.verisure.es", billsByDomain)?.value?.first()?.name)
    }

    // ── Recibos recurrentes: detección por asunto ───────────────────────────
    @Test
    fun `matchSubject reconoce una factura de luz por la marca del asunto`() {
        val match = service.matchSubject("Tu factura de Iberdrola de marzo ya está disponible", billsByDomain)
        assertEquals("iberdrola.es", match?.key)
    }

    @Test
    fun `matchSubject reconoce recibos de seguros y de telecos`() {
        assertEquals("mapfre.es", service.matchSubject("Recibo de tu póliza MAPFRE", billsByDomain)?.key)
        assertEquals("sanitas.es", service.matchSubject("Sanitas: cobro de tu cuota mensual", billsByDomain)?.key)
        assertEquals("movistar.es", service.matchSubject("Factura Movistar: ya puedes consultarla", billsByDomain)?.key)
    }

    @Test
    fun `matchSubject ignora newsletters aunque mencionen la marca`() {
        assertNull(service.matchSubject("Novedades Movistar: descubre las ofertas de fibra", billsByDomain))
        assertNull(service.matchSubject("", billsByDomain))
    }

    @Test
    fun `matchSubject devuelve null si la marca no esta en el catalogo cargado`() {
        // Vodafone es una marca conocida pero no está en este mapa de prueba.
        assertNull(service.matchSubject("Tu factura Vodafone de abril", billsByDomain))
    }

    // ── Recibos recurrentes: seguro anual pagado fraccionado ────────────────
    @Test
    fun `inferCycleFromPlan asume mensual si el importe encaja con la prima anual dividida en 12`() {
        val mapfre = billsByDomain.getValue("mapfre.es").first() // 450 €/año → 37,50 €/mes
        assertEquals(BillingCycle.MONTHLY, service.inferCycleFromPlan(mapfre, BigDecimal("38.10")))
    }

    @Test
    fun `inferCycleFromPlan mantiene el ciclo anual si el importe es la prima completa`() {
        val mapfre = billsByDomain.getValue("mapfre.es").first()
        assertNull(service.inferCycleFromPlan(mapfre, BigDecimal("445.00")))
    }

    // ── Recibos recurrentes: importe en el cuerpo ───────────────────────────
    @Test
    fun `findPrice extrae el importe de una factura de luz tipica`() {
        val body = "Hola. Ya tienes disponible tu factura de electricidad. " +
            "Periodo de facturación: 01/03/2026 - 31/03/2026. Importe total: 63,21 € " +
            "Se cargará en tu cuenta el 10/04/2026."
        val p = service.findPrice(body)
        assertEquals(BigDecimal("63.21"), p?.amount)
        assertEquals("EUR", p?.currency)
        assertNull(p?.cycle)
    }

    @Test
    fun `findPrice extrae prima anual de un recibo de seguro`() {
        val p = service.findPrice("Te recordamos que la prima anual de tu póliza de hogar es de 412,30 EUR.")
        assertEquals(BigDecimal("412.30"), p?.amount)
        assertEquals(BillingCycle.YEARLY, p?.cycle)
    }
}
