# Backend audit and fixes: SuscriptWallet (SubIA)

**Date:** 2026-10-10 · **Scope:** Spring Boot backend in Kotlin (`src/`), including migrations and `build.gradle.kts` · **Mode:** audit **with fixes**. No commits were made.
**Starting point:** the backend findings still open from `2026-10-07-auditoria-completa.md` and `2026-10-09-notificaciones.md`.

---

## 1. Summary

- **25 findings fixed:** 1 critical, 7 high, 11 medium and 6 low. Of those, 9 had been carried over from earlier audits.
- **Spring Boot 3.3.5 → 3.5.16.** I also bumped dependency-management to 1.1.7 and google-api-client to 2.9.1, and removed `spring-boot-starter-oauth2-client`, which nothing used. The upgrade broke three templates (Thymeleaf 3.1.3 rejects `T(...)` and `@environment`). That is now fixed and the change has been checked against the real app.
- **Tests:** went from 79 to **112**, with **0 failures**. `./gradlew clean build` finishes with **BUILD SUCCESSFUL**.
- **Real-world check:** I built the jar and ran it against PostgreSQL 16 (the `docker-compose` container on :5433) in a throwaway database, `subia_audit`, which I deleted afterwards. Migrations V1–V14 applied cleanly, and so did V14 on top of existing data. I tested the API with curl (login, refresh, token reuse, IDOR, validation, dashboard) and the web flow with CSRF (login, lockout, sign-up, add/edit/delete, CSV, categories, logout). The run produced **no ERROR lines in the log**.
- **Still open (§4):** email reminders (N-11, a feature that needs a product decision), the client IP behind Render's proxy (M-03a), password-reset tokens stored in plain text, and the per-user category model (that decision belongs to the user).

---

## 2. Findings fixed

Severity is Critical / High / Medium / Low. Line numbers are approximate and refer to the code after the fix.

| ID | Sev. | Area | File | Problem → fix | Test |
|---|---|---|---|---|---|
| B-01 | **Critical** | Web / upgrade | `templates/auth/login.html:178`, `subscriptions/form.html` (18 places), `subscriptions/list.html:99`, `catalog-browser.html:192` | On Spring Boot 3.5 (Thymeleaf 3.1.3), `@environment.getProperty(...)` and `T(...)` are no longer allowed, so **/login returned a 500**. The values now come from controller model attributes: `googleClientId`, `isEntity`, `trialDaysLeft` and `today`. | Real-world: every page returns 200 |
| N-12 / N-02 | **High** | Logic | `service/RenewalDates.kt` (new), `DashboardService.kt`, `ApiSubscriptionController.kt:toDto`, `SubscriptionController.kt:projected()` | `renewalDate` never moved forward. Old subscriptions dropped out of "upcoming renewals" and the app showed, then saved back, dates that had already passed. There is now a projection from the anchor date that handles the 31st of the month and 29 Feb. Dashboard, API, the web list and the CSV all show the next real charge date. Nothing is written to the database. | `RenewalDatesTest` (6), `DashboardServiceTest` |
| A-05 | **High** | Security / CSP | `config/SecurityConfig.kt:~105` | `frame-src 'none'` and `connect-src 'self'` blocked the Google Sign-In button. CSP now follows the GIS guidance (`/gsi/` in frame-src, connect-src and style-src). I also added `base-uri` and `form-action`. | Real-world: CSP header checked |
| AUTH-1 | **High** | Security | `security/WebLoginAttemptListener.kt` (new) | **Web login had no lockout.** Only the API counted failed attempts, so passwords could be guessed without limit. A listener now counts failures and resets the counter for form login only (not for the JWT events). | Real-world: 4 failures → `locked_until` |
| AUTH-2 | **High** | Security | `service/UserService.kt:findOrCreateByGoogle` | Account pre-hijacking: someone could register another person's email with their own password; when the real owner signed in with Google, the account was linked and that password stayed valid. It also returned a **500** on the web, because the account was still unverified (DisabledException). Linking an unverified account now marks it verified and **discards the earlier password**. | `UserServiceTest` (2) |
| M-03c | **High** | Security | `config/FilterRegistrationConfig.kt` (new), `SecurityConfig.kt:apiFilterChain` | Both filters were also registered as servlet filters for every URL. The 100/min API limit therefore applied to pages and static files, and `/api/auth/*` only had a rate limit **by accident**. Automatic registration is now off and `AuthRateLimitFilter` is added explicitly to the API chain. | `AuthRateLimitFilterTest` |
| DB-1 | **High** | Database | `db/migration/V14__…sql`, `model/RefreshToken.kt` | `refresh_tokens.email VARCHAR(64)` against `users.email` (254): anyone whose email is longer than 64 characters **could not sign in to the app** (500). The column is now 254. | Real-world: 85-character email → 200 |
| API-1 | **High** | Errors | `exception/ApiExceptionHandler.kt` | A locked or unverified account signing in through the API got **500 INTERNAL_ERROR**. It now gets 403 `ACCOUNT_LOCKED` or `EMAIL_NOT_VERIFIED`. I also added 400 for IllegalArgument and TypeMismatch (`/api/subscriptions/abc`), 409 for DataIntegrity, 403 for AccessDenied, and the MVC status (415/405/400) for `ErrorResponse`. Previously all of these were 500. | `ApiExceptionHandlerTest` (4) |
| M-02 | Medium | Security | `security/TokenService.kt:hash`, `RefreshTokenRepository`, V14 | Refresh tokens were stored in plain text. Only the SHA-256 is stored now. V14 converts the existing tokens with the same function, so **open app sessions are not lost** (checked against real data). | `TokenServiceTest` (4) |
| TOK-1 | Medium | Security | `TokenService.refresh`, `RefreshTokenRepository.revokeIfActive` | Two concurrent refreshes with the same token both received a new pair. Revocation is now a conditional `UPDATE`. | `TokenServiceTest` |
| M-09 | Medium | Maintenance | `service/MaintenanceJobs.kt` (new), `@EnableScheduling` | There was no cleanup at all. A job every 6 hours now deletes expired refresh tokens, reset tokens and Gmail tickets, and security events older than 180 days. Every 5 minutes it empties the rate-limiter maps. | Context starts up |
| M-03b | Medium | Security / memory | `AuthRateLimitFilter.purgeIdle`, `RateLimitFilter.purgeIdle`, `security/ClientIp.kt` | The in-memory maps grew without limit. The IP is now capped at 45 characters, because a huge `X-Forwarded-For` was being used as a map key and stored in `ip_address VARCHAR(45)`. | `AuthRateLimitFilterTest` |
| RL-1 | Medium | UX / Security | `AuthRateLimitFilter.kt` | GET requests counted toward the 10/min limit: opening /login, `/login?error` or the reset link used up quota, so after 5 failures a user got a **JSON 429 just for loading the page**. Only POST requests count now. | `AuthRateLimitFilterTest` |
| M-10 | Medium | Validation | `UserService.register`, `AuthWebController.register` | `confirmPassword` was ignored and the email format was never checked. Emails are now normalised (trim + lower case) and validated, and passwords longer than 72 characters are rejected (BCrypt truncates them silently). | `UserServiceRegistrationTest` (8) |
| M-11 | Medium | Sign-up | `UserService.register` | If the verification email was lost, there was no way to verify the account. Signing up again with an unverified email now resends a fresh link, without changing the password and without revealing that the account exists. | `UserServiceRegistrationTest` |
| RST-1 | Medium | Auth | `UserService.resetPassword` | After a password reset, an unverified account still could not sign in and old sessions stayed alive. The reset (which proves the user owns the email) now verifies the account, lifts the lockout and revokes refresh tokens. | `UserServiceTest` |
| VAL-1 | Medium | Validation | `SubscriptionService.validated`, `SubscriptionRequestDto`, `CategoryService.validated` | Names over 100 characters, prices of 9 or more digits, unknown currencies, negative prices on the web, an invalid `trialEndsAt` (`LocalDate.parse`) or an arbitrary category colour all reached the database and returned **500**. They now return 400 with a message. The currency is normalised to upper case. Colours must be strict hex (they are rendered inside `style`). | `SubscriptionServiceTest` (3), real-world |
| WEB-1 | Medium | Errors | `exception/WebExceptionHandler.kt` (new) | Opening `/subscriptions/{id}/edit` for someone else's or a non-existent subscription gave a **500**. It now gives 404, and invalid data gives 400. | Real-world: 404 |
| N-19 | Medium | Time zone | `config/TimeConfig.kt` (`Clock` bean, `app.timezone=Europe/Madrid`) | `LocalDate.now()` runs in UTC on Render, so between 00:00 and 02:00 "today" was yesterday. Dashboard, catalogue and sign-up from the catalogue now use the clock in the configured zone. | `DashboardServiceTest` |
| N-10 | Medium | Logic | `RenewalDates.isActiveTrial` | A trial whose end date had passed still counted as free forever and was excluded from totals. It now counts as paid. | `DashboardServiceTest` |
| DASH-1 | Medium | Logic | `DashboardService.getDashboardStats` | `/api/dashboard/stats` added up EUR, USD and JPY as one figure and included active trials. It now uses only the main currency and leaves trials out, the same way the web does. | `DashboardServiceTest` |
| TRIAL-1 | Low | Consistency | `ApiSubscriptionController.trialEndFor` | The API accepted a trial with no end date (the web requires one), and that trial stayed "active" forever. When the date is missing, the first charge date is used, so the app is not broken with a 400. | Real-world |
| M-05 | Low | Robustness | `EmailService`, `GmailScanService` | `RestClient` had no timeouts, so a slow Resend response hung sign-up inside its transaction. Timeouts are now connect 5 s / read 10 s for Resend and 10/20 s for Gmail. | — |
| B-02 | Low | IDOR risk | `SubscriptionService`, `SubscriptionRepository`, `DashboardService` | The legacy methods without `userId` (`findAll()`, `findActive()`, `findById(id)`, `delete(id)`, `getDashboard()`, global queries) are removed. | Compiles |
| CSV-1 | Low | Security | `SubscriptionController.csvEscape` | CSV export allowed formula injection (`=`, `+`, `-`, `@`). Such cells are now prefixed with `'`. | Real-world |
| WEB-2 | Low | Web | `config/WebConfig.kt:CurrentPathInterceptor` | Every redirect ended in `?currentPath=%2F...` because the interceptor put the value into the redirect model. | Real-world |
| W-2 | Low | Web | `AuthWebController.verifyEmailError` + `templates/auth/verify-email-error.html` | `/verify-email-error` had no route (404 after an expired link). | Real-world: 200 |
| W-3 | Low | Web | `SecurityConfig` permitAll | Favicons were not shown without a session. | Real-world: 200 |
| W-4 | Low | i18n | `CategoryController.delete` + `cat.error.in-use` key | The "category in use" message was always in Spanish. | Real-world in EN |
| A-01 | — | Dependencies | `build.gradle.kts` | Boot 3.5.16 (Tomcat, Spring Security, Spring Framework, logback, nimbus and Flyway all move up through the BOM). Unused `oauth2-client` removed; google-api-client 2.9.1. Hibernate 6.6 checked with real merges (PUT on subscription and category). | Full build + real-world |

**i18n keys added** (es/en/fr and base): `verify.error.title`, `verify.error.text`, `verify.error.retry`, `cat.error.in-use`. Portuguese has no `messages_pt.properties` on the web side.

---

## 3. Checked and fine

- **IDOR:** GET/PUT/DELETE on another user's subscription returns 404 (real-world). Gmail results are filtered by `userId`.
- **Categories:** mutations are ADMIN-only. A normal user gets 403 (real-world). I left the model as it is, as asked.
- **CSRF:** active on the web; every POST in the smoke test needed the token.
- **Secrets:** none in the code. Prod has no fallback for `JWT_SECRET` or `GOOGLE_CLIENT_SECRET`.
- **Gmail (paused):** the code builds and its tests pass. I made no functional changes (timeouts only).
- **JSON contract with the app:** field names are unchanged. `renewalDate` keeps its name; it now carries the projected date.

---

## 4. Still open, and why

| ID | Sev. | Description | Why it is not done |
|---|---|---|---|
| N-11 | High (product) | The backend has no email reminders, so web-only users never get notified. | This is a feature: it needs a UI in `settings.html`, a `reminder_days` setting per user, an idempotent `reminder_log`, a signed unsubscribe link and an **external cron** (Render free sleeps, so `@Scheduled` alone is not enough). It also needs a product decision. The design is in N-11 of the 09-10 audit. |
| M-03a | Medium | Rate limiting uses the first value of `X-Forwarded-For`, which the client can forge. | Choosing the trustworthy value depends on Render's proxy chain (Cloudflare + LB), which cannot be checked from the repo. The wrong choice would put every user in one bucket. Lockout per account (now on the web too) limits the damage. Next step: log `X-Forwarded-For` in prod, then decide. |
| RST-2 | Low | Password-reset and email-verification tokens are stored in plain text. | They are short-lived (1 h / 24 h) and single-use. Hashing them follows the same pattern as M-02; left for a later sprint to keep this change contained. |
| A-02b | Medium | Global categories (shared by every user). | The user explicitly said not to touch this. |
| N-12b | Low | The stored `renewalDate` still never moves forward; it is only projected when read. When an old record is edited, the projected date is saved, so a monthly charge on the 31st can become the 30th. | Advancing the date in the database needs a migration or job and a decision about the anchor. The projection on read already fixes everything visible. |
| B-04 / B-12 | Low | Admin email has a default in properties; the admin bootstrap reassigns orphaned subscriptions. | Harmless; no change. |
| B-11 | Low | `ZoneId.systemDefault()` in `GmailScanService` and `nextRenewalDate()` without a clock. | Gmail is paused; switch to `Clock` when it is turned back on. |
| CI | Medium | No CI; Render deploys with `-x test`. | Outside this scope (DevOps). |
| DEPS | Low | No dependency scanner (OWASP dependency-check / Renovate). | Not added so the build has no network dependency on the NVD. |

**Deployment note:** V14 needs PostgreSQL 11 or later (`sha256()`); Aiven meets that. After deploying, sessions open in the app keep working because existing tokens are converted to the hash. `APP_TIMEZONE` is optional (defaults to `Europe/Madrid`).

---

## 5. Verification

```
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew clean build
BUILD SUCCESSFUL — 8 actionable tasks: 8 executed
Tests: 112, failures 0, errors 0, skipped 0 (17 classes; before: 79 tests / 12 classes)
```
New test classes: `RenewalDatesTest`, `DashboardServiceTest`, `SubscriptionServiceTest`, `ApiExceptionHandlerTest` (standalone MockMvc), `AuthRateLimitFilterTest`. Updated: `TokenServiceTest`, `UserServiceRegistrationTest`, `UserServiceTest`.

Real-world (jar on Boot 3.5.16 + PostgreSQL 16 in a scratch DB, now deleted):
- Flyway V1→V13, a legacy plain-text token inserted, then V14 → the token is hashed and **refresh with the original value returns 200**; reusing it returns 401 and revokes the family.
- API: login with upper case and spaces returns 200; 85-character email returns 200; unverified returns 403 `EMAIL_NOT_VERIFIED`; locked returns 403 `ACCOUNT_LOCKED`; IDOR returns 404/404/404; categories as non-admin return 403; bad `trialEndsAt`, `name` over 100 or price 1e9 return 400; `/abc` returns 400; PUT (Hibernate 6.6 merge) returns 200; a monthly charge from May shows as `2026-10-15` with 5 days remaining.
- Web: 15 GET /login in a row all return 200 (no 429); 4 failed POSTs lock the account; negative price returns 400; someone else's subscription returns 404; dashboard, list, form, categories, catalogue, settings, CSV and delete-account return 200; the "category in use" message appears in English; logout works; **0 ERROR lines in the log**.

---

## Ronda 2 — verificación en vivo (2026-10-10)

Jar de Spring Boot 3.5.16 contra PostgreSQL 16 (docker, BD temporal `subia_r2`, ya borrada), recorrido con curl (cookie jar + `_csrf`) y Chrome vía Playwright como anónimo, admin y usuario normal (registrado y verificado con el enlace real de `/verify-email`).

### Decisión del usuario: categorías globales, solo admin

- Web: la lista ya ocultaba crear/editar/borrar a los no-admin (`isAdmin` en `CategoryController`) y `SecurityConfig` devuelve 403 en `/categories/new`, `/categories/*/edit` y los POST. Verificado en vivo: el usuario normal no ve ningún botón y recibe 403 si fuerza la URL o el POST.
- API: no había forma limpia de saber el rol (solo el claim `role` dentro del JWT). **Nuevo campo `role` (`"USER"` | `"ADMIN"`) en la respuesta de `POST /api/auth/login`, `POST /api/auth/refresh` y `POST /api/auth/google`** (`data.role`, junto a `accessToken`, `refreshToken`, `expiresInSeconds`, `tokenType`). Compatible: la app usa `ignoreUnknownKeys`. Tests en `TokenServiceTest`.

### Fallos encontrados en vivo y arreglados

| ID | Gravedad | Problema | Arreglo |
|----|----------|----------|---------|
| R2-01 | Media | Login web con cuenta **sin verificar o bloqueada**: `UserDetailsServiceImpl` lanzaba `DisabledException`/`LockedException` y `DaoAuthenticationProvider` las envolvía en `InternalAuthenticationServiceException` → **ERROR con traza en el log** en cada intento y el usuario solo veía «correo o contraseña incorrectos». | `loadUserByUsername` devuelve la cuenta marcada (`disabled` / `accountLocked`) y el provider lanza la excepción correcta. Nuevo `loadActiveUser()` conserva el comportamiento anterior para la API (`EMAIL_NOT_VERIFIED` / `ACCOUNT_LOCKED`, verificados) y el callback de Google. `failureHandler` redirige a `/login?error=unverified` o `?error=locked` con mensaje propio (es/en/fr). |
| R2-02 | Media | Superar el límite de `/login`, `/register`, `/forgot-password`, `/reset-password` en la web mostraba **JSON en bruto** (`{"code":"RATE_LIMIT_EXCEEDED"}`) en el navegador. | En rutas web `sendError(429)` → `error.html` con «Demasiados intentos… espera un minuto». La API sigue devolviendo el mismo JSON. Test en `AuthRateLimitFilterTest`. |
| R2-03 | Media (rendimiento) | `/catalog-browser` servía **2,1 MB** de HTML sin comprimir en cada carga (440 tarjetas). | `server.compression.enabled=true` (html, css, js, json, csv): baja a **75 KB**. |
| R2-04 | Baja | La API exigía `price > 0` (`@Positive`) y la web y `SubscriptionService` aceptan 0. | `@PositiveOrZero` en `SubscriptionRequestDto` (acepta más; no rompe clientes). |
| R2-05 | Cosmética | Abrir `/error` a mano mostraba «999». | `error.html` muestra «Error» cuando `status == 999`. |

### Verificado sin incidencias

- Todas las páginas cierran `</html>` (anónimo, usuario y admin, también con `?lang=en/fr/pt`); 404/403/400 con la página de error propia; 0 líneas ERROR en el log; en consola de Chrome solo el aviso del CDN de Tailwind y el rechazo de Google GSI al origen `localhost` (esperable en local); **ninguna violación CSP**.
- Dashboard con mensual EUR, anual EUR, semanal USD y prueba GBP: totales correctos (EUR 17,15/205,78; USD 41,14/494,00; la prueba vigente no suma) y selector de divisa.
- Asistente de 3 pasos: precarga desde catálogo, cambio de divisa (símbolo), prueba gratuita que obliga a la fecha de fin; editar (precarga de select/checkbox/fecha), borrar desde la pantalla de confirmación y desde el modal; IDOR → 404; añadir desde el catálogo; filtros y búsqueda del catálogo; CSV.
- Reset de contraseña completo (débil, no coinciden, OK, token reutilizado → `invalidToken`); verificación de email (2.º uso → `/verify-email-error`); borrado de cuenta (contraseña mala / vacía / correcta, limpia refresh tokens); bloqueo web tras intentos fallidos y reseteo del contador.
- API: login/refresh/logout, reutilización de refresh → 401 `TOKEN_REUSE_DETECTED`, CRUD de suscripciones con los campos que usa la app (`name`, `description`, `price`, `currency`, `billingCycle`, `renewalDate`, `categoryId`, `active`, `notes`, `isTrial`, `trialEndsAt`), `/api/dashboard/stats` (`gastoMensual`, `gastoAnual`, `totalSuscripciones`, `renovacionesProximas`), errores 400/401/403/404 en JSON.
- Migración V14 probada **sobre los 33 refresh tokens reales de la BD de desarrollo** dentro de una transacción con ROLLBACK: los 33 quedan hasheados y coinciden con `TokenService.hash`.
- Revisión del diff de la ronda 1 (`src/`, `build.gradle.kts`): sin regresiones detectadas; plantillas sin `T(...)`, `@environment`, `#{}` dentro de `${}`, `[/*`, Alpine ni forms POST sin `_csrf`.

### Pendiente (no se toca en esta ronda)

| ID | Gravedad | Motivo |
|----|----------|--------|
| R2-P1 | Media | `ClientIp` usa el **primer** valor de `X-Forwarded-For`, que controla el cliente: el rate limit por IP se salta mandando la cabecera (comprobado en vivo). No es regresión (antes igual). Arreglarlo bien exige saber cuántos proxys pone Render delante (tomar el último salto de confianza o `server.forward-headers-strategy`); hacerlo a ciegas podría meter a todos los usuarios en la misma IP. El bloqueo por cuenta (4 intentos) sí funciona. |
| R2-P2 | Baja | El límite de 10 POST/min por IP agrupa login **y refresh**: muchos usuarios de la app detrás del mismo CGNAT podrían recibir 429 al refrescar. Valorar sacar `/api/auth/refresh` del límite estricto. |
| R2-P3 | Baja | `/reset-password?token=<inválido>` pinta el formulario y el error solo aparece al enviar. |
| R2-P4 | Baja | La web acepta cualquier código ISO de 3 letras (p. ej. `XXX`), igual que la API; es deliberado para no romper divisas de la app, pero el selector web solo ofrece 11. |

### Verificación final

```
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew clean build
BUILD SUCCESSFUL — 8 actionable tasks: 8 executed
Tests: 116, failures 0, errors 0, skipped 0
```
Tests nuevos: `TokenServiceTest` (role en refresh de admin, serialización de `TokenResponse.role`), `UserDetailsServiceImplTest` (cuenta sin verificar por `DaoAuthenticationProvider` → `DisabledException`, no `InternalAuthenticationServiceException`), `AuthRateLimitFilterTest` (429 JSON en API / página de error en web).

---

## Ronda 3 (2026-10-10)

Arreglos de la regresión de la ronda 1 (`renewalDate` proyectada) y de los pendientes R2-P2/R2-P3. Sin commit ni cambio de versión; Gmail sigue pausado.

| ID | Sev. | Problema | Arreglo | Archivos |
|----|------|----------|---------|----------|
| R3-01 | **Alta** (regresión ronda 1, era N-12b / R2-P1 Android) | La API y el formulario web muestran la próxima renovación **proyectada**; al editar sin tocar la fecha se guardaba esa fecha y el ancla se perdía para siempre (mensual del 31 vista en noviembre → 30; anual del 29-feb → 28-feb). | `RenewalDates.anchorForUpdate`: si el ciclo no cambia y la fecha recibida es la que el servidor proyectaría desde el ancla guardada (hoy, o ayer para cubrir la medianoche y clientes con zona por detrás), **se conserva el ancla**. Cualquier otra fecha (otro día, saltarse un cobro el mismo día del cobro, cambio de ciclo) se respeta. Se aplica en el `PUT /api/subscriptions/{id}` y en `POST /subscriptions/{id}` (web). Además `SubscriptionDto` expone **`renewalAnchor`** (fecha guardada; campo nuevo, ignorado por apps antiguas) para que la app proyecte con su propio "hoy" y reenvíe el ancla. | `service/RenewalDates.kt`, `controller/api/ApiSubscriptionController.kt`, `controller/SubscriptionController.kt`, `dto/api/SubscriptionDto.kt` |
| R3-02 | Media (R2-P2) | Zona horaria: `nextRenewal` usa el reloj del servidor (Europe/Madrid). Con un móvil en América, de noche el servidor ya está en el día siguiente y el cobro de "hoy" local llegaba avanzado un ciclo (aviso perdido). | Se arregla en la app (lado sensato: solo ella sabe su zona): proyecta desde `renewalAnchor` con su fecha local. El servidor solo añade el campo y la tolerancia de "ayer" en el PUT. | (ver informe Android) |
| R3-03 | Media (R2-P3) | `/api/auth/refresh` compartía los 10 POST/min por IP con el login: detrás de CGNAT los refrescos legítimos recibían 429 (y además gastaban cupo del login). | Cubo propio `refresh|<ip>` de **60/min por IP** (`REFRESH_MAX_REQUESTS`); no consume ni agota el cupo del login y viceversa. Sigue limitado; el refresh exige un token de 256 bits y la reutilización revoca la familia. | `security/AuthRateLimitFilter.kt` |
| R3-04 | Baja (R2-P3 web) | `/reset-password?token=<inválido>` pintaba el formulario y el error solo salía al enviar. | `UserService.isResetTokenValid` (misma comprobación que `resetPassword`: existe, no usado, no caducado). El GET muestra directamente «El enlace es inválido o ha expirado», título «Enlace no válido» y botón «Solicitar nuevo enlace», sin formulario. Clave nueva `reset.invalid.title` en `messages`, `_es`, `_en`, `_fr`. | `service/UserService.kt`, `controller/AuthWebController.kt`, `templates/auth/reset-password.html`, `messages*.properties` |

**Limitación conocida (R3-01):** si el usuario elige a propósito exactamente la fecha proyectada (p. ej. el 30-nov para una del 31), se interpreta como "sin cambios"; los cobros futuros siguen la serie del 31. Apps antiguas (≤ 2.17.4) en zonas por delante de Madrid que editen justo el día del cobro pueden enviar una fecha re-proyectada desde la proyectada (posible deriva 31→30); la app nueva envía el ancla y no tiene el problema.

### Tests nuevos

- `RenewalDatesTest`: 8 casos de `anchorForUpdate` — día 31 en noviembre, día 30 en febrero, 29-feb anual, semanal, fecha cambiada a propósito (otro día, saltarse un cobro, semanal movida), cambio de ciclo, tolerancia de "ayer", reenvío del ancla.
- `SubscriptionAnchorUpdateTest` (nuevo): API (31 → se conserva; 29-feb; fecha cambiada se guarda; `renewalDate` proyectada + `renewalAnchor` en la respuesta) y formulario web (31 se conserva; fecha cambiada se guarda).
- `AuthRateLimitFilterTest`: cubo propio de refresh (60/min, independiente del login en ambos sentidos).
- `UserServiceTest`: `isResetTokenValid` con enlace válido, usado, caducado, desconocido y vacío.

### Verificación

```
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew clean build
BUILD SUCCESSFUL in 10s — 8 actionable tasks: 8 executed
Tests: 133, failures 0, errors 0, skipped 0 (18 clases; ronda 2: 116)
```

En vivo (jar + PostgreSQL 16 docker :5433, BD temporal `subia_r3`, ya borrada):
- `/reset-password?token=bogus` en es/en/fr: sin formulario, título y error traducidos, cierra `</html>`. Token válido → formulario; reset → 302 `/login?passwordReset=true`; reabrir el enlace usado → error directo.
- API: anual creada el 2024-02-29 → `GET` devuelve `renewalDate 2027-02-28`, `renewalAnchor 2024-02-29`; `PUT` con `2027-02-28` → BD sigue en **2024-02-29**; `PUT` con `2027-03-05` → se guarda 2027-03-05.
- Web: el formulario de edición muestra `2027-02-28`; enviarlo sin tocar → BD **2024-02-29**; con `2027-01-15` → se guarda.
- 12 `POST /api/auth/refresh` seguidos → 401 (ningún 429) y el login de la misma IP sigue dando 200.
- `/subscriptions`, `/dashboard`, `/login`, `/forgot-password`: 200 y `</html>`. **0 líneas ERROR** en el log.

### Pendiente

| ID | Sev. | Motivo |
|----|------|--------|
| R2-P1 / M-03a | Media | `ClientIp` sigue usando el primer `X-Forwarded-For` (falsificable). Sin cambios: depende de la cadena de proxys de Render. |
| R2-P4 | Baja | Divisas ISO arbitrarias aceptadas (deliberado). |
| RST-2, N-11, CI, DEPS | — | Sin cambios respecto a la ronda 1. |

---

## Ronda 4 (2026-10-10)

Revisión independiente de autenticación. Cada punto se verificó **antes** de arreglarlo con tests de integración nuevos contra PostgreSQL real (los tests con mocks no podían ver estos fallos). Sin commit ni cambio de versión.

| ID | Sev. | Problema (verificado) | Arreglo | Archivos |
|----|------|----------|---------|----------|
| R4-01 | **Crítica** | `TokenService.refresh()` es `@Transactional`: al detectar la reutilización de un refresh token revocaba la familia y lanzaba `BadCredentialsException("TOKEN_REUSE_DETECTED")` → **rollback** → la revocación no se guardaba y el token emitido en la rotación seguía sirviendo. | `@Transactional(noRollbackFor = [AuthenticationException::class])` en `refresh` (no hay otras escrituras que deban deshacerse en ese método). | `security/TokenService.kt` |
| R4-02 | **Crítica** | `TokenService.login()` igual: `handleFailedLogin` se unía a la transacción y la excepción posterior deshacía el contador y `lockedUntil` → **el bloqueo por intentos no funcionaba en la API** (la app). El comentario de `WebLoginAttemptListener` decía lo contrario. | Mismo `noRollbackFor` en `login`. Comentario corregido. Revisados los demás `@Transactional` con escritura + excepción: `register`, `verifyEmail` y `resetPassword` lanzan **antes** de escribir; `findOrCreateByGoogle` no lanza tras escribir y el callback web de Google no es transaccional; `GmailScanTicketService.consume` devuelve `null`; `CategoryService.delete` lanza antes de borrar. Sin más casos. | `security/TokenService.kt`, `security/WebLoginAttemptListener.kt` |
| R4-03 | Advertencia | V14 hashea los refresh tokens: en un despliegue sin corte en Render, la instancia antigua sigue guardando tokens en claro y la nueva (que busca por hash) no los encuentra → sesiones cerradas. | `findStoredToken`: `findByToken(hash(raw)) ?: findByToken(raw)` en `refresh` y `logout` (comentario «Transición V14; quitar en la próxima versión»). El token en claro se trata como válido y al rotar el nuevo se guarda hasheado. **Salvaguarda:** el respaldo no se aplica a valores con forma de hash (64 hex); si no, quien leyera la BD podría usar el hash guardado como token (anularía M-02). Los antiguos son UUID (36 car.). Limitación: el sentido contrario (instancia antigua recibiendo un token emitido por la nueva) no se puede arreglar desde el código nuevo. | `security/TokenService.kt` |
| R4-04 | Advertencia (regresión ronda 1) | El registro normaliza el email a minúsculas, pero la búsqueda solo probaba «tal cual» y «en minúsculas»: una cuenta antigua `Juan@Gmail.com` no se encontraba con `juan@gmail.com` → login fallido y **cuenta duplicada** en registro y Google; «olvidé mi contraseña» no enviaba nada. | `UserRepository.findByEmailAnyCase` (extensión): exacta (índice único) → `findFirstByEmailIgnoreCaseOrderByIdAsc`. La usan `UserService.findByEmail` (registro, Google, olvido, contador de fallos), `UserDetailsServiceImpl.loadUserByUsername` (login web y API) y `AdminBootstrapListener`. Los controladores que buscan con `findByEmail` exacto reciben el email canónico (JWT `sub` / sesión) y no cambian. Además el email de restablecimiento se envía a la dirección **guardada**, no a la escrita. | `repository/UserRepository.kt`, `service/UserService.kt`, `security/UserDetailsServiceImpl.kt`, `security/AdminBootstrapListener.kt` |
| R4-05 | Baja | El login revelaba «sin verificar» / «bloqueada» antes de comprobar la contraseña (web `?error=unverified` / `?error=locked`, API `EMAIL_NOT_VERIFIED` / `ACCOUNT_LOCKED`). | Orden nuevo en API y web: **bloqueada → contraseña → verificada**. «Sin verificar» solo se dice con la contraseña correcta; con una incorrecta, error genérico (y cuenta como fallo). Web: `DaoAuthenticationProvider` propio como `@Bean` (pre-check solo bloqueo; post-check verificación y caducidad). Tiene que ser bean global: añadido solo al chain, Spring probaba el proveedor por defecto como «padre» y volvía a salir `?error=unverified`. Códigos sin renombrar (la app Android los usa). | `security/TokenService.kt`, `config/SecurityConfig.kt`, `application.properties` (silencia el WARN informativo de Spring por ese bean) |

**Decisión en R4-05 (bloqueo):** una cuenta bloqueada se sigue rechazando con `ACCOUNT_LOCKED` / `?error=locked` **sin mirar la contraseña**. Si el mensaje dependiera de la contraseña, durante el bloqueo un atacante podría seguir probando claves y «cuenta bloqueada» le confirmaría la correcta: el bloqueo dejaría de frenar la fuerza bruta. Queda la fuga menor de que una cuenta con ≥4 fallos existe (ver R4-P2).

### Tests nuevos

- `integration/AuthIntegrationTest` (**12 tests**, `@SpringBootTest` + MockMvc contra PostgreSQL real): `integration/TestDatabase` crea una BD temporal `subia_it_<aleatorio>` en el PostgreSQL de docker-compose (:5433), Flyway aplica todas las migraciones y se borra al acabar la JVM (`DROP DATABASE … WITH (FORCE)`). Si el PostgreSQL no está levantado, la clase se **salta** (`@EnabledIf`), no falla.
  - Reutilización de refresh → el token de la rotación queda `revoked=true` **en BD**.
  - 4 logins fallidos (servicio y HTTP) → `failed_attempts=4` y `locked_until` futuro **en BD**; después ni la contraseña correcta entra.
  - Token antiguo en claro: refresh funciona, el nuevo se guarda hasheado, logout lo revoca; el hash de BD **no** sirve como token.
  - Cuenta `Juan.x@Gmail.com`: login en minúsculas, `loadUserByUsername` en mayúsculas, registro, olvido y Google → una sola cuenta.
  - API y web: sin verificar + contraseña mala → error genérico; + buena → `EMAIL_NOT_VERIFIED` / `?error=unverified`; bloqueada + buena → `LockedException` / `?error=locked`; 4 fallos web → bloqueo en BD.
- **Antes del arreglo** (mismos tests): 12 ejecutados, **8 fallos**: `expected <4> but was <0>` (bloqueo API) y `expected <1> but was <0>` (HTTP), `token emitido en la rotación debe quedar revocado ==> expected <true> but was <false>`, `Refresh token no encontrado` (token en claro), logout en claro sin revocar, `BadCredentialsException` con la cuenta en mayúsculas, `DisabledException` en lugar del error genérico y `?error=unverified` con contraseña mala. Los 4 que ya pasaban vigilan que no se rompa nada (bloqueada web/API, 4 fallos web, hash como token).
- Unitarios: `UserServiceTest` (+2: cuenta antigua con mayúsculas; prefiere coincidencia exacta), `UserDetailsServiceImplTest` (+1), `UserServiceRegistrationTest` adaptado. Dependencia de test nueva: `spring-security-test` (para `csrf()` en MockMvc).

### Verificación

```
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew clean build
BUILD SUCCESSFUL in 20s
Tests: 148, failures 0, errors 0, skipped 0 (ronda 3: 133); AuthIntegrationTest 12/12, 0 ERROR en su log
```

En vivo (jar + PostgreSQL 16 docker :5433, BD temporal `subia_live_r4`, ya borrada; docker como estaba):
- Refresh: login → R1; refresh R1 → R2; refresh R1 otra vez → `401 TOKEN_REUSE_DETECTED`; BD: token R2 `revoked = t`; usar R2 → `TOKEN_REUSE_DETECTED`.
- 4 logins fallidos por API → `401 Credenciales inválidas` ×4; BD `failed_attempts=4`, `locked_until > now()` = t; contraseña correcta → `ACCOUNT_LOCKED`.
- Token en claro insertado a mano → refresh 200; BD: el antiguo `revoked = t`, 0 tokens en claro activos; enviar el hash de BD como token → `Refresh token no encontrado`.
- Cuenta `Ana.Vieja@Gmail.com` insertada a mano: login API y web con `ana.vieja@gmail.com` → 200 / `/dashboard`; registro → `/verify-pending` y BD sigue con **1** cuenta; olvido con `ANA.VIEJA@gmail.com` → 1 token de reset para esa cuenta.
- Sin verificar: API mala → `UNAUTHORIZED`, buena → `EMAIL_NOT_VERIFIED`; web mala → `/login?error`, buena → `/login?error=unverified`. Bloqueada + buena (web) → `/login?error=locked`.
- Log: **0 ERROR**; WARN solo esperados (admin sin contraseña, cuenta bloqueada, Resend sin clave).

### Pendiente

| ID | Sev. | Motivo |
|----|------|--------|
| R4-P1 | Media | **Unicidad del email sin distinguir mayúsculas en BD.** No se añade migración: si en producción ya hay dos cuentas que solo difieren en mayúsculas, un índice único fallaría y rompería el despliegue. Comprobar antes en producción: `SELECT lower(email), count(*), string_agg(email, ', ') FROM users GROUP BY lower(email) HAVING count(*) > 1;` Si sale vacío: `CREATE UNIQUE INDEX CONCURRENTLY ux_users_email_lower ON users (lower(email));` (o una migración V16). Si hay duplicados, fusionarlos a mano antes. Mientras tanto, la búsqueda devuelve la cuenta más antigua. |
| R4-P2 | Baja | Enumeración residual: tras 4 fallos, una cuenta existente responde «bloqueada» y una inexistente sigue con el error genérico. Se acepta para no convertir el bloqueo en un oráculo de contraseñas. Opción futura: contar también en memoria los fallos de emails inexistentes y responder igual. |
| R4-P3 | Baja | Quitar el respaldo «token en claro» de `TokenService.findStoredToken` en la próxima versión (y, si quedan, hashear las filas con `length(token) <> 64` como en V14). |
| R4-P4 | Baja | El login con email inexistente no ejecuta BCrypt (responde más rápido que con una cuenta existente): fuga por tiempos. |
| R4-P5 | Baja | `AdminBootstrapListener`: si el `UPDATE` de suscripciones huérfanas falla, el `catch` no evita que la transacción quede marcada como rollback-only y el alta del admin se deshace. Solo afecta al primer arranque con `ADMIN_PASSWORD`. |
| R2-P1 / M-03a, R2-P4, RST-2, N-11, CI, DEPS | — | Sin cambios respecto a la ronda 3. |

## Ronda 5 (2026-10-10)

Dos advertencias de un revisor independiente sobre el bloqueo por intentos. Ambas se reprodujeron con tests de integración contra PostgreSQL real **antes** de arreglarlas. Sin commit ni cambio de versión.

| ID | Sev. | Problema (verificado) | Arreglo | Archivos |
|----|------|----------|---------|----------|
| R5-01 | Advertencia | `handleFailedLogin` / `resetFailedAttempts` hacían `findByEmail` → `save(user.copy(...))`, sin UPDATE atómico ni `@Version`. **Con 20 fallos concurrentes solo quedaban 3 en BD**: con ráfagas el bloqueo no llegaba. Además `save(copy)` reescribe la fila entera y podía pisar un `passwordHash` / `emailVerified` / `googleId` recién cambiado. | UPDATE por columna en `UserRepository` (`@Modifying(flushAutomatically = true, clearAutomatically = true)`): `incrementFailedAttempts` (`failedAttempts = failedAttempts + 1`), relectura con `findFailedAttempts` en la misma transacción (la fila queda bloqueada hasta el commit, así que el valor es el de este intento), `extendLock` (solo columna `lockedUntil`, nunca acorta un bloqueo más largo) y `resetFailedAttempts` (`SET failedAttempts = 0, lockedUntil = NULL`). Los métodos de `UserService` siguen siendo `@Transactional` (REQUIRED): se unen a la de `TokenService.login` (con `noRollbackFor`) y abren la suya en el listener web. Misma técnica en los demás `save(user.copy(...))` del usuario: `verifyEmail` (`markEmailVerified`, que además consume el token de forma atómica), reenvío de verificación en `register` (`updateVerificationToken`), `resetPassword` (`resetPasswordAndUnlock`) y vinculación de Google (`linkGoogle` / `linkGoogleAndVerify`). `resetPassword` consume ahora el enlace con `PasswordResetTokenRepository.markUsedIfUnused` **antes** de escribir: dos envíos simultáneos del mismo enlace ya no fijan cada uno su contraseña. Fórmula del bloqueo sin cambios (`UserService.lockMinutes`: 5 min · 2^(n-4), tope 24 h). | `repository/UserRepository.kt`, `repository/PasswordResetTokenRepository.kt`, `service/UserService.kt` |
| R5-02 | Advertencia | Bloqueo usado como denegación de servicio: con 4 contraseñas malas cualquiera bloquea una cuenta ajena y puede renovarlo. El callback **web** de Google (`loadActiveUser`) rechazaba la cuenta bloqueada (`/login?error=googleFailed`), mientras que la **API** de Google (`issueTokenPairForEmail`) no miraba el bloqueo: incoherente. | **Decisión:** el bloqueo es contra adivinar contraseñas; un login con Google verificado no lo sufre. `UserService.loginWithGoogle` (lo usan web y API): `findOrCreateByGoogle` → reinicia contador y bloqueo → exige email verificado (`DisabledException("EMAIL_NOT_VERIFIED")`, igual en ambos; tras vincular con Google siempre lo está). En la web, `UserDetailsServiceImpl.loadUserForGoogle` sustituye a `loadActiveUser`: exige verificado pero no mira el bloqueo. El login con contraseña sigue bloqueado igual que antes. | `service/UserService.kt`, `security/UserDetailsServiceImpl.kt`, `controller/AuthWebController.kt`, `controller/api/GoogleOAuthController.kt` |

### Tests nuevos

- `integration/AuthIntegrationTest` (+2, ahora 14):
  - **20 `handleFailedLogin` concurrentes** (20 hilos con `CountDownLatch`) → `failed_attempts = 20` y `locked_until` a ~24 h.
  - `resetPassword` desbloquea, verifica y deja entrar con la nueva; reutilizar el enlace → `VerificationTokenException`.
- `integration/GoogleLoginIntegrationTest` (nuevo, 3; `@MockitoBean GoogleOAuthService`): cuenta con contraseña bloqueada (6 fallos, `locked_until` +2 h) → login con Google por **API** (200 con `accessToken`) y por **web** (`/dashboard`) entra, deja `failed_attempts = 0`, `locked_until = NULL`, vincula `google_id` y no toca la contraseña; también con la cuenta ya vinculada (búsqueda por `googleId`).
- **Antes del arreglo** (mismos tests): 17 ejecutados, **4 fallos**: `expected: <20> but was: <3>` (concurrencia), `Redirected URL expected:</dashboard> but was:</login?error=googleFailed>` (web Google) y `expected: <0> but was: <6>` ×2 (la API de Google entraba pero no reiniciaba el contador).
- Unitarios: `UserServiceTest` reescrito para el nuevo contrato (sin `save`; intento 3 sin bloqueo, 4/5/6/7 → 5/10/20/40 min, tope 1440; email desconocido no escribe; reset por UPDATE; Google por columnas; `loginWithGoogle` reinicia; enlace de reset ya consumido → excepción), `UserServiceRegistrationTest` (reenvío solo actualiza el token) y `UserDetailsServiceImplTest` (+1 `loadUserForGoogle`).

### Verificación

```
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew clean build
BUILD SUCCESSFUL in 23s
Tests: 157, failures 0, errors 0, skipped 0 (ronda 4: 148); AuthIntegrationTest 14/14, GoogleLoginIntegrationTest 3/3
```

En vivo (jar + PostgreSQL 16 docker :5433, BD temporal `subia_live_r5`, ya borrada; docker como estaba):
- Registro web + enlace real de `/verify-email` → `email_verified = t`, `failed_attempts = 0`.
- **Ráfaga de 20 logins API fallidos en paralelo** (curl `&`, IP distinta cada uno): 13 × `401` + 7 × `403 ACCOUNT_LOCKED`; BD `failed_attempts = 13`, bloqueada, 1440 min. Cada fallo contado está en BD (13 = 13); los 7 últimos llegaron con el bloqueo ya guardado y se rechazaron sin mirar la contraseña (comportamiento previsto). Antes, con la misma carrera, se perdían incrementos.
- Contraseña correcta durante el bloqueo → `ACCOUNT_LOCKED`.
- Olvido (web, con `_csrf`) → token real → `/reset-password` → `/login?passwordReset=true`; BD `failed_attempts = 0`, `locked_until = NULL`, verificado, token `used = t`. Login API con la nueva → 200, con la antigua → 401 (cuenta 1 fallo). Reutilizar el enlace → `?error=invalidToken` y la contraseña que se intentó fijar no entra (401).
- Log: **0 ERROR**; WARN solo esperados (admin sin contraseña, Resend sin clave, «Cuenta bloqueada…» de la ráfaga).

### Pendiente

| ID | Sev. | Motivo |
|----|------|--------|
| R5-P1 | Media | **DoS residual del bloqueo por contraseña.** Un tercero sigue pudiendo bloquear el login *con contraseña* de una cuenta ajena (con una ráfaga, 24 h de golpe) y renovarlo. Mitigado: Google entra y reinicia, y «olvidé mi contraseña» desbloquea. Opciones futuras: bloquear por (cuenta, IP/dispositivo) en vez de solo por cuenta, CAPTCHA tras N fallos o no escalar con fallos llegados durante el bloqueo. |
| R5-P2 | Baja | Alta concurrente del **mismo** usuario nuevo con Google (dos primeros logins a la vez) puede chocar con el índice único de `email`/`google_id` → 500 en una de las dos. No visto en la práctica. |
| R5-P3 | Baja | Quedan `save(entity.copy(...))` en `refresh_tokens` (revocar familia/logout/reset), suscripciones y categorías: solo reescriben filas de esas tablas, sin el riesgo de pisar credenciales; revocar ya es idempotente. |
| R4-P1…P5, R2-P1 / M-03a, R2-P4, RST-2, N-11, CI, DEPS | — | Sin cambios respecto a la ronda 4. |

## Ronda 6 (2026-10-10)

Seis puntos de un revisor independiente. Cada uno se comprobó con tests de integración contra PostgreSQL real **antes** de tocar el código. Sin commit ni cambio de versión.

| ID | Sev. | Problema (verificado) | Arreglo | Archivos |
|----|------|----------|---------|----------|
| R6-01 | Advertencia | **Pre-hijacking por el reenvío del registro.** Un atacante registra `victima@x` con su contraseña y la cuenta queda sin verificar. Cuando la víctima se registraba, se le **reenviaba la verificación**; al pulsarla, la cuenta quedaba verificada con la contraseña del **atacante**. | Si el email existe sin verificar y con contraseña, ya no se reenvía la verificación. `clearVerificationToken` anula el enlace pendiente (también el original, que llegó al buzón de la víctima) y se envía un enlace de `/reset-password` (`EmailService.sendPendingAccountEmail`, «Activa tu cuenta y elige contraseña»). Al fijar la contraseña, `resetPassword` verifica la cuenta, así que solo decide quien controla el buzón. La respuesta del registro no cambia (`/verify-pending`). `verify.text` pasa a ser neutro en es/en/fr/por defecto («Te hemos enviado un enlace para activar tu cuenta…»), válido para los dos correos. Se reutiliza `createPasswordResetToken` (extraído de `initiatePasswordReset`). `updateVerificationToken` se elimina porque ya no se usa. | `service/UserService.kt`, `service/EmailService.kt`, `repository/UserRepository.kt`, `messages*.properties` |
| R6-02 | Advertencia | `resetPassword` solo revocaba los refresh tokens: **una sesión web abierta seguía valiendo** con la contraseña antigua. `deleteAccount` tampoco cerraba las sesiones de otros navegadores. Además, `maximumSessions(1)` usaba el `SessionRegistryImpl` en memoria. Ese registro no ve las sesiones de Spring Session JDBC y nunca se limpiaba, porque Spring Session no emite eventos de sesión del contenedor. | `config/SessionConfig.kt`: bean `SpringSessionBackedSessionRegistry` sobre el repositorio JDBC de Spring Session (busca por `PRINCIPAL_NAME` = `Authentication.name` = email **guardado**; el servicio pasa `user.email` de la BD, así que no hay problema de mayúsculas). Es **el mismo** en `maximumSessions(1).sessionRegistry(...)` y en `UserService`. Se añade `.expiredUrl("/login?expired")`. `HttpSessionEventPublisher` no hace falta: este registro consulta la BD y no depende de eventos. `resetPassword` y `deleteAccount` usan una transacción programática (`TransactionTemplate`, REQUIRED) y caducan las sesiones (`expireNow`) **después del commit**. Así no se anida la transacción REQUIRES_NEW de Spring Session, que ocuparía 2 conexiones por petición con un pool de **3** en producción, y no se cierran sesiones si el cambio se deshace. Un fallo al caducar deja un WARN y no tumba el restablecimiento. | `config/SessionConfig.kt` (nuevo), `config/SecurityConfig.kt`, `service/UserService.kt` |
| R6-03 | Baja | `findOrCreateByGoogle` **sobrescribía** un `googleId` distinto ya vinculado: quien entrara con otra cuenta de Google con el mismo email dejaba fuera a la original. | `linkGoogleAccount` (extraída): si `googleId != null && != nuevo`, lanza `InvalidGoogleTokenException`. API → 401; web → `/login?error=googleFailed`. | `service/UserService.kt` |
| R6-04 | — | El revisor decía que el formLogin web comprueba disabled/locked **antes** de la contraseña. **No es así**: el `DaoAuthenticationProvider` propio de la ronda 4 (bean global) solo mira el bloqueo en `preAuthenticationChecks`, y `isEnabled` en `postAuthenticationChecks`. El test de la ronda 4 pasa sin cambios y se le añade que, con contraseña mala, la cuenta pendiente **cuenta el intento** (`failed_attempts = 1`); también pasa. La ronda 4 tenía razón y no hay cambio de código. | — | `AuthIntegrationTest` (aserción nueva) |
| R6-05 | Baja | **Carrera en el primer login con Google** (era R5-P2): dos logins simultáneos de un usuario nuevo → `DataIntegrityViolationException` (`users_email_key`) → 500. | El alta pasa a ser `INSERT … ON CONFLICT DO NOTHING` nativo (`insertGoogleUserIfAbsent`; el resto de columnas usan los DEFAULT de V6). Espera al commit del otro alta y no aborta la transacción de PostgreSQL. Después se relee por `googleId` y, si el choque fue por email con otra cuenta, se vincula como arriba. Se descartó `REQUIRES_NEW`: 2 conexiones por petición con un pool de 3 permiten un interbloqueo del pool. | `service/UserService.kt`, `repository/UserRepository.kt` |
| R6-06 | — | Faltaban tests de integración para la vinculación de Google con una cuenta sin verificar y para los usos simultáneos del mismo enlace. | Añadidos (ver abajo). Pasan con el código de la ronda 5. | tests |

### Tests nuevos

- `integration/AccountSessionIntegrationTest` (nuevo, 5):
  - **Pre-hijacking por la web**: el atacante y luego la víctima hacen `POST /register` y ambos reciben `/verify-pending`. Tras el segundo, `email_verification_token = NULL` y el enlace del atacante da `VerificationTokenException`. Hay 1 enlace de reset; con él, la víctima fija su contraseña y la cuenta queda verificada. Login del atacante → `/login?error`; login de la víctima → `/dashboard`.
  - `resetPassword` y `deleteAccount` cierran la sesión web: cookie `SESSION` del formLogin → `/dashboard` 200 → tras la operación, `/login?expired`.
  - Dos `resetPassword` simultáneos con el mismo enlace: uno gana y el otro recibe `VerificationTokenException`; en BD queda la contraseña del ganador. Dos `verifyEmail` simultáneos: el segundo falla.
- `integration/GoogleLoginIntegrationTest` (+3, ahora 6):
  - Callback web de Google con una cuenta sin verificar → BD `password_hash IS NULL`, `email_verified = true`, token de verificación NULL y `google_id` fijado.
  - API con otro `googleId` → 401 y `google_id` sin cambios.
  - **6 primeros logins con Google simultáneos ×5 rondas** → sin errores, la misma cuenta para todos y una sola fila.
- `AuthIntegrationTest`: aserción `failed_attempts = 1` (R6-04).
- Unitarios: `UserServiceTest` (+1, googleId distinto) y `UserServiceRegistrationTest` (reenvío → anula la verificación + `sendPendingAccountEmail`, sin `sendVerificationEmail`).
- **Antes del arreglo** (mismos tests de integración): **5 fallos**:
  - `el enlace de verificación previo debe quedar invalidado ==> expected: <null> but was: <5411530f…>` (R6-01).
  - `Redirected URL expected:</login?expired> but was:<null>` en reset y en borrado (R6-02).
  - `Status expected:<401> but was:<200>` (R6-03).
  - `DataIntegrityViolationException … duplicate key value violates unique constraint "users_email_key"` (R6-05).

  Con R6-04 (incluida la aserción nueva) y los 3 tests de R6-06 en verde ya entonces.

### Verificación

```
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew clean build
BUILD SUCCESSFUL in 27s
Tests: 166, failures 0, errors 0, skipped 0 (ronda 5: 157); AccountSessionIntegrationTest 5/5, AuthIntegrationTest 14/14, GoogleLoginIntegrationTest 6/6; 0 ERROR en su log
```

En vivo (jar + PostgreSQL 16 docker :5433, BD temporal `subia_live_r6`, ya borrada; docker como estaba), todo con curl, cookies y `_csrf`:
- Atacante `POST /register` → `302 /verify-pending`; BD `email_verified = f` y token T1.
- Víctima `POST /register` → `302 /verify-pending` (misma respuesta); BD: 1 cuenta, token de verificación `NULL`, 1 enlace de reset; correo «Activa tu cuenta en SuscriptWallet».
- `GET /verify-email?token=T1` → `/verify-email-error` y la cuenta sigue sin verificar. Login del atacante → `/login?error=unverified`.
- La víctima usa el enlace → `/login?passwordReset=true`; BD verificada y enlace `used = t`. Login del **atacante → `/login?error`**; víctima → `/dashboard` y `GET /dashboard` 200.
- Sesión abierta (1 fila en `spring_session` con su email) → «olvidé mi contraseña» desde otro navegador → reset → **`GET /dashboard` con la cookie anterior → `302 /login?expired`**, y después `/login`. Contraseña antigua → `/login?error`; nueva → `/dashboard`.
- Log: **0 ERROR**; WARN solo los esperados (admin sin contraseña, Resend sin clave).

### Pendiente

| ID | Sev. | Motivo |
|----|------|--------|
| R6-P1 | Baja | Si la víctima pulsa el enlace de verificación original **antes** de registrarse ella (un correo «Verifica tu cuenta» que no pidió), la cuenta queda verificada con la contraseña del atacante. Es inherente a verificar por enlace sin pedir contraseña. Mitigación posible: que `/verify-email` pida fijar la contraseña, o exigir el reset cuando la cuenta se creó hace más de X horas. |
| R6-P2 | Baja | Repetir el registro de una cuenta pendiente envía un correo de activación al buzón, igual que «olvidé mi contraseña». Lo frena el rate limit por IP (`AuthRateLimitFilter`), no un límite por cuenta. |
| R6-P3 | Baja | Los correos (verificación, reset, activación) siguen solo en castellano (HTML en `EmailService`, sin i18n), como antes. |
| R6-P4 | Info | Con el registro respaldado por la BD, `maximumSessions(1)` se aplica de verdad entre instancias y reinicios: un login con formulario caduca la sesión web anterior del mismo usuario. Las sesiones creadas por el callback de Google no pasan por el control de concurrencia (como antes), pero sí se cierran al restablecer o borrar. |
| R5-P1, R5-P3, R4-P1…P5, R2-P1 / M-03a, R2-P4, RST-2, N-11, CI, DEPS | — | Sin cambios respecto a la ronda 5. R5-P2 queda resuelto (R6-05). |
