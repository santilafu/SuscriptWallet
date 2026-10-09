package com.subia.android.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.subia.android.R
import com.subia.shared.viewmodel.CrearCategoriaError
import com.subia.shared.viewmodel.ErrorRemoto
import com.subia.shared.viewmodel.SuscripcionesError

/**
 * Traduce un [ErrorRemoto] a texto localizado.
 *
 * @param operacion mensaje de la operación que falló ("Error al cargar el catálogo"...).
 * @param sinConexion texto para el caso sin red; por defecto el genérico.
 */
@Composable
fun textoErrorRemoto(
    error: ErrorRemoto,
    @StringRes operacion: Int,
    @StringRes sinConexion: Int = R.string.error_offline
): String = when (error) {
    ErrorRemoto.SinConexion -> stringResource(sinConexion)
    // El código HTTP ya se mostraba antes ("Error 500: ..."); se conserva para soporte.
    is ErrorRemoto.Servidor -> stringResource(R.string.error_with_code, stringResource(operacion), error.codigo)
    ErrorRemoto.Desconocido -> stringResource(operacion)
}

@Composable
fun textoErrorSuscripciones(error: SuscripcionesError): String = when (error) {
    is SuscripcionesError.CargaFallida -> textoErrorRemoto(error.causa, R.string.error_load_subscriptions)
    is SuscripcionesError.EliminacionFallida -> textoErrorRemoto(
        error.causa,
        operacion = R.string.error_delete_subscription,
        sinConexion = R.string.error_delete_offline
    )
}

@Composable
fun textoErrorCrearCategoria(error: CrearCategoriaError): String = stringResource(
    when (error) {
        CrearCategoriaError.NombreVacio -> R.string.category_error_name_required
        CrearCategoriaError.SinConexion -> R.string.category_error_offline
        CrearCategoriaError.CreacionFallida -> R.string.category_error_create_failed
    }
)
