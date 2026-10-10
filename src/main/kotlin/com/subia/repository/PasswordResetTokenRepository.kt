package com.subia.repository

import com.subia.model.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, UUID> {
    fun findByToken(token: String): PasswordResetToken?
    fun deleteByUserId(userId: Long)

    /** Marca el enlace como usado solo si no lo estaba (1 = lo ha consumido esta llamada). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.id = :id AND t.used = false")
    fun markUsedIfUnused(@Param("id") id: UUID): Int

    /** Purga de tokens de restablecimiento caducados (usados o no). */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    fun deleteExpiredBefore(@Param("cutoff") cutoff: OffsetDateTime): Int
}