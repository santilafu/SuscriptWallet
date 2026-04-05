package com.subia.dto

import jakarta.validation.constraints.NotBlank

data class GoogleAuthRequest(@field:NotBlank val idToken: String = "")