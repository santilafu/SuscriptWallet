package com.subia.exception

import com.subia.controller.AuthController
import com.subia.controller.CatalogController
import com.subia.dto.api.ApiError
import com.subia.dto.api.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * Manejo de errores en JSON solo para la API REST: el paquete `controller.api` más los dos
 * @RestController que viven fuera de él ([AuthController], [CatalogController]). Los
 * @Controller web quedan fuera y siguen con la página de error estándar de Spring Boot.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
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

    /**
     * Validaciones de negocio (p. ej. [com.subia.service.SubscriptionService.validated]) y
     * parámetros mal formados: 400 en vez de 500.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("VALIDATION_ERROR", ex.message ?: "Datos de entrada no válidos"))

    /** `/api/subscriptions/abc` y similares: antes 500, ahora 400. */
    @ExceptionHandler(TypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleTypeMismatch(ex: TypeMismatchException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("BAD_REQUEST", "Parámetro con formato no válido: ${ex.propertyName ?: ""}".trim()))

    /** Restricciones de la base de datos (FK, longitudes, unicidad) que se escapen a la validación. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ApiResponse<Nothing> {
        log.warn("Violación de integridad en /api: {}", ex.mostSpecificCause.message)
        return ApiResponse(error = ApiError("CONFLICT", "Los datos no cumplen las restricciones del servidor"))
    }

    /**
     * Login de la API con la cuenta bloqueada o sin verificar: antes caía en el handler genérico
     * y la app recibía un 500 "Error interno" en lugar de un motivo.
     */
    @ExceptionHandler(LockedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleLocked(ex: LockedException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("ACCOUNT_LOCKED", "Cuenta bloqueada temporalmente por intentos fallidos. Inténtalo más tarde."))

    @ExceptionHandler(DisabledException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleDisabled(ex: DisabledException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("EMAIL_NOT_VERIFIED", "Debes verificar tu email antes de iniciar sesión."))

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDenied(ex: AccessDeniedException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("FORBIDDEN", "No tienes permisos para acceder a este recurso."))

    @ExceptionHandler(InvalidGoogleTokenException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleInvalidGoogle(ex: InvalidGoogleTokenException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("UNAUTHORIZED", ex.message ?: "Token de Google inválido"))

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

    /** Resto de fallos de autenticación (no BadCredentials/Locked/Disabled). */
    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleAuthentication(ex: AuthenticationException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("UNAUTHORIZED", "Autenticación requerida"))

    @ExceptionHandler(JwtException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleJwtException(ex: JwtException): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("INVALID_TOKEN", "Token JWT inválido o expirado"))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        // Excepciones de Spring MVC con su propio status (parámetro obligatorio ausente, método
        // o Content-Type no soportado…): antes acababan aquí como 500.
        if (ex is ErrorResponse) {
            val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.BAD_REQUEST
            return ResponseEntity.status(ex.statusCode)
                .body(ApiResponse(error = ApiError(status.name, ex.body.detail ?: status.reasonPhrase)))
        }
        // Registramos el stacktrace completo: sin esto, los 500 de la API eran invisibles en los logs.
        log.error("Error no controlado en un endpoint /api: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse(error = ApiError("INTERNAL_ERROR", "Error interno del servidor")))
    }
}