package com.subia.repository

import com.subia.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByToken(token: String): RefreshToken?
    fun findAllByFamilyId(familyId: UUID): List<RefreshToken>
    fun findByEmail(email: String): List<RefreshToken>
}