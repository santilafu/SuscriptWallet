package com.subia.repository

import com.subia.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun findByEmailVerificationToken(token: String): User?
    fun existsByEmail(email: String): Boolean
    fun findByGoogleId(googleId: String): User?
}
