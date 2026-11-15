package com.subia.security

import com.subia.repository.UserRepository
import io.mockk.confirmVerified
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.boot.context.event.ApplicationReadyEvent

/**
 * El email del admin ya no tiene valor por defecto en el código (era un dato personal).
 * Si falta ADMIN_EMAIL o ADMIN_PASSWORD, el arranque no debe crear ningún usuario ni
 * consultar la base de datos: solo deja un aviso en el log.
 */
class AdminBootstrapListenerTest {

    private val repo = mockk<UserRepository>()
    private val evento = mockk<ApplicationReadyEvent>(relaxed = true)

    @Test
    fun `sin email de admin no toca la base de datos`() {
        AdminBootstrapListener(repo, adminEmail = "", adminPassword = "una-clave-larga").onApplicationEvent(evento)
        confirmVerified(repo)
    }

    @Test
    fun `sin contrasena de admin no toca la base de datos`() {
        AdminBootstrapListener(repo, adminEmail = "admin@example.com", adminPassword = "").onApplicationEvent(evento)
        confirmVerified(repo)
    }
}
