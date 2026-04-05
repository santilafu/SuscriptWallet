package com.subia.repository

import com.subia.model.SecurityEvent
import org.springframework.data.jpa.repository.JpaRepository

interface SecurityEventRepository : JpaRepository<SecurityEvent, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<SecurityEvent>
    fun deleteByUserId(userId: Long)
}
