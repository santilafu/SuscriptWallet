package com.subia.repository

import com.subia.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Refresh tokens. La columna `token` guarda el SHA-256 (hex) del token, nunca el valor en claro
 * (M-02, migración V14): quien lea la base de datos no obtiene sesiones válidas.
 */
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    /** Busca por el hash SHA-256 del token (ver [com.subia.security.TokenService.hash]). */
    fun findByToken(tokenHash: String): RefreshToken?
    fun findAllByFamilyId(familyId: UUID): List<RefreshToken>
    fun findByEmail(email: String): List<RefreshToken>

    /**
     * Revoca el token solo si sigue activo. Devuelve 1 si lo ha revocado esta llamada y 0 si otra
     * petición concurrente con el mismo token se adelantó (evita emitir dos pares con un token).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = :now WHERE t.id = :id AND t.revoked = false")
    fun revokeIfActive(@Param("id") id: UUID, @Param("now") now: OffsetDateTime): Int

    /** Purga de tokens caducados (ya no sirven ni para detectar reutilización). */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoff")
    fun deleteExpiredBefore(@Param("cutoff") cutoff: OffsetDateTime): Int
}
