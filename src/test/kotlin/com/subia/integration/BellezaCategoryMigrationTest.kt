package com.subia.integration

import com.subia.service.CategoryMappingService
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.sql.Connection
import java.sql.DriverManager

/**
 * Test de INTEGRACIÓN de la migración V16 (categoría "Belleza") contra PostgreSQL real.
 *
 * Cada test usa su propia BD temporal de [TestDatabase] (se borra al acabar) y migra con Flyway
 * a mano para poder pararse en la V15. Comprueba que la fila sembrada coincide con el mapeo del
 * catálogo y que la migración no duplica la categoría si el usuario ya la había creado.
 */
@EnabledIf("com.subia.integration.TestDatabase#available")
class BellezaCategoryMigrationTest {

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

    /** Filas (color, icono) de la categoría "Belleza". */
    private fun filasBelleza(url: String): List<Pair<String, String>> = conConexion(url) { c ->
        c.prepareStatement("SELECT color, icon FROM categories WHERE name = ?").use { ps ->
            ps.setString(1, "Belleza")
            ps.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.getString("color") to rs.getString("icon")) }
            }
        }
    }

    @Test
    fun `V16 siembra Belleza con su color e icono y el nombre casa con el mapeo del catalogo`() {
        val url = TestDatabase.createTemporaryDatabase()
        migrar(url)

        assertEquals(listOf("#c026d3" to "💄"), filasBelleza(url))
        assertEquals("belleza", CategoryMappingService.keyFor("Belleza"))
    }

    @Test
    fun `V16 es idempotente si la categoria ya existia creada a mano`() {
        val url = TestDatabase.createTemporaryDatabase()
        migrar(url, hasta = "15")
        conConexion(url) { c ->
            c.createStatement().use {
                it.executeUpdate("INSERT INTO categories (name, color, icon) VALUES ('Belleza', '#123456', '💅')")
            }
        }

        migrar(url)

        // Se conserva la del usuario y no aparece una segunda fila.
        assertEquals(listOf("#123456" to "💅"), filasBelleza(url))
    }
}
