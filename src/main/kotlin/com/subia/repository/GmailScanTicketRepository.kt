package com.subia.repository

import com.subia.model.GmailScanTicket
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface GmailScanTicketRepository : JpaRepository<GmailScanTicket, String> {
    fun deleteByExpiresAtBefore(cutoff: OffsetDateTime)
    fun deleteByUserId(userId: Long)

    /**
     * Marca el ticket como usado de forma atómica: solo afecta a una fila si existe, no estaba
     * usado y no ha expirado. Devuelve el número de filas afectadas (1 = consumido, 0 = inválido).
     * Evita la carrera read-then-save entre dos callbacks concurrentes con el mismo `state`.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE GmailScanTicket t SET t.used = true WHERE t.id = :id AND t.used = false AND t.expiresAt > :now")
    fun markUsedIfValid(@Param("id") id: String, @Param("now") now: OffsetDateTime): Int
}
