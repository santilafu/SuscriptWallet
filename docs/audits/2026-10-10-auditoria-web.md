# Auditoría web Thymeleaf — 2026-10-10

**Alcance:** `src/main/resources/templates/**` (18 plantillas), `static/` (solo favicons) y `messages*.properties` (es/base, en, fr).
**Modo:** corrección. Solo se tocaron plantillas, `static/` y `messages*.properties`; nada de Kotlin, `mobile/` ni Gradle. Sin commits.
**Punto de partida:** `docs/audits/2026-10-07-auditoria-completa.md` (no se repiten hallazgos ya arreglados en los sprints 1-2).

## Resumen

- **Arreglados:** 23 hallazgos (3 altos, 9 medios, 11 bajos).
- **Pendientes fuera de alcance (Kotlin/config):** 5, entre ellos el CSP que bloquea el botón de Google Sign-In (A-05 sigue abierto).
- **Patrones peligrosos conocidos:** 0. No queda Alpine (`x-data`, `@click`, `:attr`), ningún `#{}` dentro de `${}`, ningún `[/*` en JS inline, ningún `.contains()` de SpEL (solo `#lists.contains`) y todos los formularios POST llevan el token `_csrf` (13 de 13). La detección por Gmail sigue pausada (`th:if="false"` en `settings.html`, sin tocar).

## Hallazgos arreglados

Las líneas son las del archivo **antes** del cambio.

| ID | Sev. | Archivo:línea | Problema | Arreglo |
|---|---|---|---|---|
| W-01 | Alta | `subscriptions/list.html:197` + `layout.html` | El modal de borrado es `class="… flex …" hidden`. En el preflight de Tailwind, `[hidden]` y `.flex` tienen la misma especificidad y `.flex` va después, así que gana. El resultado es un overlay `fixed inset-0 z-50` con opacidad 0 que puede tapar toda la página y comerse los clics. | Regla global `[hidden]{display:none !important}` en el layout. El modal ahora tiene `role="dialog"`, `aria-modal` y `aria-labelledby`, pone el foco en «Cancelar» al abrirse y lo devuelve al botón que lo abrió al cerrarse. |
| W-02 | Alta | `subscriptions/form.html:100-305` | Asistente de 3 pasos con campos `required` en pasos ocultos (`display:none`). Si se llegaba al paso 3 con el nombre o el precio vacíos, «Guardar» no hacía **nada**: el navegador no puede enfocar un campo oculto y cancela el envío sin avisar. | «Siguiente» valida el paso actual (`reportValidity`). Al pulsar «Guardar» (o Intro), si hay un campo inválido en otro paso, salta a ese paso y muestra el aviso. |
| W-03 | Alta | `subscriptions/form.html:266-281` | Con «prueba gratuita» marcada y sin fecha de fin, el backend responde **400** (`trialEndsAt is required`) y sale una página de error. | `trialEndsAt` pasa a ser `required` cuando la casilla está marcada (también en el render de edición con `th:required`). |
| W-04 | Media | `auth/login.html:138-143` | `AuthWebController` redirige a `/login?error=googleFailed`, pero la plantilla miraba `param.googleFailed`, que nunca llega. Un fallo de Google mostraba «Correo o contraseña incorrectos». | El aviso de credenciales sale cuando `error != 'googleFailed'` y el de Google cuando `error == 'googleFailed'`. |
| W-05 | Media | `auth/register.html:285` | El backend ya valida `confirmPassword` y el formato del email (M-10) y redirige con `error=passwordMismatch|invalidEmail`, pero la plantilla solo pintaba `weakPassword`: el usuario volvía al formulario sin saber por qué. | Avisos nuevos con las claves `register.error.mismatch` y `register.error.email`. Validación en el cliente: `minlength=10`, `maxlength=72` (límite de BCrypt), `maxlength=254` en el email y comprobación de que las contraseñas coinciden con `setCustomValidity`. |
| W-06 | Media | `auth/reset-password.html:42-50` | Los enlaces de idioma eran `href="?lang=xx"` y perdían el `token`, que es `@RequestParam` obligatorio. Al cambiar de idioma salía un **400**. | `th:href="@{/reset-password(token=${token},lang='xx')}"`. Se añade la misma validación en el cliente que en el registro. |
| W-07 | Media | `dashboard.html:322-327` | La gráfica «Top 5 gastos **mensuales**» pintaba `s.price` en bruto, aunque el servicio ordena por el equivalente mensual. Una suscripción anual de 120 € aparecía como 120 €/mes. | Conversión en JS igual que `DashboardService.toMonthly` (anual / 12, semanal × 4,33). |
| W-08 | Media | `catalog-browser.html:40-96` | Las categorías del catálogo `creatividad` (17 servicios) y `citas` (16) no tenían chip, así que no se podían filtrar. | Chips nuevos con las claves `catalog.chip.creatividad` y `catalog.chip.citas` en los 3 idiomas. |
| W-09 | Media | `layout.html:351-431` | La barra de navegación no era responsive: 5 enlaces con texto, idioma, «eliminar cuenta» y «cerrar sesión» en una sola fila desbordaban en móvil y provocaban scroll horizontal de toda la página. | Por debajo de `lg` solo se muestran iconos, con `aria-label` y `title`. El logo oculta el texto por debajo de `sm`, la fila tiene `min-w-0 overflow-x-auto` y «cerrar sesión» muestra el texto solo desde `sm`. |
| W-10 | Media | `subscriptions/form.html:174` | El prefijo del precio era siempre «€», aunque se eligiera USD, GBP… | Muestra el símbolo de la moneda elegida (`data-symbol`) y se actualiza al cambiarla o al aplicar un servicio del catálogo. |
| W-11 | Media | `subscriptions/form.html:121-248` | Sin `maxlength`: un nombre de más de 100, una descripción de más de 500 o unas notas de más de 1000 caracteres (límites de V1) acaban en un error 500 de la BD. | `maxlength` 100/500/1000 y `max="99999999.99"` (DECIMAL(10,2)) en el precio. En categorías, `maxlength="100"`. |
| W-12 | Media | `auth/delete-account.html:63` | `th:utext` con el email del usuario (B-06, self-XSS). | `th:text`: el mensaje no lleva HTML, así que no se pierde nada. |
| W-13 | Media | (nuevo) `templates/error.html` | No había plantilla de error: los 403/404/500 mostraban la «Whitelabel Error Page» en inglés (por ejemplo, `/verify-email-error`, que no tiene controlador). | Página de error propia, autónoma (sin layout, para no depender de `_csrf` ni de la sesión) e i18n: `error.title.404|403|generic`, `error.text`, `error.back`. |
| W-14 | Baja | `subscriptions/list.html:29,172,189`, `dashboard.html:72,211` | Textos fijos en castellano en páginas traducidas: el aviso de importados, las pistas de los estados vacíos, «Sin categoría» y «(vence …)». | Claves nuevas `sub.imported`, `sub.empty.*.hint`, `common.no-category` y `dashboard.alert.trial-ends`. Se usa `th:with="noCat=#{…}"` para no meter `#{}` dentro de `${}`. |
| W-15 | Baja | `categories/list.html:67` | `confirm('` + mensaje + `')` concatenado dentro de `onsubmit`: un apóstrofo en una traducción rompía el handler de JS. | El texto va en `th:data-confirm` y se usa `onsubmit="return confirm(this.getAttribute('data-confirm'))"`. |
| W-16 | Baja | `categories/list.html:69` | El botón de borrar solo tenía un icono, sin nombre accesible. | `aria-label` «Eliminar categoría + nombre» y `title`. |
| W-17 | Baja | `subscriptions/form.html`, `categories/form.html` | Etiquetas sin asociar a su campo (sin `for`/`id`). | `for` e `id` en todos los campos. «Estado» pasa de `<label>` huérfano a `<p>`. `aria-describedby` en la pista del icono y en los requisitos de contraseña. |
| W-18 | Baja | `subscriptions/form.html:464` | `document.querySelector('form')` devolvía el formulario de **logout** del navbar, así que «Aplicar» del catálogo hacía scroll al principio. | Se usa `#subForm`. |
| W-19 | Baja | Varias (`#3d4d66`, `#4a5a78`, `text-slate-600`, `text-gray-500`) | Contraste insuficiente (≈2,2–3,6:1) en subtítulos, pistas, pie de página, selector de idioma, pasos inactivos del asistente y ticks de la gráfica. | Se usa `#8291a8` (≈5,5:1 sobre las tarjetas) y `slate-400`/`gray-400`. |
| W-20 | Baja | `legal/privacidad.html:11` | Enlazaba `/favicon.svg`, que no existe desde que se revirtió el favicon (404). | Línea eliminada. |
| W-21 | Baja | `auth/login.html:187` | GSI `data-width="100%"` no es válido (solo acepta píxeles entre 200 y 400): aviso en consola y ancho por defecto. | `data-width="320"`, centrado. |
| W-22 | Baja | `layout.html:71` | Chart.js cargado como `chart.js@4`, una versión flotante: un 4.x nuevo podía cambiar el comportamiento sin aviso. | Fijado a `chart.js@4.4.7`. |
| W-23 | Baja | `settings.html:111`, `messages*.properties:17` | «Versión web 3.1.0» cuando el build es 3.2.0 (M-19) y el pie decía «precios actualizados a marzo 2026» (se revisaron en octubre). | Versión 3.2.0 y pie «octubre 2026» en los 3 idiomas. |

Mejoras menores de accesibilidad que van con lo anterior: `aria-pressed` en los chips y pastillas de filtro (lista y catálogo), `type="search"` con `aria-label` en los buscadores, `role="img"` con `aria-label` en los canvas de Chart.js, `aria-hidden` en los iconos decorativos de Bootstrap Icons y `role="group"`, `lang` y `aria-current` en el selector de idioma.

## Pendientes (fuera del alcance de este agente)

| ID | Sev. | Dónde | Problema | Por qué sigue abierto / qué hacer |
|---|---|---|---|---|
| P-01 | **Alta** | `config/SecurityConfig.kt` (CSP del chain web) | Sigue con `frame-src 'none'`, `connect-src 'self'` y `style-src` sin `https://accounts.google.com`. El botón de Google Identity Services es un iframe de `accounts.google.com` y carga `accounts.google.com/gsi/style`, así que el CSP lo bloquea (A-05 del 07-10 sin arreglar). | Es Kotlin. Añadir `frame-src https://accounts.google.com`, `connect-src 'self' https://accounts.google.com` y `style-src … https://accounts.google.com/gsi/style`. Confirmarlo en DevTools (consola: «Refused to frame»). |
| P-02 | Media | `controller/AuthWebController.kt:88` | `redirect:/verify-email-error` no tiene `@GetMapping` ni plantilla. Con este cambio cae en el nuevo `error.html` (404), que se ve bien pero no explica que el enlace caducó. | Es Kotlin. Añadir `@GetMapping("/verify-email-error")` con una vista propia, o redirigir a `/login?error=verification`. |
| P-03 | Baja | `SecurityConfig.kt:80-86` | `/favicon.ico`, `/favicon-*.png` y `/apple-touch-icon.png` no están en `permitAll`. En las páginas públicas (login, registro…) el favicon se redirige a `/login` y no se ve. | Es Kotlin. Añadirlos a `permitAll` (o `PathRequest.toStaticResources().atCommonLocations()`). |
| P-04 | Baja | `controller/CategoryController.kt:105` | El `errorMessage` flash al borrar una categoría en uso es el `message` en castellano de la excepción: no se traduce. | Es Kotlin. Pasar un código (`errorCode=inUse`) y resolverlo con `#{cat.error.in-use}` en la plantilla. |
| P-05 | Media (deuda) | Todas las plantillas | M-15 sigue abierto: Tailwind por CDN JIT en producción y `script-src 'unsafe-inline'` obligado por los `onclick`/`onerror` inline. | Es una tarea de build (Tailwind CLI y CSS compilado) más un nonce en `SecurityConfig`. Mientras exista `'unsafe-inline'`, los handlers inline funcionan, así que no rompe nada. |

Otras notas:

- `gmail-results.html` y el bloque de Gmail de `settings.html` siguen sin i18n (B-06). No se tocaron porque la función está **pausada a propósito**.
- `legal/privacidad.html` solo está en castellano. Es un texto legal y se dejó así a propósito.
- `settings.html` tiene la versión y el «Spring Boot 3.3.5» escritos a mano. Si el backend sube a Spring Boot 3.5 (pendiente del 07-10), hay que actualizarlo; lo ideal es `springBoot { buildInfo() }` y leer `BuildProperties`.

## Verificación realizada (sin Gradle)

1. **Claves i18n:** script que cruza cada `#{clave}` de las 19 plantillas con los 4 `messages*.properties`. Resultado: **0 claves que falten** en algún idioma y 0 duplicadas. Los 4 ficheros tienen 284 líneas alineadas y la única clave dinámica (`sub.billing.*`) existe en todos. Ningún mensaje con argumentos `{0}` lleva un apóstrofo sin escapar, que `MessageFormat` se comería.
2. **Patrones peligrosos (grep):** Alpine, `#{}` dentro de `${}`, `[/*`, `.contains(` en SpEL, `th:utext` y formularios POST sin `_csrf`. Resultado: 0, salvo el falso positivo `#lists.contains` (que es el uso correcto).
3. **Atributos de modelo:** cruzados a mano con `SubscriptionController`, `CategoryController`, `DashboardController` (`DashboardDto`), `CatalogBrowserController`, `AuthWebController`, `DeleteAccountController`, `GmailController` y `CurrentPathInterceptor` (`currentPath`). Todos los `${…}` existen. Las rutas de los `th:href` y `th:action` existen en los controladores.
4. **Estructura HTML:** parser que comprueba el balance de etiquetas y los `id` duplicados en todas las plantillas: 0 errores.
5. **JS:** los 11 bloques `<script>` inline se extrajeron y se pasaron por `node --check` (Node 24): todos válidos. Las plantillas naturales con `th:inline="javascript"` son JS válido con sus valores por defecto.
6. **CSP:** se revisaron los orígenes externos de las plantillas frente a la política del chain web: `cdn.tailwindcss.com`, `cdn.jsdelivr.net` (Chart.js y Bootstrap Icons), Google Fonts e `icon.horse` (todos los logos del catálogo usan `domain`, no `iconUrl`). Todo está permitido salvo Google Identity Services (P-01).

**No verificado:** no se renderizaron las páginas con Spring en marcha, porque Gradle lo estaba usando el agente de backend. Conviene una pasada manual: `/login` (botón de Google y P-01), `/subscriptions` (abrir y cerrar el modal de borrado, filtrar), `/subscriptions/new` (asistente con campos vacíos, cambio de moneda, prueba gratuita sin fecha), `/dashboard` (top 5 con una suscripción anual), `/catalog-browser` (chips nuevos) y un 404 cualquiera (nuevo `error.html`), en móvil a 375 px.
