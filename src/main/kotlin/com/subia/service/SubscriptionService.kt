package com.subia.service

import com.subia.model.Subscription
import com.subia.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Servicio de negocio para la gestión de suscripciones.
 *
 * Actúa como capa intermedia entre los controladores y el repositorio JPA.
 * Centraliza las operaciones CRUD sobre [Subscription] y encapsula las
 * reglas de negocio que puedan añadirse en el futuro.
 *
 * [@Transactional] garantiza que cada operación se ejecute en una transacción de base
 * de datos. Si ocurre un error inesperado, los cambios se revierten automáticamente.
 */
@Service
@Transactional
class SubscriptionService(private val repo: SubscriptionRepository) {

    /**
     * Devuelve todas las suscripciones, activas e inactivas, sin ningún orden garantizado.
     * Se usa en la pantalla de lista completa de suscripciones (admin/legacy).
     */
    fun findAll(): List<Subscription> = repo.findAll()

    /**
     * Devuelve todas las suscripciones de un usuario.
     */
    fun findAll(userId: Long): List<Subscription> = repo.findByUserId(userId)

    /**
     * Devuelve solo las suscripciones marcadas como activas.
     * Se usa en el dashboard para calcular totales y renovaciones próximas (legacy).
     */
    fun findActive(): List<Subscription> = repo.findByActiveTrue()

    /**
     * Devuelve solo las suscripciones activas de un usuario.
     */
    fun findActive(userId: Long): List<Subscription> = repo.findByUserIdAndActiveTrue(userId)

    /**
     * Busca una suscripción por su ID.
     *
     * @throws NoSuchElementException si no existe ninguna suscripción con ese ID.
     */
    fun findById(id: Long): Subscription =
        repo.findById(id).orElseThrow { NoSuchElementException("Suscripción $id no encontrada") }

    /**
     * Busca una suscripción por ID verificando que pertenece al usuario indicado.
     *
     * @throws NoSuchElementException si no existe o no pertenece al usuario.
     */
    fun findById(id: Long, userId: Long): Subscription =
        repo.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("Suscripción $id no encontrada")

    /**
     * Guarda una suscripción nueva o actualiza una existente.
     * Si [subscription.id] es 0, JPA hace INSERT; si tiene un ID válido, hace UPDATE.
     *
     * @return La entidad guardada con el ID asignado por la base de datos.
     */
    fun save(subscription: Subscription): Subscription = repo.save(subscription)

    /**
     * Guarda una suscripción asignándole el userId indicado.
     */
    fun save(subscription: Subscription, userId: Long): Subscription =
        repo.save(subscription.copy(userId = userId))

    /**
     * Elimina permanentemente la suscripción con el ID indicado.
     * No comprueba si el ID existe; si no existe, Spring Data lanza [EmptyResultDataAccessException].
     */
    fun delete(id: Long) = repo.deleteById(id)

    /**
     * Elimina la suscripción verificando que pertenece al usuario indicado.
     *
     * @throws NoSuchElementException si no existe o no pertenece al usuario.
     */
    fun delete(id: Long, userId: Long) {
        findById(id, userId) // verifica ownership
        repo.deleteById(id)
    }
}
