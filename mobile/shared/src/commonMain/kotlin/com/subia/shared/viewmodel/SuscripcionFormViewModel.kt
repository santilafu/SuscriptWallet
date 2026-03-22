package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.CatalogItem
import com.subia.shared.model.Category
import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Subscription
import com.subia.shared.network.NetworkException
import com.subia.shared.repository.CatalogRepository
import com.subia.shared.repository.CategoryRepository
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
 * Incluye soporte para el selector de catálogo en línea (categoría → servicio).
 */
class SuscripcionFormViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val catalogRepository: CatalogRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FormUiState>(FormUiState.Idle)
    val uiState: StateFlow<FormUiState> = _uiState.asStateFlow()

    val nombre = MutableStateFlow("")
    val descripcion = MutableStateFlow("")
    val precio = MutableStateFlow("")
    val moneda = MutableStateFlow("EUR")
    val periodoFacturacion = MutableStateFlow("MONTHLY")
    val fechaRenovacion = MutableStateFlow("")
    /** Id de la categoría asignada a la suscripción (campo real del formulario). */
    val categoriaId = MutableStateFlow<Long?>(null)
    val notas = MutableStateFlow("")

    // ── Selector de catálogo en línea ──────────────────────────────────────

    private val _categorias = MutableStateFlow<List<Category>>(emptyList())
    /** Lista de categorías disponibles para el selector de catálogo. */
    val categorias: StateFlow<List<Category>> = _categorias.asStateFlow()

    private val _serviciosPorCategoria = MutableStateFlow<List<CatalogItem>>(emptyList())
    /** Servicios del catálogo correspondientes a la categoría seleccionada en el selector. */
    val serviciosPorCategoria: StateFlow<List<CatalogItem>> = _serviciosPorCategoria.asStateFlow()

    /**
     * Id de la categoría elegida en el selector de catálogo (UI únicamente).
     * No confundir con [categoriaId], que es el campo real de la suscripción.
     */
    val categoriaSeleccionadaId = MutableStateFlow<Long?>(null)

    private val _cargandoServicios = MutableStateFlow(false)
    /** Indica si se están cargando los servicios del catálogo para la categoría elegida. */
    val cargandoServicios: StateFlow<Boolean> = _cargandoServicios.asStateFlow()

    init {
        cargarCategorias()
    }

    /** Carga la lista de categorías al inicializar el ViewModel. */
    private fun cargarCategorias() {
        viewModelScope.launch {
            categoryRepository.getAll()
                .onSuccess { _categorias.value = it }
        }
    }

    /**
     * Selecciona una categoría en el selector de catálogo y carga sus servicios.
     * Pasar `null` limpia la selección y la lista de servicios.
     */
    fun seleccionarCategoriaDelSelector(categoriaId: Long?) {
        categoriaSeleccionadaId.value = categoriaId
        _serviciosPorCategoria.value = emptyList()
        if (categoriaId == null) return

        viewModelScope.launch {
            _cargandoServicios.value = true
            catalogRepository.getByCategory(categoriaId)
                .onSuccess { _serviciosPorCategoria.value = it }
            _cargandoServicios.value = false
        }
    }

    /**
     * Aplica el servicio elegido en el selector de catálogo al formulario.
     * Llama a [prerellenarDesdeCatalogo] y además asigna la [categoriaId] de la suscripción
     * buscando la categoría cuyo nombre coincida con [CatalogItem.categoriaKey] (sin distinguir
     * mayúsculas ni acentos). Si no hay coincidencia exacta se usa la primera categoría disponible.
     * También actualiza [categoriaSeleccionadaId] para que el desplegable de la UI refleje la
     * selección.
     */
    fun seleccionarServicioDelCatalogo(item: CatalogItem) {
        prerellenarDesdeCatalogo(item)

        // Normaliza una cadena eliminando acentos y pasándola a minúsculas para comparar
        fun String.normalizar(): String =
            this.lowercase()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u').replace('ñ', 'n')

        val claveBuscada = item.categoriaKey.normalizar()
        val categoriaEncontrada = _categorias.value.firstOrNull { cat ->
            cat.nombre.normalizar() == claveBuscada
        } ?: _categorias.value.firstOrNull()

        categoriaEncontrada?.let { cat ->
            categoriaId.value = cat.id
            categoriaSeleccionadaId.value = cat.id
        }
    }

    // ── Operaciones existentes ─────────────────────────────────────────────

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
        item.precioMensual?.let {
            precio.value = it.toString()
            periodoFacturacion.value = item.periodoFacturacion.ifBlank { "MONTHLY" }
        }
        moneda.value = item.moneda
    }

    /** Establece la categoría de la suscripción desde el desplegable standalone del formulario. */
    fun seleccionarCategoria(id: Long) {
        categoriaId.value = id
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
            categoriaId.value == null || categoriaId.value == 0L -> {
                _uiState.value = FormUiState.Error("Selecciona una categoría")
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
