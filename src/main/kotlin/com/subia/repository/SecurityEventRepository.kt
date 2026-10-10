package com.subia.repository

import com.subia.model.SecurityEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface SecurityEventRepository : JpaRepository<SecurityEvent, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<SecurityEvent>
    fun deleteByUserId(userId: Long)

    /** Purga de eventos de auditoría antiguos (retención limitada). */
    @Modifying
    @Query("DELETE FROM SecurityEvent e WHERE e.createdAt < :cutoff")
    fun deleteOlderThan(@Param("cutoff") cutoff: OffsetDateTime): Int
}
