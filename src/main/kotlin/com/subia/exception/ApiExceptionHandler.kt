package com.subia.exception

import com.subia.dto.api.ApiError
import com.subia.dto.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*

@RestControllerAdvice
class ApiExceptionHandler {

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

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneric(ex: Exception): ApiResponse<Nothing> =
        ApiResponse(error = ApiError("INTERNAL_ERROR", "Error interno del servidor"))
}