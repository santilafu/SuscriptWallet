package com.subia.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Configuración de Spring MVC.
 *
 * Registra un interceptor que añade [currentPath] al modelo de cada petición,
 * permitiendo que layout.html resalte el enlace de navegación activo sin
 * depender de #request (eliminado en Thymeleaf 3.1).
 */
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(CurrentPathInterceptor())
    }
}

/**
 * Interceptor que inyecta la ruta actual en el modelo como [currentPath].
 *
 * Se ejecuta después de que el controlador procesa la petición, justo antes
 * de que Thymeleaf renderice la plantilla.
 */
class CurrentPathInterceptor : HandlerInterceptor {

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        modelAndView?.model?.put("currentPath", request.servletPath)
    }
}