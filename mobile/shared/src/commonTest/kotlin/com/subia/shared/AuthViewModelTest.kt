package com.subia.shared

import com.subia.shared.network.ApiException
import com.subia.shared.network.NetworkException
import com.subia.shared.viewmodel.AuthError
import com.subia.shared.viewmodel.AuthViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de la lógica pura de [AuthViewModel]: validación de credenciales y traducción
 * de excepciones a [AuthError] tipado (sin textos; la UI los localiza).
 *
 * `AuthRepository` depende de `TokenStorage` (expect class con contexto de plataforma),
 * por eso se prueban las funciones del companion y no una instancia del ViewModel.
 */
class AuthViewModelTest {

    @Test
    fun validarCredenciales_ambosRellenos_noDevuelveError() {
        assertNull(AuthViewModel.validarCredenciales("ana@mail.com", "secreto"))
    }

    @Test
    fun validarCredenciales_emailVacio_devuelveCredencialesVacias() {
        assertEquals(AuthError.CredencialesVacias, AuthViewModel.validarCredenciales("", "secreto"))
    }

    @Test
    fun validarCredenciales_passwordEnBlanco_devuelveCredencialesVacias() {
        assertEquals(AuthError.CredencialesVacias, AuthViewModel.validarCredenciales("ana@mail.com", "   "))
    }

    @Test
    fun mapearErrorLogin_401_devuelveCredencialesIncorrectas() {
        assertEquals(AuthError.CredencialesIncorrectas, AuthViewModel.mapearErrorLogin(ApiException(401, "Unauthorized")))
    }

    @Test
    fun mapearErrorLogin_403_devuelveCredencialesIncorrectas() {
        assertEquals(AuthError.CredencialesIncorrectas, AuthViewModel.mapearErrorLogin(ApiException(403, "Forbidden")))
    }

    @Test
    fun mapearErrorLogin_networkException_devuelveSinConexion() {
        assertEquals(AuthError.SinConexion, AuthViewModel.mapearErrorLogin(NetworkException("timeout")))
    }

    @Test
    fun mapearErrorLogin_500_devuelveDesconocidoConDetalle() {
        val error = AuthViewModel.mapearErrorLogin(ApiException(500, "Internal"))
        assertTrue(error is AuthError.Desconocido)
        assertEquals("Error 500: Internal", error.detalle)
    }

    @Test
    fun mapearErrorGoogle_apiException_devuelveGoogleNoVerificado() {
        assertEquals(AuthError.GoogleNoVerificado, AuthViewModel.mapearErrorGoogle(ApiException(401, "Unauthorized")))
    }

    @Test
    fun mapearErrorGoogle_networkException_devuelveSinConexion() {
        assertEquals(AuthError.SinConexion, AuthViewModel.mapearErrorGoogle(NetworkException("sin red")))
    }
}
