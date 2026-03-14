# Changelog — SubIA

Todas las versiones siguen [Semantic Versioning](https://semver.org/lang/es/):
- **MAJOR** — cambio incompatible o rediseño completo
- **MINOR** — nueva funcionalidad compatible con lo anterior
- **PATCH** — corrección de errores o ajustes menores

---

## [1.2.0] — 2026-03-13

API REST completa (P1) para consumo desde la app móvil.

### Añadido
- API REST completa (P1): endpoints `/api/subscriptions` (CRUD), `/api/categories` (CRUD), `/api/dashboard`
- `ApiResponse<T>` wrapper genérico con `ApiError` estructurado `{ data, error }`
- `ApiExceptionHandler` (`@RestControllerAdvice`): gestión de 404, 409, 400 y 500 con mensajes en castellano
- DTOs separados para request/response bajo `dto/api/`: `SubscriptionRequestDto`, `SubscriptionResponseDto`, `CategoryRequestDto`, `CategoryResponseDto`, `DashboardStatsDto`
- Controladores REST bajo `controller/api/`: `ApiSubscriptionController`, `ApiCategoryController`, `ApiDashboardController`

### Notas técnicas
- `DashboardService.getDashboard()` (no `getDashboardData()`)
- `activeCount` calculado con `subscriptionService.findActive().size`
- `alertCount` calculado con `dash.alertRenewals.size`
- PUT usa `save()` con id seteado en la entidad (no método `update()` separado)

---

## [1.1.0] — 2026-03-13

Migración de base de datos a PostgreSQL para persistencia real.

### Cambiado
- Migración H2 → PostgreSQL 16 (Docker, puerto 5433)
- Añadido driver PostgreSQL y `flyway-database-postgresql` para soporte PG16
- `docker-compose.yml` en la raíz para arrancar PostgreSQL con un solo comando
- Chivato de conexión a BD en startup (`ApplicationRunner` en `SubIaApplication.kt`)

---

## [1.0.0] — 2026-03-13

Primera versión funcional completa de la aplicación web.

### Añadido
- Dashboard con gasto mensual/anual, gráfico de dona por categoría (Chart.js 4) y alertas de renovación a 7 y 30 días
- Lista de suscripciones con búsqueda en tiempo real y filtros por categoría (pills interactivos)
- Formulario de nueva suscripción con selector de catálogo AJAX (categoría → servicio → autorellenar)
- Catálogo estático de 80+ servicios con precios en EUR actualizados a marzo 2026
- 10 categorías predefinidas (IA, Streaming, Música, Software, Cloud, Gaming, Seguridad, Noticias y Lectura, Salud y Deporte, Desarrollo) creadas por Flyway en el primer arranque
- CRUD completo de suscripciones y categorías
- Diseño con tema oscuro: navbar gradiente, tarjetas con acento de color, Inter font, animación fadeInUp
- Spring Security con headers HTTP (X-Frame-Options, X-Content-Type-Options)
- Migraciones Flyway (V1 schema + V2 seed categorías)
- Endpoint REST `/api/catalog` (base para la futura app móvil)
- `PROMPT.md` con roadmap completo: PostgreSQL, API REST, app móvil, tests y seguridad

### Corregido
- `#httpServletRequest` eliminado en Thymeleaf 3.1 → resuelto con interceptor `CurrentPathInterceptor`
- Sesión HTTP no se puede crear después de que el response está comprometido → CSRF desactivado para uso local
- Enum `BillingCycle` comparado con string en SpEL siempre devolvía falso → `cycle.name() == 'MONTHLY'`

### Stack
- Kotlin 2.1.20 · Spring Boot 3.3.5 · Java 21 · H2 (en memoria) · Flyway · Thymeleaf 3.1 · Bootstrap 5.3

---

## [1.3.0] — 2026-03-14

Autenticación JWT (P2) para securizar la API REST de cara a la app móvil.

### Añadido
- Autenticación JWT (P2): POST /api/auth/login, /refresh, /logout
- Refresh tokens persistidos en PostgreSQL con rotación automática (Flyway V3)
- Rate limiting 100 req/min por IP (Bucket4j)
- CORS configurado para /api/**
- Respuestas 401/403 en formato JSON estándar (`CustomAuthEntryPoint` + `CustomAccessDeniedHandler`)
- Dual FilterChain: /api/** stateless JWT, /** permitAll Thymeleaf (sin romper la web)
- `JwtConfig.kt` separado de `JwtService.kt` para evitar dependencia circular
- `TokenService` con auto-detección de hash BCrypt en `app.auth.password`

---

## [2.0.0] — pendiente

### Planificado
- Autenticación JWT (access token 15 min + refresh token 30 días)
- App móvil con Kotlin Multiplatform Mobile (KMM) + Compose Multiplatform
- Notificaciones push de renovación (Firebase Cloud Messaging)
- Widget en pantalla de inicio (iOS WidgetKit / Android Glance)
- Soporte offline con sincronización