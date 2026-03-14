package com.subia.security

import com.subia.model.RefreshToken
import com.subia.repository.RefreshTokenRepository
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

data class TokenPair(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)

@Service
class TokenService(
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${jwt.access-token-ttl-minutes:15}") private val accessTtlMinutes: Long,
    @Value("\${jwt.refresh-token-ttl-days:30}") private val refreshTtlDays: Long,
    @Value("\${app.auth.username}") private val adminUsername: String,
    @Value("\${app.auth.password}") private val adminPasswordRaw: String
) {
    private lateinit var adminPasswordHash: String

    @PostConstruct
    fun init() {
        // Si ya es un hash BCrypt lo usamos directamente; si no, lo hasheamos al arrancar
        adminPasswordHash = if (adminPasswordRaw.startsWith("\$2a\$") || adminPasswordRaw.startsWith("\$2b\$")) {
            adminPasswordRaw
        } else {
            passwordEncoder.encode(adminPasswordRaw)
        }
    }

    @Transactional
    fun login(username: String, rawPassword: String): TokenPair {
        if (username != adminUsername || !passwordEncoder.matches(rawPassword, adminPasswordHash)) {
            throw BadCredentialsException("Credenciales inválidas")
        }
        return issueTokenPair(username)
    }

    @Transactional
    fun refresh(rawRefreshToken: String): TokenPair {
        val entity = refreshTokenRepository.findByToken(rawRefreshToken)
            ?: throw BadCredentialsException("Refresh token no encontrado")
        if (entity.revoked) throw BadCredentialsException("Refresh token revocado")
        if (entity.expiresAt.isBefore(OffsetDateTime.now())) throw BadCredentialsException("Refresh token expirado")
        refreshTokenRepository.save(entity.copy(revoked = true, revokedAt = OffsetDateTime.now()))
        return issueTokenPair(entity.username)
    }

    @Transactional
    fun logout(rawRefreshToken: String) {
        val entity = refreshTokenRepository.findByToken(rawRefreshToken) ?: return
        if (!entity.revoked) {
            refreshTokenRepository.save(entity.copy(revoked = true, revokedAt = OffsetDateTime.now()))
        }
    }

    private fun issueTokenPair(username: String): TokenPair {
        val accessToken = jwtService.generateAccessToken(username)
        val rawRefreshToken = java.util.UUID.randomUUID().toString()
        val now = OffsetDateTime.now()
        refreshTokenRepository.save(
            RefreshToken(
                token = rawRefreshToken,
                username = username,
                issuedAt = now,
                expiresAt = now.plusDays(refreshTtlDays)
            )
        )
        return TokenPair(accessToken, rawRefreshToken, accessTtlMinutes * 60)
    }
}