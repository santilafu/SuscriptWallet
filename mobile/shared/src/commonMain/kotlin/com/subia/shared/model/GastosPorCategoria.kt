package com.subia.shared.model

import kotlinx.datetime.LocalDate

/** Las dos vistas de la tarjeta "Gasto por categoría" del Inicio. */
enum class VistaCategorias {
    /**
     * Lo que se cobra DE VERDAD en el mes natural de hoy (del día 1 al último, también los días
     * ya pasados): la misma serie que el calendario y "Este mes pagas". Un anual solo cuenta si
     * su cobro cae este mes; un semanal, tantas veces como cobros tenga en el mes.
     */
    ESTE_MES,

    /**
     * Total al año: importe × 12 / intervalo (semanal × 52), ver [Periodicidad.totalAnualInterno].
     * Un gasto con fecha de fin suma como mucho las cuotas que le quedan.
     */
    AL_ANIO
}

/** Gasto de una categoría en una divisa según la vista elegida, con su peso dentro de esa divisa. */
data class GastoCategoria(
    val categoriaId: Long?,
    /** Nombre de la categoría; vacío si el gasto no tiene categoría (la UI pone la etiqueta localizada). */
    val nombre: String,
    val moneda: String,
    /** Suma de [GastoDeCategoria.importe] de sus gastos (2 decimales, regla de [Periodicidad]). */
    val total: Double,
    /** Porcentaje (0..100) sobre el total de la vista en la misma divisa. */
    val porcentaje: Int,
    /** Gastos de la categoría, de mayor a menor importe (lo que se despliega al tocarla). */
    val gastos: List<GastoDeCategoria>
) {
    val numSuscripciones: Int get() = gastos.size
}

/**
 * Un gasto dentro de una categoría: lo que suma en la vista ([importe], [numCobros] cobros de
 * [precio]) y lo que se paga en cada cobro ([precio] cada [periodo]).
 */
data class GastoDeCategoria(
    val suscripcionId: Long,
    val nombre: String,
    val moneda: String,
    val precio: Double,
    val periodo: PeriodoCobro,
    val importe: Double,
    /**
     * Cobros que suma [importe]. En [VistaCategorias.ESTE_MES], los que caen en el mes (1 en casi
     * todos; 4 o 5 en un semanal). En [VistaCategorias.AL_ANIO], los del año (12 un mensual,
     * 6 uno cada 2 meses, 1 un anual, 52 un semanal).
     */
    val numCobros: Int
)

/**
 * "Gasto por categoría" de las suscripciones **activas** en la [vista] elegida, agrupado por
 * categoría y divisa (sin convertir divisas). Orden: EUR, USD, resto alfabético y, dentro de cada
 * divisa, de mayor a menor total. Las categorías sin nada que sumar en la vista no aparecen (en
 * "Este mes", las que no tienen ningún cobro en el mes).
 *
 * Todo se suma en diezmilésimas y se redondea al final ([Periodicidad]); en "Este mes" la suma de
 * las categorías de una divisa da exactamente `ProyeccionCobros.totalMesCompleto`.
 *
 * @param hoy fecha del móvil; solo se usa en [VistaCategorias.ESTE_MES] (mes natural de [hoy]).
 * @param nombreCategoria id → nombre; un id desconocido se muestra como "Categoría N".
 */
fun gastosPorCategoria(
    subs: List<Subscription>,
    hoy: LocalDate,
    vista: VistaCategorias,
    nombreCategoria: Map<Long, String> = emptyMap()
): List<GastoCategoria> {
    val gastos: List<GastoInterno> = when (vista) {
        VistaCategorias.ESTE_MES -> gastosDelMes(subs, hoy)
        VistaCategorias.AL_ANIO -> gastosDelAnio(subs, hoy)
    }
    val totalPorMoneda = gastos.groupBy { it.moneda }.mapValues { (_, lista) -> lista.sumOf { it.interno } }

    return gastos
        .groupBy { it.categoriaId to it.moneda }
        .map { (clave, lista) ->
            val (catId, moneda) = clave
            val interno = lista.sumOf { it.interno }
            val totalDivisa = totalPorMoneda[moneda] ?: 0L
            GastoCategoria(
                categoriaId = catId,
                nombre = catId?.let { nombreCategoria[it] ?: "Categoría $it" } ?: "",
                moneda = moneda,
                total = Periodicidad.aCifraVisible(interno),
                porcentaje = if (totalDivisa > 0) {
                    (interno.toDouble() / totalDivisa * 100.0 + 0.5).toInt().coerceIn(0, 100)
                } else 0,
                gastos = lista
                    .sortedWith(compareByDescending<GastoInterno> { it.interno }.thenBy { it.gasto.nombre })
                    .map { it.gasto }
            )
        }
        .sortedWith(
            compareBy<GastoCategoria> { ordenDivisa(it.moneda) }
                .thenByDescending { it.total }
                .thenBy { it.nombre }
        )
}

/** Un gasto con su importe interno (diezmilésimas) para sumar sin perder céntimos. */
private class GastoInterno(val categoriaId: Long?, val moneda: String, val interno: Long, val gasto: GastoDeCategoria)

/**
 * "Este mes": los cobros del mes natural de [hoy] tal y como los proyecta [cobrosDelMes] (la misma
 * serie del calendario y de "Este mes pagas"), agrupados por suscripción.
 */
private fun gastosDelMes(subs: List<Subscription>, hoy: LocalDate): List<GastoInterno> =
    cobrosDelMes(subs, hoy)
        .flatMap { it.cobros }
        .groupBy { it.suscripcionId }
        .values
        .map { cobros ->
            val primero = cobros.first()
            val interno = cobros.sumOf { Periodicidad.aDiezmilesimas(it.importe) }
            GastoInterno(
                categoriaId = primero.categoriaId,
                moneda = primero.moneda,
                interno = interno,
                gasto = GastoDeCategoria(
                    suscripcionId = primero.suscripcionId,
                    nombre = primero.nombre,
                    moneda = primero.moneda,
                    precio = primero.importe,
                    periodo = primero.periodo,
                    importe = Periodicidad.aCifraVisible(interno),
                    numCobros = cobros.size
                )
            )
        }

/**
 * "Al año": importe × 12 / intervalo de cada suscripción activa (semanal × 52). Un gasto con fecha
 * de fin suma solo lo que le queda: mín(cuotas restantes, cobros al año) × importe (decisión del
 * usuario, igual en el backend). Los terminados no suman.
 */
private fun gastosDelAnio(subs: List<Subscription>, hoy: LocalDate): List<GastoInterno> =
    subs.filter { it.cuentaEnTotales(hoy) }.map { sub ->
        val periodo = sub.periodoCobro()
        val porAnio = cobrosAlAnio(periodo)
        val restantes = sub.restantesAlAnio(hoy)
        val interno = sub.totalAlAnioInterno(hoy)
        GastoInterno(
            categoriaId = sub.categoriaId,
            moneda = sub.moneda,
            interno = interno,
            gasto = GastoDeCategoria(
                suscripcionId = sub.id,
                nombre = sub.nombre,
                moneda = sub.moneda,
                precio = sub.precio,
                periodo = periodo,
                importe = Periodicidad.aCifraVisible(interno),
                numCobros = restantes ?: porAnio
            )
        )
    }

/**
 * Lo que suma un gasto "Al año" (diezmilésimas): con fecha de fin, mín(cuotas restantes, cobros
 * al año) × importe; sin ella, la tasa anual. Es la misma cifra que "Gasto anual" en la web.
 */
internal fun Subscription.totalAlAnioInterno(hoy: LocalDate): Long =
    restantesAlAnio(hoy)?.let { Periodicidad.aDiezmilesimas(precio) * it } ?: totalAnualInterno()

/** Cuotas restantes si son menos que los cobros de un año; null si el gasto suma su tasa anual. */
private fun Subscription.restantesAlAnio(hoy: LocalDate): Int? =
    plazoGasto(this, hoy)?.cuotasRestantes?.takeIf { it < cobrosAlAnio(periodoCobro()) }

/** Cobros en un año según el periodo: 12 / meses, o 52, 26 y 365 en los ciclos por días. */
private fun cobrosAlAnio(periodo: PeriodoCobro): Int = when (periodo) {
    is PeriodoCobro.PorMeses -> Periodicidad.MESES_EN_ANIO / periodo.meses
    PeriodoCobro.Semanal -> 52
    PeriodoCobro.Quincenal -> 26
    PeriodoCobro.Diario -> 365
}
