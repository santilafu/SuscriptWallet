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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

/** Campos del formulario que pueden llevar un error asociado (validación en vivo). */
enum class Campo { Nombre, Precio, FechaRenovacion, Categoria }

sealed interface FormUiState {
    data object Idle : FormUiState
    data object Loading : FormUiState
    data object Success : FormUiState
    data class Error(val error: FormError) : FormUiState
}

/**
 * ViewModel para crear y editar suscripciones.
 *
 * - Valida en vivo: [errores] expone un mapa campo → error recalculado con cada tecla y
 *   [puedeGuardar] es `true` solo cuando el mapa está vacío.
 * - Autocompleta desde el catálogo: [sugerencias] filtra el catálogo por el texto del nombre
 *   para que la UI lo muestre como desplegable; [seleccionarServicioDelCatalogo] prerrellena.
 * - Detecta cambios sin guardar: [hayCambios] compara una huella de los campos con la que se
 *   fijó al abrir el formulario (vacío, edición o prerrelleno).
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

    private val _categorias = MutableStateFlow<List<Category>>(emptyList())
    /** Categorías disponibles para el desplegable de categoría. */
    val categorias: StateFlow<List<Category>> = _categorias.asStateFlow()

    private val _catalogo = MutableStateFlow<List<CatalogItem>>(emptyList())
    /** Catálogo completo (se carga una vez; si falla la red, el autocompletado queda vacío). */
    val catalogo: StateFlow<List<CatalogItem>> = _catalogo.asStateFlow()

    private val _catalogoSeleccionado = MutableStateFlow<CatalogItem?>(null)
    /** Último servicio elegido del catálogo (para pintar su logo junto al nombre). */
    val catalogoSeleccionado: StateFlow<CatalogItem?> = _catalogoSeleccionado.asStateFlow()

    // ── Validación en vivo ─────────────────────────────────────────────────

    /** Errores por campo, recalculados con cada cambio. Vacío cuando todo es válido. */
    val errores: StateFlow<Map<Campo, FormError>> = combine(
        nombre, precio, fechaRenovacion, categoriaId
    ) { n, p, f, c -> validarCampos(n, p, f, c) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, validarCampos("", "", "", null))

    /** `true` cuando no hay errores de validación. El botón Guardar se habilita con esto. */
    val puedeGuardar: StateFlow<Boolean> = errores.map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Autocompletado desde el catálogo ───────────────────────────────────

    /**
     * Servicios del catálogo cuyo nombre contiene el texto escrito (máx. [MAX_SUGERENCIAS]).
     * Vacío si el campo está en blanco o si el texto coincide exactamente con el servicio ya
     * elegido (para no reabrir el desplegable justo después de seleccionar).
     */
    val sugerencias: StateFlow<List<CatalogItem>> = combine(
        nombre, _catalogo, _catalogoSeleccionado
    ) { texto, items, elegido -> filtrarCatalogo(texto, items, elegido) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── Cambios sin guardar ────────────────────────────────────────────────

    private val huellaInicial = MutableStateFlow(huella())

    @Suppress("UNCHECKED_CAST")
    private val huellaActual: Flow<String> = combine(
        listOf(
            nombre, descripcion, precio, moneda, periodoFacturacion,
            fechaRenovacion, categoriaId, notas, esPrueba, fechaFinPrueba
        ) as List<Flow<Any?>>
    ) { valores -> valores.joinToString("\u0001") }

    /** `true` si algún campo difiere de lo que había al abrir el formulario. */
    val hayCambios: StateFlow<Boolean> = combine(huellaActual, huellaInicial) { actual, inicial ->
        actual != inicial
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            categoryRepository.getAll().onSuccess { _categorias.value = it }
        }
        viewModelScope.launch {
            catalogRepository.getAll().onSuccess { _catalogo.value = it }
        }
    }

    /** Fija el estado actual como "sin cambios" (tras cargar para editar o prerrellenar). */
    fun marcarComoSinCambios() {
        huellaInicial.value = huella()
    }

    private fun huella(): String = listOf(
        nombre.value, descripcion.value, precio.value, moneda.value, periodoFacturacion.value,
        fechaRenovacion.value, categoriaId.value, notas.value, esPrueba.value, fechaFinPrueba.value
    ).joinToString("\u0001")

    /**
     * Aplica un servicio del catálogo al formulario: nombre, precio, ciclo, moneda, prueba y
     * categoría (buscando la categoría del usuario cuyo nombre coincide con
     * [CatalogItem.categoriaKey], sin distinguir mayúsculas ni acentos).
     */
    fun seleccionarServicioDelCatalogo(item: CatalogItem) {
        prerellenarDesdeCatalogo(item, BillingCycle.fromWire(item.periodoFacturacion))
        _catalogoSeleccionado.value = item

        val categoriaEncontrada = buscarCategoriaDeClave(item.categoriaKey, _categorias.value)
        categoriaEncontrada?.let { categoriaId.value = it.id }
    }

    /**
     * Prerrellena buscando en el catálogo por nombre (p. ej. desde el empty state de la lista).
     * Pone el nombre al instante y, cuando el catálogo esté cargado, completa el resto.
     */
    fun prerellenarPorNombre(nombreServicio: String) {
        nombre.value = nombreServicio
        marcarComoSinCambios()
        viewModelScope.launch {
            val items = _catalogo.first { it.isNotEmpty() }
            val item = items.firstOrNull { it.nombre.equals(nombreServicio, ignoreCase = true) }
                ?: items.firstOrNull { it.nombre.contains(nombreServicio, ignoreCase = true) }
            if (item != null) {
                // Espera a las categorías (si llegan) para poder asignar la del servicio.
                if (_categorias.value.isEmpty()) {
                    runCatching { _categorias.first { it.isNotEmpty() } }
                }
                seleccionarServicioDelCatalogo(item)
                marcarComoSinCambios()
            }
        }
    }

    private val _cargandoEdicion = MutableStateFlow(false)
    /** `true` mientras se descarga la suscripción a editar (la UI bloquea el formulario). */
    val cargandoEdicion: StateFlow<Boolean> = _cargandoEdicion.asStateFlow()

    /**
     * Modo edición por id: descarga la suscripción y precarga los campos. Si falla, deja el
     * formulario vacío y expone [FormError.SinConexion]/[FormError.GuardadoFallido] para que
     * la UI avise (el usuario puede reintentar volviendo a entrar).
     */
    fun cargarParaEditar(id: Long) {
        if (_cargandoEdicion.value) return
        viewModelScope.launch {
            _cargandoEdicion.value = true
            subscriptionRepository.getById(id)
                .onSuccess { cargarParaEditar(it) }
                .onFailure { _uiState.value = FormUiState.Error(mapearErrorGuardado(it)) }
            _cargandoEdicion.value = false
        }
    }

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
        marcarComoSinCambios()
    }

    /**
     * Precarga nombre, precio y periodo desde un ítem del catálogo según el ciclo elegido.
     *
     * El importe sale de [CatalogItem.precioPara], que respeta el ciclo propio del servicio
     * (un seguro anual de 450 € no se convierte en 5400 €/año).
     */
    fun prerellenarDesdeCatalogo(item: CatalogItem, cicloElegido: BillingCycle) {
        nombre.value = item.nombre
        val precioElegido = item.precioPara(cicloElegido)
        precioElegido?.let { precio.value = formatearPrecioParaCampo(it) }
        periodoFacturacion.value = cicloElegido.wire
        moneda.value = item.moneda
        _catalogoSeleccionado.value = item
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

    /** Establece la categoría de la suscripción desde el desplegable del formulario. */
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
                .onSuccess {
                    marcarComoSinCambios()
                    SuscripcionesCambios.notificar()
                    _uiState.value = FormUiState.Success
                }
                .onFailure { error -> _uiState.value = FormUiState.Error(mapearErrorGuardado(error)) }
        }
    }

    fun resetState() { _uiState.value = FormUiState.Idle }

    companion object {
        const val MAX_SUGERENCIAS = 8

        /**
         * Convierte el texto del importe a número aceptando coma o punto decimal y espacios.
         * Si aparecen ambos separadores, el último es el decimal ("1.234,56" → 1234.56;
         * "1,234.56" → 1234.56). Devuelve `null` si no es un número.
         */
        fun parsearPrecio(texto: String): Double? {
            val limpio = texto.trim().replace(" ", "").replace(" ", "")
            if (limpio.isEmpty()) return null
            val ultimaComa = limpio.lastIndexOf(',')
            val ultimoPunto = limpio.lastIndexOf('.')
            val normalizado = when {
                ultimaComa >= 0 && ultimoPunto >= 0 ->
                    if (ultimaComa > ultimoPunto) limpio.replace(".", "").replace(',', '.')
                    else limpio.replace(",", "")
                ultimaComa >= 0 -> limpio.replace(',', '.')
                else -> limpio
            }
            return normalizado.toDoubleOrNull()
        }

        /** Texto del importe para el campo: sin ".0" sobrante ("9.99", "12"). */
        fun formatearPrecioParaCampo(valor: Double): String {
            val redondeado = kotlin.math.round(valor * 100) / 100
            return if (redondeado == kotlin.math.floor(redondeado)) redondeado.toLong().toString()
            else redondeado.toString()
        }

        /** Validación por campo: devuelve un mapa con el error de cada campo inválido. */
        fun validarCampos(nombre: String, precio: String, fechaRenovacion: String, categoriaId: Long?): Map<Campo, FormError> {
            val errores = linkedMapOf<Campo, FormError>()
            if (nombre.isBlank()) errores[Campo.Nombre] = FormError.NombreVacio
            val precioDouble = parsearPrecio(precio)
            if (precioDouble == null || precioDouble <= 0) errores[Campo.Precio] = FormError.PrecioInvalido
            if (fechaRenovacion.isBlank()) errores[Campo.FechaRenovacion] = FormError.FechaRenovacionVacia
            if (categoriaId == null || categoriaId == 0L) errores[Campo.Categoria] = FormError.CategoriaNoSeleccionada
            return errores
        }

        /**
         * Validación pura del formulario. Devuelve el primer [FormError] encontrado
         * (en el orden visual de los campos) o `null` si todo es válido.
         */
        fun validar(nombre: String, precio: String, fechaRenovacion: String, categoriaId: Long?): FormError? =
            validarCampos(nombre, precio, fechaRenovacion, categoriaId).values.firstOrNull()

        /** Traduce la excepción del repositorio al error tipado que entiende la UI. */
        fun mapearErrorGuardado(error: Throwable): FormError = when (error) {
            is NetworkException -> FormError.SinConexion
            else -> FormError.GuardadoFallido
        }

        /** Filtro del autocompletado (función pura para poder probarla). */
        fun filtrarCatalogo(texto: String, items: List<CatalogItem>, elegido: CatalogItem?): List<CatalogItem> {
            val consulta = texto.trim()
            if (consulta.isBlank()) return emptyList()
            if (elegido != null && elegido.nombre.equals(consulta, ignoreCase = true)) return emptyList()
            val normalizada = consulta.normalizar()
            return items
                .filter { it.nombre.normalizar().contains(normalizada) }
                // El catálogo repite servicios en "prueba" (pruebas gratis) y en su categoría real:
                // en el desplegable salían dos "Spotify Premium" idénticos. Se queda uno por
                // nombre, preferiendo el de la categoría real (la que se asignará al elegirlo).
                .groupBy { it.nombre.normalizar() }
                .map { (_, mismos) -> mismos.firstOrNull { it.categoriaKey != "prueba" } ?: mismos.first() }
                .sortedWith(compareBy({ !it.nombre.normalizar().startsWith(normalizada) }, { it.nombre }))
                .take(MAX_SUGERENCIAS)
        }

        /**
         * Nombre sembrado de la categoría → clave del catálogo. Copia de
         * `CategoryMappingService.NAME_TO_KEY` del backend: "Hogar y suministros" no se parece a
         * "hogar" ni "Telecomunicaciones" a "telecos", así que comparar nombres no basta.
         */
        private val NOMBRE_A_CLAVE: Map<String, String> = mapOf(
            "ia" to "ia", "streaming" to "streaming", "musica" to "musica", "software" to "software",
            "cloud" to "cloud", "gaming" to "gaming", "seguridad" to "seguridad",
            "noticias y lectura" to "noticias", "salud y deporte" to "salud", "desarrollo" to "desarrollo",
            "prueba gratuita" to "prueba", "finanzas" to "finanzas", "educacion" to "educacion",
            "creatividad y foto" to "creatividad", "citas y social" to "citas",
            "hogar y suministros" to "hogar", "seguros" to "seguros",
            "telecomunicaciones" to "telecos", "transporte" to "transporte"
        )

        /**
         * Categoría del usuario que corresponde a la clave del catálogo: por el mapeo de nombres
         * sembrados o, si el usuario la creó a mano, por nombre igual a la clave.
         */
        fun buscarCategoriaDeClave(clave: String, categorias: List<Category>): Category? {
            val buscada = clave.trim().normalizar()
            if (buscada.isEmpty()) return null
            return categorias.firstOrNull { NOMBRE_A_CLAVE[it.nombre.trim().normalizar()] == buscada }
                ?: categorias.firstOrNull { it.nombre.trim().normalizar() == buscada }
        }

        /** Minúsculas y sin acentos, para comparar nombres y claves de categoría. */
        private fun String.normalizar(): String =
            lowercase()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u').replace('ñ', 'n')
    }
}
