package com.subia.shared.storage

import com.subia.shared.model.AuthTokens

/**
 * Interfaz para el almacenamiento de tokens JWT.
 * Permite sustituir la implementación real ([TokenStorage]) por un fake en tests
 * sin depender del `expect class` que requiere contexto de plataforma.
 */
interface TokenStorageProvider {
    /** Persiste los tokens JWT en almacenamiento seguro. */
    fun saveTokens(tokens: AuthTokens)
    /** Devuelve los tokens almacenados, o `null` si no existen. */
    fun getTokens(): AuthTokens?
    /** Elimina todos los tokens del almacenamiento. */
    fun clearTokens()
    /** Comprueba si hay tokens persistidos. */
    fun hasTokens(): Boolean
}
