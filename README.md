# Suscript Wallet — Gestor de suscripciones

![Version](https://img.shields.io/badge/versión-3.0.1-6366f1?style=flat-square)
![Android](https://img.shields.io/badge/Android-2.12.7-3ddc84?style=flat-square&logo=android&label=App)
![Android](https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=flat-square&logo=android)
![Stack](https://img.shields.io/badge/Spring%20Boot-3.3.5-6db33f?style=flat-square&logo=springboot)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7f52ff?style=flat-square&logo=kotlin)
![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk)
![License](https://img.shields.io/badge/uso-personal-lightgrey?style=flat-square)

> Controla todos tus servicios de pago en un solo lugar. Disponible como **app Android nativa** y **aplicación web**. Visualiza el gasto mensual y anual, filtra por categoría, recibe alertas de renovación y añade nuevas suscripciones con precios actuales gracias al catálogo integrado.

---

## ✨ Características

| Función | Web | Android |
|---------|:---:|:-------:|
| 👤 **Multi-usuario** — registro con email y contraseña | ✅ | ✅ |
| 🔐 **Login con Google** — Credential Manager en Android, One Tap en web | ✅ | ✅ |
| 📊 **Dashboard** — gasto por divisa, gráfico por categoría, alertas de renovación | ✅ | ✅ |
| 📈 **Top 5 gastos mensuales** — bar chart Vico en el Dashboard | — | ✅ |
| 🔍 **Búsqueda en tiempo real** de suscripciones | ✅ | ✅ |
| 🏷️ **Filtro por categoría** — chips interactivos | ✅ | ✅ |
| 📋 **Catálogo integrado** — 320+ servicios, prerellena el formulario automáticamente | ✅ | ✅ |
| 🌐 **Catálogo browser** — busca y añade servicios con un clic desde /catalog-browser | ✅ | ✅ |
| 🖼️ **Logos de servicios** (Spotify, Netflix, ChatGPT…) | ✅ | ✅ |
| ⚙️ **Pantalla de Ajustes** — recordatorios configurables y exportación de datos | — | ✅ |
| 📤 **Exportar suscripciones a CSV** — guardado con Storage Access Framework | — | ✅ |
| 💾 **Modo offline** — caché persistente, stale-while-revalidate | — | ✅ |
| 🔔 **Notificaciones de renovación** — aviso configurable 1/3/7/14 días antes (WorkManager) | — | ✅ |
| 🔒 **Tokens seguros** — EncryptedSharedPreferences (Android Keystore) | — | ✅ |
| 🗂️ **Categorías predefinidas** — 10 listas desde el primer arranque | ✅ | ✅ |
| 💱 **Multi-divisa** — totales separados por EUR/USD/GBP | — | ✅ |
| 🆓 **Trial Tracker** — alertas de pruebas gratuitas por vencer | ✅ | ✅ |

---

## 🔌 API REST

Disponible desde v1.2.0. Todos los endpoints devuelven `{ "data": ..., "error": null }` o `{ "data": null, "error": { "code": "...", "message": "..." } }`.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/subscriptions` | Lista todas las suscripciones |
| GET | `/api/subscriptions/{id}` | Detalle de una suscripción |
| POST | `/api/subscriptions` | Crear suscripción (body JSON) |
| PUT | `/api/subscriptions/{id}` | Editar suscripción (body JSON) |
| DELETE | `/api/subscriptions/{id}` | Eliminar suscripción |
| GET | `/api/categories` | Lista todas las categorías |
| GET | `/api/categories/{id}` | Detalle de una categoría |
| POST | `/api/categories` | Crear categoría (body JSON) |
| PUT | `/api/categories/{id}` | Editar categoría (body JSON) |
| DELETE | `/api/categories/{id}` | Eliminar categoría |
| GET | `/api/dashboard` | Stats: gasto mensual/anual, activas, alertas de renovación |
| GET | `/api/dashboard/stats` | Stats para la app móvil (camelCase, renovaciones próximas con diasRestantes) |
| GET | `/api/catalog` | Todos los servicios del catálogo |
| GET | `/api/catalog?categoryId=X` | Servicios filtrados por categoría |

---

## 🔑 Autenticación API

Desde v1.3.0 los endpoints `/api/**` (salvo `/api/auth/**`, `/api/catalog` y `/api/catalog/**`) requieren un JWT Bearer token.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Recibe `{email, password}` → devuelve `{accessToken, refreshToken, expiresInSeconds, tokenType}` |
| POST | `/api/auth/refresh` | Recibe `{refreshToken}` → devuelve tokens renovados (rotación automática + reuse detection) |
| POST | `/api/auth/logout` | Invalida el refresh token → 204 No Content |
| POST | `/api/auth/google` | Recibe `{idToken}` (Google ID Token) → devuelve tokens JWT |

**Ejemplo de uso con curl**:

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@email.com","password":"tu_contraseña"}' | jq -r '.accessToken')

# 2. Llamada autenticada
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/subscriptions

# 3. Renovar token
curl -s -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"<tu_refresh_token>\"}"

# 4. Logout
curl -X POST http://localhost:8081/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

Los errores de autenticación se devuelven en JSON estándar: `{ "data": null, "error": { "code": "UNAUTHORIZED", "message": "..." } }`.

---

## 🛠 Tecnologías

### Backend

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin 2.1 |
| Framework | Spring Boot 3.3 |
| Seguridad | Spring Security 6 + JWT (HMAC-SHA256, Nimbus) + Bucket4j |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 17.9 (Aiven managed cloud) · PostgreSQL 16 (Docker, desarrollo local) |
| Migraciones | Flyway |
| Frontend | Thymeleaf + Tailwind CSS CDN + Alpine.js CDN + Chart.js 4 |
| Tipografía | Inter (Google Fonts) |
| Build | Gradle (Kotlin DSL) |

### App móvil (KMM)

| Capa | Tecnología |
|------|------------|
| Multiplatform | Kotlin Multiplatform Mobile (KMM) |
| UI Android | Jetpack Compose + Material 3 |
| UI iOS | Compose Multiplatform 1.7.3 *(próximamente)* |
| Networking | Ktor Client 3.1.0 (CIO en Android, Darwin en iOS) |
| Serialización | Kotlinx Serialization 1.8.0 |
| DI | Koin 4.0.0 (KMP-compatible) |
| Navegación | AndroidX Navigation Compose 2.8.5 (type-safe routes) |
| Almacenamiento seguro | EncryptedSharedPreferences (Android) / Keychain Services (iOS) |
| Arquitectura | MVVM + StateFlow + sealed UiState en `commonMain` |
| Build | Gradle 8.9 + Version Catalog (`libs.versions.toml`) |

---

## 🚀 Cómo ejecutar

### Backend

**Requisitos previos**: JDK 21 o superior y Docker.

Variables de entorno requeridas en producción: `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `GOOGLE_CLIENT_ID`, `RESEND_API_KEY`, `AIVEN_DB_PASSWORD`, `APP_BASE_URL`. En desarrollo los valores por defecto son suficientes para arrancar.

```bash
# 1. Arrancar PostgreSQL con Docker
docker-compose up -d

# 2. Arrancar la aplicación
JAVA_HOME=/ruta/a/tu/jdk ./gradlew bootRun
```

> **Cloud**: usa el perfil `prod` (`./gradlew bootRun --args='--spring.profiles.active=prod'`) para conectar a Aiven sin necesidad de Docker.

> **Producción**: el backend está desplegado en Render y disponible en **https://suscriptwallet.onrender.com**.

La aplicación arranca en **http://localhost:8081** (desarrollo local).

Los datos persisten en un volumen Docker (`subia_data`) entre reinicios.

### App móvil (Android)

**Requisitos previos**: Android Studio Hedgehog o superior, JDK 21, Android SDK 35.

```bash
# Crear mobile/local.properties (SDK path + URL del backend)
echo "sdk.dir=/ruta/a/Android/Sdk" > mobile/local.properties
echo "API_BASE_URL=http://10.0.2.2:8081" >> mobile/local.properties
```

1. Abrir **Android Studio** → `Open` → seleccionar la carpeta `mobile/`
2. Esperar la sincronización de Gradle
3. Seleccionar `androidApp` como módulo de ejecución
4. Lanzar en emulador o dispositivo con Android 8.0+ (API 26)

Para generar el bundle de release listo para Play Store:

```bash
cd mobile/
./gradlew bundleRelease
```

El AAB se genera en `mobile/androidApp/build/outputs/bundle/release/`.

---

## 📁 Estructura del proyecto

### Backend

```
src/main/kotlin/com/subia/
├── SubIaApplication.kt              # Punto de entrada
├── config/
│   └── SecurityConfig.kt            # CSRF, headers de seguridad
├── controller/
│   ├── CatalogBrowserController.kt  # GET /catalog-browser — buscador de servicios
│   ├── CatalogController.kt         # GET /api/catalog — servicios conocidos (JSON)
│   ├── CategoryController.kt        # CRUD /categories (web)
│   ├── DashboardController.kt       # GET /dashboard (web)
│   ├── SubscriptionController.kt    # CRUD /subscriptions (web) + add-from-catalog
│   └── api/
│       ├── ApiSubscriptionController.kt  # REST /api/subscriptions (CRUD JSON)
│       ├── ApiCategoryController.kt      # REST /api/categories (CRUD JSON)
│       └── ApiDashboardController.kt     # REST /api/dashboard + /stats (JSON)
├── dto/
│   ├── DashboardDto.kt              # Datos calculados del dashboard (web)
│   ├── DashboardMobileDto.kt        # DTOs para /api/dashboard/stats (mobile)
│   └── api/
│       ├── ApiResponse.kt           # Wrapper genérico { data, error }
│       ├── ApiError.kt              # Estructura de error { code, message }
│       ├── SubscriptionRequestDto.kt
│       ├── SubscriptionResponseDto.kt
│       ├── CategoryRequestDto.kt
│       ├── CategoryResponseDto.kt
│       └── DashboardStatsDto.kt
├── model/
│   ├── CatalogItem.kt               # Servicio del catálogo (no entidad JPA)
│   ├── Category.kt                  # Entidad — tabla categories
│   └── Subscription.kt             # Entidad — tabla subscriptions + enum BillingCycle
├── repository/
│   ├── CategoryRepository.kt
│   └── SubscriptionRepository.kt    # Queries personalizadas (activas, rango de fechas)
└── service/
    ├── CatalogService.kt            # Catálogo estático con 285+ servicios en EUR
    ├── CategoryService.kt
    ├── DashboardService.kt          # Normalización de precios (mensual/anual) + mobile stats
    └── SubscriptionService.kt

src/main/resources/
├── application.properties
├── db/migration/
│   ├── V1__init_schema.sql          # Tablas categories y subscriptions
│   └── V2__seed_categories.sql      # 10 categorías predefinidas
└── templates/
    ├── layout.html                  # Plantilla base (navbar, CSS, Chart.js)
    ├── catalog-browser.html         # Buscador de 170+ servicios — añadir con un clic
    ├── dashboard.html               # Dashboard con gráfico de dona
    ├── categories/
    │   ├── form.html
    │   └── list.html                # Grid de tarjetas
    └── subscriptions/
        ├── confirm-delete.html
        ├── form.html                # Formulario con selector de catálogo (AJAX)
        └── list.html                # Grid con logos, badges de prueba, links de cancelación
```

### App móvil (KMM)

```
mobile/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml           # Version catalog (Kotlin 2.1.20, Compose MP 1.7.3, Ktor 3.1.0)
│   └── wrapper/gradle-wrapper.properties
├── shared/                          # Módulo KMM — lógica compartida Android + iOS
│   └── src/
│       ├── commonMain/kotlin/com/subia/shared/
│       │   ├── model/               # AuthTokens, Subscription, Category, CatalogItem, DashboardSummary
│       │   ├── network/             # ApiClient (Ktor + JWT refresh con Mutex), ApiRoutes
│       │   ├── repository/          # Auth, Dashboard, Subscription, Category, Catalog
│       │   ├── viewmodel/           # 6 ViewModels con StateFlow + sealed UiState
│       │   ├── storage/             # expect TokenStorage
│       │   ├── platform/            # expect PlatformContext, expect createHttpEngine()
│       │   └── di/SharedModule.kt
│       ├── androidMain/             # actual implementations (EncryptedSharedPreferences, CIO)
│       └── iosMain/                 # actual implementations (Keychain Services, Darwin)
└── androidApp/                      # Módulo Android
    └── src/androidMain/kotlin/com/subia/android/
        ├── SubIAApp.kt              # Application — Koin startKoin
        ├── MainActivity.kt          # Single Activity + auth check
        ├── navigation/AppNavGraph.kt # Type-safe routes (@Serializable)
        └── ui/
            ├── SubIAApp.kt          # Root composable + NavigationBar
            ├── ServiceLogo.kt        # Logos de servicios conocidos (Compose)
            ├── theme/               # Suscript Wallet palette, dark MaterialTheme
            └── screens/             # Login, Dashboard, Suscripciones, Detalle, Form, Categorías, Catálogo
```

---

## 📦 Catálogo de servicios

Los precios están definidos en `CatalogService.kt` y son de **marzo 2026**. Para actualizar un precio, edita el valor correspondiente en esa clase.

| Categoría | Servicios incluidos |
|-----------|---------------------|
| 🤖 IA | Claude Pro, ChatGPT, Cursor, Copilot, Gemini, Perplexity, Midjourney, Grok, Canva |
| 🎬 Streaming | Netflix, Disney+, Amazon Prime, YouTube Premium, Apple TV+, Max, Crunchyroll, DAZN, Movistar+, Filmin, Paramount+... |
| 🎵 Música | Spotify, Apple Music, Tidal, Amazon Music, YouTube Music, Deezer |
| 💻 Software | Microsoft 365, Adobe CC, Notion, Figma, Todoist, Grammarly, Duolingo, Affinity Suite... |
| ☁️ Cloud | Google One, iCloud+, Dropbox, OneDrive, Backblaze, pCloud, Mega, Box, Sync.com, Tresorit... |
| 🎮 Gaming | Xbox Game Pass, PS Plus, EA Play, Nintendo Online, Apple Arcade, Humble Choice, GeForce Now... |
| 🔒 Seguridad | NordVPN, ExpressVPN, Mullvad, ProtonVPN, 1Password, Bitwarden, Malwarebytes, Surfshark... |
| 📰 Noticias | Kindle Unlimited, Readwise, Scribd, El País, NYT, Blinkist, Audible, Medium, Substack... |
| 🏃 Salud | Strava, Whoop, Garmin, MyFitnessPal, Calm, Headspace, Apple Fitness+, Peloton, Freeletics... |
| 🛠️ Desarrollo | GitHub, GitLab, Vercel, Railway, Sentry, JetBrains, Datadog, Linear, Postman, Fly.io... |
| 🆓 Prueba gratuita | ChatGPT Plus, Claude Pro, NordVPN, Spotify, YouTube Premium, Duolingo, Headspace y más... |

---

## 🔒 Seguridad

- **Multi-usuario real**: cada usuario solo accede a sus propios datos (IDOR prevention en capa de repositorio).
- **Contraseñas**: BCrypt cost 12 (mayor coste computacional para atacantes que el estándar cost 10).
- **Lockout exponencial**: 5 intentos fallidos → bloqueo con tiempo creciente (5 min → 15 → 60 → 1440).
- **Anti-enumeración**: los errores de login/registro nunca revelan si un email existe.
- **Refresh token rotation**: cada uso del refresh token emite uno nuevo. Si se detecta reutilización de un token ya revocado, se revoca toda la familia de sesión.
- **Rate limiting**: 10 req/min por IP en endpoints de autenticación (separado del límite global de 100/min).
- **Google OAuth**: el ID token de Google se verifica siempre en el servidor (validación de `aud` e `iss`), nunca en el cliente.
- **Cabeceras HTTP**: `Strict-Transport-Security`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy`, `Referrer-Policy`.
- **CSRF**: activo en la web (Spring Security). La API REST es stateless (JWT).
- **Spring Session JDBC**: las sesiones web persisten en PostgreSQL — sobreviven reinicios del servidor.
- **Audit log**: tabla `security_events` registra cada login, fallo, registro, lockout y eliminación de cuenta con IP y user-agent.

---

## 💡 Cómo funciona el selector de catálogo

1. En el formulario de nueva suscripción, elige una **categoría** del primer desplegable.
2. La app hace una llamada AJAX a `/api/catalog?categoryId=X` y carga los servicios de esa categoría.
3. Selecciona el servicio y pulsa **Aplicar** — el formulario se rellena automáticamente con el nombre, descripción, precio, moneda y ciclo de facturación.
4. Completa los campos restantes (fecha de renovación, notas) y guarda.

---

## 📋 Versiones

Consulta [CHANGELOG.md](CHANGELOG.md) para el historial completo de cambios.

| Versión | Fecha      | Descripción                          |
|---------|------------|--------------------------------------|
| 1.0.0   | 2026-03-13 | Primera versión funcional completa   |
| 1.1.0   | 2026-03-13 | Migración a PostgreSQL               |
| 1.2.0   | 2026-03-13 | API REST completa (P1)               |
| 1.3.0   | 2026-03-14 | Autenticación JWT (P2)               |
| 1.4.0   | 2026-03-14 | UI redesign — Tailwind dark mode     |
| 2.0.0   | 2026-03-14 | App móvil KMM (Android) + Compose Multiplatform |
| 2.1.0   | 2026-03-14 | App funcional con datos reales, logos, rename Suscript Wallet |
| 2.2.0   | 2026-03-22 | Notificaciones, caché persistente, gráfico por categoría, multi-divisa, prefill catálogo, filtros, DatePicker |
| 2.3.0   | 2026-03-22 | Rename → SuscriptWallet, selector catálogo inline en formulario, gráfico donut, totales anuales, fix guardar suscripción, navegación desde dashboard, notificación exacta a 3 días |
| 2.4.0   | 2026-03-22 | Rediseño visual completo — headers con gradiente, cards custom, logos con fallback, UI estilo fintech en todas las pantallas |
| 2.5.0   | 2026-03-22 | Seguimiento de pruebas gratuitas — campo isTrial, alertas en dashboard y lista, catálogo con 12 trials reales, notificaciones Android |
| 2.6.0   | 2026-03-22 | Logos de servicios en la web — icon.horse con inicial como fallback, dominios en los 86 servicios del catálogo |
| 2.7.0   | 2026-03-22 | Enlace directo de cancelación en cada suscripción — botón "Cancelar" con URL oficial de baja para los 86 servicios |
| 2.8.0   | 2026-03-22 | Catálogo browser — 170+ servicios, búsqueda y filtros, añadir con un clic desde /catalog-browser |
| 2.9.0   | 2026-03-22 | Catálogo ampliado a 285+ servicios — 4 nuevas categorías: Finanzas, Educación, Creatividad y Citas |
| 2.10.0  | 2026-03-22 | Fix crítico mapeo de categorías (citas/noticias no aparecían), +50 apps Play Store populares (320+ total) |
| 2.10.1  | 2026-03-22 | Fix logos: dominios corregidos (LiveOne, Pokémon GO, Clash Royale, Brawl Stars), Stadia eliminado → Minecraft Realms |
| 2.10.2  | 2026-03-22 | Fix logos en catálogo Android: campo `domain` en CatalogItem propagado a ServiceLogo para resolver logos correctamente desde el servidor |
| 2.11.0  | 2026-04-11 | Nuevo icono de app (logo ocupa frame completo), pantalla de login rediseñada estilo web — fondo oscuro, card, "¿Olvidaste tu contraseña?" y "Crear cuenta" |
| 2.12.0  | 2026-04-11 | **Login con Google en Android** (Credential Manager API), pantalla **Ajustes** con recordatorios configurables (1/3/7/14 días) y **exportar a CSV**, gráfico **Top 5 gastos mensuales** en Dashboard con Vico |
| 3.0.0   | 2026-04-06 | **Multi-usuario** — registro/login email+contraseña, Google One Tap, BCrypt cost 12, lockout exponencial, refresh token rotation+reuse detection, Spring Session JDBC, IDOR prevention, audit log, reset de contraseña, eliminación de cuenta (requisito Google Play), headers de seguridad hardened |
| 3.0.1   | 2026-04-06 | Fix UI web — dark theme en todas las páginas (causa raíz: tailwind.config antes del CDN + CSS fallbacks), iconos de servicios visibles, gráfico Chart.js y selector de catálogo funcionando (fix CSP: unsafe-inline en script-src, icon.horse en img-src, fuentes externas) · Android 2.10.0 (versionCode 11): AdMob banner |
| 2.12.7  | 2026-04-13 | Android — preparación interna para v2.13.0 (catálogo modelo dual pricing): `CatalogItem` con `precioAnual` e `iconUrl` opcionales + helpers (`hasBothCycles`, `monthlyEquivalent`, `annualSavingsPercent`), enum `BillingCycle` interno al formulario y migración de `prerellenarDesdeCatalogo` a la nueva firma con ciclo. Sin cambios visibles para el usuario; 100% backward compatible con el backend actual gracias a `ignoreUnknownKeys`. Cubierto por `CatalogItemSerializationTest` y `CatalogItemHelpersTest` en commonTest |
