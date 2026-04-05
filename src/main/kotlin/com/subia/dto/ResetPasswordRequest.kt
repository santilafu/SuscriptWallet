package com.subia.dto

import jakarta.validation.constraints.NotBlank

data class ResetPasswordRequest(
    @field:NotBlank val token: String = "",
    @field:NotBlank val newPassword: String = "",
    @field:NotBlank val confirmPassword: String = ""
)