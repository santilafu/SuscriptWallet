package com.subia.shared.viewmodel

/** Pantalla con la que arranca la app (o a la que va el login). Ver [destinoInicial]. */
enum class DestinoInicial { LOGIN, ONBOARDING, INICIO }

/**
 * Decide la pantalla inicial a partir de la sesión y del onboarding. Se usa tanto al arrancar
 * (sesión restaurada) como justo tras iniciar sesión, para que ambos caminos coincidan: el
 * onboarding solo cuenta como visto cuando el usuario lo termina o lo salta. Si lo cerró con
 * "atrás" (es la raíz de la pila y la app se cierra), al volver a abrir aparece de nuevo.
 */
fun destinoInicial(sesionIniciada: Boolean, onboardingCompletado: Boolean): DestinoInicial = when {
    !sesionIniciada -> DestinoInicial.LOGIN
    !onboardingCompletado -> DestinoInicial.ONBOARDING
    else -> DestinoInicial.INICIO
}
