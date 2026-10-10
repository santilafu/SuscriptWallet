package com.subia.security

import com.subia.service.UserService
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.stereotype.Component

/**
 * Bloqueo por intentos fallidos también en el login WEB (formulario).
 *
 * Sin este listener el formLogin de la web no contaba los fallos: se podían probar contraseñas
 * sin límite por cuenta (solo frenaba el rate limit por IP, que se salta cambiando de IP o de
 * cabecera X-Forwarded-For). Aquí cada `handleFailedLogin` va en su propia transacción.
 *
 * Nota (ronda 4): hasta la ronda 4 la API (`TokenService.login`) tampoco bloqueaba en la práctica,
 * aunque lo pareciera: el contador se escribía dentro de su transacción y la BadCredentialsException
 * posterior hacía rollback. Se arregló con `noRollbackFor` (ver AuthIntegrationTest).
 *
 * Solo se atienden autenticaciones usuario/contraseña ([UsernamePasswordAuthenticationToken]):
 * las de JWT de la API también publican eventos de éxito en cada petición y no deben tocar la BD.
 */
@Component
class WebLoginAttemptListener(private val userService: UserService) {

    @EventListener
    fun onFailure(event: AuthenticationFailureBadCredentialsEvent) {
        val auth = event.authentication as? UsernamePasswordAuthenticationToken ?: return
        val email = auth.name?.takeIf { it.isNotBlank() } ?: return
        userService.handleFailedLogin(email)
    }

    @EventListener
    fun onSuccess(event: AuthenticationSuccessEvent) {
        val auth = event.authentication as? UsernamePasswordAuthenticationToken ?: return
        userService.resetFailedAttempts(auth.name)
    }
}
