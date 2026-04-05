package com.subia.repository

import com.subia.model.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, UUID> {
    fun findByToken(token: String): PasswordResetToken?
    fun deleteByUserId(userId: Long)
}