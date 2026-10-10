package com.subia.exception

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

/**
 * Errores de los controladores web (Thymeleaf) con el status correcto.
 *
 * Antes, abrir `/subscriptions/{id}/edit` con un id ajeno o inexistente (NoSuchElementException)
 * o enviar un formulario con datos fuera de rango (IllegalArgumentException de la validación)
 * daba un 500. Ahora se responde 404/400 con `sendError`, que sigue mostrando la página de
 * error estándar de Spring Boot.
 *
 * Tiene menor prioridad que [ApiExceptionHandler], que sigue atendiendo la API en JSON.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice(basePackages = ["com.subia.controller"])
class WebExceptionHandler {

    private val log = LoggerFactory.getLogger(WebExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException, response: HttpServletResponse) {
        response.sendError(HttpStatus.NOT_FOUND.value(), ex.message)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException, response: HttpServletResponse) {
        log.debug("Petición web rechazada: {}", ex.message)
        response.sendError(HttpStatus.BAD_REQUEST.value(), ex.message)
    }
}
