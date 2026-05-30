package com.subia.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/** Páginas legales públicas (sin sesión): política de privacidad, etc. */
@Controller
class LegalController {

    @GetMapping("/privacidad")
    fun privacidad(): String = "legal/privacidad"
}
