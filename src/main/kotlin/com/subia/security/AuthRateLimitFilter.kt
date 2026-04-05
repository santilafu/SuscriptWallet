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

@Component
class AuthRateLimitFilter : OncePerRequestFilter() {

    private val protectedPaths = setOf(
        "/login",
        "/register",
        "/api/auth/login",
        "/api/auth/google",
        "/api/auth/refresh",
        "/forgot-password",
        "/reset-password"
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
        if (protectedPaths.none { path.equals(it, ignoreCase = true) }) {
            filterChain.doFilter(request, response)
            return
        }

        val ip = extractIp(request)
        val now = System.currentTimeMillis()
        val cutoff = now - windowMillis

        val timestamps = requestTimestamps.computeIfAbsent(ip) { ArrayDeque() }

        synchronized(timestamps) {
            // Limpiar timestamps antiguos
            while (timestamps.isNotEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst()
            }

            if (timestamps.size >= maxRequests) {
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.addHeader("Retry-After", "60")
                response.writer.write("""{"code":"RATE_LIMIT_EXCEEDED"}""")
                return
            }

            timestamps.addLast(now)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractIp(request: HttpServletRequest): String {
        val forwarded = request.getHeader("X-Forwarded-For")
        return if (!forwarded.isNullOrBlank()) {
            forwarded.split(",").first().trim()
        } else {
            request.remoteAddr ?: "unknown"
        }
    }
}
