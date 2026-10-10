package com.subia.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.session.SessionRegistry
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session
import org.springframework.session.security.SpringSessionBackedSessionRegistry

/**
 * Registro de sesiones web (auditoría ronda 6).
 *
 * Las sesiones web viven en PostgreSQL (Spring Session JDBC). Antes, `maximumSessions(1)` usaba el
 * `SessionRegistryImpl` en memoria que Spring crea por defecto: no veía las sesiones guardadas en
 * la BD (ni las de otra instancia o anteriores a un reinicio) y, como Spring Session no publica
 * eventos de sesión de servlet, nunca se limpiaba. Además no había forma de cerrar las sesiones de
 * un usuario al restablecer su contraseña o borrar su cuenta.
 *
 * `SpringSessionBackedSessionRegistry` consulta la tabla de Spring Session por el nombre del
 * principal (columna PRINCIPAL_NAME, que Spring Session rellena con `Authentication.name`, es
 * decir, el email guardado de la cuenta). Lo usan la gestión de sesiones concurrentes de
 * [SecurityConfig] y `UserService` para caducar sesiones. No necesita `HttpSessionEventPublisher`:
 * no depende de eventos de sesión del contenedor (que con Spring Session no se emiten).
 */
@Configuration
class SessionConfig {

    @Bean
    fun sessionRegistry(sessionRepository: FindByIndexNameSessionRepository<out Session>): SessionRegistry {
        @Suppress("UNCHECKED_CAST")
        return SpringSessionBackedSessionRegistry(sessionRepository as FindByIndexNameSessionRepository<Session>)
    }
}
