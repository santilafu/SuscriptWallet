# Suscript Wallet — Gestor de suscripciones y recibos

![Web](https://img.shields.io/badge/web-3.2.0-6366f1?style=flat-square)
![App](https://img.shields.io/badge/App-2.18.0-3ddc84?style=flat-square&logo=android)
![Android](https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=flat-square&logo=android)
![Stack](https://img.shields.io/badge/Spring%20Boot-3.5.16-6db33f?style=flat-square&logo=springboot)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7f52ff?style=flat-square&logo=kotlin)
![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk)
![License](https://img.shields.io/badge/uso-personal-lightgrey?style=flat-square)

> Reúne en un solo sitio todo lo que pagas de forma periódica: suscripciones (Netflix, Spotify, ChatGPT…) y recibos recurrentes (luz, seguros, fibra y móvil…). Calcula cuánto supone al mes y al año aunque cada gasto se cobre con una periodicidad distinta, y avisa antes de cada cobro.

Hay dos clientes sobre el mismo backend:

- **App Android** (Kotlin Multiplatform + Jetpack Compose), publicada en Google Play. Versión del código: **2.18.0** (versionCode 37). No hay app de iOS.
- **Web** Thymeleaf servida por el propio backend, en **https://suscriptwallet.onrender.com** (Render).

---

## ✨ Características

| Función | Web | Android |
|---------|:---:|:-------:|
| Cuentas propias: registro con email y contraseña (con verificación por email) | ✅ | ✅ (el registro y "¿Olvidaste tu contraseña?" abren la web) |
| Login con Google (Google Identity Services en web, Credential Manager en Android) | ✅ | ✅ |
| **Periodicidad de pago**: semanal, mensual, cada 2, 3 o 6 meses y anual, con su equivalente mensual y anual | ✅ | ✅ |
| **Recibos recurrentes** (luz, gas, agua, telecos, seguros, alarma, transporte…) con importe orientativo | ✅ | ✅ |
| Inicio con **"Gasto mensual medio"** y **"Este mes pagas"** | — | ✅ |
| **Tira de cobros** con vista de 14 días o del mes en curso | — | ✅ |
| **Gasto por categoría** desplegable (al tocar una categoría se ven sus gastos) | — | ✅ |
| Dashboard con gasto mensual/anual por divisa, gráficos y renovaciones próximas | ✅ | ✅ |
| Catálogo de unos 435 planes con precio oficial en España (ver [Catálogo](#-catálogo-de-servicios)) | ✅ | ✅ |
| Seguimiento de pruebas gratuitas con fecha de fin | ✅ | ✅ |
| Logos de servicios y enlace directo de cancelación | ✅ | ✅ |
| Filtro por categoría en la lista de suscripciones | ✅ | ✅ |
| Búsqueda en la lista de suscripciones | ✅ | — (la app busca solo en el catálogo) |
| Multi-divisa (totales separados por moneda) | ✅ | ✅ |
| Exportar suscripciones a CSV | ✅ | ✅ |
| Avisos de renovación configurables (1, 3, 7 o 14 días antes; WorkManager) | — | ✅ |
| Widget de pantalla de inicio (Jetpack Glance) | — | ✅ |
| Resumen anual compartible ("Tu año en suscripciones") | — | ✅ |
| Onboarding inicial | — | ✅ |
| Modo offline con caché persistente | — | ✅ |
| Material You (colores del sistema en Android 12+) | — | ✅ |
| Idiomas | es / en / fr | es / en / fr / pt |
| Eliminación de cuenta (requisito de Google Play), en `/delete-account` | ✅ | — |

**Detección de suscripciones por Gmail: PAUSADA.** El código existe en backend, web y app, pero está oculto: `Funciones.GMAIL = false` en Android y `th:if="false"` en la tarjeta de `settings.html`. No está disponible para los usuarios.

### Cómo se calculan los importes

La regla vive en [`service/Periodicidad.kt`](src/main/kotlin/com/subia/service/Periodicidad.kt) (backend) y en `shared/model/Periodicidad.kt` (app):

- Cada gasto guarda `intervaloMeses` (1, 2, 3, 6 o 12; columna añadida en la migración V15). `billingCycle` (MONTHLY/YEARLY/WEEKLY) se deriva de él para que las apps antiguas, que solo conocen MONTHLY y YEARLY, sigan funcionando.
- Equivalente mensual = importe / intervalo. Total anual = importe × 12 / intervalo.
- Semanal: equivalente mensual = importe × 52 / 12; total anual = importe × 52. Web y app usan el mismo factor.
- Siempre `BigDecimal` y `HALF_UP`: 4 decimales en los cálculos internos y 2 decimales solo en las cifras que se muestran. Ejemplo: 10 € cada 3 meses → 3,3333 €/mes internamente → 3,33 € en pantalla.

---

## 🔌 API REST

La consume la app Android. Todas las respuestas usan el envoltorio `{ "data": ..., "error": null }` o `{ "data": null, "error": { "code": "...", "message": "..." } }`.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/subscriptions` | Lista las suscripciones del usuario |
| GET | `/api/subscriptions/{id}` | Detalle de una suscripción |
| POST | `/api/subscriptions` | Crear suscripción (body JSON) |
| PUT | `/api/subscriptions/{id}` | Editar suscripción (body JSON) |
| DELETE | `/api/subscriptions/{id}` | Eliminar suscripción |
| GET | `/api/categories` | Lista las categorías |
| GET | `/api/categories/{id}` | Detalle de una categoría |
| POST / PUT / DELETE | `/api/categories[/{id}]` | Gestionar categorías (solo rol `ADMIN`) |
| GET | `/api/dashboard` | Gasto mensual/anual, activas, alertas de renovación |
| GET | `/api/dashboard/stats` | Estadísticas en el formato que usa la app |
| GET | `/api/catalog` | Catálogo completo (público, sin token) |
| GET | `/api/catalog?categoryId=X` | Catálogo filtrado por categoría |

El body de alta/edición de suscripciones admite `intervaloMeses` (1, 2, 3, 6 o 12). Es **opcional** para no romper las apps publicadas que no lo envían: si falta en un alta se deduce de `billingCycle`, y si falta en una edición con el mismo ciclo se conserva el valor guardado. Cualquier otro valor devuelve 400.

Los endpoints `/api/gmail/scan/*` existen pero pertenecen a la detección por Gmail, que está pausada.

---

## 🔑 Autenticación API

Los endpoints `/api/**` requieren un JWT Bearer, salvo `POST /api/auth/{login,refresh,logout,google}` y `GET /api/catalog`.

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | `{email, password}` → `{accessToken, refreshToken, expiresInSeconds, tokenType, role}` dentro de `data` |
| POST | `/api/auth/refresh` | `{refreshToken}` → tokens nuevos (rotación + detección de reutilización) |
| POST | `/api/auth/logout` | `{refreshToken}` → invalida el refresh token (204 No Content) |
| POST | `/api/auth/google` | `{idToken}` (ID token de Google) → tokens JWT |

El access token dura 15 minutos y el refresh token 30 días (`application.properties`).

**Ejemplo con curl** (backend local):

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"usuario@ejemplo.com","password":"tu_contraseña"}' | jq -r '.data.accessToken')

# 2. Llamada autenticada
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/subscriptions

# 3. Renovar token
curl -s -X POST http://localhost:8081/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<tu_refresh_token>"}'

# 4. Logout
curl -X POST http://localhost:8081/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<tu_refresh_token>"}'
```

---

## 🛠 Tecnologías

### Backend y web

| Capa | Tecnología |
|------|------------|
| Lenguaje | Kotlin 2.1.20 sobre Java 21 |
| Framework | Spring Boot 3.5.16 |
| Seguridad | Spring Security + OAuth2 Resource Server (JWT) + Bucket4j (rate limiting) |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL: Aiven en producción, PostgreSQL 16 en Docker en local |
| Migraciones | Flyway (V1 a V16) |
| Sesiones web | Spring Session JDBC |
| Emails | Resend (verificación de cuenta y restablecer contraseña) |
| Internacionalización web | Spring MessageSource (es / en / fr) |
| Frontend | Thymeleaf + Tailwind CSS (CDN) + Chart.js 4.4.7 + **JS vanilla** |
| Tipografía web | DM Sans (Google Fonts) |
| Build | Gradle 9.0 (Kotlin DSL) |

La web sirve una Content-Security-Policy sin `unsafe-eval`, así que **no se puede usar Alpine.js** ni nada que dependa de `eval`/`new Function`: todo el JS de las plantillas es vanilla. Además, todo formulario POST debe llevar el token CSRF; sin él, Spring responde 403.

### App móvil

| Capa | Tecnología |
|------|------------|
| Multiplataforma | Kotlin Multiplatform: el módulo `shared` tiene modelos, red, repositorios y ViewModels |
| UI | Jetpack Compose + Material 3 (Compose Multiplatform 1.7.3) |
| SDK | minSdk 26 (Android 8.0), compileSdk y targetSdk 36 |
| Red | Ktor Client 3.1.0 (motor `Android`, que respeta la NetworkSecurityPolicy) |
| Serialización | kotlinx.serialization 1.8.0 |
| DI | Koin 4.0.0 |
| Navegación | AndroidX Navigation Compose 2.8.5 |
| Gráficos | Vico 2.0.0-beta.2 |
| Avisos y widget | WorkManager 2.9.1, Jetpack Glance 1.1.1 |
| Almacenamiento seguro | EncryptedSharedPreferences |
| Anuncios | AdMob (play-services-ads 25.2.0) |
| Tipografía | Plus Jakarta Sans |
| Build | Gradle 8.13 + AGP 8.13.2 + Version Catalog (`mobile/gradle/libs.versions.toml`) |

El módulo `shared` declara también targets de iOS (`iosMain`), pero no existe ninguna app de iOS.

---

## 🚀 Cómo ejecutar

### Backend y web

**Requisitos previos**: JDK 21 y Docker.

```bash
# 1. Arrancar PostgreSQL (puerto 5433 en tu máquina)
docker compose up -d postgres

# 2. Arrancar la aplicación
./gradlew bootRun
```

La aplicación arranca en **http://localhost:8081**. Flyway crea el esquema y las categorías al arrancar. Los datos persisten en el volumen Docker `subia_data`.

En local no hace falta definir ninguna variable de entorno: `application.properties` trae valores de desarrollo. Sin `ADMIN_PASSWORD` no se crea el usuario administrador, y sin `RESEND_API_KEY` los emails no se envían (solo se registra un aviso en el log).

#### Variables de entorno de producción

Se configuran en el panel de Render. **Nunca las subas al repositorio.**

| Obligatorias (sin ellas el perfil `prod` no arranca o no funciona) | Opcionales |
|---|---|
| `AIVEN_DB_PASSWORD` | `AIVEN_DB_USER` |
| `JWT_SECRET` (≥ 32 caracteres) | `ADMIN_EMAIL` |
| `GOOGLE_CLIENT_ID` | `RESEND_API_KEY` |
| `GOOGLE_CLIENT_SECRET` (el perfil `prod` lo exige aunque Gmail esté pausado) | `RESEND_FROM_EMAIL` |
| `ADMIN_PASSWORD` | `APP_TIMEZONE` (por defecto `Europe/Madrid`) |
| `APP_BASE_URL` | |

### Tests del backend

```bash
./gradlew test
```

Los tests de integración (`src/test/kotlin/com/subia/integration/`) arrancan Spring contra el PostgreSQL de Docker: cada ejecución crea una base de datos temporal `subia_it_<aleatorio>`, aplica todas las migraciones y la borra al terminar. La base de datos de desarrollo `subia` no se toca. Si el contenedor no está levantado, esos tests **se saltan** (no fallan), así que levántalo antes con `docker compose up -d postgres`.

### App móvil (Android)

**Requisitos previos**: Android Studio con soporte para AGP 8.13, JDK 17 o superior (vale el JBR que trae Android Studio) y Android SDK 36.

1. Crea `mobile/local.properties` (está en `.gitignore`). Claves que lee el build:
   - `sdk.dir`: ruta del Android SDK.
   - `API_BASE_URL`: URL del backend. Si falta, se usa `http://10.0.2.2:8081` (el backend local visto desde el emulador).
   - `SUBIA_GOOGLE_WEB_CLIENT_ID`: Web Client ID de Google para el login con Google.
   - `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`: solo para firmar la release.
2. Abre la carpeta `mobile/` en Android Studio y espera a que sincronice Gradle.
3. Ejecuta el módulo `androidApp` en un emulador o dispositivo con Android 8.0+ (API 26).

Desde la terminal (dentro de `mobile/`):

```bash
# Tests del módulo shared
./gradlew :shared:testDebugUnitTest

# Compilar la app
./gradlew :androidApp:assembleDebug

# Bundle firmado para Google Play
./gradlew :androidApp:bundleRelease
```

El AAB se genera en `mobile/androidApp/build/outputs/bundle/release/`.

Otras herramientas:

- `python mobile/tools/i18n/check_strings.py`: comprueba que los `strings.xml` de es/en/fr/pt estén completos.
- `-Psubia.demo=true`: build de demostración solo para capturas de Play (backend local, sin anuncios y applicationId `com.subia.android.demo`, instalable junto a la app real).

---

## ☁️ Despliegue

- **Backend y web**: Render construye la imagen con el [`Dockerfile`](Dockerfile) (multi-stage, JDK 21) desde la rama `main` y la arranca con el perfil `prod`. El Dockerfile compila con `-x test`, así que **los tests no se ejecutan en Render**: pásalos en local antes de hacer push.
- **Después de hacer push, comprueba en el panel de Render que se ha desplegado el último commit.** Si el Auto-Deploy no salta, lánzalo a mano con **Manual Deploy**.
- **App Android**: el AAB se sube a mano a Google Play Console. Las notas de cada versión están en `docs/` (por ejemplo, [docs/play-release-notes-2.18.0.md](docs/play-release-notes-2.18.0.md)).

---

## 📁 Estructura del proyecto

```
.
├── build.gradle.kts, Dockerfile, docker-compose.yml, render.yaml
├── src/main/kotlin/com/subia/
│   ├── config/          # Seguridad (CSRF, CSP, JWT), sesiones, CORS, zona horaria
│   ├── controller/      # Controladores web (Thymeleaf): dashboard, suscripciones, categorías,
│   │   │                #   catálogo, ajustes, auth, privacidad, borrar cuenta, Gmail (pausado)
│   │   └── api/         # API REST: suscripciones, categorías, dashboard, login con Google, Gmail
│   ├── dto/             # DTOs de la web y de la API
│   ├── exception/       # Manejo de errores web y API
│   ├── model/           # Entidades JPA (User, Subscription, Category, RefreshToken…) y CatalogItem
│   ├── repository/      # Repositorios Spring Data
│   ├── security/        # JWT, tokens, Google OAuth, rate limiting, bootstrap del admin
│   └── service/         # Lógica: Periodicidad, RenewalDates, Dashboard, Catalog, emails…
├── src/main/resources/
│   ├── application.properties, application-prod.properties
│   ├── messages*.properties        # Textos de la web (es / en / fr)
│   ├── db/migration/               # Flyway V1–V16
│   └── templates/                  # Plantillas Thymeleaf (layout, dashboard, auth/, subscriptions/…)
├── src/test/kotlin/com/subia/      # Tests unitarios y de integración (integration/)
├── mobile/
│   ├── gradle/libs.versions.toml   # Versiones de la app
│   ├── shared/src/
│   │   ├── commonMain/             # model/, network/, repository/, viewmodel/, cache/, storage/, di/
│   │   ├── commonTest/             # Tests de shared
│   │   ├── androidMain/            # Implementaciones Android (motor HTTP, almacenamiento cifrado)
│   │   └── iosMain/                # Implementaciones iOS (sin app que las use)
│   ├── androidApp/src/androidMain/kotlin/com/subia/android/
│   │   ├── ui/screens/             # Login, Inicio, Suscripciones, Detalle, Formulario, Categorías,
│   │   │                           #   Catálogo, Ajustes, Onboarding, Resumen anual
│   │   ├── ui/components/          # Tira de cobros, gasto por categoría, formato de importes…
│   │   ├── worker/                 # Avisos de renovación (WorkManager)
│   │   ├── widget/                 # Widget de inicio (Glance)
│   │   └── navigation/, auth/, util/
│   └── tools/                      # Scripts: i18n, iconos, capturas y vídeos de Play
└── docs/                           # Auditorías, revisión del catálogo, textos de Play
```

### Migraciones Flyway

| Versión | Qué hace |
|---|---|
| V1–V2 | Tablas `categories` y `subscriptions`; 10 categorías iniciales |
| V3 | Refresh tokens |
| V4 | Campos de prueba gratuita |
| V5 | Categorías Finanzas, Educación, Creatividad y Citas |
| V6–V7 | Usuarios y `user_id` en suscripciones |
| V8 | Auditoría de seguridad (`security_events`) |
| V9 | Familias de refresh tokens |
| V10 | Tokens de restablecer contraseña |
| V11–V12 | Tablas del escaneo de Gmail (funcionalidad pausada) |
| V13 | Categorías de recibos: Hogar y suministros, Seguros, Telecomunicaciones, Transporte |
| V14 | Refresh tokens guardados como hash; email más largo |
| V15 | `intervalo_meses` (periodicidad de pago) |
| V16 | Categoría Belleza |

---

## 📦 Catálogo de servicios

El catálogo está en [`CatalogService.kt`](src/main/kotlin/com/subia/service/CatalogService.kt): es estático, vive en memoria y no se guarda en la base de datos. Tiene **435 planes** repartidos en 19 categorías, más una categoría "Prueba gratuita" que se genera a partir de 17 de esos planes (452 entradas en total).

Los precios son los oficiales de España con IVA y se **revisaron en noviembre de 2026**. Las fuentes de cada precio están en [`docs/catalogo/revision-2026-11/`](docs/catalogo/revision-2026-11/). Para cambiar un precio, edita su fila en `CatalogService.kt`.

| Categoría | Ejemplos |
|-----------|----------|
| IA | Claude Pro, ChatGPT Plus, Google AI Pro, Perplexity Pro, Cursor Premium, Midjourney |
| Streaming | Netflix, Disney+, Amazon Prime, HBO Max, Movistar+, Filmin, DAZN, SkyShowtime |
| Música | Spotify Premium, Apple Music, TIDAL, Amazon Music Unlimited, Deezer |
| Software | Microsoft 365, Adobe Creative Cloud, Notion, Figma, Todoist, LinkedIn Premium |
| Cloud | Google AI Plus/Pro, iCloud+, Dropbox Plus, pCloud, Mega, Backblaze |
| Gaming | Xbox Game Pass, PlayStation Plus, Nintendo Switch Online, GeForce Now, Discord Nitro |
| Seguridad | NordVPN, ExpressVPN, ProtonVPN, 1Password, Bitwarden, Surfshark |
| Noticias y lectura | Kindle Unlimited, Audible, Storytel, El País, The New York Times |
| Salud y deporte | Strava, Garmin Connect+, Headspace, Calm, Basic-Fit, VivaGym |
| Desarrollo | GitHub, GitLab, Vercel, JetBrains, Sentry, Fly.io |
| Finanzas | Revolut, N26, bunq, YNAB, cuota de autónomo (RETA) |
| Educación | Duolingo Super, Coursera Plus, Babbel, Platzi, cuota de colegio |
| Creatividad y foto | VSCO, Lightroom, CapCut Pro, Picsart |
| Citas y social | Tinder, Bumble, Hinge, Meetic |
| Hogar y suministros | Iberdrola, Endesa, Naturgy, Holaluz, Octopus, Canal de Isabel II, Verisure, comunidad de propietarios |
| Telecomunicaciones | Movistar, Vodafone, Orange, Digi, MásMóvil, Yoigo, Pepephone, Lowi, O2 |
| Seguros | Mapfre, AXA, Allianz, Mutua Madrileña, Línea Directa, Sanitas, Adeslas, DKV |
| Transporte | Abono Transporte Madrid, T-usual (ATM Barcelona), Bicimad, Bicing, Uber One, Via-T |
| Belleza | Blissim, LOOKFANTASTIC Beauty Box |

Algunas filas llevan `variablePrice = true` y la app y la web las marcan como importe aproximado:

- **Recibos recurrentes** (luz, gas, agua, telecos, seguros…): el precio es orientativo y el usuario lo ajusta al añadirlo.
- **Servicios que cobran en dólares**: el precio en USD se convierte a euros con el tipo de referencia del BCE indicado en `CatalogService.kt` y se le suma el IVA español. El cargo real depende del cambio del día.

---

## 🔒 Seguridad

- **Multi-usuario**: cada usuario solo accede a sus propios datos (filtro por usuario en la capa de repositorio).
- **Contraseñas**: BCrypt con coste 12.
- **Bloqueo exponencial**: a partir del 4.º intento fallido la cuenta se bloquea 5 min, luego 10, 20, 40… hasta un máximo de 24 h.
- **Anti-enumeración**: los errores de login y registro no revelan si un email existe.
- **Rotación de refresh tokens**: cada uso emite uno nuevo; si se reutiliza uno ya revocado, se revoca toda la familia de la sesión. Los refresh tokens se guardan como hash (V14).
- **Rate limiting**: 10 peticiones/min por IP en los endpoints de autenticación, aparte del límite global de 100/min.
- **Google OAuth**: el ID token se verifica siempre en el servidor (`aud` e `iss`).
- **Cabeceras HTTP**: `X-Frame-Options: DENY` y Content-Security-Policy propia, además de las que Spring Security añade por defecto.
- **CSRF**: activo en la web. La API REST es stateless (JWT).
- **Sesiones web** en PostgreSQL (Spring Session JDBC): sobreviven a reinicios del servidor.
- **Registro de auditoría**: la tabla `security_events` guarda logins, fallos, registros, bloqueos y borrados de cuenta.

---

## 💡 Cómo funciona el selector de catálogo (web)

1. En el formulario de nueva suscripción, elige una **categoría** en el primer desplegable.
2. La página llama a `/api/catalog?categoryId=X` y carga los servicios de esa categoría.
3. Elige el servicio y pulsa **Aplicar**: se rellenan el nombre, la descripción, el precio, la moneda y la periodicidad.
4. Completa el resto (fecha de renovación, notas) y guarda.

También puedes añadir servicios con un clic desde `/catalog-browser`.

---

## 📋 Versiones

El historial detallado de [CHANGELOG.md](CHANGELOG.md) solo llega hasta la 2.12.0. Para lo posterior, consulta `git log`.

Versiones recientes de la app Android:

| Versión | Cambios principales |
|---------|---------------------|
| 2.18.0 (vc 37) | Periodicidad de pago (cada 2, 3 o 6 meses además de semanal, mensual y anual), "Gasto mensual medio" y "Este mes pagas", vista "Este mes" en la tira de cobros, gasto por categoría desplegable, categoría Belleza, precios del catálogo revisados (noviembre de 2026), mejoras con letra grande y en horizontal |
| 2.17.5 – 2.17.8 | Auditoría de octubre de 2026, contrato JSON alineado con el backend, recomendaciones de Play (edge-to-edge, reducción de recursos, símbolos nativos en el AAB), motor HTTP que respeta la NetworkSecurityPolicy |
| 2.17.4 | Avisos de cobro más fiables, arreglos de idioma y portugués |
| 2.17.0 – 2.17.3 | Recibos recurrentes, Inicio con proyección de cobros, targetSdk 36, logo 2026 y splash nuevo, detección por Gmail pausada |
| 2.16.x | Resumen anual compartible, onboarding, widget de inicio; arreglo de un cierre al arrancar |

Backend y web: versión **3.2.0** (`build.gradle.kts`). Incluye la web en es/en/fr, la auditoría de octubre de 2026, el paso a Spring Boot 3.5, la periodicidad de pago (V15) y la categoría Belleza (V16).

---

## 📚 Documentación

- [docs/catalogo/revision-2026-11/](docs/catalogo/revision-2026-11/): fuentes de los precios del catálogo.
- [docs/audits/](docs/audits/): informes de auditoría de Android, backend y web.
- [docs/play-store-listing-es.md](docs/play-store-listing-es.md): textos de la ficha de Google Play.
- [docs/play-release-notes-2.18.0.md](docs/play-release-notes-2.18.0.md): notas de la versión 2.18.0 en los cuatro idiomas.
- [PRIVACY.md](PRIVACY.md): política de privacidad (la web también la sirve en `/privacidad`).
