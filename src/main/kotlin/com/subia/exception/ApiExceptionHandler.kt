package com.subia.exception

import com.subia.controller.AuthController
import com.subia.controller.CatalogController
import com.subia.dto.api.ApiError
import com.subia.dto.api.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * Manejo de errores en JSON solo para la API REST: el paquete `controller.api` más los dos
 * @RestController que viven fuera de él ([AuthController], [CatalogController]). Los
 * @Controller web quedan fuera y siguen con la página de error estándar de Spring Boot.
 */
@RestControllerAdvice(
    basePackages = ["com.subia.controller.api"],
    assignableTypes = [AuthController::class, CatalogController::class]
)
class ApiExceptionHandler {

    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: NoSuchElementException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("NOT_FOUND", ex.message ?: "Recurso no encontrado"))

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(ex: IllegalStateException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("CONFLICT", ex.message ?: "Conflicto"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): ApiResponse<Nothing> {
        val details = ex.bindingResult.allErrors.map { e ->
            if (e is FieldError) "${e.field}: ${e.defaultMessage}" else e.defaultMessage ?: "Error de validación"
        }
        return ApiResponse(error = ApiError("VALIDATION_ERROR", "Datos de entrada no válidos", details))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("BAD_REQUEST", "Cuerpo de la petición malformado o valor de enum inválido"))

    @ExceptionHandler(BadCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleBadCredentials(ex: BadCredentialsException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("UNAUTHORIZED", ex.message ?: "Credenciales inválidas"))

    /**
     * Respeta el status y el motivo de las ResponseStatusException lanzadas por los controladores
     * (p. ej. 401 "Usuario no encontrado"); antes caían en el handler genérico como 500.
     */
    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiResponse<Nothing>> {
        val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        return ResponseEntity.status(ex.statusCode)
            .body(ApiResponse(error = ApiError(status.name, ex.reason ?: status.reasonPhrase)))
    }

    @ExceptionHandler(JwtException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleJwtException(ex: JwtException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("INVALID_TOKEN", "Token JWT inválido o expirado"))

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneric(ex: Exception): ApiResponse<Nothing> {
        // Registramos el stacktrace completo: sin esto, los 500 de la API eran invisibles en los logs.
        log.error("Error no controlado en un endpoint /api: {}", ex.message, ex)
        return ApiResponse(error = ApiError("INTERNAL_ERROR", "Error interno del servidor"))
    }
}