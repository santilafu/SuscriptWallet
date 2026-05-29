package com.subia.model

/**
 * Catálogo de divisas soportadas por la aplicación.
 *
 * Se incluye una selección de las divisas más usadas a nivel mundial (no todas las ISO 4217),
 * con su símbolo de visualización. El código ISO se almacena en [Subscription.currency].
 */
object Currencies {

    /** Divisas soportadas: código ISO 4217 → símbolo de visualización. Orden = orden en los selectores. */
    val SUPPORTED: Map<String, String> = linkedMapOf(
        "EUR" to "€",
        "USD" to "$",
        "GBP" to "£",
        "JPY" to "¥",
        "CHF" to "CHF",
        "CAD" to "C$",
        "AUD" to "A$",
        "CNY" to "CN¥",
        "MXN" to "MX$",
        "BRL" to "R$",
        "INR" to "₹"
    )

    /** Símbolo de una divisa; si no está soportada, devuelve el propio código. */
    fun symbol(code: String): String = SUPPORTED[code] ?: code
}
