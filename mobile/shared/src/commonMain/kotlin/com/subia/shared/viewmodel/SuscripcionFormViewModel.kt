package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.BillingCycle
import com.subia.shared.model.CatalogItem
import com.subia.shared.model.Category
import com.subia.shared.model.NuevaSuscripcionRequest
import com.subia.shared.model.Periodicidad
import com.subia.shared.model.Subscription
import com.subia.shared.model.mesesPorCobro
import com.subia.shared.network.NetworkException
import com.subia.shared.repository.CatalogRepository
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.SubscriptionRepository
import kotlinx.coroutines.Job
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
import com.subia.shared.model.proximaRenovacion
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus

/**
 * Error tipado del formulario. El ViewModel no conoce idiomas: cada plataforma
 * traduce el caso a su recurso localizado (en Android, `R.string`).
 */
sealed interface FormError {
    /** El nombre del servicio está vacío. */
    data object NombreVacio : FormError
    /** El importe no es un número o es negativo. */
    data object PrecioInvalido : FormError
    /** No se ha elegido fecha de renovación. */
    data object FechaRenovacionVacia : FormError
    /** No se ha elegido categoría. */
    data object CategoriaNoSeleccionada : FormError
    /** "Periodo de prueba" activado sin fecha de fin (la web también la exige). */
    data object FechaFinPruebaVacia : FormError
    /** Fallo de red al guardar. */
    data object SinConexion : FormError
    /** Cualquier otro fallo del servidor al guardar. */
    data object GuardadoFallido : FormError
}

/** Campos del formulario que pueden llevar un error asociado (validación en vivo). */
enum class Campo { Nombre, Precio, FechaRenovacion, FechaFinPrueba, Categoria }

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
    private val categoryRepository: CategoryRepository,
    /** "Hoy" del móvil (inyectable en tests). */
    private val hoy: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
) : ViewModel() {

    private val _uiState = MutableStateFlow<FormUiState>(FormUiState.Idle)
    val uiState: StateFlow<FormUiState> = _uiState.asStateFlow()

    val nombre = MutableStateFlow("")
    val descripcion = MutableStateFlow("")
    val precio = MutableStateFlow("")
    val moneda = MutableStateFlow("EUR")
    val periodoFacturacion = MutableStateFlow("MONTHLY")
    /**
     * Cada cuántos meses se cobra (1, 2, 3, 6 o 12). Para cambiarlo usa [seleccionarIntervalo],
     * que deriva también [periodoFacturacion] (12 → YEARLY, el resto → MONTHLY). Si la UI cambia
     * [periodoFacturacion] directamente (selector antiguo), lo que se envía y los valores en vivo
     * usan [intervaloCoherente], así que nunca viaja una pareja contradictoria (YEARLY + 1).
     */
    val intervaloMeses = MutableStateFlow(1)
    val fechaRenovacion = MutableStateFlow("")
    /** Id de la categoría asignada a la suscripción (campo real del formulario). */
    val categoriaId = MutableStateFlow<Long?>(null)
    val notas = MutableStateFlow("")
    val esPrueba = MutableStateFlow(false)
    val fechaFinPrueba = MutableStateFlow<String?>(null)
    /** Plazo opcional (préstamos, financiaciones): ISO `yyyy-MM-dd` o `null` si no tiene. */
    val fechaInicio = MutableStateFlow<String?>(null)
    val fechaFin = MutableStateFlow<String?>(null)

    /**
     * Estado activo/inactivo de la suscripción en edición. El formulario no lo muestra, pero hay
     * que devolverlo en el PUT: si no, el backend la reactivaba (su default es `true`).
     */
    private var activa: Boolean = true

    /**
     * R3-01: ancla guardada en el servidor y fecha/periodo mostrados al abrir la edición. Si el
     * usuario guarda sin tocar la fecha ni el periodo se reenvía el ANCLA, no la próxima renovación
     * mostrada: así un cobro del día 31 (o del 29-feb) no pasa a ser del 30 para siempre.
     */
    private var anclaCargada: String? = null
    private var fechaCargada: String? = null
    private var periodoCargado: String? = null
    private var intervaloCargado: Int? = null

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
        combine(nombre, precio, fechaRenovacion) { n, p, f -> Triple(n, p, f) },
        combine(categoriaId, esPrueba, fechaFinPrueba) { c, e, fp -> Triple(c, e, fp) }
    ) { (n, p, f), (c, e, fp) -> validarCampos(n, p, f, c, e, fp) }
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

    // ── Importe en vivo según la periodicidad ──────────────────────────────

    /**
     * Equivalente mensual (2 decimales) del importe tecleado con la periodicidad elegida:
     * 90 € cada 3 meses → 30.0. `null` si el importe no es válido. Misma regla que el Inicio
     * ([Periodicidad]); un semanal usa × 52 / 12.
     */
    val equivalenteMensualEnVivo: StateFlow<Double?> =
        combine(precio, periodoFacturacion, intervaloMeses) { p, periodo, intervalo ->
            calcularEnVivo(p) { importe -> Periodicidad.equivalenteMensualInterno(importe, periodo, intervaloCoherente(periodo, intervalo)) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Total anual (2 decimales) del importe tecleado con la periodicidad elegida:
     * 90 € cada 3 meses → 360.0. `null` si el importe no es válido.
     */
    val totalAnualEnVivo: StateFlow<Double?> =
        combine(precio, periodoFacturacion, intervaloMeses) { p, periodo, intervalo ->
            calcularEnVivo(p) { importe -> Periodicidad.totalAnualInterno(importe, periodo, intervaloCoherente(periodo, intervalo)) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Cambios sin guardar ────────────────────────────────────────────────

    private val huellaInicial = MutableStateFlow(huella())

    @Suppress("UNCHECKED_CAST")
    private val huellaActual: Flow<String> = combine(
        listOf(
            nombre, descripcion, precio, moneda, periodoFacturacion, intervaloMeses,
            fechaRenovacion, categoriaId, notas, esPrueba, fechaFinPrueba, fechaInicio, fechaFin
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
        nombre.value, descripcion.value, precio.value, moneda.value, periodoFacturacion.value, intervaloMeses.value,
        fechaRenovacion.value, categoriaId.value, notas.value, esPrueba.value, fechaFinPrueba.value,
        fechaInicio.value, fechaFin.value
    ).joinToString("\u0001")

    /**
     * Elige la periodicidad (1, 2, 3, 6 o 12 meses) y deriva el ciclo como el backend:
     * 12 → YEARLY; 1, 2, 3 y 6 → MONTHLY. Un valor no permitido se ignora.
     */
    fun seleccionarIntervalo(meses: Int) {
        if (!Periodicidad.esValido(meses)) return
        intervaloMeses.value = meses
        periodoFacturacion.value = Periodicidad.cicloDeIntervalo(meses)
    }

    /**
     * Elige el ciclo con el selector de ciclo (MONTHLY/YEARLY/WEEKLY) y ajusta el intervalo para
     * que la pareja sea coherente ([intervaloCoherente]): YEARLY → 12, WEEKLY → 1 y MONTHLY
     * conserva un intervalo de 1, 2, 3 o 6 meses.
     */
    fun seleccionarPeriodo(periodo: String) {
        periodoFacturacion.value = periodo
        intervaloMeses.value = intervaloCoherente(periodo, intervaloMeses.value)
    }

    /** Intervalo que se envía y con el que se calcula: el elegido, coherente con el ciclo. */
    private fun intervaloEfectivo(): Int = intervaloCoherente(periodoFacturacion.value, intervaloMeses.value)

    /**
     * Aplica un servicio del catálogo al formulario: nombre, precio, ciclo, moneda, prueba y
     * categoría (buscando la categoría del usuario cuyo nombre coincide con
     * [CatalogItem.categoriaKey], sin distinguir mayúsculas ni acentos).
     */
    fun seleccionarServicioDelCatalogo(item: CatalogItem) {
        prerellenarDesdeCatalogo(item, BillingCycle.fromWire(item.periodoFacturacion))
        _catalogoSeleccionado.value = item
        aplicarCategoriaDelCatalogo(item)
    }

    /** Espera pendiente de las categorías para asignar la del último servicio elegido. */
    private var esperaCategoria: Job? = null

    /**
     * Asigna la categoría del usuario que corresponde a [CatalogItem.categoriaKey].
     *
     * Desde la pestaña Catálogo, la pantalla aplica el servicio en su primera composición, cuando
     * el `init` aún no ha recibido las categorías: con la lista vacía no se encontraba nada y la
     * categoría quedaba sin elegir. Si aún no han llegado, se asigna al llegar, salvo que el
     * usuario ya haya elegido una; y si el formulario estaba "sin cambios", lo sigue estando.
     */
    private fun aplicarCategoriaDelCatalogo(item: CatalogItem) {
        esperaCategoria?.cancel()
        if (_categorias.value.isNotEmpty()) {
            buscarCategoriaDeClave(item.categoriaKey, _categorias.value)?.let { categoriaId.value = it.id }
            return
        }
        esperaCategoria = viewModelScope.launch {
            val categorias = _categorias.first { it.isNotEmpty() }
            if (categoriaId.value != null) return@launch
            val encontrada = buscarCategoriaDeClave(item.categoriaKey, categorias) ?: return@launch
            val sinCambios = huella() == huellaInicial.value
            categoriaId.value = encontrada.id
            if (sinCambios) marcarComoSinCambios()
        }
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
        // "12" y no "12.0" (Double.toString), igual que al prerrellenar desde el catálogo.
        precio.value = formatearPrecioParaCampo(suscripcion.precio)
        moneda.value = suscripcion.moneda
        activa = suscripcion.activa
        periodoFacturacion.value = suscripcion.periodoFacturacion
        // mesesPorCobro: un YEARLY de un backend antiguo (sin el campo, intervalo 1) se carga
        // como 12; un semanal no va por meses → 1 (el backend lo ignora).
        intervaloMeses.value = suscripcion.mesesPorCobro() ?: 1
        intervaloCargado = intervaloEfectivo()
        // Próxima renovación calculada con el "hoy" del móvil desde el ancla (R3-02).
        val proxima = proximaRenovacion(suscripcion, hoy())?.toString() ?: suscripcion.fechaRenovacion
        fechaRenovacion.value = proxima
        anclaCargada = suscripcion.fechaAncla?.trim()?.takeIf { it.isNotEmpty() }
        fechaCargada = proxima
        periodoCargado = suscripcion.periodoFacturacion
        categoriaId.value = suscripcion.categoriaId
        notas.value = suscripcion.notas
        esPrueba.value = suscripcion.esPrueba
        fechaFinPrueba.value = suscripcion.fechaFinPrueba
        fechaInicio.value = suscripcion.fechaInicio?.trim()?.takeIf { it.isNotEmpty() }
        fechaFin.value = suscripcion.fechaFin?.trim()?.takeIf { it.isNotEmpty() }
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
        intervaloMeses.value = if (cicloElegido == BillingCycle.YEARLY) Periodicidad.MESES_EN_ANIO else 1
        moneda.value = item.moneda
        _catalogoSeleccionado.value = item
        val diasPrueba = item.diasPrueba
        if (diasPrueba != null) {
            esPrueba.value = true
            fechaFinPrueba.value = (hoy() + DatePeriod(days = diasPrueba)).toString()
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

        validar(
            nombre.value, precio.value, fechaRenovacion.value, categoriaId.value,
            esPrueba.value, fechaFinPrueba.value
        )?.let { error ->
            _uiState.value = FormUiState.Error(error)
            return
        }

        val request = NuevaSuscripcionRequest(
            nombre = nombre.value.trim(),
            descripcion = descripcion.value.trim(),
            precio = precioDouble!!,
            moneda = moneda.value,
            periodoFacturacion = periodoFacturacion.value,
            fechaRenovacion = fechaAEnviar(esEdicion),
            categoriaId = categoriaId.value,
            activa = if (esEdicion) activa else true,
            notas = notas.value.trim(),
            esPrueba = esPrueba.value,
            fechaFinPrueba = if (esPrueba.value) fechaFinPrueba.value else null,
            intervaloMeses = intervaloEfectivo(),
            // "" borra la fecha en el backend; siempre viajan (ver NuevaSuscripcionRequest).
            fechaInicio = fechaInicio.value.orEmpty(),
            fechaFin = fechaFin.value.orEmpty()
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

    /**
     * Fecha del PUT/POST: el ancla original si se edita sin tocar fecha, periodo ni intervalo
     * (R3-01). Cambiar el intervalo (p. ej. de mensual a trimestral) es un cambio deliberado:
     * la serie nueva parte de la fecha que se ve en el formulario.
     */
    internal fun fechaAEnviar(esEdicion: Boolean): String {
        val ancla = anclaCargada
        return if (esEdicion && ancla != null &&
            fechaRenovacion.value == fechaCargada && periodoFacturacion.value == periodoCargado &&
            intervaloEfectivo() == intervaloCargado
        ) ancla else fechaRenovacion.value
    }

    companion object {
        const val MAX_SUGERENCIAS = 8

        /**
         * Intervalo coherente con el ciclo del formulario. Aquí manda el último selector tocado:
         * - YEARLY → 12 y WEEKLY (y los otros por días) → 1, elija lo que elija el intervalo.
         * - MONTHLY (o desconocido) → el intervalo si es 1, 2, 3 o 6; un 12 que quedó de un
         *   anual, o un valor no permitido, pasa a 1 (el usuario ha elegido "mensual").
         * Distinto de [Periodicidad.mesesPorCobro], que interpreta datos del servidor.
         */
        fun intervaloCoherente(periodo: String, intervalo: Int): Int = when (periodo.trim().uppercase()) {
            "YEARLY", "ANNUAL", "ANNUALLY" -> Periodicidad.MESES_EN_ANIO
            "WEEKLY", "BIWEEKLY", "DAILY" -> 1
            "QUARTERLY" -> 3
            else -> if (Periodicidad.esValido(intervalo) && intervalo != Periodicidad.MESES_EN_ANIO) intervalo else 1
        }

        /**
         * Cifra visible (2 decimales) del cálculo [interno] sobre el importe tecleado, o `null`
         * si el importe no es un número válido (vacío, negativo o fuera de rango).
         */
        private fun calcularEnVivo(textoPrecio: String, interno: (Double) -> Long): Double? {
            val importe = parsearPrecio(textoPrecio)?.takeIf { it >= 0 } ?: return null
            return runCatching { Periodicidad.aCifraVisible(interno(importe)) }.getOrNull()
        }

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
        fun validarCampos(
            nombre: String,
            precio: String,
            fechaRenovacion: String,
            categoriaId: Long?,
            esPrueba: Boolean = false,
            fechaFinPrueba: String? = null
        ): Map<Campo, FormError> {
            val errores = linkedMapOf<Campo, FormError>()
            if (nombre.isBlank()) errores[Campo.Nombre] = FormError.NombreVacio
            val precioDouble = parsearPrecio(precio)
            // 0 es válido (servicios gratuitos): la API acepta @PositiveOrZero desde la ronda 2.
            if (precioDouble == null || precioDouble < 0) errores[Campo.Precio] = FormError.PrecioInvalido
            if (fechaRenovacion.isBlank()) errores[Campo.FechaRenovacion] = FormError.FechaRenovacionVacia
            // Igual que la web: una prueba gratuita sin fecha de fin no se puede guardar. Sin ella
            // la prueba no avisa de cuándo empieza a cobrarse (el backend usa la fecha de
            // renovación como fin, pero el usuario no lo ve).
            if (esPrueba && fechaFinPrueba.isNullOrBlank()) errores[Campo.FechaFinPrueba] = FormError.FechaFinPruebaVacia
            if (categoriaId == null || categoriaId == 0L) errores[Campo.Categoria] = FormError.CategoriaNoSeleccionada
            return errores
        }

        /**
         * Validación pura del formulario. Devuelve el primer [FormError] encontrado
         * (en el orden visual de los campos) o `null` si todo es válido.
         */
        fun validar(
            nombre: String,
            precio: String,
            fechaRenovacion: String,
            categoriaId: Long?,
            esPrueba: Boolean = false,
            fechaFinPrueba: String? = null
        ): FormError? =
            validarCampos(nombre, precio, fechaRenovacion, categoriaId, esPrueba, fechaFinPrueba).values.firstOrNull()

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
            "telecomunicaciones" to "telecos", "transporte" to "transporte",
            "belleza" to "belleza"
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
