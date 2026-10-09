package com.subia.shared

import com.russhwolf.settings.MapSettings
import com.subia.shared.cache.CacheRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Pruebas de la limpieza de datos de cuenta al cerrar sesión ([CacheRepository.borrarDatosDeUsuario]). */
class CacheRepositoryTest {

    @Test
    fun borrarDatosDeUsuario_quitaDatosYMarcas_conservaPreferenciasDelDispositivo() {
        val settings = MapSettings()
        val cache = CacheRepository(settings)
        CacheRepository.CLAVES_DATOS_USUARIO.forEach { clave ->
            cache.saveString(clave, "[]")
            cache.saveTimestamp(clave)
        }
        cache.saveString("catalog", "[]")
        settings.putInt("notification_days_before", 7)
        settings.putBoolean("onboarding_completed", true)

        cache.borrarDatosDeUsuario()

        CacheRepository.CLAVES_DATOS_USUARIO.forEach { clave ->
            assertNull(cache.getString(clave), clave)
            assertTrue(cache.isStale(clave), "$clave sin marca de tiempo")
        }
        // El catálogo es público y las preferencias son del dispositivo, no de la cuenta.
        assertEquals("[]", cache.getString("catalog"))
        assertEquals(7, settings.getInt("notification_days_before", 0))
        assertTrue(settings.getBoolean("onboarding_completed", false))
    }
}
