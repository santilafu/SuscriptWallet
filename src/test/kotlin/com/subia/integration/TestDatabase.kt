package com.subia.integration

import org.springframework.test.context.DynamicPropertyRegistry
import java.sql.DriverManager
import java.util.UUID

/**
 * Base de datos PostgreSQL TEMPORAL para los tests de integración (auditoría ronda 4).
 *
 * Los tests con mocks no detectaban fallos de transacciones (rollback de escrituras al lanzar
 * una excepción), así que estos tests arrancan Spring entero contra un PostgreSQL real: el de
 * `docker-compose.yml` (localhost:5433). Se crea una BD nueva `subia_it_<aleatorio>` (Flyway
 * aplica todas las migraciones) y se borra al terminar la JVM de tests. La BD de desarrollo
 * `subia` no se toca.
 *
 * Si el PostgreSQL de docker no está levantado, los tests de integración se SALTAN (no fallan):
 * ver [available], usado con `@EnabledIf`.
 */
object TestDatabase {
    private const val HOST = "localhost:5433"
    const val USER = "subia_user"
    const val PASSWORD = "subia_pass"
    private const val ADMIN_URL = "jdbc:postgresql://$HOST/postgres"

    /** true si el PostgreSQL de docker-compose responde. */
    @JvmStatic
    fun available(): Boolean = runCatching {
        DriverManager.getConnection(ADMIN_URL, USER, PASSWORD).use { it.isValid(2) }
    }.getOrDefault(false)

    /** BD temporal compartida por los tests que arrancan Spring (Flyway la migra entera). */
    private val url: String by lazy { createTemporaryDatabase() }

    /**
     * Crea una BD `subia_it_<aleatorio>` VACÍA y devuelve su URL JDBC; se borra al terminar la
     * JVM de tests. La usan también los tests que migran con Flyway paso a paso (p. ej. hasta
     * una versión concreta), que no pueden compartir la BD ya migrada de Spring.
     */
    fun createTemporaryDatabase(): String {
        val dbName = "subia_it_" + UUID.randomUUID().toString().replace("-", "").take(12)
        DriverManager.getConnection(ADMIN_URL, USER, PASSWORD).use { conn ->
            conn.createStatement().use { it.execute("CREATE DATABASE $dbName") }
        }
        Runtime.getRuntime().addShutdownHook(Thread {
            runCatching {
                DriverManager.getConnection(ADMIN_URL, USER, PASSWORD).use { conn ->
                    conn.createStatement().use { it.execute("DROP DATABASE IF EXISTS $dbName WITH (FORCE)") }
                }
            }
        })
        return "jdbc:postgresql://$HOST/$dbName"
    }

    fun register(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { url }
        registry.add("spring.datasource.username") { USER }
        registry.add("spring.datasource.password") { PASSWORD }
        // Sin admin de arranque ni emails reales
        registry.add("app.admin.password") { "" }
        registry.add("resend.api-key") { "" }
    }
}
