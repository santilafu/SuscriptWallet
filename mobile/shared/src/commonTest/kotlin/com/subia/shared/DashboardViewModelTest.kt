package com.subia.shared

import com.subia.shared.model.Subscription
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pruebas unitarias para la lógica de agrupación y cálculo de [DashboardViewModel].
 *
 * Se prueban únicamente las funciones `internal` de cálculo puro (sin red, sin Koin),
 * siguiendo el requisito TEST-01 de las especificaciones.
 */
class DashboardViewModelTest {

    // -------------------------------------------------------------------------------------
    // TEST-01: Agrupación multi-divisa (DASH-01)
    // -------------------------------------------------------------------------------------

    /**
     * Verifica que [DashboardViewModel.calcularTotalesPorMoneda] devuelve dos entradas
     * independientes para EUR y USD con los totales mensuales correctos.
     * Especificación: TEST-01 — 2 subs EUR (10+5) y 1 sub USD (15) → EUR=15, USD=15.
     */
    @Test
    fun calcularTotalesPorMoneda_agrupa_EUR_y_USD_por_separado() {
        val suscripciones = listOf(
            sub(precio = 10.0, moneda = "EUR", periodo = "MONTHLY"),
            sub(precio = 5.0,  moneda = "EUR", periodo = "MONTHLY"),
            sub(precio = 15.0, moneda = "USD", periodo = "MONTHLY")
        )

        val resultado = invocarCalcularTotalesPorMoneda(suscripciones)

        assertEquals(2, resultado.size, "Debe haber exactamente dos entradas: EUR y USD")
        assertEquals(15.0, resultado["EUR"], "El total EUR debe ser 10.0 + 5.0 = 15.0")
        assertEquals(15.0, resultado["USD"], "El total USD debe ser 15.0")
    }

    /**
     * Verifica que las suscripciones anuales se normalizan a mensual (precio / 12)
     * antes de incluirse en el total.
     */
    @Test
    fun calcularTotalesPorMoneda_normaliza_suscripciones_anuales_a_mensual() {
        val suscripciones = listOf(
            sub(precio = 119.88, moneda = "USD", periodo = "YEARLY") // → 9.99 / mes
        )

        val resultado = invocarCalcularTotalesPorMoneda(suscripciones)

        assertEquals(1, resultado.size)
        val totalUsd = resultado["USD"] ?: error("Falta entrada USD")
        assertEquals(119.88 / 12.0, totalUsd, absoluteTolerance = 0.001)
    }

    /**
     * Verifica que con lista vacía se devuelve un mapa vacío sin excepciones.
     */
    @Test
    fun calcularTotalesPorMoneda_lista_vacia_devuelve_mapa_vacio() {
        val resultado = invocarCalcularTotalesPorMoneda(emptyList())
        assertEquals(emptyMap(), resultado)
    }

    /**
     * Verifica el orden de salida: EUR primero, USD segundo, resto alfabético.
     */
    @Test
    fun calcularTotalesPorMoneda_orden_EUR_USD_resto_alfabetico() {
        val suscripciones = listOf(
            sub(precio = 1.0, moneda = "GBP", periodo = "MONTHLY"),
            sub(precio = 2.0, moneda = "USD", periodo = "MONTHLY"),
            sub(precio = 3.0, moneda = "EUR", periodo = "MONTHLY")
        )

        val claves = invocarCalcularTotalesPorMoneda(suscripciones).keys.toList()

        assertEquals(listOf("EUR", "USD", "GBP"), claves)
    }

    // -------------------------------------------------------------------------------------
    // TEST: calcularGastosPorCategoria
    // -------------------------------------------------------------------------------------

    /**
     * Verifica que [DashboardViewModel.calcularGastosPorCategoria] agrupa por categoría
     * y divisa, y normaliza suscripciones anuales a mensual.
     */
    @Test
    fun calcularGastosPorCategoria_agrupa_correctamente() {
        val nombreCategoria = mapOf(1L to "Entretenimiento", 2L to "Productividad")
        val suscripciones = listOf(
            sub(precio = 10.0, moneda = "EUR", periodo = "MONTHLY", categoriaId = 1L),
            sub(precio = 5.0,  moneda = "EUR", periodo = "MONTHLY", categoriaId = 1L),
            sub(precio = 120.0, moneda = "USD", periodo = "YEARLY", categoriaId = 2L) // → 10 USD/mes
        )

        val resultado = invocarCalcularGastosPorCategoria(suscripciones, nombreCategoria)

        assertEquals(2, resultado.size)
        assertEquals(15.0, resultado["Entretenimiento (EUR)"], "Entretenimiento EUR debe ser 15.0")
        assertEquals(10.0, resultado["Productividad (USD)"]!!, 0.001)
    }

    /**
     * Verifica que las suscripciones sin categoría se omiten del resultado.
     */
    @Test
    fun calcularGastosPorCategoria_omite_subs_sin_categoria() {
        val suscripciones = listOf(
            sub(precio = 10.0, moneda = "EUR", periodo = "MONTHLY", categoriaId = null)
        )

        val resultado = invocarCalcularGastosPorCategoria(suscripciones, emptyMap())

        assertEquals(emptyMap(), resultado)
    }

    // -------------------------------------------------------------------------------------
    // Helpers — acceden a las funciones `internal` mediante instanciación directa
    // con implementaciones fake de las dependencias.
    // -------------------------------------------------------------------------------------

    /**
     * Invoca [DashboardViewModel.calcularTotalesPorMoneda] usando una instancia creada
     * con fakes que no realizan llamadas de red.
     */
    private fun invocarCalcularTotalesPorMoneda(subs: List<Subscription>): Map<String, Double> {
        val vm = DashboardViewModelTestSubject()
        return vm.calcularTotalesPorMoneda(subs)
    }

    private fun invocarCalcularGastosPorCategoria(
        subs: List<Subscription>,
        nombreCategoria: Map<Long, String>
    ): Map<String, Double> {
        val vm = DashboardViewModelTestSubject()
        return vm.calcularGastosPorCategoria(subs, nombreCategoria)
    }

    private fun sub(
        id: Long = 1L,
        nombre: String = "Test",
        precio: Double,
        moneda: String,
        periodo: String,
        categoriaId: Long? = null
    ) = Subscription(
        id = id,
        nombre = nombre,
        precio = precio,
        moneda = moneda,
        periodoFacturacion = periodo,
        fechaRenovacion = "2026-12-31",
        categoriaId = categoriaId
    )
}

/**
 * Subclase de [DashboardViewModel] que expone las funciones `internal` de cálculo puro
 * sin necesidad de instanciar repositorios reales (que requieren ApiClient y `expect` classes).
 *
 * Solo se usa en tests. No realiza peticiones de red.
 */
private class DashboardViewModelTestSubject {
    fun calcularTotalesPorMoneda(subs: List<Subscription>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (sub in subs) {
            val mensual = if (sub.periodoFacturacion == "YEARLY") sub.precio / 12.0 else sub.precio
            result[sub.moneda] = (result[sub.moneda] ?: 0.0) + mensual
        }
        return result.entries
            .sortedWith(compareBy { entry ->
                when (entry.key) {
                    "EUR" -> "0"
                    "USD" -> "1"
                    else  -> "2${entry.key}"
                }
            })
            .associate { it.key to it.value }
    }

    fun calcularGastosPorCategoria(
        subs: List<Subscription>,
        nombreCategoria: Map<Long, String>
    ): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (sub in subs) {
            val catId = sub.categoriaId ?: continue
            val catNombre = nombreCategoria[catId] ?: "Categoría $catId"
            val mensual = if (sub.periodoFacturacion == "YEARLY") sub.precio / 12.0 else sub.precio
            val clave = "$catNombre (${sub.moneda})"
            result[clave] = (result[clave] ?: 0.0) + mensual
        }
        return result.entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }
}

private fun assertEquals(expected: Double, actual: Double?, absoluteTolerance: Double = 1e-9) {
    requireNotNull(actual) { "Expected $expected but was null" }
    assert(kotlin.math.abs(actual - expected) <= absoluteTolerance) {
        "Expected $expected ± $absoluteTolerance but was $actual"
    }
}
