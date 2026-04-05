package com.subia.security

import com.subia.repository.UserRepository
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("Usuario no encontrado: $email")

        val lockedUntil = user.lockedUntil
        if (lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now())) {
            throw LockedException("Cuenta bloqueada hasta $lockedUntil")
        }

        if (!user.emailVerified) {
            throw DisabledException("EMAIL_NOT_VERIFIED")
        }

        return User.builder()
            .username(user.email)
            .password(user.passwordHash ?: "")
            .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            .build()
    }
}
