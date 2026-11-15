package com.subia.shared.model

import kotlin.math.abs

/**
 * Periodicidad de pago de un gasto (cada cuántos meses se cobra) y la regla ÚNICA de redondeo
 * del dinero en la app. Es la copia de `Periodicidad.kt` del backend: las cifras de la app y las
 * de la web/API deben coincidir al céntimo.
 *
 * ## Periodicidad
 * Cada suscripción trae `intervaloMeses`: 1 mensual, 2 bimestral, 3 trimestral, 6 semestral o
 * 12 anual ([VALORES_PERMITIDOS]). `billingCycle` llega derivado (12 → YEARLY, el resto →
 * MONTHLY). WEEKLY es aparte: el backend le guarda intervalo 1 y lo ignora. [mesesPorCobro]
 * es el único sitio que decide cada cuántos meses se cobra un gasto.
 *
 * ## Regla de redondeo (dinero)
 * El precio llega como `Double`, que no representa exactamente 0,1 ni 9,99: sumar y dividir
 * Doubles acumula errores (0,1 + 0,2 = 0,30000000000000004) y `kotlin.math.round` redondea
 * "a par" (2,5 → 2), no "hacia arriba" como el backend. Por eso:
 * - El importe se pasa a un entero ([Long]) de **diezmilésimas** (4 decimales) con
 *   [aDiezmilesimas], leyendo sus dígitos decimales: 9,99 € → 99 900.
 * - Los cálculos internos (equivalente mensual y total anual de cada gasto) se hacen con esos
 *   enteros y dividen con redondeo HALF_UP explícito ([dividirHalfUp]): 10 € / 3 → 33 333
 *   (3,3333 €).
 * - Las sumas se hacen sobre los internos, y solo la cifra FINAL que se muestra se redondea a
 *   2 decimales HALF_UP con [aCifraVisible]: 33 333 → 3,33 €; tres de ellos, 99 999 → 10,00 €.
 *
 * Diferencia conocida con el backend: allí el importe se divide con un solo redondeo; aquí se
 * redondea primero a 4 decimales y luego se divide. Con precios de 2 decimales (los que guarda
 * la base de datos, `DECIMAL(10,2)`) el resultado es idéntico.
 */
object Periodicidad {

    /** Intervalos admitidos (los mismos que exige el backend). */
    val VALORES_PERMITIDOS: Set<Int> = setOf(1, 2, 3, 6, 12)

    const val MESES_EN_ANIO = 12

    /** Decimales de los cálculos internos (diezmilésimas). */
    const val DECIMALES_INTERNOS = 4

    /** Decimales de las cifras que se muestran (céntimos). */
    const val DECIMALES_VISIBLES = 2

    /**
     * Mayor importe absoluto que se acepta en [aDiezmilesimas]. El backend limita el precio a
     * 99 999 999,99; este margen garantiza que importe × 10 000 × 365 cabe en un [Long].
     */
    const val IMPORTE_MAXIMO = 1_000_000_000_000.0

    private const val DIEZMILESIMAS_POR_UNIDAD = 10_000L
    private const val DIEZMILESIMAS_POR_CENTIMO = 100L

    /** `true` si [intervaloMeses] es uno de los [VALORES_PERMITIDOS]. */
    fun esValido(intervaloMeses: Int): Boolean = intervaloMeses in VALORES_PERMITIDOS

    /**
     * Ciclo que el backend deriva del intervalo: 12 → "YEARLY"; 1, 2, 3 y 6 → "MONTHLY".
     * Lo usa el formulario al elegir un intervalo (WEEKLY se elige aparte, con su propio selector).
     */
    fun cicloDeIntervalo(intervaloMeses: Int): String =
        if (intervaloMeses == MESES_EN_ANIO) "YEARLY" else "MONTHLY"

    /**
     * Meses entre dos cobros de un gasto, o `null` si se cobra por días o semanas
     * (WEEKLY, BIWEEKLY, DAILY), que siguen su cálculo propio.
     *
     * Interpretación robusta (igual que `Periodicidad.normalizar` del backend):
     * - YEARLY/ANNUAL con intervalo 1 → 12. Es un backend antiguo o una caché guardada antes de
     *   que existiera el campo: el 1 por defecto no significa "mensual".
     * - QUARTERLY (valor antiguo de la app) → 3.
     * - Intervalo fuera de [VALORES_PERMITIDOS] (dato corrupto) → el natural del ciclo
     *   (12 si es anual, 1 en otro caso), para no dividir entre 0 ni inventar periodicidades.
     * - En el resto (MONTHLY o ciclo desconocido) manda el intervalo.
     */
    fun mesesPorCobro(periodoFacturacion: String, intervaloMeses: Int): Int? {
        val periodo = periodoFacturacion.trim().uppercase()
        return when (periodo) {
            "WEEKLY", "BIWEEKLY", "DAILY" -> null
            "QUARTERLY" -> 3
            "YEARLY", "ANNUAL", "ANNUALLY" ->
                if (intervaloMeses == 1 || !esValido(intervaloMeses)) MESES_EN_ANIO else intervaloMeses
            else -> if (esValido(intervaloMeses)) intervaloMeses else 1
        }
    }

    /**
     * Pasa un importe a diezmilésimas con redondeo HALF_UP (5 o más hacia arriba, alejándose
     * del cero). Lee los dígitos decimales del número ([Double.toString] da la representación
     * decimal más corta que vuelve al mismo Double: 9,99 → "9.99"), así que no hereda el error
     * binario de multiplicar 9,99 × 10 000.
     *
     * @throws IllegalArgumentException si el importe no es finito o supera [IMPORTE_MAXIMO].
     */
    fun aDiezmilesimas(importe: Double): Long {
        require(importe.isFinite() && abs(importe) <= IMPORTE_MAXIMO) { "Importe fuera de rango: $importe" }
        val texto = importe.toString()
        val negativo = texto.startsWith('-')
        val sinSigno = texto.removePrefix("-")
        // Puede venir en notación científica ("1.0E-5" en JVM, "1e-7" en JS).
        val posExponente = sinSigno.indexOfFirst { it == 'E' || it == 'e' }
        val mantisa = if (posExponente >= 0) sinSigno.substring(0, posExponente) else sinSigno
        val exponente = if (posExponente >= 0) sinSigno.substring(posExponente + 1).removePrefix("+").toInt() else 0
        val decimales = mantisa.substringAfter('.', missingDelimiterValue = "")
        val digitos = mantisa.substringBefore('.') + decimales

        // valor = digitos × 10^(exponente − nº decimales); se quiere valor × 10^4.
        val desplazamiento = exponente - decimales.length + DECIMALES_INTERNOS
        val absoluto = if (desplazamiento >= 0) {
            digitos.toLong() * potenciaDeDiez(desplazamiento)
        } else {
            val quitar = -desplazamiento
            val conservados = digitos.dropLast(quitar)
            val base = if (conservados.isEmpty()) 0L else conservados.toLong()
            // HALF_UP solo necesita el primer dígito descartado: si es 5 o más, lo descartado es >= 0,5.
            val primerDescartado = if (quitar <= digitos.length) digitos[digitos.length - quitar] else '0'
            if (primerDescartado >= '5') base + 1 else base
        }
        return if (negativo) -absoluto else absoluto
    }

    /**
     * Equivalente mensual = importe / intervaloMeses, en diezmilésimas (HALF_UP).
     * Ej.: 10 € cada 3 meses → 33 333 (3,3333 €).
     */
    fun equivalenteMensual(importe: Double, intervaloMeses: Int): Long {
        require(esValido(intervaloMeses)) { "Periodicidad no válida: $intervaloMeses" }
        return dividirHalfUp(aDiezmilesimas(importe), intervaloMeses.toLong())
    }

    /**
     * Total anual = importe × 12 / intervaloMeses, en diezmilésimas (HALF_UP). Se multiplica
     * ANTES de dividir: como todos los intervalos dividen a 12 el resultado es exacto
     * (90 € trimestral → 360 €, sin pasar por 30 × 12).
     */
    fun totalAnual(importe: Double, intervaloMeses: Int): Long {
        require(esValido(intervaloMeses)) { "Periodicidad no válida: $intervaloMeses" }
        return dividirHalfUp(aDiezmilesimas(importe) * MESES_EN_ANIO, intervaloMeses.toLong())
    }

    /** Cifra final para mostrar: diezmilésimas → euros con 2 decimales HALF_UP (99 999 → 10.0). */
    fun aCifraVisible(diezmilesimas: Long): Double =
        dividirHalfUp(diezmilesimas, DIEZMILESIMAS_POR_CENTIMO) / 100.0

    /** Diezmilésimas → importe con sus 4 decimales, sin redondear (33 333 → 3.3333). */
    fun aImporte(diezmilesimas: Long): Double = diezmilesimas / DIEZMILESIMAS_POR_UNIDAD.toDouble()

    /**
     * Equivalente mensual interno (diezmilésimas) de un gasto, según su ciclo e intervalo.
     * Los ciclos por días/semanas mantienen su fórmula de siempre: semanal × 52 / 12,
     * quincenal × 26 / 12 y diario × 365 / 12.
     */
    fun equivalenteMensualInterno(precio: Double, periodoFacturacion: String, intervaloMeses: Int): Long {
        mesesPorCobro(periodoFacturacion, intervaloMeses)?.let { return equivalenteMensual(precio, it) }
        val cobrosAlAnio = cobrosAlAnioSinMeses(periodoFacturacion)
        return dividirHalfUp(aDiezmilesimas(precio) * cobrosAlAnio, MESES_EN_ANIO.toLong())
    }

    /**
     * Total anual interno (diezmilésimas) de un gasto. Para los ciclos por meses es
     * [totalAnual]; para los semanales, precio × 52 (antes era mensual × 12, que es lo mismo).
     */
    fun totalAnualInterno(precio: Double, periodoFacturacion: String, intervaloMeses: Int): Long {
        mesesPorCobro(periodoFacturacion, intervaloMeses)?.let { return totalAnual(precio, it) }
        return aDiezmilesimas(precio) * cobrosAlAnioSinMeses(periodoFacturacion)
    }

    /** Cobros al año de los ciclos que no van por meses. */
    private fun cobrosAlAnioSinMeses(periodoFacturacion: String): Long =
        when (periodoFacturacion.trim().uppercase()) {
            "WEEKLY" -> 52L
            "BIWEEKLY" -> 26L
            "DAILY" -> 365L
            else -> MESES_EN_ANIO.toLong()
        }

    /**
     * División entera con redondeo HALF_UP (la mitad se aleja del cero), como
     * `RoundingMode.HALF_UP` de Java. La división de [Long] de Kotlin trunca, por eso se mira
     * el resto: si es la mitad del divisor o más, se suma (o resta) uno.
     */
    internal fun dividirHalfUp(dividendo: Long, divisor: Long): Long {
        require(divisor > 0) { "Divisor no válido: $divisor" }
        val cociente = dividendo / divisor
        val resto = dividendo % divisor
        return when {
            abs(resto) * 2 < divisor -> cociente
            dividendo < 0 -> cociente - 1
            else -> cociente + 1
        }
    }

    private fun potenciaDeDiez(exponente: Int): Long {
        var resultado = 1L
        repeat(exponente) { resultado *= 10 }
        return resultado
    }
}

/** Meses entre cobros de esta suscripción, o `null` si es semanal/quincenal/diaria (ver [Periodicidad.mesesPorCobro]). */
fun Subscription.mesesPorCobro(): Int? = Periodicidad.mesesPorCobro(periodoFacturacion, intervaloMeses)

/** Equivalente mensual en diezmilésimas (para sumar sin perder céntimos; ver [Periodicidad]). */
fun Subscription.equivalenteMensualInterno(): Long =
    Periodicidad.equivalenteMensualInterno(precio, periodoFacturacion, intervaloMeses)

/** Total anual en diezmilésimas (importe × 12 / intervalo; semanal × 52). */
fun Subscription.totalAnualInterno(): Long =
    Periodicidad.totalAnualInterno(precio, periodoFacturacion, intervaloMeses)
