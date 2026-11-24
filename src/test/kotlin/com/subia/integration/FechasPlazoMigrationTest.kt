package com.subia.integration

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Test de INTEGRACIÓN de la migración V17 (fecha_inicio / fecha_fin) contra PostgreSQL real.
 * BD temporal de [TestDatabase] (se borra al acabar), Flyway a mano para pararse en la V16.
 */
@EnabledIf("com.subia.integration.TestDatabase#available")
class FechasPlazoMigrationTest {

    private val url = TestDatabase.createTemporaryDatabase()

    private fun migrarHasta(version: String) {
        Flyway.configure()
            .dataSource(url, TestDatabase.USER, TestDatabase.PASSWORD)
            .locations("classpath:db/migration")
            .target(version)
            .load()
            .migrate()
    }

    private fun <T> conConexion(block: (Connection) -> T): T =
        DriverManager.getConnection(url, TestDatabase.USER, TestDatabase.PASSWORD).use(block)

    private fun insertar(c: Connection, inicio: String?, fin: String?): Int = c.createStatement().use {
        fun fecha(f: String?) = f?.let { "DATE '$it'" } ?: "NULL"
        it.executeUpdate(
            "INSERT INTO subscriptions (name, price, billing_cycle, renewal_date, category_id, fecha_inicio, fecha_fin) " +
                "VALUES ('X', 1.00, 'MONTHLY', DATE '2026-01-01', (SELECT MIN(id) FROM categories), ${fecha(inicio)}, ${fecha(fin)})"
        )
    }

    @Test
    fun `V17 deja las filas existentes sin fechas`() {
        migrarHasta("16")
        conConexion { c ->
            c.createStatement().use {
                it.executeUpdate(
                    "INSERT INTO subscriptions (name, price, billing_cycle, renewal_date, category_id) " +
                        "VALUES ('Netflix', 12.99, 'MONTHLY', DATE '2026-01-31', (SELECT MIN(id) FROM categories))"
                )
            }
        }

        migrarHasta("17")

        conConexion { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT fecha_inicio, fecha_fin FROM subscriptions").use { rs ->
                    assertTrue(rs.next())
                    assertNull(rs.getDate("fecha_inicio"))
                    assertNull(rs.getDate("fecha_fin"))
                }
            }
        }
    }

    @Test
    fun `el CHECK de V17 exige inicio menor o igual que fin`() {
        migrarHasta("17")
        conConexion { c ->
            assertEquals(1, insertar(c, null, null))
            assertEquals(1, insertar(c, "2026-01-15", null))
            assertEquals(1, insertar(c, null, "2025-01-15"))   // fin pasado: se acepta
            assertEquals(1, insertar(c, "2026-01-15", "2026-01-15"))
            assertEquals(1, insertar(c, "2026-01-15", "2027-01-15"))
            assertThrows(SQLException::class.java) { insertar(c, "2026-02-01", "2026-01-31") }
        }
    }

    @Test
    fun `V17 es idempotente - volver a ejecutarla no falla y el CHECK sigue`() {
        migrarHasta("17")
        val sql = javaClass.getResource("/db/migration/V17__fechas_plazo.sql")!!.readText()
        conConexion { c ->
            c.createStatement().use { it.execute(sql) }
            assertThrows(SQLException::class.java) { insertar(c, "2026-02-01", "2026-01-31") }
        }
    }
}
