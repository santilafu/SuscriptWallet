# Changelog — SubIA

Todas las versiones siguen [Semantic Versioning](https://semver.org/lang/es/):
- **MAJOR** — cambio incompatible o rediseño completo
- **MINOR** — nueva funcionalidad compatible con lo anterior
- **PATCH** — corrección de errores o ajustes menores

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

## [1.1.0] — pendiente

### Planificado
- Migración de H2 a PostgreSQL 16 con persistencia real
- `docker-compose.yml` para arrancar PostgreSQL con un solo comando

---

## [1.2.0] — pendiente

### Planificado
- API REST completa: `/api/subscriptions`, `/api/categories`, `/api/dashboard`
- DTOs separados de las entidades JPA
- Respuestas de error estructuradas `{data, error}`
- Validación de entrada con Bean Validation en todos los endpoints

---

## [1.3.0] — pendiente

### Planificado
- Rediseño completo de la interfaz (tema oscuro avanzado, glassmorphism, micro-interacciones)
- Vista grid / lista con toggle en la pantalla de suscripciones
- Toast notifications al guardar/borrar
- Modal de confirmación de borrado (eliminar página confirm-delete)
- Cálculo en tiempo real de equivalencia mensual/anual en el formulario
- Alpine.js para reactividad ligera
- Selector de catálogo visual en cards (no dropdown)

---

## [2.0.0] — pendiente

### Planificado
- Autenticación JWT (access token 15 min + refresh token 30 días)
- App móvil con Kotlin Multiplatform Mobile (KMM) + Compose Multiplatform
- Notificaciones push de renovación (Firebase Cloud Messaging)
- Widget en pantalla de inicio (iOS WidgetKit / Android Glance)
- Soporte offline con sincronización