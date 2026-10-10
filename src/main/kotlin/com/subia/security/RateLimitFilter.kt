package com.subia.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Límite global de la API (`rate-limit.requests-per-minute` por IP). Solo actúa dentro del
 * chain de la API: su registro automático como filtro de servlet está desactivado en
 * [com.subia.config.FilterRegistrationConfig] (antes también limitaba páginas web y estáticos).
 */
@Component
class RateLimitFilter(
    @Value("\${rate-limit.requests-per-minute:100}") private val requestsPerMinute: Long
) : OncePerRequestFilter() {

    private class Entry(val bucket: Bucket) {
        @Volatile var lastSeen: Long = System.currentTimeMillis()
    }

    private val buckets = ConcurrentHashMap<String, Entry>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val key = ClientIp.of(request)
        val entry = buckets.computeIfAbsent(key) {
            Entry(
                Bucket.builder()
                    .addLimit(
                        Bandwidth.builder()
                            .capacity(requestsPerMinute)
                            .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                            .build()
                    )
                    .build()
            )
        }
        entry.lastSeen = System.currentTimeMillis()
        if (entry.bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.addHeader("Retry-After", "60")
            response.writer.write("""{"data":null,"error":{"code":"RATE_LIMIT_EXCEEDED","message":"Demasiadas peticiones. Reintenta en 60 segundos.","details":[]}}""")
        }
    }

    /**
     * Elimina los buckets sin uso en los últimos 2 minutos (ya estarían llenos de nuevo).
     * Sin esto el mapa crecía sin límite (M-03). Lo llama [com.subia.service.MaintenanceJobs].
     */
    fun purgeIdle(now: Long = System.currentTimeMillis()): Int {
        val cutoff = now - Duration.ofMinutes(2).toMillis()
        val before = buckets.size
        buckets.entries.removeIf { it.value.lastSeen < cutoff }
        return before - buckets.size
    }
}
