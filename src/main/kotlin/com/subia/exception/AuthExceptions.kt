package com.subia.exception

class VerificationTokenException(message: String) : RuntimeException(message)
class InvalidGoogleTokenException(message: String = "Token de Google inválido") : RuntimeException(message)
class WeakPasswordException(message: String) : RuntimeException(message)
