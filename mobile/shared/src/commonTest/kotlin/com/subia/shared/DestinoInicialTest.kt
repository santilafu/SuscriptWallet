package com.subia.shared

import com.subia.shared.viewmodel.DestinoInicial
import com.subia.shared.viewmodel.destinoInicial
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pantalla con la que arranca la app (y a la que va el login): el onboarding solo se da por
 * visto cuando el usuario lo termina o lo salta. Si salió de él con "atrás" (la app se cierra
 * porque es la raíz de la pila), al volver a abrir debe aparecer otra vez.
 */
class DestinoInicialTest {

    @Test
    fun sinSesion_login_aunqueElOnboardingEsteCompletado() {
        assertEquals(DestinoInicial.LOGIN, destinoInicial(sesionIniciada = false, onboardingCompletado = true))
        assertEquals(DestinoInicial.LOGIN, destinoInicial(sesionIniciada = false, onboardingCompletado = false))
    }

    @Test
    fun conSesion_sinOnboardingCompletado_onboarding() {
        assertEquals(DestinoInicial.ONBOARDING, destinoInicial(sesionIniciada = true, onboardingCompletado = false))
    }

    @Test
    fun conSesion_yOnboardingCompletado_inicio() {
        assertEquals(DestinoInicial.INICIO, destinoInicial(sesionIniciada = true, onboardingCompletado = true))
    }
}
