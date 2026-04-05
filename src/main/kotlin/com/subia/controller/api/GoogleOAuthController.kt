package com.subia.controller.api

import com.subia.controller.TokenResponse
import com.subia.dto.GoogleAuthRequest
import com.subia.dto.api.ApiResponse
import com.subia.exception.InvalidGoogleTokenException
import com.subia.security.GoogleOAuthService
import com.subia.security.TokenService
import com.subia.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/auth")
class GoogleOAuthController(
    private val googleOAuthService: GoogleOAuthService,
    private val userService: UserService,
    private val tokenService: TokenService
) {

    @PostMapping("/google")
    fun googleLogin(
        @Valid @RequestBody req: GoogleAuthRequest,
        request: HttpServletRequest
    ): ApiResponse<TokenResponse> {
        return try {
            val payload = googleOAuthService.verifyAndGetPayload(req.idToken)
            val email = payload.email
            val googleId = payload.subject

            val user = userService.findOrCreateByGoogle(email, googleId)

            val pair = tokenService.issueTokenPairForEmail(email)

            ApiResponse(data = TokenResponse(pair.accessToken, pair.refreshToken, pair.expiresInSeconds))
        } catch (ex: InvalidGoogleTokenException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.message)
        }
    }
}
