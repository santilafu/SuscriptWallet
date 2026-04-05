package com.subia.security

import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminBootstrapListener(
    private val userRepository: UserRepository,
    @Value("\${app.admin.email:santilafu.dev@gmail.com}") private val adminEmail: String,
    @Value("\${app.admin.password:}") private val adminPassword: String
) : ApplicationListener<ApplicationReadyEvent> {

    private val log = LoggerFactory.getLogger(AdminBootstrapListener::class.java)
    private val passwordEncoder = BCryptPasswordEncoder(12)

    @Transactional
    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        if (adminPassword.isBlank()) {
            log.warn("app.admin.password no configurado — saltando bootstrap de admin")
            return
        }

        if (userRepository.existsByEmail(adminEmail)) {
            log.debug("Usuario admin ya existe: {}", adminEmail)
            return
        }

        val passwordHash = passwordEncoder.encode(adminPassword)
        val adminUser = User(
            email = adminEmail,
            passwordHash = passwordHash,
            emailVerified = true,
            role = UserRole.ADMIN
        )

        val saved = userRepository.save(adminUser)
        log.info("Admin user bootstrapped: {}", adminEmail)

        // Asignar suscripciones sin propietario al admin
        try {
            val em = event.applicationContext.getBean(jakarta.persistence.EntityManager::class.java)
            val updated = em.createNativeQuery(
                "UPDATE subscriptions SET user_id = :adminId WHERE user_id IS NULL"
            )
                .setParameter("adminId", saved.id)
                .executeUpdate()
            if (updated > 0) {
                log.info("Asignadas {} suscripciones huérfanas al admin", updated)
            }
        } catch (ex: Exception) {
            log.warn("No se pudo asignar suscripciones huérfanas al admin: {}", ex.message)
        }
    }
}
