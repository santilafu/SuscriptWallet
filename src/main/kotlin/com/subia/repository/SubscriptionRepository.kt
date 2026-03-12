package com.subia.repository

import com.subia.model.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

/**
 * Repositorio JPA para la entidad [Subscription].
 *
 * Spring Data genera automáticamente la implementación en tiempo de ejecución.
 * Además de las operaciones CRUD heredadas de [JpaRepository], define consultas
 * específicas del negocio mediante la convención de nombres y @Query JPQL.
 */
interface SubscriptionRepository : JpaRepository<Subscription, Long> {

    /**
     * Devuelve todas las suscripciones marcadas como activas.
     * Spring Data deriva la consulta del nombre del método: WHERE active = true.
     * Se usa en el dashboard y para el cálculo de totales.
     */
    fun findByActiveTrue(): List<Subscription>

    /**
     * Comprueba si existe alguna suscripción asociada a la categoría indicada.
     * Se usa en [com.subia.service.CategoryService] para impedir el borrado de
     * categorías que todavía tienen suscripciones.
     *
     * @param categoryId ID de la categoría a comprobar.
     * @return true si hay al menos una suscripción con esa categoría.
     */
    fun existsByCategoryId(categoryId: Long): Boolean

    /**
     * Devuelve las suscripciones activas cuya fecha de renovación cae entre [from] y [to] (inclusive).
     * Se usa en el dashboard para mostrar las renovaciones próximas a 30 días y las alertas a 7 días.
     *
     * Usa JPQL explícito porque la lógica de rango no se puede expresar limpiamente
     * solo con el nombre del método.
     *
     * @param from Fecha de inicio del rango (normalmente hoy).
     * @param to   Fecha de fin del rango (normalmente hoy + 7 o + 30 días).
     */
    @Query("SELECT s FROM Subscription s WHERE s.active = true AND s.renewalDate BETWEEN :from AND :to")
    fun findActiveRenewingBetween(from: LocalDate, to: LocalDate): List<Subscription>
}
