package com.subia.repository

import com.subia.model.Subscription
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repositorio JPA para la entidad [Subscription].
 *
 * Spring Data genera automáticamente la implementación en tiempo de ejecución.
 * Todas las consultas de negocio van filtradas por usuario: los métodos "legacy" sin `userId`
 * (findByActiveTrue, rangos de renovación globales…) se eliminaron porque reutilizarlos por
 * error exponía suscripciones de otros usuarios (B-02). Las renovaciones próximas se calculan
 * en memoria con la fecha proyectada ([com.subia.service.RenewalDates]), no por SQL sobre la
 * `renewalDate` guardada, que nunca se avanza (N-12).
 */
interface SubscriptionRepository : JpaRepository<Subscription, Long> {

    /** Devuelve todas las suscripciones de un usuario. */
    fun findByUserId(userId: Long): List<Subscription>

    /** Elimina todas las suscripciones de un usuario. */
    fun deleteByUserId(userId: Long)

    /** Devuelve solo las suscripciones activas de un usuario. */
    fun findByUserIdAndActiveTrue(userId: Long): List<Subscription>

    /** Busca una suscripción por ID verificando que pertenece al usuario. */
    fun findByIdAndUserId(id: Long, userId: Long): Subscription?

    /**
     * Comprueba si existe alguna suscripción asociada a la categoría indicada.
     * Se usa en [com.subia.service.CategoryService] para impedir el borrado de
     * categorías que todavía tienen suscripciones.
     */
    fun existsByCategoryId(categoryId: Long): Boolean
}
