package com.subia.service

import com.subia.model.Category
import com.subia.repository.CategoryRepository
import com.subia.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Servicio de negocio para la gestión de categorías.
 *
 * Actúa como capa intermedia entre los controladores y los repositorios JPA.
 * Toda la lógica de negocio relacionada con categorías (validaciones, reglas) vive aquí,
 * no en el controlador ni en el repositorio.
 *
 * [@Transactional] garantiza que cada operación de escritura se ejecute dentro de una
 * transacción de base de datos. Si ocurre un error, los cambios se revierten automáticamente.
 */
@Service
@Transactional
class CategoryService(
    private val repo: CategoryRepository,

    // Se necesita el repositorio de suscripciones para comprobar dependencias antes de borrar
    private val subscriptionRepo: SubscriptionRepository
) {

    /**
     * Devuelve todas las categorías almacenadas, sin ningún orden garantizado.
     * Spring Data aplica la transacción heredada de la clase.
     */
    fun findAll(): List<Category> = repo.findAll()

    /**
     * Busca una categoría por su ID.
     *
     * @throws NoSuchElementException si no existe ninguna categoría con ese ID.
     */
    fun findById(id: Long): Category =
        repo.findById(id).orElseThrow { NoSuchElementException("Categoría $id no encontrada") }

    /**
     * Guarda una categoría nueva o actualiza una existente.
     * Si [category.id] es 0 (valor por defecto), JPA hace INSERT; si tiene un ID válido, hace UPDATE.
     *
     * @return La entidad guardada con el ID asignado por la base de datos.
     */
    fun save(category: Category): Category = repo.save(category)

    /**
     * Elimina una categoría por su ID.
     *
     * Antes de borrar, comprueba que ninguna suscripción haga referencia a esta categoría.
     * Si las hay, lanza [IllegalStateException] para que el controlador pueda mostrar un mensaje
     * de error al usuario en lugar de dejar que la base de datos lance una excepción de FK.
     *
     * @throws IllegalStateException si la categoría tiene suscripciones asociadas.
     */
    fun delete(id: Long) {
        if (subscriptionRepo.existsByCategoryId(id)) {
            throw IllegalStateException("No se puede eliminar: la categoría tiene suscripciones asociadas.")
        }
        repo.deleteById(id)
    }
}