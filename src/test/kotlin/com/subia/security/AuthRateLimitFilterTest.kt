package com.subia.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AuthRateLimitFilterTest {

    private val filter = AuthRateLimitFilter()

    private fun hit(method: String, path: String, ip: String = "1.2.3.4"): Int {
        val req = MockHttpServletRequest(method, path).apply { remoteAddr = ip }
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, MockFilterChain())
        return res.status
    }

    @Test
    fun `los GET de las paginas de login no consumen cupo`() {
        repeat(30) { assertEquals(200, hit("GET", "/login")) }
        assertEquals(200, hit("POST", "/login"))
    }

    @Test
    fun `el POST numero 11 en un minuto se rechaza con 429`() {
        repeat(10) { assertEquals(200, hit("POST", "/api/auth/login")) }
        assertEquals(429, hit("POST", "/api/auth/login"))
        // Otra IP no se ve afectada
        assertEquals(200, hit("POST", "/api/auth/login", ip = "5.6.7.8"))
    }

    @Test
    fun `purgeIdle elimina las IPs sin actividad reciente`() {
        hit("POST", "/login")
        assertEquals(1, filter.trackedIps())
        filter.purgeIdle(now = System.currentTimeMillis() + 120_000)
        assertEquals(0, filter.trackedIps())
    }

    @Test
    fun `la IP se acota a 45 caracteres aunque X-Forwarded-For sea enorme`() {
        val req = MockHttpServletRequest("POST", "/login").apply {
            addHeader("X-Forwarded-For", "a".repeat(5000) + ", 10.0.0.1")
        }
        assertEquals(45, ClientIp.of(req).length)
    }

    @Test
    fun `el 429 es JSON en la API y pagina de error en los formularios web`() {
        repeat(10) { hit("POST", "/api/auth/login", ip = "9.9.9.9") }
        val apiRes = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest("POST", "/api/auth/login").apply { remoteAddr = "9.9.9.9" }, apiRes, MockFilterChain())
        assertEquals(429, apiRes.status)
        assertTrue(apiRes.contentAsString.contains("RATE_LIMIT_EXCEEDED"))

        val webRes = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest("POST", "/login").apply { remoteAddr = "9.9.9.9" }, webRes, MockFilterChain())
        assertEquals(429, webRes.status)
        assertTrue(webRes.isCommitted)
        assertFalse(webRes.contentAsString.contains("RATE_LIMIT_EXCEEDED"))
    }

    @Test
    fun `refresh tiene cubo propio de 60 por minuto y no comparte cupo con el login`() {
        val ip = "100.64.0.1" // CGNAT
        // 10 logins agotan el cupo de login…
        repeat(10) { assertEquals(200, hit("POST", "/api/auth/login", ip)) }
        assertEquals(429, hit("POST", "/api/auth/login", ip))
        // …pero los refrescos de la misma IP siguen pasando hasta 60
        repeat(AuthRateLimitFilter.REFRESH_MAX_REQUESTS) { assertEquals(200, hit("POST", "/api/auth/refresh", ip)) }
        assertEquals(429, hit("POST", "/api/auth/refresh", ip))
        // Y agotar el refresh no afecta al login de otra IP ni a su refresh
        assertEquals(200, hit("POST", "/api/auth/refresh", "100.64.0.2"))
    }

    @Test
    fun `agotar el refresh no consume cupo de login`() {
        val ip = "100.64.0.3"
        repeat(AuthRateLimitFilter.REFRESH_MAX_REQUESTS) { hit("POST", "/api/auth/refresh", ip) }
        assertEquals(429, hit("POST", "/api/auth/refresh", ip))
        assertEquals(200, hit("POST", "/api/auth/login", ip))
    }
}
