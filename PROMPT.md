
# SubIA — Briefing completo para asistente IA

> **Instrucciones para el asistente**: Lee este documento entero antes de tocar código.
> Responde siempre en castellano de España. Sin rodeos, sin ejemplos innecesarios.
> Cuando termines una tarea, actualiza CHANGELOG.md, PROMPT.md y README.md si corresponde, luego haz commit y push.

---

## Visión del producto

SubIA es un ecosistema multiplataforma de gestión de suscripciones de pago para uso personal.

El usuario necesita controlar en un solo lugar todos sus servicios de suscripción:
cuánto gasta, cuándo renuevan, qué tiene activo y qué no.

**Plataformas objetivo**:
- **Web** (implementada): Spring Boot + Thymeleaf, accesible en http://localhost:8081
- **Móvil** (por implementar): app nativa iOS y Android que consume una API REST del backend

**Principios no negociables**:
- Todo en **EUR** — mercado europeo, nunca USD.
- UI y mensajes en **castellano de España** (ni inglés, ni variantes latinoamericanas).
- Datos del usuario son privados — nunca compartir, nunca loguear precios o notas.
- La app web y la app móvil comparten el mismo backend Spring Boot.

---

## Entorno de desarrollo

| Variable   | Valor                                              |
|------------|----------------------------------------------------|
| OS         | Windows 11                                         |
| Shell      | Git Bash — usar sintaxis Unix, nunca CMD/PowerShell |
| JDK        | OpenJDK 25 — `C:/Users/santi/.jdks/openjdk-25.0.1` |
| Puerto     | **8081** (el 8080 está ocupado por prunsrv Windows) |
| URL local  | http://localhost:8081                              |

```bash
# Arrancar PostgreSQL (Docker) — necesario antes de bootRun
docker-compose up -d

# Arrancar la aplicación
JAVA_HOME="/c/Users/santi/.jdks/openjdk-25.0.1" ./gradlew bootRun

# Matar proceso en 8081 si está ocupado
netstat -ano | grep ":8081"
taskkill //PID <pid> //F
```

---

## Stack tecnológico actual (web)

| Capa          | Tecnología                                          |
|---------------|-----------------------------------------------------|
| Lenguaje      | Kotlin 2.1.20                                       |
| Framework     | Spring Boot 3.3.5                                   |
| Seguridad     | Spring Security 6.3 + JWT HMAC-SHA256 (Nimbus) + Bucket4j |
| Persistencia  | Spring Data JPA + Hibernate 6.5                     |
| Base de datos | PostgreSQL 16 (Docker, puerto 5433)                 |
| Migraciones   | Flyway (V1 schema, V2 seed categorías, V3 refresh_tokens) |
| Frontend      | Thymeleaf 3.1 + Layout Dialect                      |
| UI web        | Tailwind CSS CDN + Alpine.js CDN + Chart.js 4.4    |
| Tipografía    | Inter (Google Fonts)                                |
| Build         | Gradle Kotlin DSL                                   |

## Stack planificado (móvil)

| Capa          | Tecnología recomendada                              |
|---------------|-----------------------------------------------------|
| iOS           | Swift + SwiftUI                                     |
| Android       | Kotlin + Jetpack Compose                            |
| Multiplataforma (alternativa) | Kotlin Multiplatform Mobile (KMM) + Compose Multiplatform |
| API           | REST JSON expuesta por el mismo backend Spring Boot |
| Autenticación móvil | JWT (Bearer token) — ver sección de seguridad  |
| Almacenamiento local | SwiftData (iOS) / Room (Android) para caché offline |

---

## Estructura del proyecto (estado actual)

```
src/main/kotlin/com/subia/
├── SubIaApplication.kt
├── config/
│   ├── SecurityConfig.kt          # CSRF off, headers HTTP seguridad
│   └── WebConfig.kt               # Interceptor: currentPath → modelo Thymeleaf
├── controller/
│   ├── CatalogController.kt       # GET /api/catalog?categoryId=X → JSON
│   ├── CategoryController.kt      # CRUD /categories (web)
│   ├── DashboardController.kt     # GET /dashboard (web)
│   ├── SubscriptionController.kt  # CRUD /subscriptions (web)
│   └── api/
│       ├── ApiSubscriptionController.kt  # REST /api/subscriptions
│       ├── ApiCategoryController.kt      # REST /api/categories
│       └── ApiDashboardController.kt     # REST /api/dashboard
├── dto/
│   ├── DashboardDto.kt
│   └── api/
│       ├── ApiResponse.kt         # Wrapper genérico { data, error }
│       ├── ApiError.kt
│       ├── SubscriptionRequestDto.kt + SubscriptionResponseDto.kt
│       ├── CategoryRequestDto.kt + CategoryResponseDto.kt
│       └── DashboardStatsDto.kt
├── exception/
│   └── ApiExceptionHandler.kt     # @RestControllerAdvice: 404, 409, 400, 500
├── model/
│   ├── CatalogItem.kt             # data class, no entidad JPA
│   ├── Category.kt                # Entidad JPA → tabla categories
│   └── Subscription.kt           # Entidad JPA → tabla subscriptions
│                                  # enum BillingCycle {MONTHLY, YEARLY, WEEKLY}
├── repository/
│   ├── CategoryRepository.kt
│   └── SubscriptionRepository.kt
└── service/
    ├── CatalogService.kt          # 80+ servicios en EUR, precios marzo 2026
    ├── CategoryService.kt
    ├── DashboardService.kt        # Normalización precios a mensual/anual
    └── SubscriptionService.kt

src/main/resources/
├── application.properties
├── db/migration/
│   ├── V1__init_schema.sql        # Tablas categories y subscriptions
│   └── V2__seed_categories.sql    # 10 categorías con emoji y color
└── templates/                     # Thymeleaf
    ├── layout.html
    ├── dashboard.html
    ├── categories/{form,list}.html
    └── subscriptions/{form,list,confirm-delete}.html
```

### Schema SQL actual

```sql
CREATE TABLE categories (
    id    BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    color VARCHAR(20)  NOT NULL DEFAULT '#6c757d',
    icon  VARCHAR(50)  NOT NULL DEFAULT ''
);

CREATE TABLE subscriptions (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    description   VARCHAR(500)   NOT NULL DEFAULT '',
    price         DECIMAL(10,2)  NOT NULL,
    currency      VARCHAR(10)    NOT NULL DEFAULT 'EUR',
    billing_cycle VARCHAR(20)    NOT NULL,  -- MONTHLY | YEARLY | WEEKLY
    renewal_date  DATE           NOT NULL,
    category_id   BIGINT         NOT NULL REFERENCES categories(id),
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    notes         VARCHAR(1000)  NOT NULL DEFAULT ''
);
```

---

## Funcionalidades implementadas

| Funcionalidad                        | Estado | Notas técnicas |
|--------------------------------------|--------|----------------|
| Dashboard con stat cards             | ✅     | Mensual, anual, activas, próximas renovaciones |
| Gráfico de dona por categoría        | ✅     | Chart.js 4, datos inline Thymeleaf |
| Alertas renovación (7 y 30 días)     | ✅     | Tabla en dashboard |
| Lista de suscripciones               | ✅     | Tabla responsive |
| Búsqueda en tiempo real              | ✅     | JS puro, filtra nombre/descripción/notas |
| Filtros por categoría (pills)        | ✅     | JS puro, sin recarga |
| Crear suscripción                    | ✅     | Formulario HTML5 |
| Selector de catálogo AJAX            | ✅     | GET /api/catalog?categoryId=X → JSON |
| Editar suscripción                   | ✅     | Mismo template, sin selector catálogo |
| Eliminar con confirmación            | ✅     | Página confirm-delete |
| CRUD categorías                      | ✅     | Grid de cards con color e icono |
| 10 categorías predefinidas           | ✅     | Flyway V2, presentes desde arranque |
| Catálogo 80+ servicios en EUR        | ✅     | CatalogService.kt estático |
| Navbar activo                        | ✅     | currentPath via WebConfig interceptor |
| API REST JSON /api/catalog           | ✅     | Base para la app móvil |
| API REST /api/subscriptions (CRUD)   | ✅     | v1.2.0 — controlador + DTOs + wrapper ApiResponse |
| API REST /api/categories (CRUD)      | ✅     | v1.2.0 — controlador + DTOs |
| API REST /api/dashboard (stats)      | ✅     | v1.2.0 — gasto mensual/anual, activas, alertas |
| ApiExceptionHandler                  | ✅     | v1.2.0 — 404, 409, 400, 500 en castellano |
| JWT auth — POST /api/auth/login      | ✅     | v1.3.0 — HMAC-SHA256, devuelve accessToken + refreshToken |
| JWT auth — POST /api/auth/refresh    | ✅     | v1.3.0 — rotación de tokens, invalida el anterior |
| JWT auth — POST /api/auth/logout     | ✅     | v1.3.0 — invalida refresh token, 204 |
| Refresh tokens en PostgreSQL         | ✅     | v1.3.0 — tabla refresh_tokens, Flyway V3 |
| Dual FilterChain (web + API)         | ✅     | v1.3.0 — /api/** stateless JWT, /** Thymeleaf permitAll |
| Rate limiting Bucket4j               | ✅     | v1.3.0 — 100 req/min por IP |
| CORS /api/**                         | ✅     | v1.3.0 — configurado para la app móvil |
| 401/403 JSON estándar                | ✅     | v1.3.0 — CustomAuthEntryPoint + CustomAccessDeniedHandler |

---

## Tareas pendientes (por orden de prioridad)

### ~~P0 — Migrar H2 → PostgreSQL~~ ✅ COMPLETADO (v1.1.0, 2026-03-13)

PostgreSQL 16 activo via Docker en puerto 5433. Driver y flyway-database-postgresql añadidos.
`docker-compose.yml` en la raíz. `ApplicationRunner` en `SubIaApplication.kt` verifica la conexión al arrancar.

---

### ~~P1 — API REST completa para la app móvil~~ ✅ COMPLETADO (v1.2.0, 2026-03-13)

API REST completa implementada. Controladores bajo `controller/api/`, DTOs bajo `dto/api/`.

**Endpoints implementados**:

```
# Suscripciones
GET    /api/subscriptions              → lista completa
GET    /api/subscriptions/{id}         → detalle
POST   /api/subscriptions              → crear  (body JSON)
PUT    /api/subscriptions/{id}         → editar (body JSON)
DELETE /api/subscriptions/{id}         → borrar

# Categorías
GET    /api/categories                 → lista completa
GET    /api/categories/{id}            → detalle
POST   /api/categories                 → crear
PUT    /api/categories/{id}            → editar
DELETE /api/categories/{id}            → borrar

# Dashboard
GET    /api/dashboard                  → stats (gasto mensual, anual, activas, alertas renovación)

# Catálogo (ya existía)
GET    /api/catalog                    → todos los servicios
GET    /api/catalog?categoryId=X       → filtrado por categoría
```

**Formato de respuesta estándar**:
```json
{
  "data": { ... },
  "error": null
}
```
O en error:
```json
{
  "data": null,
  "error": { "code": "NOT_FOUND", "message": "Suscripción no encontrada" }
}
```

**Adaptaciones reales vs diseño original**:
- `DashboardService.getDashboard()` (no `getDashboardData()`)
- `activeCount` se obtiene de `subscriptionService.findActive().size`
- `alertCount` se obtiene de `dash.alertRenewals.size`
- PUT usa `save()` con el id seteado en la entidad (no existe método `update()` separado)

**Archivos creados**:
- `controller/api/ApiSubscriptionController.kt`
- `controller/api/ApiCategoryController.kt`
- `controller/api/ApiDashboardController.kt`
- `dto/api/ApiResponse.kt` — wrapper genérico `ApiResponse<T>`
- `dto/api/ApiError.kt`
- `dto/api/SubscriptionRequestDto.kt` + `SubscriptionResponseDto.kt`
- `dto/api/CategoryRequestDto.kt` + `CategoryResponseDto.kt`
- `dto/api/DashboardStatsDto.kt`
- `exception/ApiExceptionHandler.kt` — `@RestControllerAdvice` para 404, 409, 400, 500

---

### ~~P2 — Seguridad de la API REST (para app móvil)~~ ✅ COMPLETADO (v1.3.0, 2026-03-14)

JWT HMAC-SHA256 via `spring-boot-starter-oauth2-resource-server` + Nimbus. La app web sigue sin autenticación (uso local).

**Endpoints de autenticación implementados**:

```
POST /api/auth/login    → { accessToken, refreshToken, expiresInSeconds, tokenType }
POST /api/auth/refresh  → rotación de tokens (invalida el anterior)
POST /api/auth/logout   → 204 No Content
```

**Rutas públicas** (no requieren JWT):
- `/api/auth/**`
- `/api/catalog`
- `/api/catalog/**`
- `/**` (toda la interfaz Thymeleaf)

**Rutas protegidas** (requieren `Authorization: Bearer <token>`):
- Todo `/api/**` excepto las públicas anteriores

**Variables de entorno**:

| Variable | Default (dev) | Producción |
|----------|---------------|------------|
| `JWT_SECRET` | `SubIA-dev-secret-key-32chars-min` | Clave HMAC aleatoria de mínimo 32 caracteres |
| `APP_AUTH_PASSWORD` | `password` (texto plano, se hashea al arrancar) | Hash BCrypt — generar con `htpasswd -bnBC 10 "" <pass>` |

**Gotchas de implementación** (ver también tabla de gotchas más abajo):
- `JwtConfig.kt` separado de `JwtService.kt` para evitar dependencia circular con Spring.
- `TokenService` auto-detecta si `app.auth.password` es un hash BCrypt (`$2a$...`) o texto plano y lo hashea al arrancar.
- Arranque de desarrollo: solo `./gradlew bootRun` sin env vars adicionales.

**Flyway V3** — tabla `refresh_tokens` creada por migración. No modificar V1/V2.

---

### ~~P3 — Rediseño completo de la interfaz web~~ ✅ COMPLETADO (v1.4.0, 2026-03-14)

Bootstrap 5.3 eliminado. Tailwind CSS CDN + Alpine.js CDN. Dark mode permanente (`html class="dark"`, fondo `#0a0f1e`). 7 templates reescritos: layout, dashboard, subscriptions/list, subscriptions/form, subscriptions/confirm-delete, categories/list, categories/form. Glassmorphism en stat cards. Grid de cards con color dinámico de categoría vía `th:style`. Wizard 3 pasos en formulario con Alpine.js. Modal inline de borrado. Chart.js adaptado a dark mode. Toda la lógica Thymeleaf preservada.

La interfaz actual es funcional pero visualmente anticuada. Objetivo: nivel SaaS moderno.

**Inspiración**: Linear, Vercel Dashboard, Raycast.

#### Sistema de diseño

Paleta (CSS variables en `:root`):
```css
--bg-base:      #0a0f1e;   /* fondo principal */
--bg-surface:   #111827;   /* cards, sidebar */
--bg-elevated:  #1a2235;   /* modales, dropdowns */
--bg-hover:     #1e2d45;   /* hover state */
--border:       rgba(255,255,255,0.07);
--text-primary: #f1f5f9;
--text-muted:   #64748b;
--text-subtle:  #334155;
--accent:       #6366f1;   /* índigo — acción principal */
--accent-hover: #4f46e5;
--danger:       #ef4444;
--success:      #22c55e;
--warning:      #f59e0b;
```

Reglas base:
- Glassmorphism en cards destacadas: `background: rgba(255,255,255,0.03); backdrop-filter: blur(8px); border: 1px solid var(--border)`.
- `transition: all 0.2s ease` en todos los elementos interactivos.
- `border-radius: 12px` en cards, `8px` en botones, `6px` en inputs.
- Sin sombras de caja oscuras — usar `box-shadow: 0 0 0 1px var(--border), 0 4px 24px rgba(0,0,0,0.4)`.

#### Mejoras por página

**Dashboard**:
- Stat cards "hero metric": número muy grande, icono de color a la derecha, badge de tendencia.
- Card extra: gasto diario estimado (mensual ÷ 30).
- Dona + leyenda como lista de categorías con su importe y barra de progreso relativa.
- Renovaciones como timeline vertical: borde izquierdo de color, badge de días restantes
  (rojo < 7 días, naranja < 30 días, verde el resto).

**Lista de suscripciones**:
- Toggle tabla/grid (botones con `bi-list` y `bi-grid`, estado en localStorage).
- Vista grid: card con borde izquierdo del color de la categoría, nombre grande,
  precio destacado, badge ciclo, badge categoría, días hasta renovar, acciones en hover.
- Filtros de categoría con contador: "Streaming (3)", "IA (2)".
- Ordenación al hacer click en cabeceras de columna.
- Animación de entrada escalonada (stagger) con CSS animation-delay.

**Formulario nueva suscripción**:
- Selector de catálogo como grid visual de cards clickables (no dropdown).
- Wizard en 3 pasos numerados: "Servicio" → "Precio" → "Cuándo renueva".
- Cálculo en tiempo real: al escribir precio y ciclo → muestra "= X€/mes · X€/año".
- Toast de éxito al guardar (Flash Attribute + Bootstrap 5 Toast en layout.html).
- Modal de confirmación al borrar (Bootstrap 5 Modal, eliminar la página confirm-delete).

**Implementación**:
- Crear `src/main/resources/static/css/app.css` con todo el CSS custom.
- Añadir **Alpine.js** via CDN para reactividad ligera (toggle vista, wizard steps, contador):
  `<script defer src="https://cdn.jsdelivr.net/npm/alpinejs@3/dist/cdn.min.js"></script>`
- Los toasts: `redirectAttributes.addFlashAttribute("toastMessage", "Suscripción guardada")`,
  el layout los detecta y activa el Toast de Bootstrap.

---

### P4 — App móvil KMM + Compose Multiplatform en Android Studio

**Estrategia**: Kotlin Multiplatform Mobile (KMM) + Compose Multiplatform. Desarrollar en Android Studio.
Permite compartir lógica de negocio (llamadas API, modelos, validaciones) entre iOS y Android,
manteniendo UI nativa por plataforma o compartida con Compose.

**Funcionalidades de la app móvil** (mismas que web, adaptadas a móvil):
- Dashboard con resumen de gasto y próximas renovaciones.
- Lista de suscripciones con búsqueda y filtro por categoría.
- Formulario de nueva suscripción con selector de catálogo.
- Notificaciones push de renovación (7 días y 1 día antes) — Firebase Cloud Messaging.
- Widget en pantalla de inicio (iOS WidgetKit, Android Glance) con gasto mensual.
- Soporte offline: caché local con sincronización al reconectar.
- Modo oscuro por defecto (coherente con la web).

**Estructura de carpetas KMM sugerida**:
```
subia-mobile/
├── shared/                    # Código compartido iOS + Android
│   ├── commonMain/
│   │   ├── api/               # Cliente HTTP (Ktor)
│   │   ├── model/             # Modelos de datos
│   │   ├── repository/        # Lógica de acceso a datos
│   │   └── usecase/           # Casos de uso de negocio
│   ├── iosMain/               # Código específico iOS
│   └── androidMain/           # Código específico Android
├── androidApp/                # App Android (Jetpack Compose)
└── iosApp/                    # App iOS (SwiftUI)
```

**Librería HTTP en KMM**: Ktor Client con serialización kotlinx.serialization.

---

## Plan de testing

### Tests backend (JUnit 5 + Spring Boot Test)

#### Unit tests — `src/test/kotlin/com/subia/`

| Clase a testear      | Qué testear                                                              |
|----------------------|--------------------------------------------------------------------------|
| `DashboardService`   | Normalización de precios: MONTHLY, YEARLY, WEEKLY → mensual y anual correctos. Casos límite: precio 0, ciclo WEEKLY con precio raro. |
| `CatalogService`     | `getItemsForCategory` devuelve la categoría correcta para cada clave. Que no devuelve vacío. Que el fallback devuelve todo el catálogo. |
| `SubscriptionService`| CRUD básico: save, findAll, findById lanza excepción si no existe, delete funciona. |
| `CategoryService`    | No se puede borrar una categoría con suscripciones asociadas (lanza `IllegalStateException`). |

Ejemplo de test unitario con Mockito:
```kotlin
@ExtendWith(MockitoExtension::class)
class DashboardServiceTest {
    @Mock lateinit var subscriptionRepo: SubscriptionRepository
    @InjectMocks lateinit var service: DashboardService

    @Test
    fun `precio YEARLY se normaliza a mensual dividiendo entre 12`() {
        // given
        val sub = Subscription(price = BigDecimal("120.00"), billingCycle = BillingCycle.YEARLY, ...)
        whenever(subscriptionRepo.findByActiveTrue()).thenReturn(listOf(sub))
        // when
        val dto = service.getDashboardData()
        // then
        assertThat(dto.monthlyTotal).isEqualByComparingTo(BigDecimal("10.00"))
    }
}
```

#### Integration tests — con `@SpringBootTest`

| Test                          | Qué verifica                                                         |
|-------------------------------|----------------------------------------------------------------------|
| `CatalogControllerTest`       | GET /api/catalog devuelve 200 y JSON válido. Con categoryId devuelve solo esa categoría. |
| `SubscriptionControllerTest`  | Flujo completo: crear → listar → editar → borrar. Que POST redirige (302). |
| `CategoryControllerTest`      | No se puede borrar categoría con suscripciones (muestra error en la vista). |
| `DashboardControllerTest`     | GET /dashboard devuelve 200 con los atributos correctos en el modelo. |

Usar `@Sql` para limpiar y preparar datos antes de cada test de integración.
Usar TestContainers para PostgreSQL en los tests de integración (no H2):
```kotlin
@Container
val postgres = PostgreSQLContainer("postgres:16")
```

#### Tests de API REST (cuando se implemente)

- Usar `MockMvc` + `@WebMvcTest` para tests de capa controlador sin levantar la app entera.
- Verificar: status HTTP correcto, Content-Type `application/json`, estructura del JSON.
- Verificar autenticación: endpoints protegidos devuelven 401 sin token, 403 con token sin permisos.
- Verificar validación: datos inválidos devuelven 400 con mensaje de error.

#### Tests de seguridad

- Verificar que `/api/**` (excepto `/api/catalog` y `/api/auth`) devuelve 401 sin Authorization header.
- Verificar que un token expirado devuelve 401.
- Verificar que los headers de seguridad están presentes en todas las respuestas:
  `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`.
- Verificar que SQL injection básico en parámetros de búsqueda no rompe nada (JPA lo previene, pero testear).
- Verificar que precios negativos son rechazados (validación de entrada).

### Tests frontend (Playwright o Cypress)

Para tests end-to-end de la interfaz web:

```
tests/
├── dashboard.spec.ts      # Carga el dashboard, verifica stat cards, gráfico visible
├── subscriptions.spec.ts  # Crear suscripción completa, aparece en lista, editar, borrar
├── catalog.spec.ts        # Selector de catálogo: elegir categoría → aparecen servicios → aplicar rellena form
├── categories.spec.ts     # CRUD categorías, error al borrar con suscripciones
└── search.spec.ts         # Búsqueda en tiempo real filtra correctamente
```

### Tests app móvil

- Unit tests de la lógica compartida KMM (repositorios, casos de uso).
- Tests de snapshot para componentes UI clave.
- Tests de integración contra un servidor de desarrollo real.
- Tests de notificaciones push en simulador.

### Cobertura mínima esperada

| Capa                    | Cobertura mínima |
|-------------------------|------------------|
| Servicios backend       | 80%              |
| Controladores REST      | 70%              |
| Controladores web MVC   | 60%              |
| Flujos E2E críticos     | 100% (crear, editar, borrar suscripción) |

---

## Seguridad — checklist completo

### Implementado
- [x] Headers: `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`
- [x] HTTPS en producción (Tomcat lo maneja o reverse proxy nginx)
- [x] Sin exposición de stack traces al cliente (Spring Boot default)
- [x] Entidades JPA con queries parametrizadas (Hibernate previene SQL injection)

### Pendiente (web)
- [ ] CSRF reactivar cuando se exponga a internet (usar `CookieCsrfTokenRepository` con sesión
      inicializada antes de renderizar — actualmente desactivado porque da problemas con Thymeleaf Layout Dialect)
- [ ] `Content-Security-Policy` header restrictivo
- [ ] `Strict-Transport-Security` en producción
- [ ] `Referrer-Policy` y `Permissions-Policy`
- [ ] Rate limiting en endpoints de escritura (evitar spam de suscripciones)
- [ ] Validación de entrada en todos los `@RequestParam` del backend (precio > 0, longitudes máximas)
- [ ] Logs de auditoría: quién creó/modificó qué y cuándo (columnas `created_at`, `updated_at`)

### Pendiente (API REST para móvil)
- [x] Autenticación JWT con access token (15 min) + refresh token (30 días, en BD) — v1.3.0
- [x] Endpoint `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout` — v1.3.0
- [x] CORS configurado solo para los orígenes permitidos — v1.3.0
- [x] Rate limiting: 100 req/min por IP (Bucket4j) — v1.3.0
- [x] Respuestas de error 401/403 estructuradas en JSON — v1.3.0
- [ ] Validación con Bean Validation (`@NotBlank`, `@Positive`, `@Size`) en todos los DTOs
- [ ] Sanitización de campos de texto libre (notas, descripción) — evitar XSS si se renderiza en web
- [ ] Tests de penetración básicos antes de exponer a internet

### Pendiente (app móvil)
- [ ] Tokens JWT en Keychain (iOS) / EncryptedSharedPreferences (Android)
- [ ] Certificate pinning para la conexión al backend
- [ ] Biometría para desbloquear la app (Face ID / fingerprint)
- [ ] Borrado seguro de datos locales al cerrar sesión
- [ ] No loguear datos sensibles en producción (precios, notas)
- [ ] Ofuscación del código con R8 (Android) / Bitcode (iOS)

---

## Catálogo de servicios (CatalogService.kt)

Estático en memoria, 80+ servicios, precios de **marzo 2026** en EUR.

| Clave           | Ejemplos                                                                          |
|-----------------|-----------------------------------------------------------------------------------|
| `ia`            | Claude Pro 16.99€, ChatGPT Plus 18.99€, Cursor Pro 18.99€, Copilot 9.99€        |
| `streaming`     | Netflix Std 17.99€, Disney+ 13.99€, Amazon Prime 4.99€, Max 9.99€, DAZN 19.99€  |
| `musica`        | Spotify 11.99€, Apple Music 10.99€, Tidal HiFi 9.99€                             |
| `software`      | Microsoft 365 7.99€/mes, Adobe CC 60.49€/mes, Figma 12€, Notion 9.49€           |
| `cloud`         | Google One 2TB 9.99€, iCloud+ 2TB 9.99€, Dropbox Plus 11.99€                    |
| `gaming`        | Xbox Game Pass Ultimate 17.99€, PS Plus Extra 14.99€, EA Play 4.99€              |
| `seguridad`     | NordVPN 4.29€, 1Password 2.99€, Bitwarden 0€, Mullvad 5€                        |
| `noticias`      | Kindle Unlimited 9.99€, Readwise 7.99€, Audible 9.99€                           |
| `salud`         | Strava 7.99€, Whoop 30€, Headspace 12.99€, Calm 14.99€                          |
| `desarrollo`    | GitHub Pro 3.67€, JetBrains All Products 24.90€, Vercel Pro 17.43€               |

`getItemsForCategory(name)` hace fuzzy match por palabras clave en el nombre.
Los precios son estáticos. Para actualizarlos: editar `CatalogService.kt` directamente.
YEARLY = precio anual total (no mensual × 12).

---

## Gotchas del stack — errores ya cometidos, no repetir

| Error                                           | Causa                                             | Solución aplicada                                   |
|-------------------------------------------------|---------------------------------------------------|-----------------------------------------------------|
| `#httpServletRequest` / `#request` null         | Thymeleaf 3.1 los eliminó                         | `WebConfig.kt` inyecta `currentPath` via interceptor |
| `Cannot create session after response committed` | CSRF HttpSession-based + Thymeleaf Layout Dialect | CSRF desactivado en local (`csrf.disable()`)        |
| Circular dependency Flyway ↔ EntityManager      | `spring.jpa.defer-datasource-initialization=true` | Eliminar esa propiedad, Flyway gestiona todo        |
| Kotlin 1.9.x no soporta Java 25                 | `IllegalArgumentException` en `JavaVersion.parse` | Kotlin ≥ 2.0 en `build.gradle.kts`                 |
| Enum vs String en SpEL: siempre falso           | `cycle == 'MONTHLY'` compara objeto con string    | `cycle.name() == 'MONTHLY'`                         |
| `sed -i` corrompe ficheros Kotlin en Windows    | Encoding UTF-8 + Git Bash                         | Nunca usar `sed` para editar código                 |
| Puerto 5432 en conflicto en Windows             | PostgreSQL local de Windows ocupa el 5432          | Docker expone en 5433 (`- "5433:5432"`)             |
| Dependencia circular JwtConfig ↔ JwtService     | Spring intenta inyectar beans en loop              | `JwtConfig.kt` separado: solo propiedades, sin `@Service` |
| `APP_AUTH_PASSWORD` texto plano en producción   | BCrypt hash esperado en runtime                    | `TokenService` auto-detecta `$2a$...` y hashea al arrancar |
| JWT_SECRET demasiado corto                      | Nimbus rechaza claves < 32 bytes para HMAC-SHA256  | Default dev: `SubIA-dev-secret-key-32chars-min`     |

---

## Convenciones del proyecto

- **Castellano** en UI, comentarios y mensajes de error al usuario.
- **KDoc** en todas las clases y funciones públicas de Kotlin.
- **PRG** (Post/Redirect/Get) en todos los formularios Thymeleaf. El POST nunca devuelve vista.
- **`@RequestParam`** en controladores Thymeleaf. **`@RequestBody`** solo en REST controllers.
- **EUR** siempre. No implementar lógica multi-divisa.
- **No modificar V1/V2 de Flyway**. Cambios de schema → V3, V4...
- **No usar `sed`** para editar archivos en este entorno.
- **DTOs** para todo lo que sale de la API REST. Las entidades JPA no se serializan directamente.
- Precio `YEARLY` en catálogo = total anual, no mensual. `DashboardService` divide entre 12 para normalizar.

## Lo que NO hay que cambiar sin motivo explícito

- `DashboardService.kt` — normalización de precios funciona correctamente.
- `WebConfig.kt` + `CurrentPathInterceptor` — necesario para navbar activo con Thymeleaf 3.1.
- `CatalogController.kt` + `/api/catalog` — funciona, base de la app móvil.
- Schema SQL V1 — compatible con PostgreSQL tal cual, no tocar.
- Seed data V2 — 10 categorías definitivas, no añadir más por migración.

---

## Versioning y gestión de documentación

**El asistente IA es responsable de mantener estos tres archivos actualizados en cada sesión.**

### Versión actual: `1.4.0`

Esquema: `MAJOR.MINOR.PATCH`

| Tipo de cambio                              | Qué incrementar |
|---------------------------------------------|-----------------|
| Corrección de bug, ajuste menor, docs       | PATCH (1.0.x)   |
| Nueva funcionalidad (PostgreSQL, API, UI)   | MINOR (1.x.0)   |
| Cambio que rompe compatibilidad, rediseño total | MAJOR (x.0.0) |

### Protocolo al terminar cada sesión de desarrollo

1. **`build.gradle.kts`** — actualizar `version = "X.Y.Z"` con la nueva versión.

2. **`CHANGELOG.md`** — añadir una nueva entrada al principio con formato:
   ```
   ## [X.Y.Z] — YYYY-MM-DD
   ### Añadido / Cambiado / Corregido / Eliminado
   - Descripción concisa del cambio
   ```
   Marcar la versión anterior como completada si estaba en "pendiente".

3. **`README.md`** — actualizar el badge de versión y la tabla de versiones al final.

4. **`PROMPT.md`** — actualizar:
   - La versión en el encabezado de esta sección.
   - Mover funcionalidades completadas a la tabla de "implementadas".
   - Marcar las tareas pendientes completadas.
   - Añadir nuevos gotchas si se descubrieron problemas nuevos.

5. **Git** — hacer commit con mensaje descriptivo y push:
   ```bash
   git add -A
   git commit -m "vX.Y.Z — descripción del cambio"
   git push origin main
   ```

### Historial de versiones

| Versión | Fecha      | Descripción                          | Estado     |
|---------|------------|--------------------------------------|------------|
| 1.0.0   | 2026-03-13 | Primera versión funcional web        | Publicada  |
| 1.1.0   | 2026-03-13 | Migración a PostgreSQL               | Publicada  |
| 1.2.0   | 2026-03-13 | API REST completa (P1)               | Publicada  |
| 1.3.0   | 2026-03-14 | Autenticación JWT (P2)               | Publicada  |
| 1.4.0   | 2026-03-14 | UI redesign — Tailwind dark mode (P3) | Publicada  |
| 2.0.0   | —          | App móvil KMM + Compose Multiplatform (P4) | Pendiente  |