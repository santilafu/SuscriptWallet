package com.subia.security

import com.subia.repository.UserRepository
import com.subia.repository.findByEmailAnyCase
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

    /**
     * Carga la cuenta para el formLogin web. Una cuenta bloqueada o sin verificar NO lanza
     * excepción aquí: se devuelve marcada (`accountLocked` / `disabled`) y las comprobaciones
     * previas de `DaoAuthenticationProvider` lanzan LockedException / DisabledException.
     * Antes se lanzaban desde aquí y Spring las envolvía en InternalAuthenticationServiceException:
     * ERROR con traza en el log y el usuario solo veía "correo o contraseña incorrectos" (ronda 2).
     */
    override fun loadUserByUsername(email: String): UserDetails {
        // Sin distinguir mayúsculas: también encuentra cuentas antiguas como `Juan@Gmail.com` (ronda 4)
        val user = userRepository.findByEmailAnyCase(email)
            ?: throw UsernameNotFoundException("Usuario no encontrado")

        val lockedUntil = user.lockedUntil
        val locked = lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now())

        return User.builder()
            .username(user.email)
            .password(user.passwordHash ?: "")
            .authorities(SimpleGrantedAuthority("ROLE_${user.role.name}"))
            .accountLocked(locked)
            .disabled(!user.emailVerified)
            .build()
    }

    /**
     * Como [loadUserByUsername] pero lanza [LockedException] / [DisabledException] ("EMAIL_NOT_VERIFIED")
     * si la cuenta no puede entrar. Para flujos con contraseña que no pasen por
     * `DaoAuthenticationProvider`. El callback web de Google usa [loadUserForGoogle] (ronda 5).
     */
    fun loadActiveUser(email: String): UserDetails {
        val details = loadUserByUsername(email)
        if (!details.isAccountNonLocked) throw LockedException("Cuenta bloqueada")
        if (!details.isEnabled) throw DisabledException("EMAIL_NOT_VERIFIED")
        return details
    }

    /**
     * Para el callback web de Google (ronda 5): como [loadActiveUser] pero SIN el bloqueo por
     * intentos fallidos, que protege contra adivinar contraseñas y no aplica a un login con Google
     * verificado (la API de Google tampoco lo aplica). Sí exige el email verificado.
     */
    fun loadUserForGoogle(email: String): UserDetails {
        val details = loadUserByUsername(email)
        if (!details.isEnabled) throw DisabledException("EMAIL_NOT_VERIFIED")
        return User.withUserDetails(details).accountLocked(false).build()
    }
}
