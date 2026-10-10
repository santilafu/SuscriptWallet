package com.subia.exception

import com.subia.controller.AuthController
import com.subia.security.TokenService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

/**
 * Errores de la API que antes acababan en el handler genérico como 500 "INTERNAL_ERROR".
 */
class ApiExceptionHandlerTest {

    private val tokenService = mockk<TokenService>()
    private val mvc: MockMvc = MockMvcBuilders.standaloneSetup(AuthController(tokenService))
        .setControllerAdvice(ApiExceptionHandler())
        .build()

    private fun login(email: String) = mvc.post("/api/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email":"$email","password":"x"}"""
    }

    @Test
    fun `cuenta bloqueada devuelve 403 ACCOUNT_LOCKED`() {
        every { tokenService.login("locked@example.com", "x") } throws LockedException("bloqueada")

        login("locked@example.com").andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("ACCOUNT_LOCKED") }
        }
    }

    @Test
    fun `email sin verificar devuelve 403 EMAIL_NOT_VERIFIED`() {
        every { tokenService.login("new@example.com", "x") } throws DisabledException("EMAIL_NOT_VERIFIED")

        login("new@example.com").andExpect {
            status { isForbidden() }
            jsonPath("$.error.code") { value("EMAIL_NOT_VERIFIED") }
        }
    }

    @Test
    fun `Content-Type no soportado devuelve 415 y no 500`() {
        mvc.post("/api/auth/login") {
            contentType = MediaType.TEXT_PLAIN
            content = "hola"
        }.andExpect {
            status { isUnsupportedMediaType() }
        }
    }

    @Test
    fun `IllegalArgumentException de validacion devuelve 400`() {
        every { tokenService.login("bad@example.com", "x") } throws IllegalArgumentException("dato malo")

        login("bad@example.com").andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("VALIDATION_ERROR") }
        }
    }
}
