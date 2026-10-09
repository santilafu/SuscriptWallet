package com.subia.shared.viewmodel

import com.subia.shared.network.ApiException
import com.subia.shared.network.NetworkException

/**
 * Causa tipada de un fallo al hablar con el servidor. Igual que [FormError], el ViewModel no
 * contiene textos: cada plataforma lo traduce a su recurso localizado, combinándolo con el
 * mensaje de la operación concreta (cargar el catálogo, eliminar una suscripción...).
 */
sealed interface ErrorRemoto {
    /** Fallo de red o timeout. */
    data object SinConexion : ErrorRemoto

    /**
     * El servidor respondió con un código HTTP de error. Antes se mostraba "Error 500: ...",
     * así que conservamos el código para que la UI lo siga enseñando (ayuda a dar soporte).
     */
    data class Servidor(val codigo: Int) : ErrorRemoto

    /** Cualquier otro fallo (respuesta vacía, JSON inesperado...). */
    data object Desconocido : ErrorRemoto

    companion object {
        fun desde(error: Throwable?): ErrorRemoto = when (error) {
            is NetworkException -> SinConexion
            is ApiException -> Servidor(error.statusCode)
            else -> Desconocido
        }
    }
}
