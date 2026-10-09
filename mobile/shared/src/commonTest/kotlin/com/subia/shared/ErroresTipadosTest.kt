package com.subia.shared

import com.subia.shared.network.ApiException
import com.subia.shared.network.NetworkException
import com.subia.shared.viewmodel.CategoriasViewModel
import com.subia.shared.viewmodel.CrearCategoriaError
import com.subia.shared.viewmodel.ErrorRemoto
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Traducción de excepciones a errores tipados (sin textos: la UI los localiza).
 */
class ErroresTipadosTest {

    @Test
    fun errorRemoto_networkException_esSinConexion() {
        assertEquals(ErrorRemoto.SinConexion, ErrorRemoto.desde(NetworkException("timeout")))
    }

    @Test
    fun errorRemoto_apiException_conservaElCodigo() {
        assertEquals(ErrorRemoto.Servidor(503), ErrorRemoto.desde(ApiException(503, "Service Unavailable")))
    }

    @Test
    fun errorRemoto_otroFalloONulo_esDesconocido() {
        assertEquals(ErrorRemoto.Desconocido, ErrorRemoto.desde(IllegalStateException("Respuesta vacía")))
        assertEquals(ErrorRemoto.Desconocido, ErrorRemoto.desde(null))
    }

    @Test
    fun crearCategoria_networkException_esSinConexion() {
        assertEquals(CrearCategoriaError.SinConexion, CategoriasViewModel.mapearErrorCreacion(NetworkException("x")))
    }

    @Test
    fun crearCategoria_errorDelServidor_esCreacionFallida() {
        assertEquals(CrearCategoriaError.CreacionFallida, CategoriasViewModel.mapearErrorCreacion(ApiException(500, "x")))
    }
}
