package com.subia.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

/**
 * Reloj de la aplicación con la zona horaria de los usuarios (N-19).
 *
 * Render ejecuta la JVM en UTC: con `LocalDate.now()` el "hoy" del servidor iba un día por
 * detrás entre las 00:00 y las 02:00 de España y los días restantes de las renovaciones salían
 * mal. Toda fecha "de calendario" debe calcularse con `LocalDate.now(clock)`.
 * La zona se puede cambiar con la propiedad `app.timezone` (variable `APP_TIMEZONE`).
 */
@Configuration
class TimeConfig {

    @Bean
    fun clock(@Value("\${app.timezone:Europe/Madrid}") zone: String): Clock =
        Clock.system(ZoneId.of(zone))
}
