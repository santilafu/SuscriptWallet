# SubIA — Gestor de suscripciones

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

## 🛠 Tecnologías

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin 2.1 |
| Framework | Spring Boot 3.3 |
| Seguridad | Spring Security (CSRF + headers) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | H2 (en memoria) |
| Migraciones | Flyway |
| Frontend | Thymeleaf + Bootstrap 5 + Chart.js 4 |
| Tipografía | Inter (Google Fonts) |
| Build | Gradle (Kotlin DSL) |

---

## 🚀 Cómo ejecutar

**Requisito**: JDK 21 o superior.

```bash
# Con la variable JAVA_HOME apuntando a tu JDK:
JAVA_HOME=/ruta/a/tu/jdk ./gradlew bootRun
```

La aplicación arranca en **http://localhost:8081**.

> **Nota**: la base de datos es H2 en memoria. Los datos no persisten entre reinicios.
> Para persistencia, cambia en `application.properties`:
> ```properties
> spring.datasource.url=jdbc:h2:file:./subia-data
> ```

---

## 📁 Estructura del proyecto

```
src/main/kotlin/com/subia/
├── SubIaApplication.kt              # Punto de entrada
├── config/
│   └── SecurityConfig.kt            # CSRF, headers de seguridad
├── controller/
│   ├── CatalogController.kt         # GET /api/catalog — servicios conocidos (JSON)
│   ├── CategoryController.kt        # CRUD /categories
│   ├── DashboardController.kt       # GET /dashboard
│   └── SubscriptionController.kt    # CRUD /subscriptions
├── dto/
│   └── DashboardDto.kt              # Datos calculados del dashboard
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

- **CSRF**: todos los formularios `POST` incluyen un token CSRF generado por Spring Security e inyectado automáticamente por Thymeleaf.
- **Cabeceras HTTP**: `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, `X-XSS-Protection`.
- **Sin autenticación**: app de uso personal, sin exposición a internet. Si necesitas añadir login, extiende `SecurityConfig.kt`.

---

## 💡 Cómo funciona el selector de catálogo

1. En el formulario de nueva suscripción, elige una **categoría** del primer desplegable.
2. La app hace una llamada AJAX a `/api/catalog?categoryId=X` y carga los servicios de esa categoría.
3. Selecciona el servicio y pulsa **Aplicar** — el formulario se rellena automáticamente con el nombre, descripción, precio, moneda y ciclo de facturación.
4. Completa los campos restantes (fecha de renovación, notas) y guarda.