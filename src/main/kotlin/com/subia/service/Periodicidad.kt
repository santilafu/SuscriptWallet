package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.Subscription
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Periodicidad de pago de un gasto (cada cuántos meses se cobra) y la regla ÚNICA de redondeo
 * del dinero.
 *
 * ## Periodicidad
 * Cada suscripción guarda `intervaloMeses`: 1 mensual, 2 bimestral, 3 trimestral, 6 semestral
 * o 12 anual ([VALORES_PERMITIDOS]). El `billingCycle` pasa a ser DERIVADO de ese intervalo
 * (12 → YEARLY, el resto → MONTHLY) para que las apps antiguas, que solo conocen MONTHLY/YEARLY,
 * sigan viendo un ciclo que entienden. WEEKLY es un caso aparte: guarda intervalo 1 y lo ignora
 * (sus cálculos siguen en [DashboardService] y [RenewalDates] como hasta ahora).
 *
 * ## Regla de redondeo (dinero)
 * - Siempre [BigDecimal], nunca Double.
 * - Cálculos internos (equivalente mensual / anual de cada gasto) a [ESCALA_INTERNA] = 4
 *   decimales con [RoundingMode.HALF_UP]. Así sumar muchos gastos no acumula error de céntimos.
 * - Solo las cifras FINALES que se muestran o devuelven (totales, gasto por categoría, precio
 *   guardado) se redondean a [ESCALA_VISIBLE] = 2 decimales HALF_UP con [aCifraVisible].
 *
 * Ejemplo: 10 € cada 3 meses → 3,3333 €/mes interno → 3,33 € visible.
 */
object Periodicidad {

    /** Intervalos admitidos (también los exige el CHECK de la migración V15). */
    val VALORES_PERMITIDOS: Set<Int> = setOf(1, 2, 3, 6, 12)

    const val MESES_EN_ANIO = 12
    const val ESCALA_INTERNA = 4
    const val ESCALA_VISIBLE = 2
    val REDONDEO: RoundingMode = RoundingMode.HALF_UP

    private val DOCE = BigDecimal(MESES_EN_ANIO)

    /** true si [intervaloMeses] es uno de los [VALORES_PERMITIDOS]. */
    fun esValido(intervaloMeses: Int): Boolean = intervaloMeses in VALORES_PERMITIDOS

    /**
     * Aplica la invariante entre `billingCycle` e `intervaloMeses` y devuelve la pareja coherente.
     * Es la misma regla para guardar ([SubscriptionService.validated]) y para calcular, así que
     * una entidad construida a mano sin pasar por la validación da los mismos resultados.
     *
     * - WEEKLY → intervalo 1 (se ignora).
     * - YEARLY con intervalo 1 → intervalo 12: es un cliente antiguo que no conoce el campo y
     *   el valor por defecto 1 no significa "mensual".
     * - intervalo 12 → YEARLY; intervalo 1, 2, 3 o 6 → MONTHLY.
     *
     * @throws IllegalArgumentException si el intervalo no es uno de [VALORES_PERMITIDOS].
     */
    fun normalizar(ciclo: BillingCycle, intervaloMeses: Int): Pair<BillingCycle, Int> {
        // Se valida antes de mirar el ciclo: un 0 o un -1 es un dato corrupto incluso en WEEKLY.
        require(esValido(intervaloMeses)) {
            "Periodicidad no válida: $intervaloMeses (valores permitidos: ${VALORES_PERMITIDOS.joinToString(", ")})"
        }
        return when {
            ciclo == BillingCycle.WEEKLY -> BillingCycle.WEEKLY to 1
            ciclo == BillingCycle.YEARLY && intervaloMeses == 1 -> BillingCycle.YEARLY to MESES_EN_ANIO
            intervaloMeses == MESES_EN_ANIO -> BillingCycle.YEARLY to MESES_EN_ANIO
            else -> BillingCycle.MONTHLY to intervaloMeses
        }
    }

    /**
     * Periodicidad que hay que GUARDAR a partir de lo que envía un cliente (API o web), ya
     * normalizada. Existe para no romper las apps publicadas (≤ 2.17.8), que no conocen
     * `intervaloMeses` y siempre envían solo MONTHLY/YEARLY/WEEKLY:
     *
     * - Con [intervaloRecibido] → manda el intervalo y el ciclo se deriva de él (12 → YEARLY,
     *   1/2/3/6 → MONTHLY). Solo WEEKLY se respeta tal cual (su intervalo se valida y se ignora).
     * - Sin intervalo y editando ([guardado] != null) con el MISMO ciclo que el guardado → se
     *   conserva el intervalo guardado: una app antigua que edita un gasto trimestral recibe y
     *   reenvía MONTHLY, y no debe convertirlo en mensual.
     * - Sin intervalo en un alta, o con el ciclo cambiado → se deduce del ciclo
     *   (MONTHLY → 1, YEARLY → 12, WEEKLY → 1).
     *
     * @throws IllegalArgumentException si [intervaloRecibido] no es uno de [VALORES_PERMITIDOS]
     *   (la API y la web lo convierten en un 400).
     */
    fun resolver(
        cicloRecibido: BillingCycle,
        intervaloRecibido: Int?,
        guardado: Subscription? = null
    ): Pair<BillingCycle, Int> = when {
        intervaloRecibido != null && cicloRecibido == BillingCycle.WEEKLY ->
            normalizar(BillingCycle.WEEKLY, intervaloRecibido)
        // Se parte de MONTHLY para que el intervalo mande: YEARLY + 1 sería "anual" en
        // normalizar (regla de clientes antiguos), pero aquí el cliente SÍ ha enviado el 1.
        intervaloRecibido != null -> normalizar(BillingCycle.MONTHLY, intervaloRecibido)
        guardado != null && guardado.billingCycle == cicloRecibido ->
            normalizar(guardado.billingCycle, guardado.intervaloMeses)
        else -> normalizar(cicloRecibido, 1)
    }

    /**
     * Meses entre dos cobros de un gasto NO semanal, ya normalizado con [normalizar]
     * (p. ej. YEARLY con el intervalo por defecto 1 → 12).
     */
    fun mesesPorCobro(ciclo: BillingCycle, intervaloMeses: Int): Int {
        require(ciclo != BillingCycle.WEEKLY) { "Un gasto semanal no se cobra por meses" }
        return normalizar(ciclo, intervaloMeses).second
    }

    /** Equivalente mensual = importe / intervaloMeses, a escala interna (4 decimales). */
    fun equivalenteMensual(importe: BigDecimal, intervaloMeses: Int): BigDecimal {
        require(esValido(intervaloMeses)) { "Periodicidad no válida: $intervaloMeses" }
        return importe.divide(BigDecimal(intervaloMeses), ESCALA_INTERNA, REDONDEO)
    }

    /**
     * Total anual = importe × 12 / intervaloMeses, a escala interna (4 decimales).
     * Se multiplica ANTES de dividir: como todos los intervalos dividen a 12, el resultado es
     * exacto (90 € trimestral → 360 €, sin pasar por 30,0000 × 12).
     */
    fun totalAnual(importe: BigDecimal, intervaloMeses: Int): BigDecimal {
        require(esValido(intervaloMeses)) { "Periodicidad no válida: $intervaloMeses" }
        return importe.multiply(DOCE).divide(BigDecimal(intervaloMeses), ESCALA_INTERNA, REDONDEO)
    }

    /** Redondeo de una cifra final para mostrarla o devolverla: 2 decimales HALF_UP. */
    fun aCifraVisible(importe: BigDecimal): BigDecimal = importe.setScale(ESCALA_VISIBLE, REDONDEO)
}
