package com.subia.android.ui.components

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formatea un importe con el símbolo de su divisa según el locale del usuario
 * ("47,96 €", "$12.00"). Si el código de divisa no es ISO válido, cae a "valor CÓDIGO"
 * para no tirar la pantalla por un dato raro del servidor.
 */
fun formatearImporte(valor: Double, moneda: String = "EUR", decimales: Int = 2): String {
    val locale = Locale.getDefault()
    return runCatching {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(moneda)
            minimumFractionDigits = decimales
            maximumFractionDigits = decimales
        }.format(valor)
    }.getOrElse { "%.${decimales}f %s".format(locale, valor, moneda) }
}
