package com.subia.dto.api

data class ApiResponse<T>(
    val data: T? = null,
    val error: ApiError? = null
)

data class ApiError(
    val code: String,
    val message: String,
    val details: List<String> = emptyList()
)