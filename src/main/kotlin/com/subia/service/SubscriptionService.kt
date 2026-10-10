package com.subia.service

import com.subia.model.Subscription
import com.subia.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Servicio de negocio para la gestión de suscripciones.
 *
 * Actúa como capa intermedia entre los controladores y el repositorio JPA.
 * Todas las operaciones exigen el `userId` del propietario: los métodos sin usuario
 * (findAll(), findActive(), findById(id), delete(id)) se eliminaron por riesgo de IDOR (B-02).
 *
 * Antes de guardar se valida y normaliza la entidad ([validated]) para que datos fuera de los
 * límites de la tabla (nombre > 100, precio con más de 8 cifras, divisa rara…) den un 400 en
 * lugar de un 500 por violación de la base de datos.
 */
@Service
@Transactional
class SubscriptionService(private val repo: SubscriptionRepository) {

    /** Devuelve todas las suscripciones de un usuario. */
    fun findAll(userId: Long): List<Subscription> = repo.findByUserId(userId)

    /** Devuelve solo las suscripciones activas de un usuario. */
    fun findActive(userId: Long): List<Subscription> = repo.findByUserIdAndActiveTrue(userId)

    /**
     * Busca una suscripción por ID verificando que pertenece al usuario indicado.
     *
     * @throws NoSuchElementException si no existe o no pertenece al usuario.
     */
    fun findById(id: Long, userId: Long): Subscription =
        repo.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("Suscripción $id no encontrada")

    /**
     * Actualiza una suscripción existente (ya con su `userId`) o la inserta si `id` es 0.
     * Los llamadores deben haber comprobado antes la propiedad con [findById].
     */
    fun save(subscription: Subscription): Subscription {
        requireNotNull(subscription.userId) { "La suscripción debe tener propietario" }
        return repo.save(validated(subscription))
    }

    /** Guarda una suscripción asignándole el userId indicado. */
    fun save(subscription: Subscription, userId: Long): Subscription =
        repo.save(validated(subscription.copy(userId = userId)))

    /**
     * Elimina la suscripción verificando que pertenece al usuario indicado.
     *
     * @throws NoSuchElementException si no existe o no pertenece al usuario.
     */
    fun delete(id: Long, userId: Long) {
        repo.delete(findById(id, userId))
    }

    companion object {
        /** Máximo admitido por la columna `price DECIMAL(10,2)`. */
        val MAX_PRICE = BigDecimal("99999999.99")
        private val CURRENCY_REGEX = Regex("^[A-Z]{3}$")

        /**
         * Valida y normaliza una suscripción contra los límites del esquema (V1/V4).
         * @throws IllegalArgumentException con un mensaje legible si algo no cuadra.
         */
        fun validated(s: Subscription): Subscription {
            val name = s.name.trim()
            require(name.isNotEmpty()) { "El nombre es obligatorio" }
            require(name.length <= 100) { "El nombre no puede superar 100 caracteres" }
            require(s.description.length <= 500) { "La descripción no puede superar 500 caracteres" }
            require(s.notes.length <= 1000) { "Las notas no pueden superar 1000 caracteres" }
            require(s.price.signum() >= 0) { "El precio no puede ser negativo" }
            val price = s.price.setScale(2, RoundingMode.HALF_UP)
            require(price <= MAX_PRICE) { "El precio es demasiado alto" }
            val currency = s.currency.trim().uppercase()
            require(CURRENCY_REGEX.matches(currency)) { "Divisa no válida: ${s.currency.take(10)}" }
            return s.copy(name = name, price = price, currency = currency)
        }
    }
}
