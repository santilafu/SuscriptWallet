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
     * Se usa en la pantalla de lista completa de suscripciones.
     */
    fun findAll(): List<Subscription> = repo.findAll()

    /**
     * Devuelve solo las suscripciones marcadas como activas.
     * Se usa en el dashboard para calcular totales y renovaciones próximas.
     */
    fun findActive(): List<Subscription> = repo.findByActiveTrue()

    /**
     * Busca una suscripción por su ID.
     *
     * @throws NoSuchElementException si no existe ninguna suscripción con ese ID.
     */
    fun findById(id: Long): Subscription =
        repo.findById(id).orElseThrow { NoSuchElementException("Suscripción $id no encontrada") }

    /**
     * Guarda una suscripción nueva o actualiza una existente.
     * Si [subscription.id] es 0, JPA hace INSERT; si tiene un ID válido, hace UPDATE.
     *
     * @return La entidad guardada con el ID asignado por la base de datos.
     */
    fun save(subscription: Subscription): Subscription = repo.save(subscription)

    /**
     * Elimina permanentemente la suscripción con el ID indicado.
     * No comprueba si el ID existe; si no existe, Spring Data lanza [EmptyResultDataAccessException].
     */
    fun delete(id: Long) = repo.deleteById(id)
}
