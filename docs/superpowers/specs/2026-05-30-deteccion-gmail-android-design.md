# Detección de suscripciones por Gmail en Android — Diseño

**Fecha:** 2026-05-30
**Estado:** Aprobado (pendiente de plan de implementación)
**Autor:** santilafu (con Claude Code)

## 1. Objetivo

Llevar a la app Android la funcionalidad que ya existe en la web: **detectar
automáticamente las suscripciones del usuario leyendo su correo de Gmail y
autorrellenarlas**, para que no tenga que añadirlas a mano. El usuario revisa lo
detectado y elige qué dar de alta.

## 2. Principio rector

La lógica de detección **no se duplica**. El "cerebro" (buscar correos, casar
dominios contra el catálogo, extraer precio/moneda/ciclo con regex, elegir el
mejor plan) ya vive y está testeado en el backend Spring Boot
(`GmailScanService.kt`). Esta feature **expone esa lógica como API REST** y
añade una **UI nativa** en Android. Un único sitio que mantener; el día de
mañana iOS reutiliza la misma API y los mismos modelos `shared`.

## 3. Contexto actual (hechos verificados)

- **Web:** `GmailScanService.kt` y `GmailController.kt` implementan el flujo
  completo (OAuth Authorization Code, scope `gmail.readonly`, sin refresh token;
  búsqueda en Gmail con query ES/EN; matching de dominio en cascada
  exacto→subdominio→registrable; regex de precio `(€|$|£|EUR|USD|GBP)`;
  detección de ciclo MONTHLY/YEARLY/WEEKLY; elección del plan más cercano por
  diferencia relativa). Hoy guarda los resultados en la **sesión HTTP** del
  navegador.
- **Android:** cliente delgado de un backend REST (Spring Boot + JWT). Patrón
  MVVM + Kotlin Multiplatform (lógica en `shared/commonMain`), Compose, Koin,
  Ktor, caché stale-while-revalidate. No persiste de forma autónoma; el servidor
  es la fuente de verdad.
- **Onboarding:** ya existe (`OnboardingScreen`, HorizontalPager de 3 páginas;
  flag `onboarding_completed` en `OnboardingPrefs`; se decide mostrarlo tras el
  login en `SubIAApp.kt`).
- **Deep links:** NO existen (solo el intent-filter MAIN/LAUNCHER). Hay que
  añadirlos.
- **Custom Tabs:** NO se usan; hoy las URLs externas se abren con
  `LocalUriHandler`/`ACTION_VIEW`. Hay que añadir `androidx.browser`.

## 4. Arquitectura del flujo

Elegido: **Custom Tab → backend** (el `client-secret` nunca sale del servidor y
se reutiliza casi todo el código web).

```
ONBOARDING (paso opcional)  ó  AJUSTES (entrada permanente)
   │  "Detecta tus suscripciones con Gmail"
   ▼
Android: POST /api/gmail/scan/ticket            (con JWT)
   │      backend devuelve { connectUrl }
   │      ticket de 1 solo uso, TTL ~10 min, ligado al userId + meses,
   │      viaja dentro del parámetro `state` de OAuth
   ▼
Android abre Custom Tab → connectUrl
   ▼
Google: pantalla de consentimiento (scope gmail.readonly, access_type=online)
   ▼
callback al BACKEND → recupera userId del ticket → escanea Gmail
   │                   → guarda lo detectado en tabla temporal por usuario
   ▼
backend redirige a  subia://gmail/done  →  reabre la app (deep link)
   ▼
Android: GET /api/gmail/scan/results            (con JWT)  →  lista seleccionable
   ▼
Usuario marca cuáles añadir  →  POST /api/gmail/scan/add  →  altas  →  Dashboard
```

### Por qué "ticket"

La Custom Tab abre un navegador con cookies propias; **no comparte el JWT** de
la app. El ticket de un solo uso resuelve la asociación de identidad: lo emite el
backend autenticando por JWT, viaja en el `state` de OAuth (que ya se valida por
CSRF), y el callback recupera de él el `userId`. Así los resultados se guardan
ligados al usuario real, no a una sesión anónima de navegador.

## 5. Componentes

### 5.1 Backend (Spring Boot)

`GmailScanService.kt` queda **intacto**. El flujo web actual
(`/gmail/connect`, sesión, Thymeleaf) **se conserva**; se añade en paralelo el
camino "ticket" para no romper la web.

Nuevos / adaptados:

| Endpoint | Auth | Responsabilidad |
|---|---|---|
| `POST /api/gmail/scan/ticket` | JWT | Crea ticket de un solo uso (UUID, TTL ~10 min) ligado a `userId` + `months`. Devuelve `{ connectUrl }`. |
| `GET /oauth/gmail/callback` *(adaptado)* | — | Si el `state` trae ticket: recupera `userId`, escanea, guarda detecciones en tabla temporal por usuario y redirige a `subia://gmail/done`. Si no, comportamiento web actual (sesión). |
| `GET /api/gmail/scan/results` | JWT | Devuelve las `DetectedSubscription` guardadas para el usuario. |
| `POST /api/gmail/scan/add` | JWT | Da de alta las seleccionadas (reutiliza la lógica de `add-selected`); purga la tabla temporal. |

- **Persistencia temporal:** tabla `gmail_scan_result` (o cache con TTL) keyed
  por `userId` (+ `scanId`). Se purga al añadir o por TTL. Necesaria porque
  Render puede ser multi-instancia y la sesión HTTP no sirve para el camino API.
- **Modelos de transporte:** DTO JSON equivalente a `DetectedSubscription`
  (serviceName, domain, senderEmail, lastSeen, effectivePrice, effectiveCurrency,
  effectiveCycle, priceFromEmail, categoryKey/categoryId, índice/id estable).

### 5.2 Android (Compose + `shared` KMP)

- **Dependencia** `androidx.browser` (Custom Tabs) + helper para abrirlas.
- **Deep link** en `AndroidManifest`: intent-filter con
  `<data android:scheme="subia" android:host="gmail" />`, capturado por
  `MainActivity` (path `done`).
- **`GmailScanScreen`** (Compose) con estados:
  `intro → conectando → esperandoRetorno → resultados → añadiendo → hecho`
  y ramas `vacío` / `error`. La lista de resultados muestra por ítem: nombre del
  servicio, precio detectado vs. catálogo, ciclo, "última vez visto", checkbox.
- **`GmailScanViewModel`** + **`GmailScanRepository`** + DTOs serializables en
  `shared/commonMain` (reutilizable por iOS).
- **Rutas REST nuevas** añadidas a `ApiRoutes.kt`.
- **Integración UI:**
  - Un 4º paso opcional en `OnboardingScreen` ("Detecta tus suscripciones
    automáticamente" + "Ahora no") que navega a `GmailScanScreen`.
  - Un botón permanente en `SettingsScreen` ("Detectar suscripciones con Gmail").

## 6. Flujo de datos

1. El ViewModel pide ticket → recibe `connectUrl` → abre Custom Tab.
2. Backend hace OAuth + escaneo + guarda por `userId` → deep link de retorno.
3. `MainActivity` recibe `subia://gmail/done` → navega/avisa a `GmailScanScreen`.
4. El ViewModel pide `results` → pinta lista seleccionable.
5. Selección → `add` → al éxito refresca caché de suscripciones y vuelve al
   Dashboard.

## 7. Manejo de errores

| Caso | Comportamiento |
|---|---|
| Consentimiento denegado/cancelado | Vuelve a `intro` con aviso "No se completó la conexión". |
| Timeout esperando el deep link | Estado `esperandoRetorno` con botón "Reintentar" / "Ya autoricé". |
| Sin resultados | Estado vacío amable: "No encontramos suscripciones en tu correo". |
| Ticket / JWT caducado | Mensaje claro + reintentar (re-pedir ticket / re-login). |
| Sin red | Error de red estándar de la app. |

## 8. Privacidad y seguridad

- Scope mínimo `gmail.readonly`; `access_type=online` (sin refresh token).
- **No se persisten correos**: solo se proponen servicios. La tabla temporal de
  resultados se purga al añadir o por TTL.
- El `client-secret` permanece solo en el backend.
- `state` validado (CSRF) y portador del ticket de un solo uso.
- Las altas toman los datos de la tabla temporal del servidor, no de campos
  manipulables por el cliente.

## 9. Testing

- **Backend:** ciclo de vida del ticket (emisión, un solo uso, expiración),
  `results` filtrado por `userId`, `add` (altas + purga). La lógica de detección
  ya tiene tests (`GmailScanServiceTest`).
- **Android:** test del `GmailScanViewModel` (transiciones de estado:
  idle→connecting→awaiting→results→adding→done y ramas error/vacío).

## 10. Riesgos y dependencias externas

1. **Verificación de Google (bloquea el lanzamiento masivo, no el desarrollo).**
   `gmail.readonly` es un *restricted scope*. Publicar en producción para
   cualquier usuario exige verificación de Google + evaluación de seguridad CASA
   (proceso de semanas; también aplica a la web). Con la app en "Testing" y test
   users se puede desarrollar y probar sin verificación. **Acción:** arrancar el
   trámite en paralelo desde el día 1.
2. **Deep link + Custom Tab (primera vez en la app).** Algunos navegadores o
   escenarios pueden no disparar el deep link; por eso el estado
   `esperandoRetorno` incluye reintento manual ("Ya autoricé").

## 11. Alcance

Feature cohesiva → cabe en un único spec/plan. No requiere descomposición.

## 12. Fuera de alcance (YAGNI)

- Detección desde otros proveedores de correo (Outlook, IMAP genérico).
- Refresco periódico/automático en segundo plano del escaneo.
- Migrar el flujo web existente al modelo "ticket" (se conservan ambos).
- Custom Tabs para el resto de URLs externas de la app (mejora futura aparte).
