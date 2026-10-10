package com.subia.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.ArrayDeque
import java.util.Deque
import java.util.concurrent.ConcurrentHashMap

/**
 * Límite de 10 intentos/minuto por IP en los endpoints de autenticación.
 *
 * `/api/auth/refresh` tiene su propio cubo, más holgado ([REFRESH_MAX_REQUESTS]/min por IP,
 * R3-03): compartía las 10 peticiones con el login y, detrás de un CGNAT (muchos móviles con la
 * misma IP pública), los refrescos legítimos de la app recibían 429. Sigue limitado porque cada
 * intento con un token robado/adivinado cuesta una consulta, pero el refresh ya exige un token
 * de 256 bits y su reutilización revoca la familia, así que no es un vector de fuerza bruta.
 *
 * Solo cuenta las peticiones POST (los envíos de credenciales). Antes también contaban los GET:
 * abrir /login, volver a /login?error tras un fallo, o abrir el enlace de /reset-password
 * consumían cupo, y con 5 intentos fallidos el usuario recibía un 429 en JSON al cargar la página.
 */
@Component
class AuthRateLimitFilter : OncePerRequestFilter() {

    private val protectedPaths = setOf(
        "/login",
        "/register",
        "/api/auth/login",
        "/api/auth/google",
        REFRESH_PATH,
        "/forgot-password",
        "/reset-password",
        "/auth/google/callback"
    )

    private val requestTimestamps = ConcurrentHashMap<String, Deque<Long>>()

    private val maxRequests = 10
    private val windowMillis = 60_000L

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        if (!request.method.equals("POST", ignoreCase = true) ||
            protectedPaths.none { path.equals(it, ignoreCase = true) }
        ) {
            filterChain.doFilter(request, response)
            return
        }

        val ip = ClientIp.of(request)
        val now = System.currentTimeMillis()
        val cutoff = now - windowMillis

        // Cubo separado para el refresh: no consume (ni agota) el cupo del login, y viceversa.
        val isRefresh = path.equals(REFRESH_PATH, ignoreCase = true)
        val key = if (isRefresh) "refresh|$ip" else ip
        val limit = if (isRefresh) REFRESH_MAX_REQUESTS else maxRequests
        val timestamps = requestTimestamps.computeIfAbsent(key) { ArrayDeque() }

        synchronized(timestamps) {
            // Limpiar timestamps antiguos
            while (timestamps.isNotEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst()
            }

            if (timestamps.size >= limit) {
                response.addHeader("Retry-After", "60")
                if (path.startsWith("/api/")) {
                    response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.writer.write("""{"code":"RATE_LIMIT_EXCEEDED"}""")
                } else {
                    // Formularios web: antes el navegador mostraba el JSON en bruto. Con sendError
                    // se pinta la página de error (error.html) con un mensaje de "espera un minuto".
                    response.sendError(HttpStatus.TOO_MANY_REQUESTS.value())
                }
                return
            }

            timestamps.addLast(now)
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Elimina las IPs sin intentos en la última ventana. Sin esto el mapa crecía sin límite
     * (una entrada por IP vista desde el arranque, M-03). Lo llama [com.subia.service.MaintenanceJobs].
     */
    fun purgeIdle(now: Long = System.currentTimeMillis()): Int {
        val cutoff = now - windowMillis
        var removed = 0
        val it = requestTimestamps.entries.iterator()
        while (it.hasNext()) {
            val deque = it.next().value
            val idle = synchronized(deque) { deque.isEmpty() || deque.peekLast() < cutoff }
            if (idle) { it.remove(); removed++ }
        }
        return removed
    }

    internal fun trackedIps(): Int = requestTimestamps.size

    companion object {
        const val REFRESH_PATH = "/api/auth/refresh"
        /** Refrescos por minuto e IP: holgado para CGNAT, aún corta un bucle o un abuso. */
        const val REFRESH_MAX_REQUESTS = 60
    }
}
