package com.subia.shared.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Señal compartida "las suscripciones han cambiado". Cada pantalla tiene su propia instancia
 * de ViewModel (ámbito de la entrada de navegación), así que el formulario y el detalle no
 * pueden hablar directamente con la lista: notifican aquí y la lista recarga en silencio.
 *
 * Sustituye al patrón anterior de invalidar la caché y recargar en cada `RESUMED`, que hacía
 * parpadear la lista al volver del detalle aunque nada hubiera cambiado.
 */
object SuscripcionesCambios {
    private val _version = MutableStateFlow(0L)
    /** Contador monótono; cada cambio (crear, editar, eliminar) lo incrementa. */
    val version: StateFlow<Long> = _version.asStateFlow()

    fun notificar() {
        _version.value = _version.value + 1
    }
}
