# Changelog — Suscript Wallet

Todas las versiones siguen [Semantic Versioning](https://semver.org/lang/es/):
- **MAJOR** — cambio incompatible o rediseño completo
- **MINOR** — nueva funcionalidad compatible con lo anterior
- **PATCH** — corrección de errores o ajustes menores

---

## [2.12.0] — 2026-04-11

### Mobile (Android)

#### Added
- **Login con Google** usando Credential Manager API (`androidx.credentials` + `googleid`) — botón "Continuar con Google" en la pantalla de login, bajo divisor "o" e icono oficial multicolor. El `idToken` se valida contra el endpoint existente `POST /api/auth/google` y el flujo de sesión reutiliza `EncryptedSharedPreferences` del login email/contraseña.
- **Pantalla de Ajustes** accesible desde el dropdown del TopAppBar. Incluye:
  - **Recordatorios configurables**: selector 1/3/7/14 días antes de una renovación o fin de trial (persistido en SharedPreferences `subia_cache/notification_days_before`, default 3).
  - **Exportar suscripciones a CSV** vía `ActivityResultContracts.CreateDocument` (Storage Access Framework). El fichero se llama `subia_suscripciones.csv` y contiene todas las suscripciones con todos los campos del modelo, con escape RFC 4180.
- **Top 5 gastos mensuales**: nuevo `TopSuscripcionesChartCard` en el Dashboard con un `ColumnCartesianLayer` de **Vico 2.0.0-beta.2** (primer uso real de la librería en el proyecto). Normaliza YEARLY→mensual en `DashboardViewModel.calcularTopSuscripciones`.
- Nuevos tipos en `shared/commonMain` (multiplataforma, listos para iOS):
  - `AuthRepository.loginWithGoogle(idToken): Result<Unit>`
  - `AuthViewModel.loginWithGoogle(idToken)` + `showGoogleError(mensaje)`

#### Changed
- `RenovacionWorker` ahora lee el umbral de días desde SharedPreferences en vez del literal `3`. Si no hay valor guardado, mantiene el comportamiento anterior (3 días).
- `SubIAApp.kt`: el dropdown del TopAppBar incorpora un item "Ajustes" encima de "Cerrar sesión".

#### Infrastructure
- Dependencias nuevas en `androidApp/build.gradle.kts`: `androidx.credentials:credentials:1.3.0`, `androidx.credentials:credentials-play-services-auth:1.3.0`, `com.google.android.libraries.identity.googleid:googleid:1.1.1`.
- Nueva propiedad `SUBIA_GOOGLE_WEB_CLIENT_ID` leída de `local.properties` y expuesta como `BuildConfig.SUBIA_GOOGLE_WEB_CLIENT_ID` (fallback `""`). El Web Client ID **no** se commitea al repositorio.
- Requiere registrar dos Android OAuth Clients en Google Cloud Console (proyecto `suscriptwallet`) con package `com.subia.android`: uno con el SHA-1 release y otro con el SHA-1 debug.

---

## [2.11.1] — 2026-04-03

### Infrastructure
- Despliegue backend en Render (Docker, Java 21, perfil prod)
- Dockerfile multi-stage con eclipse-temurin:21-jre-jammy
- render.yaml para auto-deploy desde GitHub
- Flyway activado en Render: migraciones V1→V5 ejecutadas automáticamente en Aiven
- HikariCP pool configurado a 10 conexiones para plan básico Aiven
- Eliminado PostgreSQLDialect explícito (Hibernate 6 auto-detección)
- spring.jpa.open-in-view desactivado en prod

### Mobile (v2.8.0 — Play Store ready)
- URL backend actualizada a https://suscriptwallet.onrender.com
- Configuración de firma release con keystore PKCS12
- ProGuard rules para R8: errorprone, Ktor JVM debug detector, Kotlinx Serialization
- Iconos de lanzador en todos los tamaños mipmap (mdpi→xxxhdpi)
- AndroidManifest actualizado con android:icon y android:roundIcon

---

## [2.11.0] — 2026-04-03

### Added
- Trial Tracker: campo `isTrial` y `trialEndsAt` en suscripciones para distinguir pruebas gratuitas de pagos reales
- Nueva categoría de catálogo "Prueba gratuita" con 12 servicios y sus días reales de trial
- Dashboard web: alerta ámbar cuando un trial vence en ≤ 7 días
- Lista de suscripciones: badge naranja/rojo "Prueba — X días" en cards de trials
- Formulario web: toggle para marcar suscripción como trial con autocompletado de fecha de fin
- REST API: campos `isTrial` y `trialEndsAt` propagados en `SubscriptionDto`
- Android WorkManager: notificación cuando un trial vence en 3 días
- KMM shared model: campos `esPrueba` y `fechaFinPrueba` en `Subscription.kt`
- Migración Flyway V4: columnas `is_trial` y `trial_ends_at` en tabla `subscriptions`

### Infrastructure
- Base de datos migrada de Docker local a Aiven (PostgreSQL 17.9 managed cloud)
- Nuevo perfil `prod` con `application-prod.properties` para conexión SSL a Aiven
- Los trials se excluyen de los totales de gasto (solo gasto real contabilizado)

---

## [2.10.2] — 2026-03-22

Fix: logos del catálogo Android resueltos desde el campo `domain` del servidor.

### Corregido
- `CatalogItem` (shared model): nuevo campo `domain` (`@SerialName("domain")`, opcional) que recibe el dominio del servidor para cada servicio del catálogo
- `ServiceLogo`: nuevo parámetro opcional `domain` — si se proporciona, tiene prioridad sobre la derivación local mediante `getLogoDomain(nombre)`
- `CatalogoScreen`: pasa `item.domain` a `ServiceLogo` para que los logos de catálogo usen el dominio exacto definido en el backend, eliminando falsos negativos en la resolución de logos

---

## [1.4.0] - 2026-03-14
### Changed
- UI redesign completo con Tailwind CSS + Alpine.js (dark mode permanente)
- Bootstrap 5.3 eliminado
- Dashboard: stat cards glassmorphism, Chart.js dark mode
- Suscripciones: grid de cards con color dinámico de categoría
- Formulario: wizard de 3 pasos con Alpine.js
- Modal inline de confirmación de borrado
- Categorías: grid de cards con preview de color

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

## [2.0.0] — 2026-03-14

App móvil nativa Android (P4) con Kotlin Multiplatform Mobile, preparada para iOS.

### Añadido

**Backend:**
- `GET /api/dashboard/stats` — endpoint específico para la app móvil. Devuelve `gastoMensual`, `gastoAnual`, `totalSuscripciones` y `renovacionesProximas` con `diasRestantes`. JSON en camelCase.
- `DashboardMobileStatsDto` + `ProximaRenovacionMobileDto` — DTOs propios para la respuesta mobile.
- `DashboardService.getDashboardStats()` — reutiliza la lógica de normalización de precios existente.

**App móvil (KMM — `mobile/`):**
- Módulo `shared` con código 100% compartido entre Android e iOS:
  - Modelos: `Subscription`, `Category`, `CatalogItem`, `DashboardSummary`, `AuthTokens`
  - `ApiClient` con Ktor 3.1.0 y refresh de JWT atómico (Mutex) para evitar race conditions
  - 5 repositorios: `AuthRepository`, `DashboardRepository`, `SubscriptionRepository`, `CategoryRepository`, `CatalogRepository`
  - 6 ViewModels con `StateFlow` y sealed `UiState` (Loading/Success/Error/Offline/SesionExpirada)
  - `expect/actual` para `TokenStorage` (EncryptedSharedPreferences / iOS Keychain), `PlatformContext` y `HttpEngine`
  - DI con Koin 4.0.0 (`sharedModule` + `androidModule`)
  - Caché en memoria por sesión para Dashboard, Suscripciones, Categorías y Catálogo
- Módulo `androidApp`:
  - Single Activity + Compose Navigation type-safe con `@Serializable` routes
  - 7 pantallas: Login, Dashboard (pull-to-refresh), Suscripciones, Detalle, Formulario crear/editar, Categorías, Catálogo
  - Detección offline automática — banner en todas las pantallas afectadas
  - Selector de catálogo en el formulario de nueva suscripción (prerrellenado automático)
  - Tema dark SubIA con paleta Indigo + Material 3
- `libs.versions.toml` — Version Catalog de Gradle para gestión centralizada de dependencias
- `mobile/local.properties` ignorado en git (SDK path + URL del backend, configuración local)

### Pendiente (P4 siguientes fases)
- Tests unitarios e instrumentados
- iOS `iosApp` (Compose Multiplatform iOS target)
- Notificaciones push de renovación
- Widget de pantalla de inicio

---

## [2.1.0] — 2026-03-14

App Android operativa con datos reales. Rename del proyecto a **Suscript Wallet**.

### Añadido
- **Logos de servicios** en pantallas de Suscripciones, Detalle y Catálogo — íconos vectoriales de Spotify, Netflix, ChatGPT, YouTube y más (`ServiceLogo.kt` + `LogoUtils.kt`)
- **`gradle.properties`** en `mobile/` — `android.useAndroidX=true`, `enableJetifier`, `kotlin.native.ignoreDisabledTargets=true` (iOS targets ignorados en Windows)

### Corregido
- **Login mobile funcional** — campo `username` (no `email`), `cleartext traffic` habilitado en el emulador, respuesta del backend envuelta en `ApiResponse` correctamente
- **Respuesta vacía en todos los endpoints** — añadidos `@SerialName` en todos los modelos KMM para mapear camelCase del backend; wrappers `ApiResponse` en `CatalogController` y `ApiDashboardController`
- **Errores de compilación KMM** — fuentes movidas de `src/main/` a `src/androidMain/` (convención KMM), imports corregidos, dependencias alineadas
- **`SuscripcionFormViewModel`** — prefilling desde catálogo y edición operativos con los nuevos `@SerialName`

### Cambiado
- Proyecto renombrado de **SubIA** a **Suscript Wallet** (launcher Android, `settings.gradle.kts`, README, CHANGELOG, repo GitHub)