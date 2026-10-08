package com.subia.android.util

/**
 * Interruptores de funciones que existen en el código pero no se muestran todavía.
 */
object Funciones {
    /**
     * Detección de suscripciones leyendo Gmail. Pausada hasta que Google verifique el scope
     * `gmail.readonly`: con `false` desaparecen sus accesos (Ajustes, lista vacía) y se ignora
     * el deep link `subia://gmail`. La pantalla y el backend siguen intactos para reactivarla.
     */
    const val GMAIL = false
}
