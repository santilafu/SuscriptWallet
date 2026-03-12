package com.subia.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Configuración de seguridad de la aplicación.
 *
 * Esta app es de uso personal (no expuesta a internet), por lo que no requiere
 * autenticación de usuarios. La configuración se enfoca en:
 *
 * 1. CSRF: desactivado para simplificar el desarrollo local.
 *    Al ser una app personal sin exposición a internet, el riesgo es mínimo.
 *    Si en el futuro se expone públicamente, activar CSRF con CookieCsrfTokenRepository.
 *
 * 2. Headers de seguridad:
 *    - X-Frame-Options: SAMEORIGIN (necesario para la consola H2).
 *    - X-Content-Type-Options: nosniff (evita MIME sniffing).
 *
 * 3. Acceso: todas las rutas son accesibles sin autenticación.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Todas las rutas son accesibles sin iniciar sesión
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            // CSRF desactivado para app de uso personal local
            .csrf { it.disable() }
            // Permite iframes del mismo origen (necesario para la consola H2)
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }

        return http.build()
    }
}