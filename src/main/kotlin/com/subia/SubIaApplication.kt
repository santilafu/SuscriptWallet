package com.subia

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import javax.sql.DataSource

/**
 * Clase principal de la aplicación SubIA.
 *
 * La anotación [@SpringBootApplication] activa el escaneo de componentes, la
 * autoconfiguración de Spring Boot y la configuración del contexto de aplicación.
 * Spring encontrará automáticamente todos los @Controller, @Service, @Repository, etc.
 * definidos dentro del paquete com.subia y sus subpaquetes.
 */
@SpringBootApplication
@EnableAsync
class SubIaApplication {

    private val log = LoggerFactory.getLogger(SubIaApplication::class.java)

    /**
     * Verifica la conexión a PostgreSQL al arrancar e imprime la URL de conexión en el log.
     * Usa [DataSource.getConnection] y [java.sql.Connection.isValid] para confirmar
     * que la conexión es real, no solo que las propiedades están configuradas.
     */
    @Bean
    fun verificarConexionBD(dataSource: DataSource) = ApplicationRunner {
        dataSource.connection.use { conn ->
            val url = conn.metaData.url
            val urlSinPassword = url.substringBefore("?") // quitar query params si los hubiera
            if (conn.isValid(2)) {
                log.info("✅ Conectado a PostgreSQL — {}", urlSinPassword)
            } else {
                log.error("❌ La conexión a PostgreSQL NO es válida — {}", urlSinPassword)
            }
        }
    }
}

/**
 * Punto de entrada de la aplicación.
 * Arranca el servidor Tomcat embebido y carga el contexto de Spring.
 */
fun main(args: Array<String>) {
    runApplication<SubIaApplication>(*args)
}