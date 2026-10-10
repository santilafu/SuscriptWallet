package com.subia.config

import com.subia.security.AuthRateLimitFilter
import com.subia.security.CustomAccessDeniedHandler
import com.subia.security.CustomAuthEntryPoint
import com.subia.security.JwtService
import com.subia.security.RateLimitFilter
import com.subia.security.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.session.SessionRegistry
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtDecoder: NimbusJwtDecoder,
    private val rateLimitFilter: RateLimitFilter,
    private val authRateLimitFilter: AuthRateLimitFilter,
    private val authEntryPoint: CustomAuthEntryPoint,
    private val accessDeniedHandler: CustomAccessDeniedHandler,
    private val userDetailsService: UserDetailsServiceImpl,
    @Value("\${cors.allowed-origins}") private val allowedOrigins: String
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean @Order(1)
    fun apiFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/**")
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/auth/google").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/catalog", "/api/catalog/**").permitAll()
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Las categorías son globales (compartidas por todos los usuarios): solo el admin las muta (A-02)
                auth.requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt {
                    it.decoder(jwtDecoder)
                    it.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(authEntryPoint)
                ex.accessDeniedHandler(accessDeniedHandler)
            }
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
            // Límite estricto de /api/auth/* (login, refresh, google). Antes solo funcionaba porque
            // el filtro se registraba también como filtro global de servlet (ver FilterRegistrationConfig).
            .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean @Order(2)
    fun webFilterChain(http: HttpSecurity, sessionRegistry: SessionRegistry): SecurityFilterChain {
        http
            // Autenticación usuario/contraseña: el bean [webAuthenticationProvider] (global)
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/login", "/register", "/verify-email", "/verify-pending", "/verify-email-error",
                    "/forgot-password", "/reset-password", "/auth/google/callback",
                    "/oauth/gmail/callback",
                    "/delete-account", "/account-deleted", "/privacidad",
                    "/css/**", "/js/**", "/images/**", "/error",
                    // Favicons: sin esto las páginas públicas (login, registro) no mostraban el icono
                    "/favicon.ico", "/favicon-*.png", "/apple-touch-icon.png"
                ).permitAll()
                // El POST de borrado de cuenta necesita principal; sin sesión devolvía 500 (B-05)
                auth.requestMatchers(HttpMethod.POST, "/delete-account").authenticated()
                // Crear/editar/borrar categorías (globales) solo para el admin; la lista sigue abierta (A-02)
                auth.requestMatchers(HttpMethod.GET, "/categories/new", "/categories/*/edit").hasRole("ADMIN")
                auth.requestMatchers(HttpMethod.POST, "/categories", "/categories/*", "/categories/*/delete").hasRole("ADMIN")
                // /api/** is fully handled by apiFilterChain (Order 1)
                auth.requestMatchers("/api/**").denyAll()
                auth.anyRequest().authenticated()
            }
            .formLogin { form ->
                form.loginPage("/login")
                form.loginProcessingUrl("/login")
                form.defaultSuccessUrl("/dashboard", true)
                // Motivo concreto para cuenta sin verificar o bloqueada; el resto, mensaje genérico
                form.failureHandler { request, response, ex ->
                    val reason = when (ex) {
                        is org.springframework.security.authentication.DisabledException -> "=unverified"
                        is org.springframework.security.authentication.LockedException -> "=locked"
                        else -> ""
                    }
                    response.sendRedirect(request.contextPath + "/login?error" + reason)
                }
            }
            .logout { logout ->
                logout.logoutUrl("/logout")
                logout.logoutSuccessUrl("/login?logout")
                logout.invalidateHttpSession(true)
                logout.deleteCookies("SESSION")
            }
            .sessionManagement { session ->
                // Registro respaldado por Spring Session (SessionConfig, ronda 6): el mismo que usa
                // UserService para caducar las sesiones al restablecer la contraseña o borrar la cuenta.
                // Una sesión caducada se cierra en su siguiente petición y va a /login?expired.
                session.maximumSessions(1)
                    .sessionRegistry(sessionRegistry)
                    .expiredUrl("/login?expired")
                session.sessionFixation().newSession()
                session.invalidSessionUrl("/login?expired")
            }
            .headers { headers ->
                headers.frameOptions { it.deny() }
                // El botón de "Iniciar sesión con Google" (GIS) se pinta en un iframe de
                // accounts.google.com/gsi y hace peticiones a ese mismo origen: con `frame-src 'none'`
                // y `connect-src 'self'` el navegador lo bloqueaba (A-05). Directivas según la guía
                // de CSP de Google Identity Services.
                headers.contentSecurityPolicy { csp ->
                    csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://accounts.google.com https://cdn.tailwindcss.com https://cdn.jsdelivr.net; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net https://accounts.google.com/gsi/style; " +
                        "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
                        "img-src 'self' data: https://icon.horse https://*.googleusercontent.com; " +
                        "connect-src 'self' https://accounts.google.com/gsi/; " +
                        "frame-src https://accounts.google.com/gsi/; " +
                        "object-src 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self' https://accounts.google.com"
                    )
                }
            }
            .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    /**
     * Proveedor del formLogin web con el orden de comprobaciones de la ronda 4:
     * cuenta bloqueada → contraseña → email verificado (igual que `TokenService.login`).
     *
     * Por defecto Spring comprueba bloqueo Y verificación ANTES de la contraseña, así que
     * `?error=unverified` revelaba el estado de la cuenta a cualquiera que escribiera el email.
     * Ahora "sin verificar" solo se indica con la contraseña correcta. El bloqueo se sigue
     * comprobando antes de la contraseña a propósito: si dependiera de ella, durante el bloqueo se
     * podrían seguir probando claves y `?error=locked` delataría la correcta.
     *
     * Es un @Bean a propósito: así Spring lo usa como proveedor del AuthenticationManager GLOBAL.
     * Si se añadía solo al chain (`http.authenticationProvider`), Spring seguía creando el
     * proveedor global por defecto a partir del UserDetailsService y, al fallar el nuestro, lo
     * probaba como "padre": con contraseña mala volvía a salir `?error=unverified`.
     */
    @Bean
    fun webAuthenticationProvider(): DaoAuthenticationProvider =
        DaoAuthenticationProvider(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder())
            setPreAuthenticationChecks { user ->
                if (!user.isAccountNonLocked) throw LockedException("Cuenta bloqueada")
            }
            setPostAuthenticationChecks { user ->
                if (!user.isEnabled) throw DisabledException("EMAIL_NOT_VERIFIED")
                if (!user.isAccountNonExpired) throw AccountExpiredException("Cuenta caducada")
                if (!user.isCredentialsNonExpired) throw CredentialsExpiredException("Credenciales caducadas")
            }
        }

    /**
     * Convierte el claim `role` del access token en la autoridad ROLE_<rol>, igual que hace
     * [UserDetailsServiceImpl] en el chain web. Tokens sin claim (anteriores) no tienen rol.
     */
    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
                val role = jwt.getClaimAsString(JwtService.ROLE_CLAIM)
                if (role.isNullOrBlank()) emptyList<GrantedAuthority>()
                else listOf(SimpleGrantedAuthority("ROLE_$role"))
            }
        }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = this@SecurityConfig.allowedOrigins.split(",").map { it.trim() }
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().also {
            it.registerCorsConfiguration("/api/**", config)
        }
    }
}
