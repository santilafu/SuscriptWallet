package com.subia.exception

class VerificationTokenException(message: String) : RuntimeException(message)
class InvalidGoogleTokenException(message: String = "Token de Google inválido") : RuntimeException(message)
class WeakPasswordException(message: String) : RuntimeException(message)

/** Datos de registro no válidos; [code] es el valor de `?error=` que recibe la página de registro. */
class InvalidRegistrationException(val code: String, message: String) : RuntimeException(message)
