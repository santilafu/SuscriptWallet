package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class RenewalDatesTest {

    private fun d(s: String): LocalDate = LocalDate.parse(s)

    @Test
    fun `fecha futura o de hoy no se mueve`() {
        assertEquals(d("2026-10-10"), RenewalDates.nextRenewal(d("2026-10-10"), BillingCycle.MONTHLY, d("2026-10-10")))
        assertEquals(d("2026-12-01"), RenewalDates.nextRenewal(d("2026-12-01"), BillingCycle.WEEKLY, d("2026-10-10")))
    }

    @Test
    fun `mensual dada de alta en mayo se proyecta al proximo cobro (N-12)`() {
        assertEquals(d("2026-10-15"), RenewalDates.nextRenewal(d("2026-05-15"), BillingCycle.MONTHLY, d("2026-10-10")))
        assertEquals(d("2026-11-05"), RenewalDates.nextRenewal(d("2026-05-05"), BillingCycle.MONTHLY, d("2026-10-10")))
    }

    @Test
    fun `dia 31 cae el 30 o 28 y vuelve al 31 (siempre desde el ancla)`() {
        assertEquals(d("2027-02-28"), RenewalDates.nextRenewal(d("2026-01-31"), BillingCycle.MONTHLY, d("2027-02-01")))
        assertEquals(d("2027-03-31"), RenewalDates.nextRenewal(d("2026-01-31"), BillingCycle.MONTHLY, d("2027-03-01")))
        assertEquals(d("2026-11-30"), RenewalDates.nextRenewal(d("2026-08-31"), BillingCycle.MONTHLY, d("2026-11-01")))
    }

    @Test
    fun `anual pasado avanza al aniversario siguiente`() {
        assertEquals(d("2027-03-01"), RenewalDates.nextRenewal(d("2024-03-01"), BillingCycle.YEARLY, d("2026-10-10")))
        // 29 de febrero en año no bisiesto → 28
        assertEquals(d("2027-02-28"), RenewalDates.nextRenewal(d("2024-02-29"), BillingCycle.YEARLY, d("2026-10-10")))
    }

    @Test
    fun `semanal muy antigua avanza en multiplos de 7 dias`() {
        val next = RenewalDates.nextRenewal(d("2020-01-06"), BillingCycle.WEEKLY, d("2026-10-10"))
        assertFalse(next.isBefore(d("2026-10-10")))
        assertTrue(next.isBefore(d("2026-10-17")))
        assertEquals(0L, java.time.temporal.ChronoUnit.DAYS.between(d("2020-01-06"), next) % 7)
    }

    @Test
    fun `prueba vigente, vencida y sin fecha`() {
        val today = d("2026-10-10")
        val base = Subscription(
            name = "X", price = BigDecimal.ONE, billingCycle = BillingCycle.MONTHLY,
            renewalDate = today, category = Category(id = 1, name = "IA"), isTrial = true
        )
        assertTrue(RenewalDates.isActiveTrial(base.copy(trialEndsAt = d("2026-10-10")), today))
        assertFalse(RenewalDates.isActiveTrial(base.copy(trialEndsAt = d("2026-10-09")), today))
        assertTrue(RenewalDates.isActiveTrial(base.copy(trialEndsAt = null), today))
        assertFalse(RenewalDates.isActiveTrial(base.copy(isTrial = false), today))
    }

    // ── R3-01: conservar el ancla al editar sin tocar la fecha ─────────────────────────────

    private fun keep(anchor: String, cycle: BillingCycle, received: String, today: String, newCycle: BillingCycle = cycle) =
        RenewalDates.anchorForUpdate(d(anchor), cycle, d(received), newCycle, d(today))

    @Test
    fun `mensual del 31 vista en noviembre conserva el 31 al guardar la fecha proyectada`() {
        assertEquals(d("2026-11-30"), RenewalDates.nextRenewal(d("2026-01-31"), BillingCycle.MONTHLY, d("2026-11-10")))
        assertEquals(d("2026-01-31"), keep("2026-01-31", BillingCycle.MONTHLY, "2026-11-30", "2026-11-10"))
        // y la serie vuelve al 31 en diciembre
        assertEquals(d("2026-12-31"), RenewalDates.nextRenewal(d("2026-01-31"), BillingCycle.MONTHLY, d("2026-12-01")))
    }

    @Test
    fun `mensual del 30 vista en febrero (28) conserva el 30`() {
        assertEquals(d("2027-02-28"), RenewalDates.nextRenewal(d("2026-10-30"), BillingCycle.MONTHLY, d("2027-02-05")))
        assertEquals(d("2026-10-30"), keep("2026-10-30", BillingCycle.MONTHLY, "2027-02-28", "2027-02-05"))
    }

    @Test
    fun `anual del 29 de febrero conserva el ancla bisiesta`() {
        assertEquals(d("2024-02-29"), keep("2024-02-29", BillingCycle.YEARLY, "2027-02-28", "2026-10-10"))
        assertEquals(d("2028-02-29"), RenewalDates.nextRenewal(d("2024-02-29"), BillingCycle.YEARLY, d("2027-03-01")))
    }

    @Test
    fun `semanal conserva el ancla y no cambia la serie`() {
        // ancla jueves 2026-01-01; hoy sábado 2026-10-10 → próximo jueves 2026-10-15
        assertEquals(d("2026-10-15"), RenewalDates.nextRenewal(d("2026-01-01"), BillingCycle.WEEKLY, d("2026-10-10")))
        assertEquals(d("2026-01-01"), keep("2026-01-01", BillingCycle.WEEKLY, "2026-10-15", "2026-10-10"))
    }

    @Test
    fun `fecha cambiada a proposito por el usuario se respeta`() {
        // Otro día del mes
        assertEquals(d("2026-11-15"), keep("2026-01-31", BillingCycle.MONTHLY, "2026-11-15", "2026-11-10"))
        // Saltarse un cobro: el día del cobro, mover al ciclo siguiente
        assertEquals(d("2026-11-15"), keep("2026-01-15", BillingCycle.MONTHLY, "2026-11-15", "2026-10-15"))
        // Semanal movida un día
        assertEquals(d("2026-10-16"), keep("2026-01-01", BillingCycle.WEEKLY, "2026-10-16", "2026-10-10"))
    }

    @Test
    fun `cambiar el ciclo usa la fecha recibida como nueva ancla`() {
        assertEquals(d("2026-11-30"),
            keep("2026-01-31", BillingCycle.MONTHLY, "2026-11-30", "2026-11-10", newCycle = BillingCycle.YEARLY))
    }

    @Test
    fun `tolera que la proyeccion se calculara ayer (medianoche o cliente por detras)`() {
        // El cliente recibió 2026-10-31 el día 31; guarda el 1-nov (el servidor ya proyectaría 30-nov)
        assertEquals(d("2026-01-31"), keep("2026-01-31", BillingCycle.MONTHLY, "2026-10-31", "2026-11-01"))
    }

    @Test
    fun `reenviar el ancla tal cual la deja igual`() {
        assertEquals(d("2026-01-31"), keep("2026-01-31", BillingCycle.MONTHLY, "2026-01-31", "2026-11-10"))
    }

    // ── Periodicidad: ciclos de intervaloMeses meses ─────────────────────────────────────

    private fun next(anchor: String, intervalo: Int, today: String, cycle: BillingCycle = BillingCycle.MONTHLY) =
        RenewalDates.nextRenewal(d(anchor), cycle, d(today), intervalo)

    @Test
    fun `31 de enero mas un mes cae el 28 de febrero o el 29 si es bisiesto`() {
        assertEquals(d("2027-02-28"), next("2027-01-31", 1, "2027-02-01"))
        assertEquals(d("2028-02-29"), next("2028-01-31", 1, "2028-02-01"))
    }

    @Test
    fun `la serie mensual del 31 vuelve al 31 de marzo tras febrero`() {
        // Se calcula siempre desde el ancla: ancla + 2 meses, no 28-feb + 1 mes (que daría 28-mar)
        assertEquals(d("2027-03-31"), next("2027-01-31", 1, "2027-03-01"))
    }

    @Test
    fun `anual del 29 de febrero con intervalo 12 cae el 28 y vuelve al 29 en bisiesto`() {
        assertEquals(d("2025-02-28"), next("2024-02-29", 12, "2024-03-01", BillingCycle.YEARLY))
        assertEquals(d("2028-02-29"), next("2024-02-29", 12, "2027-03-01", BillingCycle.YEARLY))
    }

    @Test
    fun `anual sin intervalo explicito sigue contando 12 meses`() {
        // Llamadas antiguas (y entidades YEARLY con el valor por defecto 1) no cambian
        assertEquals(d("2027-03-01"), next("2024-03-01", 1, "2026-10-10", BillingCycle.YEARLY))
        assertEquals(d("2027-03-01"), RenewalDates.nextRenewal(d("2024-03-01"), BillingCycle.YEARLY, d("2026-10-10")))
    }

    @Test
    fun `trimestral cambia de anio`() {
        assertEquals(d("2027-02-15"), next("2026-11-15", 3, "2026-11-16"))
    }

    @Test
    fun `bimestral cobra cada dos meses desde el ancla`() {
        // Serie: 10-ene, 10-mar, 10-may, 10-jul, 10-sep, 10-nov
        assertEquals(d("2026-11-10"), next("2026-01-10", 2, "2026-10-10"))
        assertEquals(d("2026-09-10"), next("2026-01-10", 2, "2026-09-10"))
    }

    @Test
    fun `semestral del 31 cae el 28 de febrero y vuelve al 31 de agosto`() {
        assertEquals(d("2026-02-28"), next("2025-08-31", 6, "2026-01-01"))
        assertEquals(d("2026-08-31"), next("2025-08-31", 6, "2026-03-01"))
    }

    @Test
    fun `la suscripcion usa su propio intervalo`() {
        val trimestral = Subscription(
            name = "Seguro", price = BigDecimal("90.00"), billingCycle = BillingCycle.MONTHLY,
            renewalDate = d("2026-01-20"), category = Category(id = 1, name = "Seguros"), intervaloMeses = 3
        )
        // Serie: 20-ene, 20-abr, 20-jul, 20-oct
        assertEquals(d("2026-10-20"), RenewalDates.nextRenewal(trimestral, d("2026-10-10")))
    }

    // ── anchorForUpdate con intervalo de meses (sprint 2) ────────────────────────────────

    @Test
    fun `cambiar el intervalo usa la fecha recibida como nueva ancla`() {
        // Mensual del 31 vista el 10-nov → proyectada 30-nov; el usuario la pasa a trimestral sin
        // tocar la fecha: es otra serie de cobros y el 30-nov pasa a ser el ancla.
        assertEquals(d("2026-11-30"), RenewalDates.anchorForUpdate(
            d("2026-01-31"), BillingCycle.MONTHLY, d("2026-11-30"), BillingCycle.MONTHLY, d("2026-11-10"),
            storedIntervalo = 1, receivedIntervalo = 3
        ))
    }

    @Test
    fun `trimestral sin cambios conserva el ancla con su propia proyeccion`() {
        // Serie trimestral del 31: 31-ene, 30-abr, 31-jul, 31-oct, 31-ene… Hoy 10-nov → 31-ene-2027.
        // Antes se proyectaba como mensual (30-nov) y no se reconocía la fecha.
        assertEquals(d("2027-01-31"), RenewalDates.nextRenewal(d("2026-01-31"), BillingCycle.MONTHLY, d("2026-11-10"), 3))
        assertEquals(d("2026-01-31"), RenewalDates.anchorForUpdate(
            d("2026-01-31"), BillingCycle.MONTHLY, d("2027-01-31"), BillingCycle.MONTHLY, d("2026-11-10"),
            storedIntervalo = 3, receivedIntervalo = 3
        ))
    }

    @Test
    fun `anual guardado con 12 y recibido con el valor por defecto es la misma periodicidad`() {
        assertEquals(d("2024-02-29"), RenewalDates.anchorForUpdate(
            d("2024-02-29"), BillingCycle.YEARLY, d("2027-02-28"), BillingCycle.YEARLY, d("2026-10-10"),
            storedIntervalo = 12
        ))
    }

    @Test
    fun `sobrecarga con la suscripcion guardada usa su intervalo`() {
        val trimestral = Subscription(
            name = "Seguro", price = BigDecimal("90.00"), billingCycle = BillingCycle.MONTHLY,
            renewalDate = d("2026-01-31"), category = Category(id = 1, name = "Seguros"), intervaloMeses = 3
        )
        assertEquals(d("2026-01-31"),
            RenewalDates.anchorForUpdate(trimestral, d("2027-01-31"), BillingCycle.MONTHLY, 3, d("2026-11-10")))
        assertEquals(d("2027-01-31"),
            RenewalDates.anchorForUpdate(trimestral, d("2027-01-31"), BillingCycle.MONTHLY, 6, d("2026-11-10")))
    }
}
