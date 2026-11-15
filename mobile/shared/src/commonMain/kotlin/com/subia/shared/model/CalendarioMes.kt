package com.subia.shared.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Rejilla de un mes natural para el calendario "Este mes": semanas de lunes a domingo, con
 * huecos vacíos antes del día 1 y después del último. Tiene de 4 filas (febrero de 28 días que
 * empieza en lunes) a 6 (un mes de 30 o 31 días que empieza en sábado o domingo).
 *
 * @property huecosIniciales celdas vacías antes del día 1 (0 si empieza en lunes, 6 si en domingo).
 * @property diasDelMes número de días del mes (28 a 31).
 */
data class CuadriculaMes(val anio: Int, val mes: Int, val huecosIniciales: Int, val diasDelMes: Int) {

    /** Semanas que ocupa el mes (4 a 6). */
    val filas: Int get() = (huecosIniciales + diasDelMes + DIAS_SEMANA - 1) / DIAS_SEMANA

    /** Celdas vacías tras el último día para completar la última semana (0 a 6). */
    val huecosFinales: Int get() = filas * DIAS_SEMANA - huecosIniciales - diasDelMes

    /** Filas de 7 celdas: el número del día (1..[diasDelMes]) o `null` en los huecos. */
    fun semanas(): List<List<Int?>> = (0 until filas).map { fila ->
        (0 until DIAS_SEMANA).map { columna ->
            (fila * DIAS_SEMANA + columna - huecosIniciales + 1).takeIf { it in 1..diasDelMes }
        }
    }

    companion object {
        const val DIAS_SEMANA = 7

        /** Rejilla del mes [mes] (1..12) de [anio]. */
        fun de(anio: Int, mes: Int): CuadriculaMes {
            val primero = LocalDate(anio, mes, 1)
            val ultimo = primero.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            // isoDayNumber: lunes = 1 ... domingo = 7.
            return CuadriculaMes(anio, mes, primero.dayOfWeek.isoDayNumber - 1, ultimo.dayOfMonth)
        }

        /** Rejilla del mes de [fecha]. */
        fun de(fecha: LocalDate): CuadriculaMes = de(fecha.year, fecha.monthNumber)
    }
}

/**
 * Día que el calendario enseña desplegado si el usuario no ha tocado ninguno: el próximo día
 * con cobros a partir de [hoy] (hoy incluido). `null` si ya no quedan cobros en [dias].
 */
fun diaPorDefectoCalendario(dias: List<DiaCobros>, hoy: LocalDate): LocalDate? =
    dias.firstOrNull { it.tieneCobros && it.fecha >= hoy }?.fecha
