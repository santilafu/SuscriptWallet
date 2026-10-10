package com.subia.config

import com.subia.security.AuthRateLimitFilter
import com.subia.security.RateLimitFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Los filtros de rate limiting son `@Component` para poder inyectarlos en los SecurityFilterChain,
 * pero Spring Boot registra además cualquier bean `Filter` en el contenedor de servlets para
 * TODAS las URLs. Eso hacía que el límite global de la API (100/min) se aplicara también a
 * páginas web, CSS e imágenes (M-03c). Aquí se desactiva ese registro automático: los filtros
 * solo actúan dentro de su chain de seguridad.
 */
@Configuration
class FilterRegistrationConfig {

    @Bean
    fun rateLimitFilterRegistration(filter: RateLimitFilter) =
        FilterRegistrationBean(filter).apply { isEnabled = false }

    @Bean
    fun authRateLimitFilterRegistration(filter: AuthRateLimitFilter) =
        FilterRegistrationBean(filter).apply { isEnabled = false }
}
