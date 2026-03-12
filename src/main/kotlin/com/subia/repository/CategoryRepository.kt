package com.subia.repository

import com.subia.model.Category
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repositorio JPA para la entidad [Category].
 *
 * Spring Data genera automáticamente la implementación en tiempo de ejecución.
 * Hereda de [JpaRepository] las operaciones CRUD básicas:
 *   - findAll(), findById(id), save(entity), deleteById(id), count(), etc.
 *
 * Si en el futuro se necesitan consultas personalizadas (p. ej. buscar por nombre),
 * se pueden añadir métodos con la convención de Spring Data o con @Query.
 */
interface CategoryRepository : JpaRepository<Category, Long>
