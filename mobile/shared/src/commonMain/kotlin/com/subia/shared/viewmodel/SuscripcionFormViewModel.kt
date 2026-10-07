package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.BillingCycle
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Error tipado del formulario. El ViewModel no conoce idiomas: cada plataforma
 * traduce el caso a su recurso localizado (en Android, `R.string`).
 */
sealed interface FormError {
    /** El nombre del servicio está vacío. */
    data object NombreVacio : FormError
    /** El importe no es un número o no es mayor que cero. */
    data object PrecioInvalido : FormError
    /** No se ha elegido fecha de renovación. */
    data object FechaRenovacionVacia : FormError
    /** No se ha elegido categoría. */
    data object CategoriaNoSeleccionada : FormError
    /** Fallo de red al guardar. */
    data object SinConexion : FormError
    /** Cualquier otro fallo del servidor al guardar. */
    data object GuardadoFallido : FormError
}

sealed interface FormUiState {
    data object Idle : FormUiState
    data object Loading : FormUiState
    data object Success : FormUiState
    data class Error(val error: FormError) : FormUiState
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
    val esPrueba = MutableStateFlow(false)
    val fechaFinPrueba = MutableStateFlow<String?>(null)

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
        prerellenarDesdeCatalogo(item, BillingCycle.fromWire(item.periodoFacturacion))

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
        esPrueba.value = suscripcion.esPrueba
        fechaFinPrueba.value = suscripcion.fechaFinPrueba
    }

    /**
     * Precarga nombre, precio y periodo desde un ítem del catálogo según el ciclo elegido.
     *
     * Para [BillingCycle.MONTHLY] usa [CatalogItem.precioMensual]; si no existe pero hay
     * precio anual, cae a `precioAnual / 12`. Para [BillingCycle.YEARLY] usa
     * [CatalogItem.precioAnual]; si no existe pero hay mensual, cae a `precioMensual * 12`.
     */
    fun prerellenarDesdeCatalogo(item: CatalogItem, cicloElegido: BillingCycle) {
        nombre.value = item.nombre
        val precioElegido: Double? = when (cicloElegido) {
            BillingCycle.MONTHLY -> item.precioMensual ?: item.precioAnual?.div(12.0)
            BillingCycle.YEARLY -> item.precioAnual ?: item.precioMensual?.times(12.0)
        }
        precioElegido?.let { precio.value = it.toString() }
        periodoFacturacion.value = cicloElegido.wire
        moneda.value = item.moneda
        val diasPrueba = item.diasPrueba
        if (diasPrueba != null) {
            esPrueba.value = true
            val hoy = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            fechaFinPrueba.value = (hoy + DatePeriod(days = diasPrueba)).toString()
        }
    }

    /** Establece la fecha de fin de prueba desde el selector de fecha. */
    fun seleccionarFechaFinPrueba(fecha: String) {
        fechaFinPrueba.value = fecha
    }

    /** Establece la categoría de la suscripción desde el desplegable standalone del formulario. */
    fun seleccionarCategoria(id: Long) {
        categoriaId.value = id
    }

    /** Envía el formulario. Si [esEdicion] es true realiza PUT, si no POST. */
    fun enviar(esEdicion: Boolean, id: Long? = null) {
        val precioDouble = parsearPrecio(precio.value)

        validar(nombre.value, precio.value, fechaRenovacion.value, categoriaId.value)?.let { error ->
            _uiState.value = FormUiState.Error(error)
            return
        }

        val request = NuevaSuscripcionRequest(
            nombre = nombre.value.trim(),
            descripcion = descripcion.value.trim(),
            precio = precioDouble!!,
            moneda = moneda.value,
            periodoFacturacion = periodoFacturacion.value,
            fechaRenovacion = fechaRenovacion.value,
            categoriaId = categoriaId.value,
            notas = notas.value.trim(),
            esPrueba = esPrueba.value,
            fechaFinPrueba = if (esPrueba.value) fechaFinPrueba.value else null
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
                .onFailure { error -> _uiState.value = FormUiState.Error(mapearErrorGuardado(error)) }
        }
    }

    fun resetState() { _uiState.value = FormUiState.Idle }

    companion object {
        /** Convierte el texto del importe a número aceptando coma o punto decimal. */
        fun parsearPrecio(texto: String): Double? = texto.replace(",", ".").toDoubleOrNull()

        /**
         * Validación pura del formulario. Devuelve el primer [FormError] encontrado
         * (en el orden visual de los campos) o `null` si todo es válido.
         */
        fun validar(nombre: String, precio: String, fechaRenovacion: String, categoriaId: Long?): FormError? {
            val precioDouble = parsearPrecio(precio)
            return when {
                nombre.isBlank() -> FormError.NombreVacio
                precioDouble == null || precioDouble <= 0 -> FormError.PrecioInvalido
                fechaRenovacion.isBlank() -> FormError.FechaRenovacionVacia
                categoriaId == null || categoriaId == 0L -> FormError.CategoriaNoSeleccionada
                else -> null
            }
        }

        /** Traduce la excepción del repositorio al error tipado que entiende la UI. */
        fun mapearErrorGuardado(error: Throwable): FormError = when (error) {
            is NetworkException -> FormError.SinConexion
            else -> FormError.GuardadoFallido
        }
    }
}
