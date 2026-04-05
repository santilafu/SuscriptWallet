package com.subia.security

import com.subia.model.RefreshToken
import com.subia.repository.RefreshTokenRepository
import com.subia.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

data class TokenPair(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long)

@Service
class TokenService(
    private val jwtService: JwtService,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userDetailsService: UserDetailsServiceImpl,
    private val userService: UserService,
    @Value("\${jwt.access-token-ttl-minutes:15}") private val accessTtlMinutes: Long,
    @Value("\${jwt.refresh-token-ttl-days:30}") private val refreshTtlDays: Long
) {

    @Transactional
    fun login(email: String, rawPassword: String): TokenPair {
        val userDetails = try {
            userDetailsService.loadUserByUsername(email)
        } catch (ex: UsernameNotFoundException) {
            throw BadCredentialsException("Credenciales inválidas")
        } catch (ex: LockedException) {
            throw ex
        } catch (ex: DisabledException) {
            throw ex
        }

        if (!passwordEncoder.matches(rawPassword, userDetails.password)) {
            userService.handleFailedLogin(email)
            throw BadCredentialsException("Credenciales inválidas")
        }

        userService.resetFailedAttempts(email)
        return issueTokenPair(email)
    }

    @Transactional
    fun refresh(rawRefreshToken: String): TokenPair {
        val entity = refreshTokenRepository.findByToken(rawRefreshToken)
            ?: throw BadCredentialsException("Refresh token no encontrado")

        if (entity.revoked) {
            // Token reuse detected — revoke entire family
            val family = refreshTokenRepository.findAllByFamilyId(entity.familyId)
            family.forEach { token ->
                if (!token.revoked) {
                    refreshTokenRepository.save(token.copy(revoked = true, revokedAt = OffsetDateTime.now()))
                }
            }
            throw BadCredentialsException("TOKEN_REUSE_DETECTED")
        }

        if (entity.expiresAt.isBefore(OffsetDateTime.now())) {
            throw BadCredentialsException("Refresh token expirado")
        }

        // Revoke the current token
        refreshTokenRepository.save(entity.copy(revoked = true, revokedAt = OffsetDateTime.now()))

        // Issue new pair with same familyId
        return issueTokenPair(entity.email, entity.familyId)
    }

    @Transactional
    fun logout(rawRefreshToken: String) {
        val entity = refreshTokenRepository.findByToken(rawRefreshToken) ?: return
        if (!entity.revoked) {
            refreshTokenRepository.save(entity.copy(revoked = true, revokedAt = OffsetDateTime.now()))
        }
    }

    @Transactional
    fun issueTokenPairForEmail(email: String): TokenPair = issueTokenPair(email)

    private fun issueTokenPair(email: String, familyId: UUID = UUID.randomUUID()): TokenPair {
        val accessToken = jwtService.generateAccessToken(email)
        val rawRefreshToken = UUID.randomUUID().toString()
        val now = OffsetDateTime.now()

        refreshTokenRepository.save(
            RefreshToken(
                token = rawRefreshToken,
                email = email,
                familyId = familyId,
                issuedAt = now,
                expiresAt = now.plusDays(refreshTtlDays)
            )
        )

        return TokenPair(accessToken, rawRefreshToken, accessTtlMinutes * 60)
    }
}
