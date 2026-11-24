package com.subia.integration

import com.subia.service.CategoryMappingService
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.sql.Connection
import java.sql.DriverManager

/**
 * Test de INTEGRACIÓN de la migración V18 (categoría "Préstamos") contra PostgreSQL real.
 *
 * Cada test usa su propia BD temporal de [TestDatabase] (se borra al acabar) y migra con Flyway
 * a mano para poder pararse en la V17. Comprueba que la fila sembrada coincide con el mapeo del
 * catálogo y que la migración no duplica la categoría si el usuario ya la había creado.
 */
@EnabledIf("com.subia.integration.TestDatabase#available")
class PrestamosCategoryMigrationTest {

    private fun migrar(url: String, hasta: String? = null) {
        Flyway.configure()
            .dataSource(url, TestDatabase.USER, TestDatabase.PASSWORD)
            .locations("classpath:db/migration")
            .apply { if (hasta != null) target(hasta) }
            .load()
            .migrate()
    }

    private fun <T> conConexion(url: String, block: (Connection) -> T): T =
        DriverManager.getConnection(url, TestDatabase.USER, TestDatabase.PASSWORD).use(block)

    /** Filas (color, icono) de la categoría "Préstamos". */
    private fun filasPrestamos(url: String): List<Pair<String, String>> = conConexion(url) { c ->
        c.prepareStatement("SELECT color, icon FROM categories WHERE name = ?").use { ps ->
            ps.setString(1, "Préstamos")
            ps.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.getString("color") to rs.getString("icon")) }
            }
        }
    }

    @Test
    fun `V18 siembra Prestamos con su color e icono y el nombre casa con el mapeo del catalogo`() {
        val url = TestDatabase.createTemporaryDatabase()
        migrar(url)

        assertEquals(listOf("#1e3a8a" to "🏦"), filasPrestamos(url))
        assertEquals("prestamos", CategoryMappingService.keyFor("Préstamos"))
    }

    @Test
    fun `V18 es idempotente si la categoria ya existia creada a mano`() {
        val url = TestDatabase.createTemporaryDatabase()
        migrar(url, hasta = "17")
        conConexion(url) { c ->
            c.createStatement().use {
                it.executeUpdate("INSERT INTO categories (name, color, icon) VALUES ('Préstamos', '#123456', '💰')")
            }
        }

        migrar(url)

        // Se conserva la del usuario y no aparece una segunda fila.
        assertEquals(listOf("#123456" to "💰"), filasPrestamos(url))
    }
}
