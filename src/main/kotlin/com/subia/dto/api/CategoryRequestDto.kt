package com.subia.dto.api

import jakarta.validation.constraints.NotBlank

data class CategoryRequestDto(
    @field:NotBlank val name: String,
    @field:NotBlank val color: String,
    @field:NotBlank val icon: String
)