# Revisión de precios del catálogo: desarrollo, cloud y seguridad

- Fecha de revisión: 2026-11-15. Todas las fuentes se consultaron ese día.
- Fichero revisado: `src/main/kotlin/com/subia/service/CatalogService.kt` (líneas 176-194, 228-248 y 338-363). No se ha tocado código.
- Criterio: precio final para un usuario en España (EUR, IVA incluido). Si el servicio solo cobra en USD, se indica `USD` y la cifra que muestra su web. Casi todas las herramientas de desarrollo cobran en USD y **sin IVA**: a un particular en España se le suma el 21 % al facturar.
- "Sin verificar": no encontré fuente fiable o la web oficial no deja leer la cifra. En esos casos se mantiene el precio actual.
- Límite encontrado: a mitad de la revisión se agotó el cupo de búsquedas web compartido. El resto se verificó solo con las páginas oficiales de precios. Las que bloquean lecturas automáticas (403/404 o cifras cargadas por JavaScript) quedan sin verificar: NordVPN, NordPass, Norton, Dashlane, Keeper, Malwarebytes, PIA, AdGuard, Tuta, JetBrains, Pingdom y Tresorit.
- Logos: se comprobó cada `domain` con `https://icon.horse/icon/<domain>` y con `https://www.google.com/s2/favicons?domain=<domain>&sz=128` (siguiendo la redirección). **Todos devuelven 200 con imagen PNG o JPEG**. Las dudas que quedan son de dominio canónico o de icono genérico (ver la columna correspondiente y la sección final).

## Tabla

| Línea | Nombre actual | Categoría | Precio actual | Ciclo | Precio verificado | Precio anual verificado | ¿Cambia? | Fuente (URL) | Confianza | Domain OK / logo | Notas |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 176 | Google One 2 TB | cloud | 9,99 € (anual 99,99) | MONTHLY | 9,99 €/mes | 99,99 €/año (fuente secundaria; la oficial solo dice "ahorra hasta un 16 %") | Sí (nombre) | https://one.google.com/about/plans?gl=ES&hl=es ; https://blog.internxt.com/es/precios-de-google-one/ | alta (mes) / media (año) | google.com OK (icon.horse 200, 1081 B). Es la "G" genérica de Google, no el icono de Google One (one.google.com: 200, 668 B) | El plan de 2 TB se llama ahora **"Google AI Plus (2 TB)"** en España |
| 177 | Google One 5 TB | cloud | 24,99 € | MONTHLY | **21,99 €/mes** | 219,99 €/año (secundaria) | Sí | https://one.google.com/about/plans?gl=ES&hl=es ; blog.internxt.com | alta (mes) / media (año) | igual que la fila 176 | Ya no hay un "5 TB" de almacenamiento puro: es **Google AI Pro (5 TB)**. Puede duplicar la fila de Google AI Pro de la categoría `ia` |
| 178 | iCloud+ 200 GB | cloud | 2,99 € | MONTHLY | 2,99 €/mes | — (no hay anual) | No | https://support.apple.com/es-es/108047 | alta | icloud.com OK (200, 3333 B) | Precios con impuestos, página publicada el 29-09-2026 |
| 179 | iCloud+ 2 TB | cloud | 9,99 € | MONTHLY | 9,99 €/mes | — | No | https://support.apple.com/es-es/108047 | alta | icloud.com OK | Faltan los planes de 50 GB, 6 TB y 12 TB |
| 180 | Dropbox Plus | cloud | 11,99 € (anual 119,88) | MONTHLY | 11,99 €/mes | 119,88 €/año (9,99 €/mes facturado anualmente) | No | https://www.dropbox.com/es_ES/plans?billing=monthly ; https://www.dropbox.com/es_ES/plans | alta | dropbox.com OK (200, 1214 B) | — |
| 181 | OneDrive 100 GB | cloud | 2,00 € | MONTHLY | 2,00 €/mes | **20,00 €/año** | Sí (añadir anual) | https://www.microsoft.com/es-es/microsoft-365/basic | alta | microsoft.com: 200, pero solo 485 B y es el logo de Microsoft, no el de OneDrive. onedrive.com da 498 B, también pequeño | Nombre oficial: **Microsoft 365 Básico** |
| 182 | Backblaze Personal | cloud | 9,00 EUR | MONTHLY | **9 USD/mes** | 99 USD/año (2 años: 189 USD) | Sí (moneda) | https://www.backblaze.com/cloud-backup/pricing ; https://frontdeskreview.com/software/cloud-storage/backblaze/ | media | backblaze.com OK (200, 3447 B) | Cobra en USD. La web oficial solo muestra el anual (99 $). Los 9 $/mes vienen de terceros |
| 183 | pCloud Premium 2 TB | cloud | 9,99 € | MONTHLY | 9,99/mes | **99,99/año** | Sí (añadir anual y nombre) | https://pcloud.com/help/general-help-center/how-much-does-pcloud-cost ; https://www.pcloud.com/es/cloud-storage-pricing-plans.html | media | pcloud.com OK (200, 3828 B) | El plan se llama **Premium Plus 2 TB**. La ayuda oficial da las cifras en USD; en EUR el importe nominal sería el mismo (no confirmado). De por vida: 379 € |
| 184 | DigitalOcean | cloud | 5,00 EUR | MONTHLY | **desde 4 USD/mes** (Droplet básico) | — | Sí | https://www.digitalocean.com/pricing/droplets | alta | digitalocean.com OK (200, 4865 B) | USD + impuestos. Pago por uso: conviene `variablePrice = true`. Hay otro escalón de 6 $ |
| 185 | Cloudflare Pro | cloud | 20,00 EUR | MONTHLY | **25 USD/mes** (facturación mensual) | 240 USD/año (20 $/mes) | Sí | https://toolradar.com/tools/cloudflare/pricing (dice verificado en 11-2026 contra la web oficial); https://www.cloudflare.com/plans/ (no muestra cifras) | media | cloudflare.com OK (200, 1457 B) | Precio por dominio. Los 20 $ son el equivalente mensual del plan anual |
| 186 | Render Starter | cloud | 7,00 EUR | MONTHLY | **7 USD/mes** (instancia Starter) | — | Sí (moneda) | https://temps.sh/blog/render-pricing-2026 ; https://render.com/pricing (cifras no legibles) | media | render.com OK en icon.horse (2900 B). El favicon de Google pesa 257 B | En 2026 Render cambió las cuotas de workspace (ver "Planes que faltan") |
| 187 | Supabase Pro | cloud | 25,00 EUR | MONTHLY | **25 USD/mes** (desde) | — | Sí (moneda) | https://supabase.com/pricing | alta | supabase.com OK (200, 12549 B) | Más uso. Solo hay facturación mensual |
| 188 | MongoDB Atlas | cloud | 57,00 EUR | MONTHLY | **desde 56,94 USD/mes** (M10 dedicado) | — | Sí | https://www.mongodb.com/pricing | alta | mongodb.com OK (200, 2958 B) | USD y por uso: `variablePrice = true`. Hay un nivel Flex de 8 a 30 $/mes que encaja mejor con un particular |
| 189 | Algolia Search | cloud | 10,00 EUR | MONTHLY | **sin cuota fija** (Grow desde 0 $, pago por uso) | — | Sí | https://www.algolia.com/pricing | alta | algolia.com OK (200, 7971 B) | Los 10 € no corresponden a ningún plan. Marcar `variablePrice = true` o quitar la fila |
| 190 | Mega Pro Lite | cloud | 4,99 € | MONTHLY | 4,99 €/mes | sin verificar | Sí (descripción) | https://subger.com/en/service/mega ; https://www.capterra.com/p/232384/MEGA/pricing/ (mega.io no se deja leer) | media-baja | mega.io OK (200, 35772 B) | **Pro Lite son 400 GB, no 2 TB** (la descripción está mal). Pro I 2 TB: 9,99 €/mes. Algún agregador muestra una estructura nueva (p. ej. "Essential" 2 TB), así que conviene revisarlo a mano |
| 191 | Box Personal Pro | cloud | 10,00 EUR | MONTHLY | ~12 €/mes (mensual) | ~10 $/mes facturado anualmente (14 $ en mensual) | Sí (baja confianza) | https://blog.internxt.com/box-pricing/ ; https://www.box.com/es-es/pricing/individual (sin cifras) | baja | box.com OK (200, 4872 B) | Personal Pro = 100 GB. La web oficial no deja leer el importe |
| 192 | Sync.com Solo Basic | cloud | 8,00 EUR | MONTHLY | **el plan ya no existe**. Equivalente: Personal 1 TB, 16 USD/mes (precio normal) | 144 USD/año (12 $/mes) | Sí | https://www.sync.com/pricing-individual/ | media | sync.com OK (200, 21139 B) | Hay promoción del 25 % el primer año (12 $/mes o 108 $/año). Pro Solo 5 TB: 32 $/mes o 336 $/año |
| 193 | Tresorit Personal | cloud | 10,00 EUR | MONTHLY | sin verificar (~13,99 $/mes según CostBench) | sin verificar (~143,88 $/año) | Sin verificar | https://tresorit.com/pricing/personal (sin cifras); https://www.costbench.com/software/document-management/tresorit/ | baja | tresorit.com OK (200, 12257 B) | Los planes son ahora Personal Lite (50 GB), Personal Essential (1 TB) y Personal Pro (4 TB) |
| 194 | Icedrive Pro | cloud | 4,99 € | MONTHLY | **7,99 €/mes** (precio normal, Pro 2 TB) | 95,88 €/año normal; 67,12 € el primer año con un 30 % de descuento | Sí | https://icedrive.net/plans | media | icedrive.net OK (200, 38052 B) | No queda claro si los 7,99 € son de facturación mensual o el equivalente anual |
| 228 | NordVPN | seguridad | 15,99 € | MONTHLY | sin verificar (terceros: Basic 1 mes entre 11,59 y 14,99 €) | — | Sin verificar | https://nordvpn.com/es-mx/pricing/ (oficial MX: Básico 14,99 USD/mes); nordvpn.com/es da 403 | baja | nordvpn.com OK (200, 2302 B) | Planes actuales: Basic, Plus, Complete/Prime. Es probable que 15,99 € esté inflado |
| 229 | ExpressVPN | seguridad | 12,95 € | MONTHLY | sin verificar (Basic 1 mes entre 12,99 y 15,99 USD según terceros) | — | Sin verificar | https://vpnoverview.com/best-vpn/expressvpn-cost-2026/ ; https://www.costbench.com/software/vpn/expressvpn/ | baja | expressvpn.com OK (200, 4556 B) | Ahora hay tres planes: Basic, Advanced y Pro |
| 230 | Mullvad VPN | seguridad | 5,00 € | MONTHLY | 5 €/mes (IVA incluido) | 60 €/año (misma tarifa) | No | https://mullvad.net/en/pricing | alta | mullvad.net OK (200, 11420 B) | 10 % de descuento si se paga con criptomoneda |
| 231 | ProtonVPN Plus | seguridad | 9,99 € | MONTHLY | 9,99/mes | **83,88/año** (renovación; primer año 35,88) | Sí (añadir anual) | https://protonvpn.com/pricing | media | proton.me OK (200, 19178 B) | La página se leyó en USD. Proton muestra la misma cifra en EUR para la UE (no comprobado en esta sesión) |
| 232 | 1Password | seguridad | 5,31 € (anual 53,00) | MONTHLY | sin verificar el mensual | 43,80 €/año sin IVA desde el 27-03-2026, que con 21 % de IVA son **53,00 €** | No | https://itdaily.com/news/business/1password-increases-subscription-prices/ ; https://1password.com/es/pricing/password-manager (USD: 3,99 $/mes anual, promoción de 2,99 $) | media | 1password.com OK (200, 6037 B) | El anual del catálogo ya refleja la subida con IVA |
| 233 | Bitwarden Premium | seguridad | 10,00 EUR | YEARLY | — | **19,80 USD/año** (+ impuestos) | Sí | https://bitwarden.com/pricing/ | alta | bitwarden.com OK (200, 5947 B) | Cobra en USD (1,65 $/mes). Families: 47,88 $/año |
| 234 | Dashlane Premium | seguridad | 4,99 € | MONTHLY | sin verificar | sin verificar | Sin verificar | https://www.dashlane.com/es/pricing-personal (los importes salen como "--") | baja | dashlane.com OK (200, 1594 B) | — |
| 235 | Malwarebytes Premium | seguridad | 3,49 € | MONTHLY | sin verificar | sin verificar | Sin verificar (el ciclo probablemente está mal) | https://www.malwarebytes.com/es/pricing | baja | malwarebytes.com OK (200, 5420 B) | La web oficial muestra el precio "/año": parece que solo hay plan anual |
| 236 | ProtonMail Plus | seguridad | 3,99 € | MONTHLY | **4,99/mes** | **47,88/año** (renovación; primer año 29,88) | Sí | https://proton.me/mail/pricing | alta | proton.me OK | 3,99 € era el equivalente mensual del plan anual antiguo |
| 237 | Surfshark Starter | seguridad | 2,49 € | MONTHLY | 2,49 €/mes es **precio promocional del plan de 24 meses** (67,23 € por 27 meses, más IVA). El precio de 1 mes no está verificado | — | Sí (ciclo o nota) | https://surfshark.com/es/pricing | media | surfshark.com OK (200, 2604 B) | Como precio mensual engaña. Planes: Starter, One y One+ (3,99 €/mes a 24 meses) |
| 238 | Keeper Security | seguridad | 3,99 € | MONTHLY | sin verificar | sin verificar | Sin verificar | https://www.keepersecurity.com/es_ES/pricing/personal-and-family.html (importes "XXX") | baja | keepersecurity.com OK (200, 29390 B) | — |
| 239 | Fastmail | seguridad | 5,00 € | MONTHLY | **6 €/mes** (Individual, IVA incluido) | **60 €/año** | Sí | https://www.fastmail.com/pricing/ | alta | fastmail.com OK (200, 9160 B) | Duo: 10 €/mes o 96 €/año. Family: 14 €/mes o 132 €/año |
| 240 | Tutanota Premium | seguridad | 3,00 € | MONTHLY | sin verificar | sin verificar | Sí (nombre y domain) | https://tuta.com/pricing ; https://tuta.com/es/pricing (cifras no legibles) | baja (precio) / alta (nombre) | tutanota.com da el mismo icono que **tuta.com** (200, 5563 B). Domain canónico: tuta.com | **"Premium" ya no existe**: planes Free, Revolutionary y Legend. La marca es ahora "Tuta" |
| 241 | ProtonDrive Plus | seguridad | 3,99 € | MONTHLY | **4,99/mes** | **47,88/año** (renovación; primer año 29,88) | Sí | https://proton.me/drive/pricing | alta | proton.me OK | 200 GB |
| 242 | NordPass Premium | seguridad | 2,99 € | MONTHLY | sin verificar | sin verificar | Sin verificar | https://nordpass.com/es/pricing/ (403) | baja | nordpass.com OK (200, 2265 B) | Lo normal es que se venda en planes de 1 o 2 años |
| 243 | Norton 360 Standard | seguridad | 4,99 € | MONTHLY | sin verificar | sin verificar | Sin verificar | https://es.norton.com/products/norton-360-standard (403) | baja | norton.com OK (200, 21260 B) | En España Norton suele venderse por años: revisar el ciclo |
| 244 | CyberGhost VPN | seguridad | 12,99 € | MONTHLY | **11,99 €/mes** (plan de 1 mes) | 2 años a 2,19 €/mes (61,32 €) | Sí | https://www.cyberghostvpn.com/es_ES/buy/cyberghost-vpn-4 | media | cyberghostvpn.com OK (200, 7259 B) | El resumen del pedido suma un 21 % de IVA. Si los 11,99 € no lo incluyen, el total sería unos 14,51 €. 6 meses: 41,94 € |
| 245 | Private Internet Access | seguridad | 11,95 € | MONTHLY | sin verificar | sin verificar | Sin verificar | https://www.privateinternetaccess.com/pricing (404) | baja | privateinternetaccess.com: 200, 975 B (pequeño, revisar) | — |
| 246 | AdGuard Premium | seguridad | 2,49 € | MONTHLY | sin verificar | sin verificar | Sí (ciclo) | https://adguard.com/en/license.html | media (ciclo) / baja (precio) | adguard.com OK (200, 6943 B) | **No hay plan mensual**: solo 1 año o de por vida, y cobra en dólares. Personal: 3 dispositivos. Family: 9 |
| 247 | Proton Pass Plus | seguridad | 3,99 € | MONTHLY | **4,99/mes** (cifra junto al plan) | **35,88/año** (renovación; primer año 23,88) | Sí | https://proton.me/pass/pricing | media-alta | proton.me OK | — |
| 248 | RoboForm Premium | seguridad | 1,99 € | MONTHLY | solo anual | **29,88 USD/año** (2,49 $/mes) | Sí (ciclo y precio) | https://www.roboform.com/pricing-personal | alta | roboform.com OK en icon.horse (9025 B). El favicon de Google pesa 319 B | No hay facturación mensual. Family: 47,75 $/año |
| 338 | GitHub Pro | desarrollo | 4,00 EUR | MONTHLY | 4 USD/mes | 48 USD/año | Sí (moneda) | https://github.com/pricing (no muestra Pro) | media | github.com OK (200, 7723 B) | No pude confirmar Pro en la página oficial en esta sesión |
| 339 | GitHub Team | desarrollo | 4,00 EUR | MONTHLY | **4 USD/usuario/mes** | — | Sí (moneda) | https://github.com/pricing | alta | github.com OK | Enterprise: 21 $/usuario/mes |
| 340 | GitLab Premium | desarrollo | 29,00 EUR | MONTHLY | — (solo anual) | **348 USD/usuario/año** (29 $/mes) | Sí (moneda y ciclo) | https://about.gitlab.com/pricing/ | alta | gitlab.com OK (200, 5298 B) | Solo se factura anualmente |
| 341 | Vercel Pro | desarrollo | 20,00 EUR | MONTHLY | **20 USD/mes** por asiento de desarrollador (+ IVA) | — | Sí (moneda) | https://vercel.com/pricing | alta | vercel.com OK (200, 1530 B) | Incluye 20 $ de crédito |
| 342 | Netlify Pro | desarrollo | 19,00 EUR | MONTHLY | **20 USD/mes** (miembros ilimitados) | — | Sí | https://www.netlify.com/pricing/ | alta | netlify.com OK (200, 1907 B) | Hay un plan nuevo, Personal, de 9 $/mes |
| 343 | Railway Hobby | desarrollo | 5,00 EUR | MONTHLY | **5 USD/mes** (incluye 5 $ de uso) | — | Sí (moneda y domain) | https://railway.com/pricing | alta | railway.app da el mismo icono que **railway.com** (200, 8607 B). Domain canónico: railway.com | Pro: 20 $/mes por workspace |
| 344 | Sentry Team | desarrollo | 26,00 EUR | MONTHLY | **29 USD/mes** | **312 USD/año** (26 $/mes) | Sí | https://sentry.io/pricing/ | alta | sentry.io OK (200, 4037 B) | Los 26 eran el precio anual tomado como mensual |
| 345 | Linear | desarrollo | 8,00 EUR | MONTHLY | sin verificar el mensual | **10 USD/usuario/mes facturado anualmente (120 $/año)** | Sí | https://linear.app/pricing | media | linear.app OK (200, 5791 B) | Plan **Basic**. Business: 16 $/usuario/mes anual |
| 346 | JetBrains All Products | desarrollo | 28,90 EUR | MONTHLY | sin verificar | sin verificar | Sin verificar | https://www.jetbrains.com/store/ (las cifras se cargan con JavaScript) | baja | jetbrains.com OK (200, 19881 B) | — |
| 347 | Postman Basic | desarrollo | 14,00 EUR | MONTHLY | **el plan Basic ya no existe**. Solo: 9 USD/mes (anual). Team: 19 USD/usuario/mes (anual) | Solo: 108 $/año | Sí | https://www.postman.com/pricing/ | alta | postman.com OK (200, 13340 B) | El precio con facturación mensual no aparece |
| 348 | Datadog Pro | desarrollo | 15,00 EUR | MONTHLY | **18 USD/host/mes** bajo demanda | 15 USD/host/mes en contrato anual | Sí (moneda y precio) | https://www.datadoghq.com/pricing/ | alta | datadoghq.com OK (200, 11545 B) | Por host: `variablePrice = true` |
| 349 | New Relic | desarrollo | 10,00 EUR | MONTHLY | **10 USD/mes** (primer usuario full, Standard) + 0,40 $/GB a partir de 100 GB | — | Sí (moneda) | https://newrelic.com/pricing | alta | newrelic.com OK (200, 9490 B) | Por uso: `variablePrice = true` |
| 350 | Grafana Cloud Pro | desarrollo | 10,00 EUR | MONTHLY | **19 USD/mes** (cuota de plataforma) + uso | — | Sí | https://grafana.com/pricing/ | alta | grafana.com: 200 pero solo 796 B (pequeño, revisar) | — |
| 351 | PagerDuty | desarrollo | 19,99 EUR | MONTHLY | **25 USD/usuario/mes** (Professional) | 252 USD/usuario/año (21 $/mes) | Sí | https://www.pagerduty.com/pricing/incident-management/ | alta | pagerduty.com OK (200, 3815 B) | Free hasta 5 usuarios |
| 352 | Pingdom | desarrollo | 10,00 EUR | MONTHLY | sin verificar | sin verificar | Sin verificar | https://www.pingdom.com/pricing/ (cifras vacías) | baja | pingdom.com OK (200, 7977 B) | Factura en €; hay opción mensual y anual |
| 353 | Fly.io | desarrollo | 5,00 EUR | MONTHLY | **sin cuota**: pago por uso. La máquina más barata cuesta 2,19 USD/mes | — | Sí | https://fly.io/pricing/ | alta | fly.io OK (200, 20445 B) | Ya no hay plan Hobby de 5 $. `variablePrice = true` |
| 354 | Neon Postgres | desarrollo | 19,00 EUR | MONTHLY | **sin mínimo**: el plan Launch es pago por uso (unos 15 $/mes con carga intermitente) | — | Sí (precio y domain) | https://neon.com/pricing | alta | neon.tech da el mismo icono que **neon.com** (200, 3400 B). Domain canónico: neon.com | Ya no hay cuota fija de 19 $. `variablePrice = true` |
| 355 | Upstash | desarrollo | 10,00 EUR | MONTHLY | **10 USD/mes** (Redis Fixed 250 MB) | — | Sí (moneda) | https://upstash.com/pricing/redis | alta | upstash.com OK (200, 13838 B) | Fixed 1 GB: 20 $. Los planes Pro están retirados |
| 356 | Tabnine Pro | desarrollo | 12,00 EUR | MONTHLY | **probablemente ya no existe** | — | Sí (revisar o eliminar) | tabnine.com/pricing devuelve un 301 a tricentis.com/contact-us, y tabnine.com a tricentis.com/products | media | tabnine.com da 200 en icon.horse, pero la web redirige a Tricentis | No hay plan individual visible. Confirmarlo a mano antes de borrar |
| 357 | Plausible Analytics | desarrollo | 9,00 EUR | MONTHLY | **9 USD/mes** (Starter, 10 000 páginas vistas) | ~90 USD/año (2 meses gratis) | Sí (moneda) | https://plausible.io/ | alta (mes) / media (año) | plausible.io OK (200, 9753 B) | Growth: 14 $. Business: 19 $ |
| 358 | Fathom Analytics | desarrollo | 14,00 EUR | MONTHLY | **15 USD/mes** (100 000 páginas vistas) | ~150 USD/año (2 meses gratis) | Sí | https://usefathom.com/pricing | alta (mes) / media (año) | usefathom.com OK (200, 8813 B) | — |
| 359 | Heroku Eco | desarrollo | 5,00 EUR | MONTHLY | **5 USD/mes** | — | Sí (moneda) | https://www.heroku.com/pricing/ | alta | heroku.com OK (200, 7843 B) | Dyno Basic: 7 $. Postgres Essential-0: 5 $ |
| 360 | Warp AI | desarrollo | 15,00 EUR | MONTHLY | **20 USD/mes** (plan "Build") | 216 USD/año (18 $/mes) | Sí | https://www.warp.dev/pricing | alta | warp.dev OK (200, 7140 B) | Renombrar a "Warp Build". Max: 200 $/mes o 180 $/mes anual |
| 361 | Retool Cloud | desarrollo | 10,00 EUR | MONTHLY | **11 €/creador/mes** (Team, facturación mensual) | 108 €/año (9 €/mes) | Sí | https://retool.com/pricing | alta | retool.com OK (200, 2700 B) | Usuarios internos: 6 €/mes o 5 €/mes anual. Probablemente sin IVA (precio B2B) |
| 362 | Pieces for Developers | desarrollo | 10,00 EUR | MONTHLY | **18,99 USD/mes** (Pro) | un 25 % menos en anual (~170,91 $/año, calculado) | Sí | https://pieces.app/pricing | alta | pieces.app OK (200, 8480 B) | Ya no hay plan Free ni Teams. Prueba de 7 días |
| 363 | Codeium Teams | desarrollo | 20,00 EUR | MONTHLY | **ya no existe**. codeium.com redirige a windsurf.com, que redirige a devin.ai. Teams: 80 $/mes por equipo + 40 $ por asiento. Pro: 20 USD/mes | — | Sí (sustituir) | https://devin.ai/pricing | media | codeium.com y windsurf.com dan el mismo icono (200, 3443 B), pero el producto vive ahora en devin.ai | Sustituir por "Windsurf / Devin Pro" (20 $) o eliminar |

## Planes que faltan

| Nombre | Precio | Ciclo | Domain | Fuente |
|---|---|---|---|---|
| iCloud+ 50 GB | 0,99 € | mensual | icloud.com | https://support.apple.com/es-es/108047 |
| iCloud+ 6 TB | 29,99 € | mensual | icloud.com | https://support.apple.com/es-es/108047 |
| iCloud+ 12 TB | 59,99 € | mensual | icloud.com | https://support.apple.com/es-es/108047 |
| Google One Basic 100 GB | 1,99 € | mensual | google.com | https://one.google.com/about/plans?gl=ES&hl=es |
| Mega Pro I 2 TB | 9,99 € (confianza media) | mensual | mega.io | https://subger.com/en/service/mega |
| Sync.com Personal 1 TB | 16 USD/mes o 144 USD/año | mensual y anual | sync.com | https://www.sync.com/pricing-individual/ |
| Sync.com Pro Solo 5 TB | 32 USD/mes o 336 USD/año | mensual y anual | sync.com | https://www.sync.com/pricing-individual/ |
| MongoDB Atlas Flex | 8–30 USD/mes (por uso) | mensual | mongodb.com | https://www.mongodb.com/pricing |
| Supabase Team | 599 USD/mes | mensual | supabase.com | https://supabase.com/pricing |
| Cloudflare Business | 250 USD/mes o 200 $/mes anual | mensual y anual | cloudflare.com | https://toolradar.com/tools/cloudflare/pricing |
| Render Pro (workspace) | 25 USD/mes cuota fija (confianza baja) | mensual | render.com | https://jwatte.com/blog/render-com-platform-review/ |
| Proton Unlimited | 119,88/año (renovación; primer año 77,88) | anual | proton.me | https://proton.me/drive/pricing |
| Proton Duo (2 TB, 2 usuarios) | 179,88/año (14,99/mes) | anual | proton.me | https://proton.me/drive/pricing |
| Fastmail Duo | 10 €/mes o 96 €/año | mensual y anual | fastmail.com | https://www.fastmail.com/pricing/ |
| Fastmail Family | 14 €/mes o 132 €/año | mensual y anual | fastmail.com | https://www.fastmail.com/pricing/ |
| Bitwarden Families | 47,88 USD/año | anual | bitwarden.com | https://bitwarden.com/pricing/ |
| 1Password Families | 5,99 USD/mes facturado anualmente (71,88 $/año) | anual | 1password.com | https://1password.com/es/pricing/password-manager |
| RoboForm Family | 47,75 USD/año | anual | roboform.com | https://www.roboform.com/pricing-personal |
| Surfshark One | 2,49 €/mes (promoción a 24 meses) | 24 meses | surfshark.com | https://surfshark.com/es/pricing |
| Netlify Personal | 9 USD/mes | mensual | netlify.com | https://www.netlify.com/pricing/ |
| Railway Pro | 20 USD/mes por workspace | mensual | railway.com | https://railway.com/pricing |
| Sentry Business | 89 USD/mes o 80 $/mes anual (960 $/año) | mensual y anual | sentry.io | https://sentry.io/pricing/ |
| Linear Business | 16 USD/usuario/mes anual (192 $/año) | anual | linear.app | https://linear.app/pricing |
| Postman Solo | 9 USD/mes anual (108 $/año) | anual | postman.com | https://www.postman.com/pricing/ |
| Postman Team | 19 USD/usuario/mes anual | anual | postman.com | https://www.postman.com/pricing/ |
| Warp Max | 200 USD/mes o 180 $/mes anual | mensual y anual | warp.dev | https://www.warp.dev/pricing |
| Windsurf / Devin Pro | 20 USD/mes | mensual | windsurf.com o devin.ai (por decidir) | https://devin.ai/pricing |
| Heroku Basic dyno | 7 USD/mes | mensual | heroku.com | https://www.heroku.com/pricing/ |
| Upstash Fixed 1 GB | 20 USD/mes | mensual | upstash.com | https://upstash.com/pricing/redis |
| Plausible Growth | 14 USD/mes | mensual | plausible.io | https://plausible.io/ |
| Datadog Infrastructure Enterprise | 23 USD/host/mes anual (27 $ bajo demanda) | mensual | datadoghq.com | https://www.datadoghq.com/pricing/ |

## Planes que ya no existen (o han cambiado de nombre o modelo)

| Línea | Plan en el catálogo | Situación actual | Fuente |
|---|---|---|---|
| 176-177 | Google One 2 TB / 5 TB | Ahora son **Google AI Plus (2 TB)** y **Google AI Pro (5 TB)** | https://one.google.com/about/plans?gl=ES&hl=es |
| 183 | pCloud Premium 2 TB | Se llama **Premium Plus 2 TB** | pcloud.com |
| 190 | Mega Pro Lite "2 TB" | Pro Lite son 400 GB. Los 2 TB son Pro I | subger.com |
| 192 | Sync.com Solo Basic | Ya no aparece. Hay Personal 1 TB, Pro Solo 5 TB y Personal Unlimited | https://www.sync.com/pricing-individual/ |
| 193 | Tresorit Personal | Se divide en Personal Lite, Essential y Pro | https://tresorit.com/pricing/personal |
| 189 | Algolia "10 €" | No hay cuota fija: Grow es pago por uso | https://www.algolia.com/pricing |
| 229 | ExpressVPN (plan único) | Ahora Basic, Advanced y Pro | vpnoverview.com, costbench.com |
| 240 | Tutanota Premium | Ya no existe. Ahora **Tuta** Revolutionary o Legend (domain tuta.com) | https://tuta.com/pricing |
| 246 | AdGuard Premium mensual | No hay mensual: solo anual o de por vida | https://adguard.com/en/license.html |
| 248 | RoboForm Premium mensual | No hay mensual: solo anual | https://www.roboform.com/pricing-personal |
| 345 | Linear (8 €) | Ahora Basic (10 $) y Business (16 $), con facturación anual | https://linear.app/pricing |
| 347 | Postman Basic | Ya no existe. Ahora Solo y Team | https://www.postman.com/pricing/ |
| 353 | Fly.io (Hobby de 5 $) | No hay planes: pago por uso | https://fly.io/pricing/ |
| 354 | Neon (19 $ fijos) | Launch y Scale sin mínimo, pago por uso. Domain neon.com | https://neon.com/pricing |
| 356 | Tabnine Pro | tabnine.com redirige a tricentis.com: no hay plan individual visible | redirección 301 comprobada el 2026-11-15 |
| 360 | Warp AI | El plan se llama ahora **Build** | https://www.warp.dev/pricing |
| 363 | Codeium Teams | Codeium pasó a Windsurf y ahora a devin.ai. Codeium Teams ya no existe | https://devin.ai/pricing |

## Logos y dominios

Todos los dominios devuelven 200 con imagen en icon.horse y en el favicon de Google. Ninguno falla. Estos son los que merece la pena revisar:

- **Dominio canónico distinto** (mismo icono, pero la marca ha cambiado):
  - railway.app → railway.com
  - neon.tech → neon.com
  - tutanota.com → tuta.com
  - codeium.com → windsurf.com, que a su vez redirige a devin.ai
- **Icono genérico de la empresa matriz:**
  - google.com en las filas de Google One: sale la "G". one.google.com da 668 B.
  - microsoft.com en OneDrive: sale el logo de Microsoft (485 B). onedrive.com da 498 B.
- **Tamaño pequeño, conviene mirarlo:**
  - grafana.com: 796 B
  - privateinternetaccess.com: 975 B
  - microsoft.com: 485 B
  - En el favicon de Google: render.com (257 B) y roboform.com (319 B). Con icon.horse son correctos.
- **El sitio redirige a otra empresa:** tabnine.com lleva a tricentis.com.
