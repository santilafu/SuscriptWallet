package com.subia.integration

import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.Subscription
import com.subia.repository.SubscriptionRepository
import com.subia.service.DashboardService
import io.mockk.every
import io.mockk.mockk
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Test de INTEGRACIÓN de la migración V15 (intervalo_meses) contra PostgreSQL real.
 *
 * Migra una BD temporal (de [TestDatabase], se borra al acabar) solo hasta la V14, mete gastos
 * como los que ya hay en producción, aplica la V15 y comprueba que cada uno recibe el intervalo
 * correcto y que los totales del dashboard no cambian. No usa Spring: Flyway a mano, para poder
 * pararse en una versión intermedia.
 */
@EnabledIf("com.subia.integration.TestDatabase#available")
class IntervaloMesesMigrationTest {

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

    private data class Fila(val id: Long, val price: BigDecimal, val cycle: BillingCycle, val intervalo: Int)

    /** Cálculo del dashboard ANTERIOR a la periodicidad, copiado tal cual, como referencia. */
    private fun antiguoMensual(f: Fila): BigDecimal = when (f.cycle) {
        BillingCycle.MONTHLY -> f.price
        BillingCycle.YEARLY -> f.price.divide(BigDecimal("12"), 4, RoundingMode.HALF_UP)
        BillingCycle.WEEKLY -> f.price.multiply(BigDecimal("4.33"))
    }

    private fun antiguoAnual(f: Fila): BigDecimal = when (f.cycle) {
        BillingCycle.MONTHLY -> f.price.multiply(BigDecimal("12"))
        BillingCycle.YEARLY -> f.price
        BillingCycle.WEEKLY -> f.price.multiply(BigDecimal("52"))
    }

    private fun leerFilas(conIntervalo: Boolean): List<Fila> = conConexion { c ->
        val columnas = "id, price, billing_cycle" + if (conIntervalo) ", intervalo_meses" else ""
        c.createStatement().use { st ->
            st.executeQuery("SELECT $columnas FROM subscriptions ORDER BY id").use { rs ->
                buildList {
                    while (rs.next()) add(
                        Fila(
                            id = rs.getLong("id"),
                            price = rs.getBigDecimal("price"),
                            cycle = BillingCycle.valueOf(rs.getString("billing_cycle")),
                            intervalo = if (conIntervalo) rs.getInt("intervalo_meses") else -1
                        )
                    )
                }
            }
        }
    }

    @Test
    fun `V15 da intervalo 1 a mensuales y semanales, 12 a anuales, y no cambia los totales`() {
        migrarHasta("14")

        conConexion { c ->
            c.createStatement().use { st ->
                // Categoría sembrada por V2; user_id es opcional en el esquema (V7)
                st.executeUpdate(
                    """
                    INSERT INTO subscriptions (name, price, billing_cycle, renewal_date, category_id) VALUES
                      ('Netflix',  12.99, 'MONTHLY', DATE '2026-01-31', (SELECT MIN(id) FROM categories)),
                      ('Amazon',   49.90, 'YEARLY',  DATE '2024-02-29', (SELECT MIN(id) FROM categories)),
                      ('Gimnasio',  5.00, 'WEEKLY',  DATE '2026-03-02', (SELECT MIN(id) FROM categories)),
                      ('Dominio',   0.07, 'YEARLY',  DATE '2025-06-15', (SELECT MIN(id) FROM categories)),
                      ('Spotify',  10.99, 'MONTHLY', DATE '2026-05-10', (SELECT MIN(id) FROM categories))
                    """.trimIndent()
                )
            }
        }
        val antes = leerFilas(conIntervalo = false)
        val antesMensual = antes.sumOf { antiguoMensual(it) }.setScale(2, RoundingMode.HALF_UP)
        val antesAnual = antes.sumOf { antiguoAnual(it) }.setScale(2, RoundingMode.HALF_UP)

        migrarHasta("15")

        val despues = leerFilas(conIntervalo = true)
        assertEquals(antes.map { it.id }, despues.map { it.id })
        despues.forEach { f ->
            val esperado = if (f.cycle == BillingCycle.YEARLY) 12 else 1
            assertEquals(esperado, f.intervalo, "intervalo de la fila ${f.id} (${f.cycle})")
        }
        assertEquals(listOf(1, 12, 1, 12, 1), despues.map { it.intervalo })

        // Totales con el cálculo NUEVO (DashboardService real) sobre los datos ya migrados
        val repo = mockk<SubscriptionRepository>()
        val cat = Category(id = 1, name = "Streaming")
        every { repo.findByUserIdAndActiveTrue(7) } returns despues.map { f ->
            Subscription(
                id = f.id, name = "S${f.id}", price = f.price, billingCycle = f.cycle,
                renewalDate = LocalDate.parse("2026-12-01"), category = cat, userId = 7,
                intervaloMeses = f.intervalo
            )
        }
        val clock = Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneId.of("Europe/Madrid"))
        val dash = DashboardService(repo, clock).getDashboard(7)

        assertEquals(antesMensual, dash.totalMonthly)
        assertEquals(antesAnual, dash.totalYearly)
    }

    @Test
    fun `el CHECK de V15 rechaza intervalos no permitidos`() {
        migrarHasta("15")

        conConexion { c ->
            c.createStatement().use { st ->
                val insertar = { intervalo: Int ->
                    st.executeUpdate(
                        "INSERT INTO subscriptions (name, price, billing_cycle, renewal_date, category_id, intervalo_meses) " +
                            "VALUES ('X', 1.00, 'MONTHLY', DATE '2026-01-01', (SELECT MIN(id) FROM categories), $intervalo)"
                    )
                }
                listOf(1, 2, 3, 6, 12).forEach { assertEquals(1, insertar(it)) }
                listOf(0, -1, 4, 13).forEach { malo ->
                    assertThrows(SQLException::class.java) { insertar(malo) }
                }
            }
        }
    }
}
