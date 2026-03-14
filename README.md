# SubIA — Gestor de suscripciones

![Version](https://img.shields.io/badge/versión-1.3.0-6366f1?style=flat-square)
![Stack](https://img.shields.io/badge/Spring%20Boot-3.3.5-6db33f?style=flat-square&logo=springboot)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7f52ff?style=flat-square&logo=kotlin)
![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk)
![License](https://img.shields.io/badge/uso-personal-lightgrey?style=flat-square)

> Controla todos tus servicios de pago en un solo lugar. Visualiza el gasto mensual y anual, filtra por categoría, recibe alertas de renovación y añade nuevas suscripciones con precios actuales gracias al catálogo integrado.

---

## ✨ Características

| Función | Descripción |
|---------|-------------|
| 📊 **Dashboard** | Gasto mensual/anual, gráfico de dona por categoría, alertas de renovación a 7 y 30 días |
| 🔍 **Búsqueda en tiempo real** | Filtra suscripciones por nombre mientras escribes, sin recargar la página |
| 🏷️ **Filtro por categoría** | Pills interactivos para ver solo las suscripciones de una categoría |
| 📋 **Catálogo integrado** | Más de 80 servicios conocidos con precios actuales (marzo 2026). Rellena el formulario automáticamente |
| 🗂️ **Categorías predefinidas** | 10 categorías listas desde el primer arranque: IA, Streaming, Música, Software, Cloud, Gaming, Seguridad, Noticias, Salud y Desarrollo |
| 🔒 **Seguridad** | CSRF en todos los formularios POST, cabeceras de seguridad (X-Frame-Options, X-Content-Type-Options) |
| 💶 **Todo en euros** | Precios del catálogo en EUR, pensado para el mercado europeo |

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
| GET | `/api/catalog` | Todos los servicios del catálogo |
| GET | `/api/catalog?categoryId=X` | Servicios filtrados por categoría |

---

## 🔑 Autenticación API

Desde v1.3.0 los endpoints `/api/**` (salvo `/api/auth/**`, `/api/catalog` y `/api/catalog/**`) requieren un JWT Bearer token.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Recibe `{username, password}` → devuelve `{accessToken, refreshToken, expiresInSeconds, tokenType}` |
| POST | `/api/auth/refresh` | Recibe `{refreshToken}` → devuelve tokens renovados (rotación automática) |
| POST | `/api/auth/logout` | Invalida el refresh token → 204 No Content |

**Ejemplo de uso con curl**:

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq -r '.accessToken')

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

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin 2.1 |
| Framework | Spring Boot 3.3 |
| Seguridad | Spring Security 6 + JWT (HMAC-SHA256, Nimbus) + Bucket4j |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 16 (Docker) |
| Migraciones | Flyway |
| Frontend | Thymeleaf + Bootstrap 5 + Chart.js 4 |
| Tipografía | Inter (Google Fonts) |
| Build | Gradle (Kotlin DSL) |

---

## 🚀 Cómo ejecutar

**Requisitos previos**: JDK 21 o superior y Docker.

Para producción se necesitan las variables de entorno `JWT_SECRET` (clave HMAC de mínimo 32 caracteres) y `APP_AUTH_PASSWORD` (hash BCrypt de la contraseña). En desarrollo, los valores por defecto son suficientes para arrancar sin configuración adicional.

```bash
# 1. Arrancar PostgreSQL con Docker
docker-compose up -d

# 2. Arrancar la aplicación
JAVA_HOME=/ruta/a/tu/jdk ./gradlew bootRun
```

La aplicación arranca en **http://localhost:8081**.

Los datos persisten en un volumen Docker (`subia_data`) entre reinicios.

---

## 📁 Estructura del proyecto

```
src/main/kotlin/com/subia/
├── SubIaApplication.kt              # Punto de entrada
├── config/
│   └── SecurityConfig.kt            # CSRF, headers de seguridad
├── controller/
│   ├── CatalogController.kt         # GET /api/catalog — servicios conocidos (JSON)
│   ├── CategoryController.kt        # CRUD /categories (web)
│   ├── DashboardController.kt       # GET /dashboard (web)
│   ├── SubscriptionController.kt    # CRUD /subscriptions (web)
│   └── api/
│       ├── ApiSubscriptionController.kt  # REST /api/subscriptions (CRUD JSON)
│       ├── ApiCategoryController.kt      # REST /api/categories (CRUD JSON)
│       └── ApiDashboardController.kt     # REST /api/dashboard (stats JSON)
├── dto/
│   ├── DashboardDto.kt              # Datos calculados del dashboard (web)
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
    ├── CatalogService.kt            # Catálogo estático con 80+ servicios en EUR
    ├── CategoryService.kt
    ├── DashboardService.kt          # Normalización de precios (mensual/anual)
    └── SubscriptionService.kt

src/main/resources/
├── application.properties
├── db/migration/
│   ├── V1__init_schema.sql          # Tablas categories y subscriptions
│   └── V2__seed_categories.sql      # 10 categorías predefinidas
└── templates/
    ├── layout.html                  # Plantilla base (navbar, CSS, Chart.js)
    ├── dashboard.html               # Dashboard con gráfico de dona
    ├── categories/
    │   ├── form.html
    │   └── list.html                # Grid de tarjetas
    └── subscriptions/
        ├── confirm-delete.html
        ├── form.html                # Formulario con selector de catálogo (AJAX)
        └── list.html                # Tabla con búsqueda y filtro en tiempo real
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
| ☁️ Cloud | Google One, iCloud+, Dropbox, OneDrive, Backblaze, pCloud |
| 🎮 Gaming | Xbox Game Pass, PS Plus, EA Play, Nintendo Online, Apple Arcade, Humble Choice |
| 🔒 Seguridad | NordVPN, ExpressVPN, Mullvad, ProtonVPN, 1Password, Bitwarden, Malwarebytes |
| 📰 Noticias | Kindle Unlimited, Readwise, Scribd, El País, NYT, Blinkist, Audible |
| 🏃 Salud | Strava, Whoop, Garmin, MyFitnessPal, Calm, Headspace, Apple Fitness+ |
| 🛠️ Desarrollo | GitHub, GitLab, Vercel, Railway, Sentry, JetBrains, Datadog, Linear, Postman |

---

## 🔒 Seguridad

- **Cabeceras HTTP**: `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`.
- **Sin autenticación**: app de uso personal, sin exposición a internet.
- **CSRF**: desactivado en local (uso personal). Para exposición pública, activar `CookieCsrfTokenRepository` en `SecurityConfig.kt`.

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
| 2.0.0   | pendiente  | Rediseño de interfaz + app móvil KMM |