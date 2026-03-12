package com.subia

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Clase principal de la aplicación SubIA.
 *
 * La anotación [@SpringBootApplication] activa el escaneo de componentes, la
 * autoconfiguración de Spring Boot y la configuración del contexto de aplicación.
 * Spring encontrará automáticamente todos los @Controller, @Service, @Repository, etc.
 * definidos dentro del paquete com.subia y sus subpaquetes.
 */
@SpringBootApplication
class SubIaApplication

/**
 * Punto de entrada de la aplicación.
 * Arranca el servidor Tomcat embebido y carga el contexto de Spring.
 */
fun main(args: Array<String>) {
    runApplication<SubIaApplication>(*args)
}