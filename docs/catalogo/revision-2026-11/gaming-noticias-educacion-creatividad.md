# Revisión de precios del catálogo: gaming, noticias, educación y creatividad

Fecha de la revisión: 2026-11-15. Fichero revisado: `src/main/kotlin/com/subia/service/CatalogService.kt`.
Precios en EUR con IVA incluido para España salvo que se indique otra moneda. No se ha modificado código.

## Cómo leer esta tabla

- **Precio verificado**: el precio normal, sin promociones. Las promociones van en Notas.
- **¿Cambia?**:
  - **Sí**: hay que cambiar el precio, el nombre, el ciclo, el `priceAnnual` o el `domain`.
  - **No**: el precio actual coincide.
  - **Sin verificar**: no hay fuente fiable y se deja el precio actual.
  - **Eliminar**: el plan ya no existe o no es una suscripción.
- **Logo**: se comprobó `https://icon.horse/icon/<domain>` y `https://www.google.com/s2/favicons?domain=<domain>&sz=128`.
  - En la primera pasada icon.horse devolvió 429 (límite de peticiones) en unos 30 dominios. Al repetir despacio, todos dieron 200 `image/png`. Google respondió 200 en todos.
  - **Aviso:** icon.horse devuelve 200 con un icono genérico de 2302 bytes incluso para dominios que no existen (probado con un dominio inventado). Un 200 no garantiza que el logo sea el real.
  - Los iconos de menos de 800 bytes se marcan como "favicon pequeño": probablemente un favicon de 16/32 px que se verá borroso.
- **Fuentes**: la web de varios periódicos españoles (elpais.com, elmundo.es, elconfidencial.com), además de ft.com, economist.com, humblebundle.com, udemy.com, masterclass.com, skillshare.com y canva.com, bloquea la descarga automática.
  - Además, el presupuesto de búsquedas web de la sesión se agotó a mitad de la categoría educación.
  - Por eso varias filas de prensa, educación y creatividad quedan como **sin verificar**.

## Tabla

| Línea | Nombre actual | Categoría | Precio actual | Ciclo | Precio verificado | Precio anual verificado | ¿Cambia? | Fuente (URL) | Confianza | Domain OK / logo | Notas |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 199 | Xbox Game Pass Ultimate | gaming | 26.99 | MONTHLY | **20.99** | — (no hay anual oficial) | Sí | https://www.muycomputer.com/?p=397893 (21-04-2026); https://www.menorca.info/actualidad/tecnologia-videojuegos-1/2026/04/22/2614351/ | media | xbox.com OK (icon 37 KB) | Rebaja permanente en abril de 2026 (de 26,99 a 20,99 €). A cambio, los Call of Duty futuros llegan al catálogo un año después. La web oficial no mostró precios en € al descargarla. |
| 200 | Xbox Game Pass Premium | gaming | 12.99 | MONTHLY | 12.99 | — | No | https://es.digitaltrends.com/videojuego/game-over-xbox-game-pass-sube-mucho-sus-precios-sufren-mexico-y-argentina/ | media | xbox.com OK | Sin cambios conocidos desde la reestructuración de octubre de 2025. |
| 201 | Xbox Game Pass Essential | gaming | 8.99 | MONTHLY | 8.99 | — | No | igual que 200 | media | xbox.com OK | Un titular de Generación Xbox habla de un Essential "más barato" en España, pero parece una oferta de códigos de 3/12 meses. No se pudo abrir. |
| 202 | PlayStation Plus Extra | gaming | 15.99 | MONTHLY | 15.99 | 125.99 | No | https://vandal.elespanol.com/noticia/1350790007/ (21-05-2026) | alta | playstation.com OK | Subida del 20-05-2026 (13,99 → 15,99). 3 meses: 43,99 €. El plan anual no cambia. |
| 203 | PlayStation Plus Essential | gaming | 9.99 | MONTHLY | 9.99 | 71.99 | No | igual que 202 | alta | playstation.com OK | 3 meses: 27,99 €. |
| 204 | PlayStation Plus Premium | gaming | 18.99 | MONTHLY | 18.99 | 151.99 | No | igual que 202 | alta | playstation.com OK | 3 meses: 54,99 €. |
| 205 | EA Play | gaming | 5.99 | MONTHLY | 5.99 | 39.99 | No | https://www.ea.com/es-es/ea-play | alta | ea.com OK | Falta EA Play Pro (ver "Planes que faltan"). |
| 206 | Nintendo Switch Online | gaming | 19.99 | YEARLY | 19.99 | 19.99 | No | https://es.digitaltrends.com/videojuego/nintendo-switch-online-precio-doble-caro/ ; https://startselect.com/es-es/nintendo-switch-online-paquete-de-expansion-suscripcion-familiar/70405 | media | nintendo.com OK (16 KB) | Plan individual de 12 meses. Faltan los planes familiares. |
| 207 | Nintendo Switch Online+ | gaming | 39.99 | YEARLY | 39.99 | 39.99 | No | igual que 206 | media | nintendo.com OK | Nombre oficial: "Nintendo Switch Online + Paquete de expansión". |
| 208 | Apple Arcade | gaming | 6.99 | MONTHLY | 6.99 | **49.99** | Sí (añadir `priceAnnual`) | https://www.apple.com/es/apple-arcade/ | alta | apple.com OK (logo de Apple genérico) | Incluido en Apple One (19,95 / 27,95 / 36,95 €). |
| 209 | Humble Choice | gaming | 11.99 | MONTHLY | sin verificar | — | Sin verificar | humblebundle.com bloquea la descarga; https://subger.com/en/service/humble-choice | baja | humblebundle.com: favicon pequeño (527 B) | Fuentes de terceros contradictorias (11,99–14,99 USD). |
| 210 | GeForce Now Priority | gaming | 10.99 | MONTHLY | 10.99 | — | Sí (renombrar) | https://www.monsterdealz.de/magazin/geforce-now-preise ; https://www.geeknetic.es/Noticia/36964/ | media | nvidia.com OK | El plan **Priority ya no existe**: ahora se llama **Performance**. 6 meses: 54,99 €. Desde el 01-01-2026 hay un límite de 100 h/mes. Falta Ultimate. La web oficial no mostró precios. |
| 211 | Shadow PC | gaming | 29.99 | MONTHLY | **32.99** (Neo) | — | Sí | https://shadow.tech/eu/shadowpc/offers/ | alta | shadow.tech OK (15 KB) | El plan "Essential" ya no existe. Gama actual: Neo Lite 25,99 €, Neo 32,99 €, Power Lite 39,99 €, Power 49,99 € (todos mensuales, sin permanencia). Conviene renombrar la fila a "Shadow PC Neo". |
| 212 | Discord Nitro | gaming | 9.99 | MONTHLY | 9.99 | (99.99 USD como referencia) | No | https://www.eesel.ai/es/blog/precios-discord | media | discord.com OK | Discord no publica una tarifa oficial en € para España. |
| 213 | Discord Nitro Basic | gaming | 2.99 | MONTHLY | 2.99 | (29.99 USD como referencia) | No | igual que 212 | media | discord.com OK | |
| 214 | Ubisoft+ Essential | gaming | 14.99 | MONTHLY | **17.99** (Ubisoft+ Premium) | — | Sí (el plan no existe) | https://vandal.elespanol.com/noticia/1350790867/ ; https://www.laps4.com/preguntas-y-respuestas/cuanto-cuesta-ubisoft-plus-espana ; https://www.playstation.com/es-es/ps-plus/ (Classics a 7,99 €) | media | **ubisoftplus.com redirige a store.ubi.com**: usar `ubisoft.com` (icon 62 KB) | No hay ningún plan "Essential". Los planes son **Ubisoft+ Premium 17,99 €/mes** y **Ubisoft+ Classics 7,99 €/mes**. |
| 215 | Roblox Premium 1000 | gaming | 9.99 | MONTHLY | 11.99 (precio antiguo) | — | Sí (el plan se cierra) | https://vandal.elespanol.com/noticia/1350788912/ ; https://startselect.com/fr-fr/blog/abonnement-roblox-premium | media | roblox.com OK | Desde el 30-04-2026 **no admite altas nuevas**. Lo sustituye **Roblox Plus**, a 4,99 USD/mes (precio en € sin verificar). Premium 450/1000/2200 costaban 5,99/11,99/23,99 €. |
| 216 | Xbox PC Game Pass | gaming | 14.99 | MONTHLY | **12.99** | — | Sí | https://www.muycomputer.com/?p=397893 | media | xbox.com OK | Rebaja permanente en abril de 2026. |
| 217 | Google Play Pass | gaming | 4.99 | MONTHLY | 4.99 | **29.99** | Sí (añadir `priceAnnual`) | https://www.geeknetic.es/Noticia/19524/ ; https://androidphoria.com/novedades/google-play-pass-disponible-espana | media | play.google.com OK | Precios de lanzamiento. No hay noticias de cambios hasta 2026. |
| 218 | Chess.com Diamond | gaming | 9.99 | MONTHLY | sin verificar (mensual) | **124.99** | Sí (añadir anual) | https://www.mydealz.de/deals/50-auf-chesscom-premium-mitgliedschaften-1-jahr-diamond-6200eur-statt-12499-platin-4249-statt-8499-und-gold-2499-statt-4999-2782320 (mayo 2026) | baja | chess.com OK | Precios de lista anuales en €: Gold 49,99, Platinum 84,99, Diamond 124,99. Con esos importes, 9,99 €/mes en mensual parece bajo. |
| 219 | Clash of Clans Gold Pass | gaming | 4.99 | MONTHLY | sin verificar | — | Sin verificar | https://www.sportsdunia.com/gaming/clash-of-clans-gold-pass-februrary-2026 | baja | supercell.com: icono de Supercell genérico (1891 B) | Pase de temporada mensual con precio regional (4,89–6,99 USD). |
| 220 | Pokémon GO Adventure | gaming | 4.99 | MONTHLY | 7.99 (USD) | — | Sí (nombre y domain) | https://www.playnews.gg/es/ofertas/pokemon-go-vale-la-pena-el-pase-go-deluxe-de-noviembre-de-2026-por-sus-7-99-usd | baja | **pokemongolive.com redirige a pokemongo.com**: cambiar el domain | No existe un pase llamado "Adventure". El pase mensual es el **Pase GO deluxe** (7,99 USD en noviembre de 2026; precio en € sin verificar). |
| 221 | Clash Royale Pass | gaming | 4.99 | MONTHLY | sin verificar | — | Sin verificar | https://topuplist.com/zh-tw/blogs/detail/clash-royale-upgraded-pass-royale-ronin-worth-it | baja | clashroyale.com: icono de Supercell genérico (1891 B) | Pass Royale Diamond a unos 11,99 USD en 2026 (fuente de terceros). |
| 222 | Brawl Stars Brawl Pass | gaming | 4.99 | MONTHLY | 8.99 (USD) | — | Sí | https://www.sportsdunia.com/gaming/brawl-stars-brawl-pass-rework | baja | brawlstars.com: icono de Supercell genérico (1891 B) | Supercell anunció Brawl Pass a 8,99 USD y Brawl Pass Plus a 12,99 USD. El importe en € no está confirmado (un vídeo da unos 15 € para Plus). |
| 223 | Minecraft Realms | gaming | 7.99 | MONTHLY | 7.99 | — | No | https://recurdash.com/subscription-pricing/minecraft-realms ; https://psprices.com/region-it/game/15347488/minecraft-realms-plus | media | minecraft.net OK | Es Realms Plus (7,99 USD / 7,99 €). Realms básico de Bedrock: 3,99 USD. |
| 253 | Kindle Unlimited | noticias | 9.99 | MONTHLY | 9.99 | — | No | https://www.elindependiente.com/de-tiendas/2026/03/09/kindle-unlimited-ahora-por-0-euros/ | alta | amazon.com: favicon pequeño (476 B, logo de Amazon). Mejor `amazon.es` | Promociones frecuentes con meses gratis. |
| 254 | Readwise Reader | noticias | 7.99 | MONTHLY | **12.99 USD** | **119.88 USD** | Sí | https://readwise.io/pricing ; https://www.readless.app/blog/readwise-reader-pricing-2026 | media | readwise.io: favicon pequeño (655 B) | Plan "Full" (Reader + Highlights): 9,99 USD/mes con pago anual o 12,99 USD/mes en mensual. Cobra en USD. Reader solo: 7,99 USD/mes con pago anual. |
| 255 | Scribd | noticias | 11.99 | MONTHLY | sin verificar | — | Sin verificar | https://www.chollometro.com/ofertas/1-mes-gratis-de-everand-premium-antigua-scribd-elige-cualquier-plan-a-0eur-audiolibros-ebooks-y-revistas-2051605 | baja | scribd.com: favicon pequeño (523 B en Google) | Los libros y audiolibros están ahora en **Everand** (antes Scribd), con sistema de créditos. Chollómetro muestra 10,99 € tachado. Scribd queda solo para documentos. |
| 256 | El País Digital | noticias | 11.00 | MONTHLY | **12.00** | **144.00** | Sí | https://www.merca2.es/2026/06/12/el-pais-suscriptores-digitales-objetivo-2029-2395775/ ; https://dircomfidencial.com/medios/el-pais-aprueba-una-subida-de-las-tarifas-de-suscripcion-digital-20261008-0404/ | media | elpais.com OK | Suscripción básica en junio de 2026: 12 €/mes o 144 €/año. En octubre de 2026 se aprobó una subida del 4,1 al 12,5 % según la modalidad, que se aplica en la renovación. Las cifras finales no se pudieron ver (la web bloquea la descarga) y podrían ser unos 13 €. |
| 257 | The New York Times | noticias | 17.00 | MONTHLY | sin verificar | — | Sin verificar | https://niemanlab.org/2024/10/el-pais-sweetens-subscription-offering-with-full-access-to-the-new-york-times | baja | nytimes.com OK | No hay precio en € para España en 2026. El País Premium incluye NYT. |
| 258 | Blinkist Premium | noticias | 12.99 | MONTHLY | **15.99** | 79.99–99.99 (sin consenso) | Sí | https://www.blinkist.com/magazine/posts/how-much-do-blinkists-premium-subscriptions-cost ; https://apps.apple.com/lv/app/id568839295?l=en-gb | media | blinkist.com: favicon pequeño (419 B) | Promociones frecuentes del 25–40 % en el plan anual. |
| 259 | Audible | noticias | 9.99 | MONTHLY | 9.99 | — | No | https://www.elindependiente.com/de-tiendas/2026/04/07/ (página de audible.es: "Después de los 30 días, 9,99 €/mes") | alta | audible.com OK; mejor `audible.es` (mismo icono) | Hubo una promo de 0,99 €/mes durante 3 meses hasta el 14-04-2026. |
| 260 | Storytel | noticias | 8.99 | MONTHLY | 8.99 | — | No | https://www.storytel.com/es/subscriptions | alta | storytel.com OK | Oferta actual: 12 meses a 4,49 €/mes. Falta el plan Family. |
| 261 | Medium | noticias | 5.00 | MONTHLY | 5 USD | **50 USD** | Sí (añadir anual) | https://www.lowermysubs.com/lower/medium | media | medium.com: favicon pequeño (679 B) | Cobra en USD. Plan "Friend of Medium": 15 USD/mes o 150 USD/año. |
| 262 | Feedly Pro | noticias | 6.00 | MONTHLY | ~6 USD (anual) / 6.99–8 USD (mensual) | 72 USD | Sin verificar | https://www.readless.app/blog/feedly-pricing-2026-free-vs-pro-limits | baja | feedly.com OK | La web oficial no cargó. Pro+: 8,25–12 USD/mes. |
| 263 | Inoreader Pro | noticias | 6.00 | MONTHLY | **8.99** (+IVA) | **80.04** (6,67 €/mes) (+IVA) | Sí | https://www.inoreader.com/pricing | alta | inoreader.com OK | La FAQ oficial indica que el IVA se suma en Europa. Con el 21 % saldría a unos 10,88 €/mes. Comprobar si se guarda con o sin IVA. |
| 264 | Overcast Premium | noticias | 9.99 | YEARLY | **14.99 USD** | 14.99 USD | Sí | https://9to5mac.com/2024/11/20/overcast-listening-history-undo-features/ ; https://alternativeto.net/software/overcast/about | media | overcast.fm OK | Subió de 9,99 a 14,99 USD/año en noviembre de 2024. Precio en € de la App Store española sin verificar. |
| 265 | The Wall Street Journal | noticias | 9.99 | MONTHLY | sin verificar | (UK: £179.88) | Sin verificar | https://pressgazette.co.uk/media-audience-and-business-data/news-subcriptions-prices-2026/ | baja | wsj.com OK | Precio completo en UK: 179,88 £/año. En USD, unos 38,99/mes. 9,99 € parece un precio promocional. |
| 266 | Financial Times | noticias | 39.99 | MONTHLY | sin verificar | (UK: £468) | Sin verificar | igual que 265 | baja | ft.com OK | Standard Digital en UK: 468 £/año (primer año 279 £). |
| 267 | The Economist | noticias | 21.99 | MONTHLY | sin verificar | — | Sin verificar | igual que 265 | baja | economist.com: **favicon muy pequeño (330 B), logo dudoso** | 23,90 £/mes en UK (enero de 2026). |
| 268 | La Vanguardia Premium | noticias | 3.49 | MONTHLY | sin verificar (~14.95 estándar) | (renovación de 99,90 €/año según OCU) | Sin verificar | https://subger.com/es/service/la-vanguardia ; https://www.ocu.org/reclamar/lista-reclamaciones-publicas/proceso-enga-C3-B1oso-en-la-renovac/08cfa9c85d823b9563 | baja | lavanguardia.com OK (12 KB) | 3,49 € parece un precio de captación. Varias reclamaciones de la OCU hablan de renovaciones anuales de 99,90 €. |
| 269 | The Athletic | noticias | 7.99 | MONTHLY | 7.99 USD | **71.99 USD** | Sí (anual y domain) | https://apppricinglab.com/iap/apple/1135216317 (marzo 2026) | media | **theathletic.com redirige a nytimes.com/athletic**. El icono sigue respondiendo; valorar `nytimes.com` | Precio en € sin verificar. |
| 270 | El Confidencial Premium | noticias | 2.99 | MONTHLY | sin verificar | — | Sin verificar | https://subger.com/en/us/service/el-confidencial-es | baja | elconfidencial.com OK | Un tercero da unos 9,99 €/mes. 2,99 € parece promocional. |
| 271 | Le Monde Numérique | noticias | 9.99 | MONTHLY | sin verificar | — | Sin verificar | abo.lemonde.fr bloquea la descarga | baja | lemonde.fr OK | |
| 272 | Caixin Global | noticias | 9.99 | MONTHLY | **25.99 USD** | **249.99 USD** | Sí | https://apps.apple.com/app/id1177372413 ; https://adapty.io/paywall-library/caixin/ (13-10-2026) | media | caixinglobal.com OK | Plan para estudiantes: 59,99 USD. |
| 395 | Coursera Plus | educacion | 49.00 | MONTHLY | **52.00** | **355.00** | Sí | https://www.coursera.org/courseraplus | alta | coursera.org OK (12 KB) | Promo de 206 € el primer año hasta el 30-11-2026. La web dice "más impuestos aplicables", así que el IVA podría no estar incluido. |
| 396 | MasterClass | educacion | 10.00 | MONTHLY | **~110/año** (Individual) | ~110 | Sí (ciclo a YEARLY) | https://www.sharesub.com/cms/es/?p=62200 (verificado en masterclass.com en junio de 2026) | baja | masterclass.com OK | **Solo hay facturación anual.** Duo: ~140 €/año. Family: ~165 €/año. |
| 397 | Skillshare Premium | educacion | 8.00 | MONTHLY | sin verificar | — | Sin verificar | https://marcandrews.com/skillshare-pricing-uk-2026-plans-costs-in-gbp-explained/ | baja | skillshare.com OK | En UK: 24 £/mes o 125 £/año (julio de 2026). Si en € es parecido, 8 € es muy bajo. |
| 398 | Brilliant Premium | educacion | 24.99 | MONTHLY | **13.75** (ver notas) | sin verificar | Sí | https://brilliant.org/subscribe/ | media | brilliant.org: favicon pequeño (802 B) | La web oficial muestra 13,75 €/mes (Individual) y 28 €/mes (Family, 6 plazas). Probablemente es el equivalente mensual con pago anual (~165 €/año). |
| 399 | Babbel Premium | educacion | 13.95 | MONTHLY | sin verificar | (FR: 95.88) | Sin verificar | https://www.mindomax.com/fr/babbel-prix (11-10-2026) | baja | babbel.com OK | En Francia, 12 meses cuestan 95,88 € (7,99 €/mes). El `priceAnnual` actual (83,40) no está confirmado. |
| 400 | Rosetta Stone | educacion | 11.99 | MONTHLY | sin verificar | — | Sin verificar | https://eu.rosettastone.com/ (sin cifras) | baja | rosettastone.com OK; la web europea es eu.rosettastone.com | Planes de 3 meses, 12 meses y de por vida. |
| 401 | Domestika Plus | educacion | 9.99 | MONTHLY | sin verificar | (~200–229 €/año según la OCU) | Sin verificar | https://www.ocu.org/reclamar/lista-reclamaciones-publicas/cobro-adicional-tras-comprar-u/9757344a8b5de5588c | baja | domestika.org OK | Parece de facturación anual. Una reclamación habla de un cargo de 228,58 € en febrero de 2026. |
| 402 | Platzi Expert | educacion | 24.99 | MONTHLY | **239.00/año** | **239.00** | Sí (ciclo a YEARLY) | https://platzi.com/precios/ | alta | platzi.com OK | Expert es **anual**. Basic: 59 €/mes. Expert Duo: 319 €/año. Expert Groups: 599 €/año. |
| 403 | DataCamp Standard | educacion | 25.00 | MONTHLY | **12/mes con pago anual** | **144.00** | Sí (renombrar) | https://www.datacamp.com/pricing | alta | datacamp.com OK | El plan "Standard" ya no existe: ahora es **Premium**. No se publica un precio mensual sin compromiso; aparece un "Special price" de 28 € sin periodo indicado. |
| 404 | Frontend Masters | educacion | 39.00 | MONTHLY | 39 USD | **390 USD** | Sí (anual y domain) | https://master.dev/join/ | alta | **frontendmasters.com redirige a master.dev** (rebranding) | Cobra en USD. |
| 405 | Udemy Personal Plan | educacion | 19.99 | MONTHLY | sin verificar | — | Sin verificar | udemy.com bloquea la descarga | baja | udemy.com OK | |
| 406 | Preply Business | educacion | 74.99 | MONTHLY | sin verificar | — | Sin verificar | — | baja | preply.com OK | Preply es un marketplace: el precio depende del tutor. "Preply Business" es para empresas. Sugerencia: `variablePrice = true` o renombrar a "Preply (clases)". |
| 407 | Photomath Plus | educacion | 7.99 | MONTHLY | sin verificar | — | Sin verificar | photomath.com/plus da 404 | baja | photomath.com OK | Photomath es ahora de Google. |
| 408 | SoloLearn Pro | educacion | 9.99 | MONTHLY | sin verificar | — | Sin verificar | sololearn.com/en/pro no muestra precios | baja | sololearn.com OK | |
| 409 | Mimo Premium | educacion | 9.99 | MONTHLY | sin verificar | — | Sí (domain) | https://mimo.org/pricing (sin cifras) | baja | **getmimo.com redirige a mimo.org** (308) | Planes actuales: Pro (solo móvil) y Max (web y móvil). La prueba gratuita solo existe con el plan anual. |
| 410 | Khan Academy SAT Prep | educacion | 14.99 | MONTHLY | **0 (gratis)** | — | Eliminar | (conocimiento previo; no se verificó en esta sesión por falta de presupuesto de búsqueda) | media | khanacademy.org: favicon pequeño (750 B) | La preparación oficial del SAT de Khan Academy es gratuita y no es una suscripción. Si se quiere un producto de pago de Khan, sería Khanmigo (precio sin verificar). |
| 411 | Duolingo Super | educacion | 8.49 | MONTHLY | 8.49 | ~83.99 (sin confirmar) | Sin verificar (anual) | https://www.gamsgo.com/es/blog/precio-duolingo-super-españa ; https://www.chollometro.com/ofertas/super-duolingo-suscripcion-de-1-ano-por-solo-162-eur-en-kinguin-2002127 | baja | duolingo.com OK (19 KB) | El mensual coincide. El anual (87,96 € en el catálogo) frente a ~83,99 € de un revendedor no está confirmado. |
| 412 | Lingoda Sprint | educacion | 99.00 | MONTHLY | sin verificar | — | Sin verificar | lingoda.com/es/precios da 404 | baja | lingoda.com OK | |
| 413 | italki Credits | educacion | 20.00 | MONTHLY | — | — | Sin verificar | — | baja | italki.com OK | No es una suscripción: se pagan créditos por uso. Sugerencia: `variablePrice = true`. |
| 414 | Pimsleur Premium | educacion | 19.99 | MONTHLY | sin verificar | — | Sin verificar | https://www.pimsleur.com/pricing (solo USD y sin desglose) | baja | pimsleur.com OK | |
| 416 | Cuota de colegio | educacion | 350.00 | MONTHLY | n/a (`variablePrice`) | — | No aplica | — | — | sin domain | Importe orientativo. No se revisa. |
| 417 | Matrícula universitaria | educacion | 1200.00 | YEARLY | n/a (`variablePrice`) | — | No aplica | — | — | sin domain | Importe orientativo. |
| 418 | Academia | educacion | 90.00 | MONTHLY | n/a (`variablePrice`) | — | No aplica | — | — | sin domain | Importe orientativo. |
| 423 | VSCO Membership | creatividad | 7.99 | MONTHLY | sin verificar | — | Sin verificar | vsco.co bloquea la descarga (403) | baja | vsco.co OK | Planes actuales: Plus y Pro. |
| 424 | Adobe Lightroom Plan | creatividad | 14.74 | MONTHLY | 14.74 | **144.33** | Sí (añadir `priceAnnual`) | https://www.adobe.com/es/products/photoshop-lightroom/plans.html | alta | lightroom.adobe.com OK | 14,74 € es el plan anual con pago mensual (con permanencia). Plan Fotografía: 24,19 €/mes. |
| 425 | Facetune Premium | creatividad | 5.99 | MONTHLY | sin verificar | — | Sin verificar | — | baja | facetuneapp.com OK (título: "Facetune") | |
| 426 | CapCut Pro | creatividad | 7.99 | MONTHLY | **29.99** (precio web) | **259.99** | Sí | https://www.capcut.com/pricing | media | capcut.com OK | La web avisa de que el precio es de referencia y que el final se ve al iniciar sesión. En la app móvil puede ser menor. Plan Ultra: 68 €/mes o 779 €/año. |
| 427 | InShot Pro | creatividad | 3.99 | MONTHLY | sin verificar | — | Sin verificar | — | baja | inshot.com OK | |
| 428 | Adobe Premiere Rush | creatividad | 12.99 | MONTHLY | — | — | Eliminar | https://www.adobe.com/es/products/premiere-rush.html | media | adobe.com OK | La página de Premiere Rush redirige a "Adobe Premiere para dispositivos móviles", que es gratuita. **Premiere Rush está descatalogado.** |
| 429 | Splice Premium | creatividad | 4.99 | MONTHLY | sin verificar | — | Sí (domain) | — | baja | **DOMAIN INCORRECTO**: splice.com es Splice, la empresa de samples de música. El editor de vídeo es `spliceapp.com` (icon 10 KB) | El cancelUrl también apunta a splice.com. |
| 430 | Unfold Pro | creatividad | 1.99 | MONTHLY | sin verificar | — | Sin verificar | — | baja | unfold.com OK (título: "Unfold — Toolkit for Storytellers") | |
| 431 | Afterlight | creatividad | 2.99 | MONTHLY | sin verificar | — | Sin verificar | — | baja | afterlight.us OK (1,3 KB) | |
| 432 | PicsArt Gold | creatividad | 11.99 | MONTHLY | **12.99** | **109.92** (9,16 €/mes) | Sí (renombrar) | https://picsart.com/pricing | alta | picsart.com OK | "Gold" ya no existe: ahora es **Picsart Pro**. Ultra: 64 €/mes (52,50 €/mes con pago anual). |
| 433 | Pixlr Premium | creatividad | 7.99 | MONTHLY | **9.99** | **95.88** (7,99 €/mes) | Sí | https://pixlr.com/pricing/ | alta | pixlr.com OK | Plus: 2,49 €/mes. Ultra: 24,99 €/mes. |
| 434 | Remini Pro | creatividad | 7.99 | MONTHLY | sin verificar | — | Sin verificar | remini.ai/pricing da 404 | baja | remini.ai OK | Suele facturarse por semanas en la app. |
| 435 | Prequel Premium | creatividad | 7.99 | MONTHLY | sin verificar | — | Sin verificar | — | baja | prequel.app OK | |
| 436 | YouCam Perfect Premium | creatividad | 4.99 | MONTHLY | sin verificar | — | Sí (domain, urgente) | — | baja | **DOMAIN PELIGROSO**: youcam.com es una web de webcams para adultos (título comprobado con curl). El oficial es `perfectcorp.com` (icon 5 KB) | Cambiar también el cancelUrl, que apunta a youcam.com/subscription. |
| 437 | Polarr Pro | creatividad | 7.99 | MONTHLY | sin verificar | — | Sin verificar | polarr.com/pricing da 404 | baja | polarr.com OK | |
| 438 | Veed Pro | creatividad | 24.00 | MONTHLY | sin verificar | — | Sin verificar | https://www.veed.io/pricing (sin cifras en el texto) | baja | veed.io OK | |
| 439 | Adobe Express Premium | creatividad | 12.09 | MONTHLY | sin verificar (9,99 USD/mes) | — | Sin verificar | https://www.adobe.com/es/express/pricing | baja | express.adobe.com OK | La página española solo mostraba USD. |

## Planes que faltan

| Nombre | Precio | Ciclo | domain | Fuente | Confianza |
|---|---|---|---|---|---|
| EA Play Pro | 16.99 (anual 119.99) | MONTHLY | ea.com | https://www.ea.com/es-es/ea-play | alta |
| Nintendo Switch Online (familiar) | 34.99 | YEARLY | nintendo.com | https://es.digitaltrends.com/videojuego/nintendo-switch-online-precio-doble-caro/ | media |
| Nintendo Switch Online + Paquete de expansión (familiar) | 69.99 | YEARLY | nintendo.com | https://startselect.com/es-es/nintendo-switch-online-paquete-de-expansion-suscripcion-familiar/70405 | media |
| GeForce NOW Ultimate | 21.99 | MONTHLY | nvidia.com | https://www.monsterdealz.de/magazin/geforce-now-preise | media |
| Shadow PC Neo Lite | 25.99 | MONTHLY | shadow.tech | https://shadow.tech/eu/shadowpc/offers/ | alta |
| Shadow PC Power | 49.99 | MONTHLY | shadow.tech | https://shadow.tech/eu/shadowpc/offers/ | alta |
| Ubisoft+ Premium (sustituye a "Essential") | 17.99 | MONTHLY | ubisoft.com | https://vandal.elespanol.com/noticia/1350790867/ | media |
| Ubisoft+ Classics | 7.99 | MONTHLY | ubisoft.com | https://www.playstation.com/es-es/ps-plus/ | alta |
| Roblox Plus | 4.99 USD (precio en € sin verificar) | MONTHLY | roblox.com | https://vandal.elespanol.com/noticia/1350788912/ | baja |
| Brawl Pass Plus | 12.99 USD | MONTHLY (temporada) | brawlstars.com | https://www.sportsdunia.com/gaming/brawl-stars-brawl-pass-rework | baja |
| Storytel Family (2 cuentas) | 15.99 | MONTHLY | storytel.com | https://www.storytel.com/es/subscriptions | alta |
| El Mundo Premium (Orbyt) | sin verificar (terceros: 11,99 €/mes; App Store: 8,99 €/mes) | MONTHLY | elmundo.es (icon OK) | https://subger.com/es/service/el-mundo | baja |
| ABC Premium, elDiario.es (socios), Expansión Premium, El Español | sin verificar | — | abc.es, eldiario.es, expansion.com, elespanol.com (iconos 200; expansion.com muy pequeño, 295 B) | No se pudieron buscar: se agotó el presupuesto de búsquedas | — |
| Platzi Basic | 59.00 | MONTHLY | platzi.com | https://platzi.com/precios/ | alta |
| Platzi Expert Duo | 319.00 | YEARLY | platzi.com | https://platzi.com/precios/ | alta |
| Brilliant Family (6 plazas) | 28.00 | MONTHLY | brilliant.org | https://brilliant.org/subscribe/ | media |
| Adobe Plan Fotografía (Lightroom + Photoshop) | 24.19 | MONTHLY (con permanencia anual) | adobe.com | https://www.adobe.com/es/products/photoshop-lightroom/plans.html | alta |
| Adobe Creative Cloud Pro | 79.30 | MONTHLY (con permanencia anual) | adobe.com | igual | alta |
| Picsart Ultra | 64.00 (anual 630) | MONTHLY | picsart.com | https://picsart.com/pricing | alta |
| Pixlr Plus | 2.49 (anual 23.88) | MONTHLY | pixlr.com | https://pixlr.com/pricing/ | alta |
| Canva Pro | sin verificar (canva.com bloquea la descarga) | MONTHLY | canva.com (icon OK) | — | — |
| Duolingo Max | 29.99 USD (precio en € sin verificar) | MONTHLY | duolingo.com | https://subger.com/es/vendor/duolingo-inc | baja |

## Planes que ya no existen o han cambiado de nombre

| Línea | Plan en el catálogo | Situación actual | Fuente |
|---|---|---|---|
| 210 | GeForce Now Priority | Renombrado a **Performance** (mismo precio, 10,99 €) | https://www.monsterdealz.de/magazin/geforce-now-preise |
| 211 | Shadow PC (precio de "Essential") | Essential ya no existe. Gama actual: Neo Lite / Neo / Power Lite / Power | https://shadow.tech/eu/shadowpc/offers/ |
| 214 | Ubisoft+ Essential | No existe. Los planes son Premium (17,99 €) y Classics (7,99 €) | https://www.laps4.com/preguntas-y-respuestas/cuanto-cuesta-ubisoft-plus-espana |
| 215 | Roblox Premium 1000 | Sin altas nuevas desde el 30-04-2026. Lo sustituye Roblox Plus | https://vandal.elespanol.com/noticia/1350788912/ |
| 220 | Pokémon GO Adventure | El nombre no existe. El pase es el **Pase GO deluxe** | https://www.playnews.gg/es/ofertas/pokemon-go-vale-la-pena-el-pase-go-deluxe-de-noviembre-de-2026-por-sus-7-99-usd |
| 255 | Scribd (libros y audiolibros) | Pasó a **Everand**, con sistema de créditos | https://bookriot.com/scribd-everand-unlimited-ebook-subscription/ |
| 403 | DataCamp Standard | Ahora se llama **Premium** | https://www.datacamp.com/pricing |
| 404 | Frontend Masters | La marca y el dominio pasaron a **master.dev** | https://master.dev/join/ |
| 410 | Khan Academy SAT Prep | Es gratuito, no es una suscripción | conocimiento previo (sin verificar en esta sesión) |
| 428 | Adobe Premiere Rush | Descatalogado. Lo sustituye Adobe Premiere para móviles, gratuito | https://www.adobe.com/es/products/premiere-rush.html |
| 432 | PicsArt Gold | Ahora se llama **Picsart Pro** | https://picsart.com/pricing |

## Resumen de dominios y logos a corregir

- **436 YouCam Perfect**: `youcam.com` es una web para adultos. Hay que cambiarlo a `perfectcorp.com`, también en el cancelUrl. Es urgente.
- **429 Splice**: `splice.com` es otra empresa (samples de música). Hay que cambiarlo a `spliceapp.com`.
- Dominios que redirigen:
  - `pokemongolive.com` → `pokemongo.com`
  - `getmimo.com` → `mimo.org`
  - `frontendmasters.com` → `master.dev`
  - `ubisoftplus.com` → `store.ubi.com` (usar `ubisoft.com`)
  - `theathletic.com` → `nytimes.com/athletic`
- Favicons pequeños (logo borroso o dudoso):
  - economist.com (330 B)
  - blinkist.com (419 B)
  - amazon.com (476 B)
  - humblebundle.com (527 B)
  - readwise.io (655 B)
  - medium.com (679 B)
  - khanacademy.org (750 B)
  - brilliant.org (802 B)
- Logo de la empresa matriz en lugar del producto:
  - brawlstars.com, clashroyale.com y supercell.com devuelven el mismo icono de Supercell (1891 B).
  - apple.com (Apple Arcade) y amazon.com (Kindle) muestran el logo de la marca matriz.
