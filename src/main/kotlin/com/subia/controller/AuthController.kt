package com.subia.controller

import com.subia.dto.api.ApiResponse
import com.subia.security.TokenService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

data class LoginRequest(@field:NotBlank val email: String = "", @field:NotBlank val password: String = "")
data class RefreshRequest(@field:NotBlank val refreshToken: String = "")
data class LogoutRequest(@field:NotBlank val refreshToken: String = "")
/**
 * Respuesta de login/refresh/Google. `role` ("USER" | "ADMIN") se añadió en la ronda 2 de la auditoría
 * para que la app sepa si puede gestionar categorías; los clientes antiguos lo ignoran (ignoreUnknownKeys).
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val tokenType: String = "Bearer",
    val role: String = "USER"
) {
    companion object {
        fun from(pair: com.subia.security.TokenPair) =
            TokenResponse(pair.accessToken, pair.refreshToken, pair.expiresInSeconds, role = pair.role)
    }
}

@RestController
@RequestMapping("/api/auth")
class AuthController(private val tokenService: TokenService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ApiResponse<TokenResponse> {
        val pair = tokenService.login(req.email, req.password)
        return ApiResponse(data = TokenResponse.from(pair))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody req: RefreshRequest): ApiResponse<TokenResponse> {
        val pair = tokenService.refresh(req.refreshToken)
        return ApiResponse(data = TokenResponse.from(pair))
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody req: LogoutRequest) = tokenService.logout(req.refreshToken)
}
