package com.subia.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.subia.model.UserRole
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-ttl-minutes:15}") private val ttlMinutes: Long
) {
    lateinit var encoder: NimbusJwtEncoder
    lateinit var decoder: NimbusJwtDecoder

    companion object {
        /** Claim con el rol del usuario (USER/ADMIN); el chain de la API lo convierte en ROLE_*. */
        const val ROLE_CLAIM = "role"
    }

    @PostConstruct
    fun init() {
        val bytes = secret.toByteArray()
        require(bytes.size >= 32) {
            "jwt.secret debe tener >= 32 bytes (256 bits). Actual: ${bytes.size} bytes. " +
            "Establece la variable de entorno JWT_SECRET."
        }
        encoder = NimbusJwtEncoder(ImmutableSecret(bytes))
        decoder = NimbusJwtDecoder
            .withSecretKey(SecretKeySpec(bytes, "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }

    /**
     * Genera el access token. Si se conoce el rol se incluye como claim para que la API pueda
     * autorizar por rol sin consultar la BD en cada petición (el TTL es corto, 15 min).
     */
    fun generateAccessToken(username: String, role: UserRole? = null): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer("subia")
            .subject(username)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(ttlMinutes * 60))
            .apply { if (role != null) claim(ROLE_CLAIM, role.name) }
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}