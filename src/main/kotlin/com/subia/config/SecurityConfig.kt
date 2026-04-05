package com.subia.config

import com.subia.security.AuthRateLimitFilter
import com.subia.security.CustomAccessDeniedHandler
import com.subia.security.CustomAuthEntryPoint
import com.subia.security.RateLimitFilter
import com.subia.security.UserDetailsServiceImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
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
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt { it.decoder(jwtDecoder) } }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(authEntryPoint)
                ex.accessDeniedHandler(accessDeniedHandler)
            }
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean @Order(2)
    fun webFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/login", "/register", "/verify-email", "/verify-pending", "/verify-email-error",
                    "/forgot-password", "/reset-password", "/auth/google/callback",
                    "/delete-account", "/account-deleted",
                    "/css/**", "/js/**", "/images/**", "/error"
                ).permitAll()
                // /api/** is fully handled by apiFilterChain (Order 1)
                auth.requestMatchers("/api/**").denyAll()
                auth.anyRequest().authenticated()
            }
            .formLogin { form ->
                form.loginPage("/login")
                form.loginProcessingUrl("/login")
                form.defaultSuccessUrl("/dashboard", true)
                form.failureUrl("/login?error")
            }
            .logout { logout ->
                logout.logoutUrl("/logout")
                logout.logoutSuccessUrl("/login?logout")
                logout.invalidateHttpSession(true)
                logout.deleteCookies("SESSION")
            }
            .sessionManagement { session ->
                session.maximumSessions(1)
                session.sessionFixation().newSession()
                session.invalidSessionUrl("/login?expired")
            }
            .headers { headers ->
                headers.frameOptions { it.deny() }
                headers.contentSecurityPolicy { csp ->
                    csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' https://accounts.google.com https://cdn.tailwindcss.com https://cdn.jsdelivr.net; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "frame-src 'none'; " +
                        "object-src 'none'"
                    )
                }
            }
            .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
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
