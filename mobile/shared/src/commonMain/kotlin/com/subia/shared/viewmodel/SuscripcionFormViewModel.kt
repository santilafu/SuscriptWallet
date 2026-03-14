package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.CatalogItem
import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Subscription
import com.subia.shared.network.NetworkException
import com.subia.shared.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FormUiState {
    data object Idle : FormUiState
    data object Loading : FormUiState
    data object Success : FormUiState
    data class Error(val mensaje: String) : FormUiState
}

/**
 * ViewModel para crear y editar suscripciones.
 * Gestiona los campos del formulario y la validación antes de enviar al servidor.
 */
class SuscripcionFormViewModel(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FormUiState>(FormUiState.Idle)
    val uiState: StateFlow<FormUiState> = _uiState.asStateFlow()

    val nombre = MutableStateFlow("")
    val descripcion = MutableStateFlow("")
    val precio = MutableStateFlow("")
    val moneda = MutableStateFlow("EUR")
    val periodoFacturacion = MutableStateFlow("MONTHLY")
    val fechaRenovacion = MutableStateFlow("")
    val categoriaId = MutableStateFlow<Long?>(null)
    val notas = MutableStateFlow("")

    /** Precarga los campos a partir de una suscripción existente (modo edición). */
    fun cargarParaEditar(suscripcion: Subscription) {
        nombre.value = suscripcion.nombre
        descripcion.value = suscripcion.descripcion
        precio.value = suscripcion.precio.toString()
        moneda.value = suscripcion.moneda
        periodoFacturacion.value = suscripcion.periodoFacturacion
        fechaRenovacion.value = suscripcion.fechaRenovacion
        categoriaId.value = suscripcion.categoriaId
        notas.value = suscripcion.notas
    }

    /** Precarga nombre, precio y categoría desde un ítem del catálogo. */
    fun prerellenarDesdeCatalogo(item: CatalogItem) {
        nombre.value = item.nombre
        item.precioMensual?.let { precio.value = it.toString(); periodoFacturacion.value = "MONTHLY" }
            ?: item.precioAnual?.let { precio.value = it.toString(); periodoFacturacion.value = "YEARLY" }
        item.categoriaId?.let { categoriaId.value = it }
    }

    /** Envía el formulario. Si [esEdicion] es true realiza PUT, si no POST. */
    fun enviar(esEdicion: Boolean, id: Long? = null) {
        val precioDouble = precio.value.replace(",", ".").toDoubleOrNull()

        when {
            nombre.value.isBlank() -> {
                _uiState.value = FormUiState.Error("El nombre del servicio es obligatorio")
                return
            }
            precioDouble == null || precioDouble <= 0 -> {
                _uiState.value = FormUiState.Error("Introduce un importe válido mayor que cero")
                return
            }
            fechaRenovacion.value.isBlank() -> {
                _uiState.value = FormUiState.Error("La fecha de renovación es obligatoria")
                return
            }
        }

        val request = NuevaSuscripcionRequest(
            nombre = nombre.value.trim(),
            descripcion = descripcion.value.trim(),
            precio = precioDouble!!,
            moneda = moneda.value,
            periodoFacturacion = periodoFacturacion.value,
            fechaRenovacion = fechaRenovacion.value,
            categoriaId = categoriaId.value,
            notas = notas.value.trim()
        )

        viewModelScope.launch {
            _uiState.value = FormUiState.Loading
            val result = if (esEdicion && id != null) {
                subscriptionRepository.update(id, request)
            } else {
                subscriptionRepository.create(request)
            }
            result
                .onSuccess { _uiState.value = FormUiState.Success }
                .onFailure { error ->
                    val mensaje = when (error) {
                        is NetworkException -> "Sin conexión. No es posible guardar cambios sin conexión"
                        else -> "No se ha podido guardar la suscripción. Inténtalo de nuevo"
                    }
                    _uiState.value = FormUiState.Error(mensaje)
                }
        }
    }

    fun resetState() { _uiState.value = FormUiState.Idle }
}
