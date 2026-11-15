# Revisión de precios del catálogo: categorías `ia` y `software`

- **Fecha de revisión:** 2026-11-15
- **Fichero revisado:** `src/main/kotlin/com/subia/service/CatalogService.kt` (líneas 34-59 `ia`, 126-171 `software`)
- **Filas revisadas:** 72 (26 `ia` + 46 `software`)
- **Método:** página oficial de precios con WebFetch siempre que respondió. Si no, fuentes secundarias de 2026 encontradas con WebSearch. Logos comprobados con `curl` contra icon.horse y Google s2/favicons.

## Criterios y avisos

- **USD:** muchos servicios (Cursor, Copilot, Midjourney, Perplexity, Zoom, Trello, Asana, Zapier, Raycast…) solo publican precio en dólares y **sin IVA**. Un usuario español paga ese importe en USD + 21 % de IVA, convertido al cambio del día por su banco. En la tabla pongo "X USD + IVA" y no invento una conversión a euros.
- **Precios EUR "sin IVA":** Slack, monday.com y Notion muestran euros, pero la página no dice si el IVA está incluido. Lo normal en B2B es que no lo esté. Va indicado en Notas.
- **Anual:** la columna "Precio anual verificado" es el **total al año** cuando la fuente lo da. Si la fuente solo da "X/mes facturado anualmente", lo indico así.
- **¿Cambia?:** "Sí" si cambia el precio nominal, el ciclo o el nombre del plan; "No" si coincide; "?" si no se pudo verificar.
- **Búsquedas:** el cupo de WebSearch de la sesión se agotó a mitad de la revisión. Desde ese punto solo usé las páginas oficiales con WebFetch. Las filas cuya página oficial devolvió 403 o vino truncada quedan como **sin verificar**, con el precio actual intacto.
- **Logos:** icon.horse responde **200 con una imagen genérica** (una letra sobre fondo gris, 256×256) cuando no encuentra el icono. Que dé 200 no basta. Revisé visualmente las imágenes sospechosas.

## Tabla de revisión

| Línea | Nombre actual | Categoría | Precio actual | Ciclo | Precio verificado | Precio anual verificado | ¿Cambia? | Fuente (URL) | Confianza (alta/media/baja) | Domain OK / logo | Notas |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 34 | Claude Pro | ia | 21.78 EUR | MONTHLY | 20 USD + IVA (web oficial, sin IVA). Los 18 € + IVA = 21,78 € del catálogo no se pudieron confirmar en la web en EUR | 200 USD/año + IVA (17 USD/mes) | No (falta añadir priceAnnual) | https://claude.com/pricing (15-11-2026) | media | claude.ai OK / logo OK | La página oficial dice "Prices shown don't include applicable tax". Si se mantiene el precio en EUR, añadir el anual equivalente |
| 35 | Claude Max 5x | ia | 108.90 EUR | MONTHLY | 100 USD + IVA. Plan renombrado a **"Claude Max"** (5x Pro) | No publicado | Sí (nombre) | https://claude.com/pricing/max (15-11-2026) | alta (USD) / baja (EUR) | claude.ai OK / logo OK | Desde ~30-10-2026 los planes son Max (100 USD) y Ultra (200 USD). Incluye 100 USD/mes de créditos API |
| 36 | Claude Max 20x | ia | 217.80 EUR | MONTHLY | 200 USD + IVA. Plan renombrado a **"Claude Ultra"** | No publicado | Sí (nombre y descripción) | https://support.claude.com/en/articles/17728952 ; https://claude.com/pricing/max (15-11-2026) | alta | claude.ai OK / logo OK | Ultra = 10x Pro semanal y sin límite de 5 h (cuentas verificadas). Los suscriptores Max 20x pasan a Ultra sin cambio de precio. La descripción "20×" ya no es correcta |
| 37 | Claude Team | ia | 30.00 EUR | MONTHLY | Team Standard: 25 USD/asiento/mes + IVA | 20 USD/asiento/mes facturado anual (240 USD/año) | Sí | https://claude.com/pricing (15-11-2026) | alta | claude.ai OK / logo OK | De 2 a 150 asientos. Asiento Premium: 125 USD/mes (100 USD anual) |
| 38 | ChatGPT Plus | ia | 23.00 EUR | MONTHLY | 23 € (IVA incl. según guías ES) | No hay plan anual | No | https://geotoolbox.ai/es/blog/precio-chatgpt ; https://www.upliora.es/blog/chatgpt-precio-2026-plus-pro-teams (ago-2026) | media | openai.com OK / logo OK (recomendado `chatgpt.com`, icono 4 KB) | La página oficial ES no mostró importes al hacer fetch |
| 39 | ChatGPT Pro | ia | 229.00 EUR | MONTHLY | 229 € (plan **"Pro 200"**, 200 USD) | No | Sí (nombre y descripción) | https://help.openai.com/articles/9793128 ; https://thenextweb.com/news/openai-devday-pro-200-usage-cut-pro-500-plan (30-09-2026) | media | openai.com OK / logo OK | Desde el 30-09-2026 las altas nuevas de Pro 200 tienen **10x** el uso de Plus, no 20x. La descripción "Pro 20x" queda obsoleta |
| 40 | ChatGPT Go | ia | 8.00 EUR | MONTHLY | 8 € | No | No | https://geotoolbox.ai/es/blog/precio-chatgpt (ago-2026) | media | openai.com OK / logo OK | Hay coberturas antiguas que dicen 9,99 €; las fuentes de 2026 dan 8 € |
| 41 | ChatGPT Pro 5x | ia | 103.00 EUR | MONTHLY | 103 € (plan **"Pro 100"**, 100 USD) | No | No (renombrar opcional) | https://www.geeknetic.es/Noticia/38245/ ; https://www.trendingtopics.eu/chatgpts-new-pro-plan-at-e103-monthly-trys-to-lure-devs-away-from-claude-code/ | media | openai.com OK / logo OK | OpenAI lo llama "Pro 100" (5x Plus) |
| 42 | Cursor Pro | ia | 20.00 EUR | MONTHLY | **Pro ya no se vende a altas nuevas.** Su equivalente es **Premium: 30 USD/mes** + IVA | 300 USD/año + IVA (25 USD/mes) | Sí (nombre y precio) | https://cursor.com/pricing (15-11-2026); https://codersera.com/blog/cursor-pricing-plans-spacex-grok-2026/ | alta (oficial) | cursor.com OK / logo OK | Gama desde el 30-10-2026: Plus 8, Premium 30, Super 100, Ultra 200 USD. El Pro antiguo pasa a Premium |
| 43 | GitHub Copilot | ia | 10.00 EUR | MONTHLY | Copilot **Pro**: 10 USD/mes + IVA | No publicado en la página (sin verificar) | No (renombrar a "Copilot Pro") | https://github.com/features/copilot/plans (15-11-2026) | alta | github.com OK / logo OK | También hay Pro+ (39 USD) y Max (100 USD) |
| 44 | Gemini Advanced | ia | 21.99 EUR | MONTHLY | **Google AI Pro**: 21,99 €/mes (5 TB) | No mostrado ("ahorra hasta un 16 %") | Sí (nombre) | https://one.google.com/about/plans?hl=es&gl=ES (15-11-2026) | alta | gemini.google.com OK / logo OK | El nombre "Gemini Advanced" ya no existe. Ahora son 5 TB, no 2 TB: corregir la descripción |
| 45 | Perplexity Pro | ia | 20.00 EUR | MONTHLY | 20 USD/mes (+IVA). Sin precio oficial en EUR | 200 USD/año | No (añadir anual) | https://geotoolbox.ai/es/blog/precio-perplexity ; https://licendi.com/es/blog/perplexity-pro-copilot-suscripciones-ia-comparativa-precios-2026/ | media | perplexity.ai OK / logo OK | La página oficial no devolvió precios al hacer fetch |
| 46 | Midjourney Standard | ia | 30.00 EUR | MONTHLY | 30 USD/mes | 288 USD/año (24 USD/mes) | No (añadir anual; moneda USD) | https://eesel.ai/blog/midjourney-pricing ; https://krater.ai/blog/midjourney-pricing (2026) | media | midjourney.com OK / **logo dudoso**: icon.horse da la letra genérica "M"; Google favicon OK (2 KB) | Solo cobra en USD. La documentación oficial respondió 403 |
| 47 | Midjourney Basic | ia | 10.00 EUR | MONTHLY | 10 USD/mes | 96 USD/año (8 USD/mes) | No (añadir anual) | Mismas fuentes que la línea 46 | media | midjourney.com / **logo dudoso** (igual que la 46) | — |
| 48 | Grok SuperGrok | ia | 30.00 EUR | MONTHLY | 30 USD/mes + IVA (no hay precio oficial en EUR) | ~300 USD/año (dato contradictorio entre fuentes) | No (cambiar el domain) | https://krater.ai/blog/grok-pricing ; https://geotoolbox.ai/blog/grok-pricing (oct-2026) | media | **domain incorrecto**: `x.com` es X, no Grok. Usar `grok.com` (icon.horse 11 KB OK) | La descripción "Grok 3" está anticuada. Existe SuperGrok Heavy (~300 USD/mes) |
| 49 | Notion AI | ia | 10.00 EUR | MONTHLY | **El complemento Notion AI ya no se vende.** La IA completa solo va en Business: 19,50 €/miembro/mes (anual) | 19,50 €/mes × 12 = 234 €/año (sin IVA, probablemente) | Sí (plan desaparecido) | https://www.notion.com/es-es/pricing (15-11-2026) | alta | notion.so funciona, pero el oficial es `notion.com` / logo OK | Free y Plus solo tienen prueba de IA. Ver "Planes que ya no existen" |
| 50 | Canva Pro | ia | 12.00 EUR | MONTHLY | 12 €/mes | 110 €/año | No | https://www.profee.com/de/articles/canva-pro-price-in-germany ; https://siecledigital.fr/2026/03/30/canva-pro-prix-fonctionnalites-et-differences-avec-la-version-gratuite/ | media | canva.com OK / logo OK (32×32, baja resolución) | La página ES de Canva respondió 403. Los datos son de DE y FR (eurozona, 2026) |
| 51 | ElevenLabs Creator | ia | 22.00 EUR | MONTHLY | 22 USD/mes + IVA | 220 USD/año (2 meses gratis) | No (moneda USD) | https://dupple.com/pricing/elevenlabs (oct-2026) | media | elevenlabs.io OK / icon.horse OK; Google favicon solo 170 B (sospechoso) | No hay prueba de 7 días: hay un primer mes al 50 %. Revisar `trialDays = 7` |
| 52 | Runway Standard | ia | 15.00 EUR | MONTHLY | 15 USD/mes | 144 USD/año (12 USD/mes) | No (añadir anual) | https://www.eesel.ai/en/blog/runway-ai-pricing ; https://gptprompts.ai/runway-pricing (2026) | media | runwayml.com OK / logo OK | runway.com sirve el mismo icono |
| 53 | Character.AI Plus | ia | 9.99 EUR | MONTHLY | c.ai+ 9,99 USD/mes | 94,99 USD/año | No (el nombre oficial es "c.ai+") | https://eesel.ai/blog/character-ai-pricing (2026) | media | character.ai OK / logo OK | No hay precio en EUR publicado |
| 54 | Suno Pro | ia | 10.00 EUR | MONTHLY | 10 USD/mes | 96 USD/año (8 USD/mes) | No (añadir anual) | https://gptprompts.ai/suno-pricing ; https://stacksheriff.com/ai-tools/suno-pricing/ (2026) | media | suno.com OK / logo OK | Premier: 30 USD/mes |
| 55 | Leonardo AI Artisan | ia | 24.00 EUR | MONTHLY | **30 USD/mes** (los 24 son el precio mensual con pago anual) | 288 USD/año (24 USD/mes) | Sí | https://flowith.io/blog/leonardo-ai-pricing-2026-free-vs-apprentice-vs-artisan ; https://dev.to/techsifted/leonardo-ai-pricing-2026-free-credits-paid-plans-and-api-costs-16a2 | media | leonardo.ai OK / **logo dudoso**: icon.horse da la letra genérica "L" (995 B); Google 3 KB OK | Apprentice: 12 USD. Maestro: 60 USD |
| 56 | Poe Basic | ia | 19.99 EUR | MONTHLY | 19,99 USD/mes. El plan se llama **"Premium"**, no "Basic" | ~17 % más barato al año (importe sin verificar) | Sí (nombre) | https://krater.ai/pricing/poe ; https://www.activepieces.com/blog/poe-subscription-pricing-2026-plans-and-point-costs.md (2026) | baja | poe.com OK / logo OK | Gama: 4,99 / 19,99 / 49,99 / 99,99 / 249,99 USD. La página oficial es poe.com/subscription_plans |
| 57 | Jasper AI Creator | ia | 49.00 EUR | MONTHLY | **Creator ya no existe.** Pro: 69 USD/asiento/mes | 708 USD/año (59 USD/mes) | Sí | https://www.jasper.ai/pricing (15-11-2026) | alta | jasper.ai OK / logo OK | El FAQ oficial dice que hay solo dos planes: Pro y Business |
| 58 | Adobe Firefly Premium | ia | 11.17 EUR | MONTHLY | 11,17 €/mes IVA incl. Pero ese precio es de **Firefly Standard**, no de Premium | No mostrado | Sí (nombre) | https://www.adobe.com/es/products/firefly/plans.html ; https://www.adobe.com/es/creativecloud/plans.html (15-11-2026) | alta | firefly.adobe.com OK / logo OK | Firefly Premium cuesta 223,68 €/mes. La descripción ya decía "Standard": solo hay que corregir el nombre |
| 59 | Krea AI Pro | ia | 35.00 EUR | MONTHLY | 35 USD/mes | ~21 USD/mes anual (descuento contradictorio entre fuentes) | No | https://www.zemith.com/en/learn/krea-pricing ; https://www.withorb.com/pricing-atlas/krea (2026) | media | krea.ai OK / logo OK (Google 495 B, pequeño) | Basic: 9 USD |
| 126 | Microsoft 365 Personal | software | 99.00 EUR | YEARLY | 99 €/año (o 10 €/mes) | 99 €/año | No | https://www.microsoft.com/es-es/microsoft-365/buy/compare-all-microsoft-365-products (15-11-2026) | alta | microsoft.com OK / logo de baja resolución (57×57, 485 B) | Promo: 69 € el primer año |
| 127 | Microsoft 365 Familiar | software | 129.00 EUR | YEARLY | 129 €/año (o 13 €/mes) | 129 €/año | No (el nombre oficial es "Familia") | Misma fuente que la línea 126 | alta | microsoft.com OK / igual que la 126 | Falta Microsoft 365 Premium (219 €/año) |
| 128 | Adobe Creative Cloud | software | 79.30 EUR | MONTHLY | 79,30 €/mes (anual con pago mensual, IVA incl.) | No mostrado | No | https://www.adobe.com/es/creativecloud/plans.html (15-11-2026) | alta | adobe.com OK / logo OK | Promo -60 % el primer año (31,71 €) hasta el 27-11 |
| 129 | Adobe Photoshop | software | 26.43 EUR | MONTHLY | 26,43 €/mes | No mostrado | No | Misma fuente que la línea 128 | alta | adobe.com OK / logo OK | — |
| 130 | Adobe Acrobat Pro | software | 24.18 EUR | MONTHLY | **24,19 €**/mes | No mostrado | Sí (+0,01 €) | Misma fuente que la línea 128 | alta | adobe.com OK / logo OK | Promo de 12,09 € los 3 primeros meses |
| 131 | Notion Plus | software | 10.00 EUR | MONTHLY | 11,50 €/miembro/mes con pago mensual | 9,50 €/mes anual (114 €/año) | Sí | https://www.notion.com/es-es/pricing (15-11-2026); https://www.notion.com/es-es/releases/2024-06-26 | media | Usar `notion.com` / logo OK | El 11,50 € mensual viene de la nota de versiones de 2024; la página actual solo muestra 9,50 €. No dice si incluye IVA |
| 132 | Figma Professional | software | 15.00 EUR | MONTHLY | Asiento Full: 20 USD/mes (mensual) | 16 USD/mes anual (192 USD/año) | Sí | https://www.figma.com/pricing/ (15-11-2026); https://userjot.com/blog/figma-pricing-2025-plans-seats-costs-explained | media | figma.com OK / logo OK | La oficial muestra 16 USD sin aclarar el ciclo. Asientos Dev 12 y Collab 3 USD |
| 133 | Sketch | software | 9.00 EUR | MONTHLY | Standard: 12 USD/editor/mes, facturación anual, sin IVA | 144 USD/año | Sí | https://www.sketch.com/pricing/ (15-11-2026) | alta | sketch.com OK / logo OK | El precio mensual no aparece en la página. Licencia Mac-only: 120 USD en pago único |
| 134 | Todoist Pro | software | 4.00 EUR | MONTHLY | **7 USD/mes** (mensual) | 60 USD/año (5 USD/mes) | Sí | https://www.todoist.com/pricing (15-11-2026) | alta | todoist.com OK / logo OK | Business: 10 USD mensual u 8 USD anual |
| 135 | Evernote Personal | software | 10.99 EUR | MONTHLY | **"Personal" ya no existe.** Ahora es **Starter**: ~14,99 USD/mes | ~99 USD/año | Sí | https://evernote.com/compare-plans (planes, sin precios); https://www.eesel.ai/blog/evernote-pricing | baja | evernote.com OK / logo OK | Advanced (antes Professional): ~24,99 USD/mes y 249,99 USD/año |
| 136 | Obsidian Sync | software | 4.00 EUR | MONTHLY | **5 USD/mes** (mensual) | 48 USD/año (4 USD/mes) | Sí | https://obsidian.md/pricing (15-11-2026) | alta | obsidian.md OK / logo OK | Ahora hay un único plan Sync; ya no salen Standard y Plus |
| 137 | Affinity Suite | software | 6.99 EUR | MONTHLY | **Gratis**: Affinity by Canva es gratuito desde oct-2025 | — | Sí (ya no es suscripción) | https://www.techradar.com/pro/affinity-by-canva-2026-review ; https://cgpress.org/archives/canva-reimagines-affinity-studio-and-releases-it-free-to-all-users.html | media | affinity.serif.com redirige; el nuevo es `affinity.studio` (mismo icono) | Las funciones de IA requieren Canva Pro. Propuesta: eliminar la fila |
| 138 | Grammarly Premium | software | 12.00 EUR | MONTHLY | Renombrado a **Grammarly Pro**: 12 €/mes. Ese es el precio con facturación anual (~144 €/año); el mensual ronda 30 USD | ~144 €/año | Sí (nombre y precio mensual) | https://www.grammarly.com/plans (15-11-2026); https://eesel.ai/blog/grammarly-pricing | media | grammarly.com OK / logo OK (32×32) | La página oficial en EUR muestra "Pro €12" sin aclarar el ciclo |
| 139 | Setapp | software | 9.99 EUR | MONTHLY | Plan Mac: **18,14 $/mes con impuestos incluidos** (14,99 + 21 %) | No mostrado ("ahorra hasta un 40 %") | Sí | https://setapp.com/pricing (15-11-2026) | media | setapp.com OK / logo OK | Mac+iOS: 22,98 $. Power User: 27,82 $. La página usa "$" sin nombrar la moneda |
| 140 | LinkedIn Premium | software | 39.90 EUR | MONTHLY | Premium Career: 39,66 €/mes | 19,82 €/mes anual (237,84 €/año) | Sí (leve) | https://www.kanbox.io/es/blog/linkedin-premium-career-precio (act. 03-02-2026) | media | linkedin.com OK / logo de baja resolución (64×64, 589 B) | Fuente secundaria; no está claro si incluye IVA |
| 141 | Zoom Pro | software | 15.09 EUR | MONTHLY | 16,99 USD/usuario/mes | 14,16 USD/mes anual (169,92 USD/año) | Sí | https://zoom.us/pricing ; https://zoom.us/es/pricing (15-11-2026) | alta | zoom.us OK / logo OK | Incluso con euros seleccionados, la página mostró USD |
| 142 | Slack Pro | software | 8.00 EUR | MONTHLY | 8,25 €/usuario/mes (mensual) | 6,75 €/mes anual (81 €/año) | Sí | https://slack.com/intl/es-es/pricing (15-11-2026) | alta | slack.com OK / logo de baja resolución (35×34) | No indica si incluye IVA (probablemente no). Promo -50 % 3 meses |
| 143 | Trello Premium | software | 10.00 EUR | MONTHLY | **12,50 USD**/usuario/mes (mensual) | 10 USD/mes anual (120 USD/año) | Sí | https://trello.com/pricing (15-11-2026) | alta | trello.com OK / logo OK | Standard: 6 USD mensual o 5 USD anual |
| 144 | Asana Starter | software | 10.99 EUR | MONTHLY | **13,49 USD**/usuario/mes (mensual) | 10,99 USD/mes anual (131,88 USD/año) | Sí | https://asana.com/es/pricing (15-11-2026) | alta | asana.com OK / logo OK | Los 10,99 del catálogo son el precio con pago anual |
| 145 | Monday.com Basic | software | 9.00 EUR | MONTHLY | 9 €/asiento/mes (anual, sin IVA). **Mínimo 3 asientos** (27 €/mes) | 9 €/mes por asiento (anual); mensual no publicado | No (falta aclarar el mínimo de asientos) | https://monday.com/pricing (15-11-2026) | alta | monday.com OK / logo OK | Standard 12 €, Pro 19 € |
| 146 | Airtable Plus | software | 10.00 EUR | MONTHLY | **Plus ya no existe.** Team: 20 USD/usuario/mes (anual) | 240 USD/año por usuario | Sí | https://airtable.com/pricing (15-11-2026) | alta | airtable.com OK / logo OK | El mensual de Team no aparece (sin verificar, ~24 USD) |
| 147 | Zapier Starter | software | 19.99 EUR | MONTHLY | Renombrado a **Professional**: 750 tareas, **29,99 USD/mes** (mensual) | 19,99 USD/mes anual (239,88 USD/año) | Sí | https://zapier.com/pricing (15-11-2026) | alta | zapier.com OK / logo OK | Factura en 19 monedas al cambio del día |
| 148 | Make Pro | software | 9.00 EUR | MONTHLY | Pro ~18,82 USD/mes (mensual, 10.000 créditos). Los 9 USD son del plan **Core** con pago anual | Pro ~16 USD/mes anual | Sí | https://www.jetadmin.io/blog/make-pricing/ (26-09-2026); https://www.make.com/en/pricing (no muestra precios) | baja | make.com OK / logo OK | Make factura ahora por créditos, no por operaciones. Corregir la descripción "10 000 operaciones" |
| 149 | Loom Business | software | 14.99 EUR | MONTHLY | 18 USD/usuario/mes | ~15 USD/mes anual ("ahorra hasta un 17 %"; importe no mostrado) | Sí | https://www.atlassian.com/software/loom/pricing (15-11-2026) | media | loom.com OK / logo OK | Business + AI: 24 USD |
| 150 | Miro Team | software | 10.00 EUR | MONTHLY | **Team ya no existe.** Starter: 8 USD/miembro/mes (anual) | 96 USD/año | Sí (nombre) | https://miro.com/pricing/ (15-11-2026) | alta | miro.com OK / logo OK | El mensual no aparece; ~10 USD es un cálculo propio a partir del 20 % de ahorro |
| 151 | Webflow Basic | software | 14.00 EUR | MONTHLY | 25 USD/mes (mensual) | 15 USD/mes anual (180 USD/año) | Sí | https://dupple.com/pricing/webflow (oct-2026); https://www.digidop.com/blog/webflow-pricing-2026-update | baja | webflow.com OK / logo OK | La página oficial falló (header overflow). En mayo de 2026 CMS y Business se fusionaron en "Premium" |
| 152 | Framer Mini | software | 5.00 EUR | MONTHLY | **Mini ya no existe.** Basic: 10 USD/mes + impuestos | No desglosado | Sí | https://www.framer.com/pricing (15-11-2026) | alta | framer.com OK / logo OK | Pro: 30 USD |
| 153 | Descript Creator | software | 24.00 EUR | MONTHLY | **35 USD/mes** (mensual) | 24 USD/mes anual (288 USD/año) | Sí | https://www.descript.com/pricing (15-11-2026) | alta | descript.com OK / logo OK | Hobbyist: 24 USD mensual o 16 USD anual |
| 154 | Bear Notes Pro | software | 2.99 EUR | MONTHLY | 2,99 $/mes | 29,99 $/año | No (añadir anual) | https://bear.app/ (15-11-2026) | alta | bear.app OK / logo OK | La web avisa de que el precio varía por país e impuestos |
| 155 | Craft Docs Pro | software | 4.99 EUR | MONTHLY | **Pro ya no existe.** Plus: 8 €/mes (mensual) | 4,80 €/mes anual (57,60 €/año). Ciclo deducido | Sí | https://www.craft.do/pricing (15-11-2026) | media | craft.do OK / logo OK | Family: 9 €/mes |
| 156 | Day One Premium | software | 3.99 EUR | MONTHLY | **Premium ya no existe.** Silver: 49,99 $/año; Gold: 74,99 $/año. Solo anual en la web | 49,99 $ (Silver) / 74,99 $ (Gold) | Sí (plan y ciclo) | https://dayoneapp.com/pricing/ (15-11-2026) | alta | dayoneapp.com OK / logo de baja resolución (32×32, 717 B) | Prueba de 1 mes dentro de la app |
| 157 | Ulysses | software | 5.99 EUR | MONTHLY | 5,99 USD/mes | 39,99 USD/año | No (añadir anual) | https://ulysses.app/pricing/ (15-11-2026) | alta | ulysses.app OK / logo OK | Precios de EE. UU.; el App Store ES puede variar |
| 158 | PDF Expert Pro | software | 6.99 EUR | MONTHLY | **No hay plan mensual.** Premium: 84,99 €/año | 84,99 €/año | Sí (ciclo YEARLY) | https://pdfexpert.com/pricing (15-11-2026) | alta | pdfexpert.com OK / logo OK | Licencia de por vida: 149,99 € |
| 159 | Fantastical Premium | software | 4.99 EUR | MONTHLY | **sin verificar**: la página oficial oculta los importes | — | ? | https://flexibits.com/pricing?product=fantastical (15-11-2026) | baja | fantastical.app OK (mismo icono que flexibits.com) | Black Friday -20 % activo |
| 160 | Raycast Pro | software | 8.00 EUR | MONTHLY | **10 USD/mes** | 96 USD/año | Sí | https://www.raycast.com/pricing (15-11-2026) | alta | raycast.com OK / logo OK | Plus 20 USD, Max 50 USD |
| 161 | Pitch Pro | software | 8.00 EUR | MONTHLY | **Pro ya no existe.** Plus: 10 €/mes, facturación anual | 120 €/año | Sí | https://pitch.com/pricing (15-11-2026) | alta | pitch.com OK / logo OK | Team 15 €, Business 20 € (anual) |
| 162 | Jira Software Standard | software | 7.75 EUR | MONTHLY | **sin verificar**: la página oficial llegó truncada y no había búsquedas disponibles | — | ? | https://www.atlassian.com/software/jira/pricing | baja | atlassian.com OK / logo OK | Se mantiene el precio actual |
| 163 | Confluence Standard | software | 5.75 EUR | MONTHLY | **sin verificar** (mismo motivo) | — | ? | https://www.atlassian.com/software/confluence/pricing | baja | atlassian.com OK / logo OK | — |
| 164 | CleanMyMac Business | software | 9.99 EUR | MONTHLY | **sin verificar**: la tienda MacPaw respondió 403 | — | ? | https://macpaw.com/store/cleanmymac | baja | cleanmymac.com OK / logo OK | El nombre "Business" no parece el plan de consumo; revisar |
| 165 | X Premium | software | 8.00 EUR | MONTHLY | **sin verificar**: help.x.com respondió 403 | — | ? | https://help.x.com/en/using-x/x-premium | baja | x.com OK / logo OK | — |
| 166 | Telegram Premium | software | 4.99 EUR | MONTHLY | **sin verificar**: el precio es regional y solo sale en @PremiumBot o en la tienda de apps | — | ? | https://telegram.org/faq_premium (15-11-2026) | baja | telegram.org OK / logo OK | El FAQ confirma un descuento anual y precio por país |
| 167 | TickTick Premium | software | 2.99 EUR | MONTHLY | **No hay plan mensual en la web.** 49,99 USD/año | 49,99 USD/año | Sí (ciclo YEARLY) | https://ticktick.com/about/upgrade (15-11-2026) | alta | ticktick.com: icon.horse OK (180×180); **Google favicon 404** | — |
| 168 | Any.do Premium | software | 3.99 EUR | MONTHLY | **7,99 USD/mes** | 4,99 USD/mes anual (59,88 USD/año) | Sí | https://www.any.do/pricing (15-11-2026) | alta | any.do OK / logo OK | Family: 9,99 USD/mes |
| 169 | Basecamp Business | software | 99.00 EUR | MONTHLY | **"Business" ya no existe.** Pro Unlimited: 329 USD/mes (mensual). El plan que cuesta 99 USD/mes es Pro Plus | Pro Unlimited: 199 USD/mes anual (2.388 USD/año) | Sí (nombre) | https://basecamp.com/pricing (15-11-2026) | alta | basecamp.com OK / logo OK | Freelancer 29, Pro 79, Pro Plus 99 USD/mes |
| 170 | Notion Team | software | 10.00 EUR | MONTHLY | **"Team" ya no existe.** Business: 19,50 €/miembro/mes (anual) | 234 €/año por miembro | Sí | https://www.notion.com/es-es/pricing (15-11-2026) | alta | Usar `notion.com` / logo OK | Duplica la línea 131 si se interpreta como Plus |
| 171 | Quizlet Plus | software | 5.99 EUR | MONTHLY | **sin verificar**: quizlet.com/upgrade respondió 403 | — | ? | https://quizlet.com/upgrade | baja | quizlet.com OK / logo de baja resolución (32×32) | — |

## Planes que faltan

| Nombre propuesto | Precio | Ciclo | Domain | Fuente |
|---|---|---|---|---|
| ChatGPT Pro 500 | 500 USD/mes. En euros no es oficial; hay una estimación de ~515 € con IVA | MONTHLY | chatgpt.com | https://help.openai.com/articles/9793128 ; https://pasqualepillitteri.it/es/news/19332/openai-lanza-pro-500-500-dolares-al-mes (lanzado el 29-09-2026) |
| Claude Team Premium (asiento) | 125 USD/mes + IVA (100 USD anual) | MONTHLY | claude.ai | https://claude.com/pricing |
| Cursor Plus | 8 USD/mes + IVA (80 USD/año) | MONTHLY | cursor.com | https://cursor.com/pricing |
| Cursor Super | 100 USD/mes + IVA (1.000 USD/año) | MONTHLY | cursor.com | https://cursor.com/pricing |
| Cursor Ultra | 200 USD/mes + IVA (2.000 USD/año) | MONTHLY | cursor.com | https://cursor.com/pricing |
| GitHub Copilot Pro+ | 39 USD/mes + IVA | MONTHLY | github.com | https://github.com/features/copilot/plans |
| GitHub Copilot Max | 100 USD/mes + IVA | MONTHLY | github.com | https://github.com/features/copilot/plans |
| Google AI Plus | 9,99 €/mes (2 TB) según la página oficial. Ojo: prensa de jun-2026 habla de 4,99 € con 400 GB | MONTHLY | gemini.google.com | https://one.google.com/about/plans?hl=es&gl=ES ; https://www.profesionalreview.com/2026/06/09/google-rebaja-ai-plus-a-499-euros-al-mes-en-espana-con-400-gb/amp/ (contradicción: confianza baja) |
| Google AI Ultra (5x) | 99,99 €/mes | MONTHLY | gemini.google.com | https://blogthinkbig.com/precios-planes-tarifas-gemini-pro-google-ai (confianza media) |
| Google AI Ultra (20x) | 219,99 €/mes | MONTHLY | gemini.google.com | Misma fuente (confianza media) |
| Perplexity Max | 200 USD/mes (2.000 USD/año) | MONTHLY | perplexity.ai | https://geotoolbox.ai/es/blog/precio-perplexity |
| Midjourney Pro | 60 USD/mes (576 USD/año) | MONTHLY | midjourney.com | https://eesel.ai/blog/midjourney-pricing |
| Midjourney Mega | 120 USD/mes (1.152 USD/año) | MONTHLY | midjourney.com | https://eesel.ai/blog/midjourney-pricing |
| SuperGrok Heavy | ~300 USD/mes | MONTHLY | grok.com | https://krater.ai/blog/grok-pricing (confianza baja) |
| Suno Premier | 30 USD/mes (288 USD/año) | MONTHLY | suno.com | https://gptprompts.ai/suno-pricing |
| ElevenLabs Starter | 6 USD/mes | MONTHLY | elevenlabs.io | https://dupple.com/pricing/elevenlabs |
| ElevenLabs Pro | 99 USD/mes | MONTHLY | elevenlabs.io | https://dupple.com/pricing/elevenlabs |
| Adobe Firefly Pro | 22,36 €/mes IVA incl. | MONTHLY | firefly.adobe.com | https://www.adobe.com/es/creativecloud/plans.html |
| Adobe Firefly Pro Plus | 55,91 €/mes IVA incl. | MONTHLY | firefly.adobe.com | Misma fuente |
| Adobe Creative Cloud Standard | 62,99 €/mes IVA incl. | MONTHLY | adobe.com | Misma fuente |
| Adobe Fotografía (Lightroom + Photoshop, 1 TB) | 24,19 €/mes IVA incl. | MONTHLY | adobe.com | Misma fuente |
| Adobe Lightroom | 14,74 €/mes IVA incl. | MONTHLY | adobe.com | Misma fuente |
| Adobe Acrobat Standard | 18,30 €/mes IVA incl. | MONTHLY | adobe.com | Misma fuente |
| Adobe Illustrator / Premiere / InDesign | 26,43 €/mes cada uno, IVA incl. | MONTHLY | adobe.com | Misma fuente |
| Microsoft 365 Premium | 219 €/año (22 €/mes) | YEARLY | microsoft.com | https://www.microsoft.com/es-es/microsoft-365/buy/compare-all-microsoft-365-products |
| Notion Business | 19,50 €/miembro/mes facturado anual | MONTHLY | notion.com | https://www.notion.com/es-es/pricing (sustituye a "Notion AI" y "Notion Team") |
| Evernote Advanced | ~24,99 USD/mes (249,99 USD/año) | MONTHLY | evernote.com | https://www.eesel.ai/blog/evernote-pricing (confianza baja) |
| Setapp Mac + iOS | 22,98 $/mes con impuestos | MONTHLY | setapp.com | https://setapp.com/pricing |
| Raycast Plus | 20 USD/mes (192 USD/año) | MONTHLY | raycast.com | https://www.raycast.com/pricing |
| Day One Gold | 74,99 $/año | YEARLY | dayoneapp.com | https://dayoneapp.com/pricing/ |
| Basecamp Pro Plus | 99 USD/mes (12 proyectos, 15 usuarios) | MONTHLY | basecamp.com | https://basecamp.com/pricing |

## Planes que ya no existen (o cambiaron de nombre)

| Línea | Plan del catálogo | Situación | Sustituto propuesto |
|---|---|---|---|
| 35 | Claude Max 5x | Renombrado (oct-2026) | "Claude Max" (100 USD) |
| 36 | Claude Max 20x | Renombrado (oct-2026) | "Claude Ultra" (200 USD, 10x semanal, sin límite de 5 h) |
| 39 / 41 | ChatGPT Pro / Pro 5x | OpenAI los llama Pro 200 / Pro 100. Pro 200 tiene 10x (no 20x) en las altas nuevas | Renombrar y corregir descripciones |
| 42 | Cursor Pro (20 USD) | Ya no se vende a altas nuevas (30-10-2026) | Cursor Premium (30 USD) |
| 44 | Gemini Advanced | Nombre retirado | Google AI Pro (21,99 €) |
| 49 | Notion AI (complemento) | Ya no se vende como complemento | Notion Business (19,50 €/mes anual) |
| 56 | Poe Basic | El plan de 19,99 USD se llama Premium | Poe Premium |
| 57 | Jasper Creator | Retirado; solo quedan Pro y Business | Jasper Pro (69 USD mensual / 59 USD anual) |
| 58 | Adobe Firefly "Premium" a 11,17 € | El precio es de Standard; Premium cuesta 223,68 € | Adobe Firefly Standard |
| 135 | Evernote Personal | Sustituido por Starter (finales de 2025) | Evernote Starter |
| 137 | Affinity Suite (suscripción) | Affinity es gratis desde oct-2025 | Eliminar la fila |
| 138 | Grammarly Premium | Renombrado | Grammarly Pro |
| 146 | Airtable Plus | No aparece en la página de precios | Airtable Team (20 USD anual) |
| 147 | Zapier Starter | Renombrado | Zapier Professional |
| 150 | Miro Team | No aparece en la página de precios | Miro Starter (8 USD anual) |
| 152 | Framer Mini | No aparece en la página de precios | Framer Basic (10 USD) |
| 155 | Craft Docs Pro | No aparece en la página de precios | Craft Plus |
| 156 | Day One Premium | Sustituido por Silver y Gold (solo anual) | Day One Silver (49,99 $/año) |
| 161 | Pitch Pro | No aparece en la página de precios | Pitch Plus (10 €/mes anual) |
| 169 | Basecamp Business | No aparece en la página de precios | Basecamp Pro Unlimited (329 USD mensual) o Pro Plus (99 USD) |
| 170 | Notion Team | Renombrado hace tiempo (Team pasó a Plus) | Notion Business |

## Logos dudosos

| Domain | Problema |
|---|---|
| midjourney.com | icon.horse devuelve la letra genérica "M" (fallback); Google favicon OK |
| leonardo.ai | icon.horse devuelve la letra genérica "L" (995 B); Google favicon OK |
| x.com (línea 48, Grok) | El dominio es incorrecto para Grok; usar `grok.com` |
| elevenlabs.io | Google favicon de solo 170 B; icon.horse OK |
| ticktick.com | Google favicon devuelve 404 (icono genérico); icon.horse OK |
| microsoft.com, linkedin.com, slack.com, dayoneapp.com, canva.com, grammarly.com, quizlet.com | Logo real pero de baja resolución (32-64 px) en icon.horse |
| notion.so | Funciona, pero el dominio oficial actual es `notion.com` |
| affinity.serif.com | El producto vive ahora en `affinity.studio` |
| openai.com | Funciona; para ChatGPT encaja mejor `chatgpt.com` |
