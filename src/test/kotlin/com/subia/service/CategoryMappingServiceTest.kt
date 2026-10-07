package com.subia.service

import com.subia.model.Category
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests del mapeo nombre de categoría ↔ clave de catálogo, que sustituye a los tres `when`
 * duplicados de los controladores (B-01).
 */
class CategoryMappingServiceTest {

    private val categoryService = mockk<CategoryService>()
    private val service = CategoryMappingService(categoryService)

    @Test
    fun `keyFor traduce las categorias sembradas originales`() {
        assertEquals("ia", CategoryMappingService.keyFor("IA"))
        assertEquals("streaming", CategoryMappingService.keyFor("Streaming"))
        assertEquals("noticias", CategoryMappingService.keyFor("Noticias y Lectura"))
        assertEquals("prueba", CategoryMappingService.keyFor("Prueba gratuita"))
        assertEquals("citas", CategoryMappingService.keyFor("Citas y social"))
    }

    @Test
    fun `keyFor traduce las categorias nuevas de recibos recurrentes`() {
        assertEquals("hogar", CategoryMappingService.keyFor("Hogar y suministros"))
        assertEquals("seguros", CategoryMappingService.keyFor("Seguros"))
        assertEquals("telecos", CategoryMappingService.keyFor("Telecomunicaciones"))
        assertEquals("transporte", CategoryMappingService.keyFor("Transporte"))
    }

    @Test
    fun `keyFor ignora espacios alrededor y devuelve null para nombres desconocidos`() {
        assertEquals("seguros", CategoryMappingService.keyFor("  Seguros "))
        assertNull(CategoryMappingService.keyFor("Mi categoría personal"))
        assertNull(CategoryMappingService.keyFor(""))
    }

    @Test
    fun `KEY_TO_NAME es el inverso exacto de NAME_TO_KEY`() {
        assertEquals(CategoryMappingService.NAME_TO_KEY.size, CategoryMappingService.KEY_TO_NAME.size)
        CategoryMappingService.NAME_TO_KEY.forEach { (name, key) ->
            assertEquals(name, CategoryMappingService.KEY_TO_NAME[key])
        }
    }

    @Test
    fun `categoryKeyToId resuelve el id de cada categoria sembrada e ignora las personalizadas`() {
        every { categoryService.findAll() } returns listOf(
            Category(id = 1, name = "Streaming"),
            Category(id = 2, name = "Seguros"),
            Category(id = 3, name = "Hogar y suministros"),
            Category(id = 9, name = "Mi categoría personal")
        )

        val map = service.categoryKeyToId()

        assertEquals(mapOf("streaming" to 1L, "seguros" to 2L, "hogar" to 3L), map)
        assertFalse(map.containsValue(9L))
    }

    @Test
    fun `categoryKeyToId devuelve un mapa vacio si no hay categorias`() {
        every { categoryService.findAll() } returns emptyList()
        assertTrue(service.categoryKeyToId().isEmpty())
    }
}
