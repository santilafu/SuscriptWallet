# Auditoría Android/KMP — SuscriptWallet (SubIA) · 2026-10-10

**Alcance:** `mobile/` (módulos `shared` y `androidApp`), v2.17.4 (versionCode 32, sin cambiar).
**Método:** lectura completa del código de `shared` y de las pantallas, worker, widget, manifest y reglas R8; contraste de cada modelo JSON con los DTO del backend (`src/main/kotlin/com/subia/dto/api`, solo lectura); build debug + release (R8), tests, lint y prueba de humo del APK release en el emulador (Inicio, Suscripciones, Detalle, Editar, Catálogo, Ajustes).
**Partida:** se revisaron `2026-10-07-auditoria-completa.md`, `2026-10-07-auditoria-diseno-apple-design.md` y `2026-10-09-notificaciones.md`. Lo arreglado allí (avisos N-01…N-20, B-08, M-12 cleartext, M-13 timeouts, CSP/Alpine…) se dio por cerrado y solo se comprobó que siguiera bien.

## Resumen

Se encontraron **3 errores altos** que el resto de auditorías no detectaron porque los tests replicaban el JSON equivocado: la app y el backend no hablaban el mismo idioma en la divisa, la descripción, el estado activo y la creación de categorías. Editar una suscripción desde el móvil podía **cambiar su divisa a EUR, borrar su descripción y reactivarla**. Todos arreglados, con tests.

| Verificación | Antes | Después |
|---|---|---|
| `:androidApp:assembleDebug` | OK | OK |
| `:shared:testDebugUnitTest` | 155 tests, 0 fallos | **164 tests, 0 fallos** |
| `:androidApp:lintDebug` | 0 errores, 101 avisos | **0 errores, 74 avisos** |
| `:androidApp:assembleRelease` (R8) | — | OK (APK 7,3 MB) |
| Prueba de humo del APK release en emulador (API 36) | — | Sin crashes, datos y navegación correctos |
| `tools/i18n/check_strings.py` | OK | OK (ahora acepta la cantidad `many` de fr/pt) |

## Hallazgos

| ID | Sev. | Estado | Hallazgo | Archivo |
|---|---|---|---|---|
| A-01 | **Alta** | Arreglado | `Subscription` leía `descripcion`/`moneda`, el backend envía `description`/`currency`: **todas las suscripciones salían en EUR y sin descripción** (lista, Inicio, totales por divisa, avisos, CSV). El test de serialización usaba las claves erróneas, por eso pasaba. | `shared/.../model/Subscription.kt:28-30` |
| A-02 | **Alta** | Arreglado | Consecuencia de A-01 al **editar**: el formulario cargaba EUR y "" y el PUT los guardaba, pisando la divisa real (USD/GBP) y la descripción en el servidor. | `SuscripcionFormViewModel.kt` (`cargarParaEditar`) |
| A-03 | **Alta** | Arreglado | `NuevaSuscripcionRequest` no enviaba `active` y `SubscriptionRequestDto.active` vale `true` por defecto: **editar en la app una suscripción desactivada en la web la reactivaba**. Ahora se conserva el estado cargado. | `Subscription.kt:48`, `SuscripcionFormViewModel.kt` |
| A-04 | **Alta** | Arreglado (parcial, ver P-01) | Crear categoría **fallaba siempre**: se enviaba `nombre` (el backend espera `name`) e `icon` vacío (`@NotBlank`) → 400. Ahora se envía `name`, color e icono (🏷️) por defecto. El backend además exige rol ADMIN (403): se muestra un mensaje claro y traducido en vez de "No se ha podido crear". | `model/Category.kt:20`, `CategoriasViewModel.kt`, `TextosError.kt` |
| M-01 | Media | Arreglado | El Inicio sumaba suscripciones **inactivas** al gasto mensual/anual, top, desglose por categoría y pruebas por vencer (el backend y la proyección ya las excluían): el hero no cuadraba con "Suscripciones activas". También afectaba al resumen anual compartible. | `DashboardViewModel.kt` (`publicarDerivados`) |
| M-02 | Media | Arreglado | El total "X activas · N €/mes" de la lista trataba una **semanal como mensual** (5 € en vez de 21,67 €). Nuevo `Subscription.gastoMensualEquivalente()` compartido por lista e Inicio. | `SuscripcionesScreen.kt:315`, `Subscription.kt` |
| M-03 | Media | Arreglado | `startDestination` dependía de `isLoggedIn` (que arrancaba en `false`): el NavHost se montaba con Login y cambiaba de grafo en el primer frame, y **al iniciar sesión el cambio de grafo competía con `onLoginSuccess`** (Inicio duplicado en la pila o onboarding saltado). Ahora la sesión se lee al crear el ViewModel y el destino inicial es fijo. | `MainActivity.kt`, `AuthViewModel.kt` |
| M-04 | Media | Arreglado | Detalle: si **fallaba el borrado**, el estado pasaba a `Error`, la suscripción "desaparecía" y quedaba un **spinner infinito**; si fallaba la carga sin caché, también. Ahora conserva la última suscripción conocida y, sin datos, muestra error con reintentar. | `SuscripcionDetalleScreen.kt` |
| M-05 | Media | Arreglado | Copia de seguridad: `allowBackup=true` sin reglas subía a Google Drive `subia_secure_prefs` (imposible de descifrar en otro móvil) y **`subia_secure_prefs_fallback` con los tokens en claro** (el respaldo que usa `TokenStorage` si el Keystore falla). Nuevas `backup_rules.xml` y `data_extraction_rules.xml` que los excluyen. | `AndroidManifest.xml`, `res/xml/` |
| B-01 | Baja | Arreglado | Crash posible (`ActivityNotFoundException`) sin navegador o sin selector de documentos: "¿Olvidaste tu contraseña?", "Regístrate", "Ir a cancelar suscripción", "Exportar" y "Compartir resumen". | `LoginScreen.kt`, `SuscripcionDetalleScreen.kt`, `SettingsScreen.kt`, `ResumenAnualScreen.kt` |
| B-02 | Baja | Arreglado | `AdView` nunca se destruía (`onRelease`): cada visita a Inicio/Suscripciones dejaba un WebView vivo con su refresco. | `ui/BannerAdView.kt` |
| B-03 | Baja | Arreglado | Insignia de prueba con días negativos ("Prueba · -12d") en pruebas vencidas. | `SuscripcionesScreen.kt` |
| B-04 | Baja | Arreglado | El widget seguía mostrando el gasto y cobros de la cuenta que cerró sesión hasta el siguiente refresco. | `ui/SubIAApp.kt` |
| B-05 | Baja | Arreglado | Exportar CSV solo leía la caché de la pestaña Suscripciones (vacío si no se había abierto). Ahora la más reciente; además BOM UTF-8 (acentos en Excel) y neutralización de fórmulas (`=`,`+`,`-`,`@`). | `SettingsScreen.kt`, `util/CsvExport.kt` |
| B-06 | Baja | Arreglado | i18n: "1 servicios disponibles" / "1 suscripciones activas" (catálogo y tarjeta compartible) → plurales en es/en/fr/pt; cantidad `many` en todos los plurales fr/pt (lint `MissingQuantity`, 22 avisos); nuevo texto de permiso de categorías en los 4 idiomas. | `res/values*/strings.xml`, `tools/i18n/check_strings.py` |
| B-07 | Baja | Arreglado | El error de "Nueva categoría" reaparecía al reabrir el diálogo; el importe se cargaba como "12.0" al editar. | `CategoriasScreen.kt`, `SuscripcionFormViewModel.kt` |

## Pendiente (y por qué)

| ID | Sev. | Motivo |
|---|---|---|
| P-01 | Media | **Categorías globales y solo-ADMIN:** un usuario normal ve el botón "+" en Categorías y siempre recibe el aviso de permisos. Decidir producto: ocultar el botón a no administradores (la API no expone el rol en el token que use la app) o hacer las categorías por usuario en el backend (`src/`, fuera de mi alcance). |
| P-02 | Media | **`renewalDate` no avanza en el backend:** el formulario de edición muestra la fecha guardada (p. ej. "30 may 2026") y la vuelve a guardar. La app ya calcula la próxima fecha real para mostrar y avisar; arreglarlo en el backend (pendiente anotado en la auditoría de avisos). |
| P-03 | Baja | Formulario: con "Periodo de prueba" activado se puede guardar sin fecha de fin (la web la exige). No rompe nada (los avisos lo tratan como de pago), pero conviene validarlo; es un cambio de UX. |
| P-04 | Baja | Detalle/Catálogo/Categorías no reaccionan a `SesionExpirada` (pantalla vacía o spinner); en la práctica Inicio y Suscripciones ya fuerzan el login. |
| P-05 | Baja | Lint restante (74 avisos, 0 errores): 20 strings sin usar, `UseKtx`, versiones de dependencias (`GradleDependency`, `security-crypto` alpha — M-14 previa), `Typos` falsos ("Anual" es español), textos de ejemplo del preview del widget. Ninguno es un fallo. |
| P-06 | Baja | `compartirResumenAnual` genera el PNG en el hilo principal (posible tirón breve). |

## Notas

- `Funciones.GMAIL=false` se ha respetado: la detección por Gmail sigue pausada y no se ha tocado.
- No se ha cambiado `versionCode`/`versionName` ni se ha hecho commit. El APK release de prueba quedó en `mobile/androidApp/build/outputs/apk/release/` y se instaló sobre la 2.17.2 del emulador (misma firma, datos conservados).
- Compatibilidad de caché: `@JsonNames("descripcion"/"moneda")` permite leer las cachés guardadas por versiones anteriores tras actualizar.

## Ronda 2 (2026-10-10)

**Alcance:** decisiones de producto pendientes (P-01, P-03, P-04, P-06), adaptación a los cambios del backend de la ronda 1 (`renewalDate` proyectada, nuevos códigos 400/403/409/415, refresh tokens con hash) y revisión del diff de la ronda 1 buscando regresiones. Solo `mobile/`; sin commit ni cambio de versión; `Funciones.GMAIL=false` intacto.

| Verificación | Ronda 1 | Ronda 2 |
|---|---|---|
| `:androidApp:assembleDebug` | OK | OK |
| `:shared:testDebugUnitTest` | 164 tests, 0 fallos | **183 tests, 0 fallos** |
| `:androidApp:lintDebug` | 0 errores, 74 avisos | 0 errores, 74 avisos |
| `:androidApp:assembleRelease` (R8) | OK | OK (APK 7,3 MB) |
| `tools/i18n/check_strings.py` | OK | OK (323 entradas es/en, 336 fr/pt) |
| Emulador (API 36, APK release) | — | Categorías y formulario (prueba gratuita) comprobados |

### Hallazgos y cambios

| ID | Sev. | Estado | Hallazgo / cambio | Archivo |
|---|---|---|---|---|
| R2-01 | **Alta** | Arreglado | **El login nunca mostraba "credenciales incorrectas" ni "sin conexión":** el POST sin token (login y Google) no miraba el código HTTP; un 401/403/429 se leía como cuerpo vacío (`Respuesta vacía` → error genérico) y un corte de red no era `NetworkException`. Ahora pasa por `parseResponse` y captura la red igual que las peticiones autenticadas. | `network/ApiClient.kt` (`post`) |
| R2-02 | **Alta** | Arreglado | **Un 429 o un 5xx en el refresh cerraba la sesión.** El backend limita `/api/auth/refresh` a 10/min por IP (con CGNAT muchos móviles comparten IP) y Render devuelve 502/503 al reiniciar: cualquier respuesta no 2xx borraba los tokens. Ahora solo 400/401/403 son "rechazado"; el resto conserva la sesión y se informa como fallo de red. | `ApiClient.tryRefresh` |
| R2-03 | Media | Arreglado | **El logout no revocaba el refresh token:** se enviaba un cuerpo vacío y el backend (`LogoutRequest`, `@NotBlank refreshToken`) respondía 400; el token seguía válido 30 días. Ahora envía `{"refreshToken": ...}` sin cabecera Bearer. | `repository/AuthRepository.kt` |
| R2-04 | Media | Arreglado (P-01) | **Categorías solo para admin:** el "+" y el diálogo se ocultan si el usuario no es administrador, con una nota explicativa; el banner offline "no se pueden crear" solo sale a quien puede. El rol sale de `data.role` de login/refresh/Google (añadido al backend en esta ronda), guardado junto a los tokens (EncryptedSharedPreferences / Keychain) y renovado en cada refresco; si el backend no lo envía, se lee el claim `role` del access token (`JwtService.ROLE_CLAIM`), sin verificar firma: solo decide la UI, el servidor sigue autorizando. Por defecto oculto (sin rol → no admin). Se mantiene el mensaje claro si el servidor devuelve 403. | `model/AuthTokens.kt` (`role`), `storage/TokenStorage.{android,ios}.kt`, `network/JwtRol.kt` (`esAdmin`), `AuthRepository.esAdmin`, `CategoriasViewModel.puedeCrear`, `CategoriasScreen.kt`, `di/SharedModule.kt` |
| R2-05 | Media | Arreglado | **Códigos nuevos del backend en el login:** `ApiException` lleva el código de negocio del cuerpo (`{"error":{"code"}}` o `{"code"}` del filtro de límite). Cuenta bloqueada (403 `ACCOUNT_LOCKED`), email sin verificar (403 `EMAIL_NOT_VERIFIED`) y demasiados intentos (429) tienen mensaje propio en es/en/fr/pt en vez de "credenciales incorrectas". | `ApiClient.kt`, `AuthViewModel.kt`, `LoginScreen.kt`, `strings.xml` |
| R2-06 | Baja | Arreglado (P-03) | Prueba gratuita sin fecha de fin: nuevo `FormError.FechaFinPruebaVacia`; Guardar se desactiva y el campo muestra el error en cuanto aparece (también al editar una prueba antigua sin fecha). | `SuscripcionFormViewModel.kt`, `SuscripcionFormScreen.kt` |
| R2-07 | Baja | Arreglado (P-04) | Detalle, Catálogo y Categorías reaccionan a `SesionExpirada` como Inicio y Suscripciones (`cerrarSesion` → login). El detalle ya no se queda con un spinner infinito. | `SuscripcionDetalleScreen.kt`, `CatalogoScreen.kt`, `CategoriasScreen.kt`, `ui/SubIAApp.kt` |
| R2-08 | Baja | Arreglado | Formulario: nombre ≤ 100, descripción ≤ 500 y notas ≤ 1000 caracteres (límites nuevos de `SubscriptionRequestDto`); antes un texto largo acababa en 400 → "No se ha podido guardar". | `SuscripcionFormScreen.kt` |
| R2-10 | Baja | Arreglado | El importe 0 ya es válido en el formulario (la API acepta `@PositiveOrZero`); solo se rechazan negativos y textos no numéricos. Texto del error actualizado en los 4 idiomas. | `SuscripcionFormViewModel.kt`, `strings.xml` |
| R2-09 | Baja | Arreglado (P-06) | La imagen del resumen anual se pinta en `Dispatchers.Default` y se guarda en `IO`; el bitmap se recicla y un doble toque no abre dos selectores. | `util/ImageShare.kt`, `ResumenAnualScreen.kt` |

### `renewalDate` proyectada por el backend

- **Sin doble cálculo:** `proximaRenovacion`/`ocurrencias` devuelven la propia fecha si ya es ≥ hoy, así que proyectar una fecha ya proyectada es idempotente. Lista, detalle, Inicio (tira, meses) y `RenovacionWorker`/`avisosPendientes` siguen dando la misma fecha; las claves de avisos (`COBRO:id@fecha`) no cambian, así que no se repiten avisos.
- **Formulario:** al editar se carga la próxima renovación real (antes la fecha de alta, P-02 queda resuelto por el backend).
- **CSV:** exporta la próxima renovación, no la fecha de alta.

### Pendiente

| ID | Sev. | Motivo |
|---|---|---|
| R2-P1 | Media | **Backend — anclas del día 29-31 se pierden al editar.** La API ya no expone la fecha ancla, solo la proyectada: una mensual del día 31 vista en noviembre llega como `2026-11-30`; si el usuario edita (aunque no toque la fecha), el PUT guarda el 30 y desde entonces cobra los 30 (igual con 29 feb anual). Arreglo en `src/`: en el PUT, si `renewalDate` coincide con `RenewalDates.nextRenewal(guardada)`, conservar la guardada; o exponer `renewalAnchor` en `SubscriptionDto`. Mientras tanto, la app solo desplaza días dentro del mismo mes en la proyección a 12 meses (los totales por mes no cambian). |
| R2-P2 | Baja | **Backend — "hoy" del servidor.** `nextRenewal` usa el reloj del servidor; si su zona va por delante de la del móvil (usuario en América con servidor UTC), un cobro de "hoy" local llega ya avanzado y su aviso del mismo día se pierde. Conviene fijar la zona de `Clock` a Europe/Madrid o aceptar la fecha del cliente. |
| R2-P3 | Baja | **Backend — límite de `/api/auth/refresh` por IP** (10/min): con CGNAT puede devolver 429 a usuarios legítimos. La app ya no cierra sesión por ello (R2-02), pero la petición original falla como "sin conexión". Considerar limitar por refresh token/familia en vez de por IP. |
| R2-P4 | Baja | El cambio de `TokenStorage.ios.kt` (guardar el rol en el Keychain) no se ha compilado: sin Mac no hay target iOS en esta máquina. |
| R2-P5 | Baja | En el emulador la sesión iniciada es de administrador (el "+" se ve, como debe). El caso usuario normal está cubierto por tests (`Ronda2AuditoriaTest`), no probado a mano con otra cuenta. |
| P-05 | Baja | Sin cambios (74 avisos de lint, 0 errores). |

### Revisión del diff de la ronda 1

Se releyeron los 26 archivos modificados (`git diff -- mobile/`). Sin regresiones: destino inicial fijo de `MainActivity` (sin `checkSession`, el valor inicial ya es real), `SideEffect` del detalle, filtro de activas del Inicio, `gastoMensualEquivalente`, `@JsonNames` de caché, `NuevaCategoriaRequest` sin defaults, BOM/fórmulas del CSV, `onRelease` del banner y widget al cerrar sesión. Los fallos R2-01 y R2-03 venían de antes de la ronda 1 (no son regresiones).

## Ronda 3 (2026-10-10)

**Alcance:** adaptar la app al arreglo del backend para no perder el día 31 / 29-feb al editar (R3-01) y al desfase de zona horaria servidor/móvil (R3-02). Incluye en la verificación los últimos cambios de la ronda 2 (`data.role` guardado con los tokens, importe 0). Sin commit ni cambio de versión; `Funciones.GMAIL=false` intacto.

| ID | Sev. | Cambio | Archivo |
|---|---|---|---|
| R3-01 | **Alta** | `Subscription.fechaAncla` (`renewalAnchor`, opcional: backends antiguos no lo envían → se usa `renewalDate`). El formulario guarda el ancla, la fecha y el periodo mostrados al abrir la edición; si se guarda **sin tocar fecha ni periodo se reenvía el ancla** en vez de la próxima renovación (el 31 no pasa a 30; el 29-feb no pasa a 28). Si el usuario cambia la fecha o el periodo se envía lo que eligió. El backend tiene además su propia salvaguarda (ver informe backend). | `model/Subscription.kt`, `viewmodel/SuscripcionFormViewModel.kt` |
| R3-02 | Media | **Zona horaria:** `proximaRenovacion`/`ocurrencias` (lista, detalle, Inicio, widget y `avisosPendientes` del `RenovacionWorker`) proyectan desde el **ancla** con el "hoy" del **móvil**. Antes partían de `renewalDate` ya proyectada con el "hoy" de Madrid: un usuario en América, de noche, recibía el cobro de hoy avanzado un ciclo y perdía el aviso; y desde una fecha recortada (30-nov) salía 30-dic en vez de 31-dic. El formulario también carga la próxima renovación local (antes la del servidor). | `model/ProyeccionCobros.kt`, `SuscripcionFormViewModel.kt` (`hoy` inyectable) |
| — | — | Revisión de `TokenStorage.ios.kt` (cambio de la ronda 2, no compilable sin Mac): coherente con Android — misma semántica de `role` (se guarda si llega, se borra si no), `clearTokens` lo incluye, `getTokens` lo lee, claves distintas por plataforma como antes; el `return null` dentro de `memScoped` es válido (inline). | — |

### Verificación

| Verificación | Ronda 2 | Ronda 3 |
|---|---|---|
| `:androidApp:assembleDebug :shared:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleRelease` | OK | **BUILD SUCCESSFUL in 1m 36s** (186 tareas) |
| `:shared:testDebugUnitTest` | 181 tests | **192 tests, 0 fallos, 0 errores** |
| `:androidApp:lintDebug` | 0 errores, 74 avisos | 0 errores, 74 avisos |
| `:androidApp:assembleRelease` (R8) | OK | OK (APK 7,3 MB) |
| `tools/i18n/check_strings.py` | OK | OK (323 es/en, 336 fr/pt; sin strings nuevos) |

Tests nuevos (`Ronda3AuditoriaTest`): deserialización con y sin `renewalAnchor`; proyección desde el ancla (31-dic, 29-feb bisiesto); servidor un día por delante → el cobro de hoy local sigue saliendo y se avisa (`diasRestantes = 0`); sin ancla se comporta como antes; formulario: sin tocar la fecha reenvía el ancla, con fecha cambiada envía la nueva, con periodo cambiado envía la fecha mostrada, semanal con servidor por delante carga la fecha local y reenvía el ancla.

### Pendiente

| ID | Sev. | Motivo |
|---|---|---|
| R3-P1 | Baja | Con un backend sin `renewalAnchor` (anterior a esta ronda) el desfase de zona sigue igual que antes; se resuelve al desplegar el backend de la ronda 3 junto con la app. |
| R3-P2 | Baja | El CSV exporta `renewalDate` del servidor (próxima renovación con el "hoy" de Madrid); puede diferir un ciclo de lo que ve el usuario en el borde del día. Cosmético. |
| R2-P5, P-05 | Baja | Sin cambios. |
