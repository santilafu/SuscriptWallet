# Revisión de precios del catálogo: streaming, música y prueba

Fecha de revisión: 2026-11-15. Fichero revisado: `src/main/kotlin/com/subia/service/CatalogService.kt`.
Precios en EUR con IVA incluido para España. Solo es una investigación: no se ha tocado código.

## Cómo leer la tabla

- **Precio verificado**: precio mensual actual en España. Si pone "sin verificar", se mantiene el precio actual porque no hay fuente fiable.
- **Precio anual verificado**: valor para `priceAnnual` (pago anual único). Si solo hay "compromiso de 12 meses con pago mensual", se indica aparte.
- **Logo**: resultado de `https://icon.horse/icon/<domain>` (IH) y `https://www.google.com/s2/favicons?domain=<domain>&sz=128` (G), en bytes. Google responde 301 y, al seguir la redirección, 200. Marco **logo dudoso** si IH devuelve menos de unos 700 bytes, porque suele ser un favicon genérico o diminuto.
- La consulta a icon.horse se hizo con pausas porque daba 429 (límite de peticiones). Todas las peticiones repetidas devolvieron 200 image/png.

## Tabla

| Línea | Nombre actual | Categoría | Precio actual | Ciclo | Precio verificado | Precio anual verificado | ¿Cambia? | Fuente (URL) | Confianza (alta/media/baja) | Domain OK / logo | Notas |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 64 | Netflix Estándar | streaming | 14.99 | MONTHLY | 14.99 | no existe | No | https://help.netflix.com/es-es/node/24926 (página oficial, consultada 2026-11-15) | alta | netflix.com OK / IH 1519 B, G 1468 B | Subida a 14,99 € desde el 20-04-2026. Miembro extra: 5,99 € con anuncios y 6,99 € sin anuncios. |
| 65 | Netflix Premium | streaming | 21.99 | MONTHLY | 21.99 | no existe | No | https://help.netflix.com/es-es/node/24926 | alta | netflix.com OK | — |
| 66 | Netflix con anuncios | streaming | 8.99 | MONTHLY | 8.99 | no existe | No | https://help.netflix.com/es-es/node/24926 | alta | netflix.com OK | El nombre oficial es "Estándar con anuncios". El plan Básico ya no se ofrece. |
| 67 | Disney+ | streaming | 10.99 | MONTHLY | 10.99 | 109.90 | No | https://www.disneyplus.com/es-es (oficial, 2026-11-15) | alta | disneyplus.com OK / IH 68771 B | Hasta el 24-11-2026 hay una promo de 2 meses a 7,99 €. No hay que cambiar el precio base. |
| 68 | Disney+ con anuncios | streaming | 6.99 | MONTHLY | 6.99 | no existe | No | https://www.disneyplus.com/es-es | alta | disneyplus.com OK | Promo temporal de 4,99 €. |
| 69 | Disney+ Premium | streaming | 15.99 | MONTHLY | 15.99 | 159.90 | No | https://www.disneyplus.com/es-es | alta | disneyplus.com OK | — |
| 70 | Amazon Prime | streaming | 4.99 | MONTHLY | 4.99 | 49.90 | No | https://hipertextual.com/cine-television/amazon-prime-video-sin-anuncios-aumento-precio-2026/ (13-03-2026) y declaraciones de Amazon a Europa Press antes del Prime Day 2026 | media | **Cambiar a amazon.es**. **Logo dudoso**: IH 476 B y G 658 B (favicon genérico de la "a"). primevideo.com da IH 2958 B. | La subida de Prime Video en EE. UU. ("Prime Video Ultra") no se aplica en España. |
| 71 | YouTube Premium | streaming | 13.99 | MONTHLY | **15.99** | no existe | **Sí (+2,00)** | https://www.avpasion.com/youtube-premium-subir-de-precio-plan-familiar-30/ (21-08-2026); https://quasa.io/es/media/youtube-premium-sube-a-15-99-en-espana-la-familia-pagara-29-99 (26-08-2026) | media-alta | youtube.com OK / IH 3081 B, G 794 B | Altas nuevas desde el 21-08-2026; suscriptores actuales desde octubre de 2026. Por App Store cuesta 20,99 €. |
| 72 | Apple TV+ | streaming | 9.99 | MONTHLY | 9.99 | no existe | No (nombre sí) | https://www.apple.com/es/apple-tv/ (oficial, 2026-11-15) | alta | apple.com da el logo genérico de Apple (IH 2187 B). **Mejor tv.apple.com** (IH 4826 B, G 3217 B). | Apple ha quitado el "+": ahora se llama "Apple TV". Prueba gratuita de 7 días confirmada. |
| 73 | Max | streaming | 6.99 | MONTHLY | 6.99 | 69.90 | No (nombre/dominio sí) | https://www.hbomax.com/es/es (oficial, 2026-11-15) | alta | **Cambiar a hbomax.com** (IH 2735 B, igual que max.com, que redirige). Hay que actualizar también el cancelUrl. | El servicio vuelve a llamarse "HBO Max" desde julio de 2025. Plan oficial: "Básico con anuncios". |
| 74 | Max sin anuncios | streaming | 10.99 | MONTHLY | 10.99 | 109.00 | No (nombre/dominio sí) | https://www.hbomax.com/es/es | alta | hbomax.com | Renombrar a "HBO Max Estándar". |
| 75 | Max Premium | streaming | 15.99 | MONTHLY | 15.99 | 159.00 | No (nombre/dominio sí) | https://www.hbomax.com/es/es | alta | hbomax.com | Renombrar a "HBO Max Premium". |
| 76 | Crunchyroll Mega Fan | streaming | 7.49 | MONTHLY | 7.49 | 59.99 | No | https://www.xatakamovil.com/streaming/crunchyroll-acaba-anunciar-inevitable-subida-precio-sus-suscripciones-ves-mucho-anime-facil-elegir (04-02-2026) | media | crunchyroll.com OK / IH 11311 B | La web oficial no se pudo leer (se carga con JS). |
| 77 | DAZN | streaming | 25.99 | MONTHLY | 25.99 | 285.99 | No | https://roams.es/actualidad/telefonia/dazn-baja-futbol-premium-precios-ver-laliga-competiciones/ (temporada 2026/27) | media | dazn.com OK / IH 6218 B | 25,99 € es la cuota mensual con compromiso anual. Sin permanencia cuesta 44,99 €/mes (ver planes que faltan). |
| 78 | DAZN Fútbol | streaming | 14.99 | MONTHLY | 14.99 | 164.99 | No | misma de roams.es; https://www.advanced-television.com/2026/08/19/spain-pay-tv-football-price-war-continues/ | media | dazn.com OK | Con compromiso anual. Sin permanencia: 29,99 €/mes. |
| 79 | Movistar+ | streaming | 9.99 | MONTHLY | 9.99 | 99.90 | No | https://www.movistarplus.es/ (oficial, 2026-11-15) | alta | movistar.es funciona (IH 5968 B), pero el dominio del servicio es **movistarplus.es** (IH 3184 B). Recomiendo movistarplus.es. | Nombre oficial: "Plan Libre: Cine, Series y Deportes", sin permanencia. El anual de 99,90 € está confirmado. |
| 80 | Filmin | streaming | 9.99 | MONTHLY | 9.99 | 99.00 | No | https://roams.es/actualidad/telefonia/filmin-sube-el-precio-de-su-plan-anual-esto-es-lo-que-pagaras-a-partir-de-ahora/; https://www.diariodeleon.es/cultura/251205/2068562/plataformas-streaming-espanolas-precios-catalogo.html (act. 06-01-2026) | media | filmin.es OK / IH 1064 B (pequeño, revisar a ojo) | Las promos de verano (69 € al año) caducaron el 03-08-2026. |
| 81 | Atresplayer Premium | streaming | 4.99 | MONTHLY | sin verificar (se deja 4.99) | sin verificar (se deja 49.99) | ? | https://www.diariodeleon.es/... (06-01-2026: 4,99 €/mes, 49,99 €/año) frente a una guía de GamsGo de 2026: con anuncios 5,99 €, sin anuncios 7,99 € y familiar 9,99 € | baja | atresplayer.com OK / IH 7067 B | Las fuentes se contradicen y la web oficial se carga con JS. Un hilo de Bandaancha habla de una subida en la que el plan individual pasa a tener anuncios. **Revisar a mano en atresplayer.com.** |
| 82 | SkyShowtime | streaming | 8.99 | MONTHLY | 8.99 | **no hay pago anual único**. Con compromiso de 12 meses: 6,69 €/mes (80,28 €/año) | **Sí (anual)** | https://www.skyshowtime.com/es (oficial, 2026-11-15) | alta | skyshowtime.com OK / IH 35787 B | El `priceAnnual` 62.99 es incorrecto: propongo 80.28 o null. El nombre actual es correcto ("Estándar" sin anuncios). |
| 83 | Paramount+ | streaming | 7.99 | MONTHLY | no se vende suelto en España | — | **Sí (eliminar o marcar)** | https://roams.es/actualidad/telefonia/tres-futuros-posibles-skyshowtime-espana-fusion-paramount-warner/ (2026) | media | paramountplus.com / IH 2301 B | En España su contenido llega por SkyShowtime y por Movistar Plus+. No he encontrado ninguna suscripción independiente con precio propio. |
| 84 | Twitch Turbo | streaming | 8.99 | MONTHLY | **11.99** | no existe | **Sí (+3,00)** | https://alternativeto.net/news/2023/5/twitch-raises-subscription-price-for-twitch-turbo-in-multiple-countries; https://www.slashgear.com/1297157/ad-free-twitch-turbo-subscription-price-hike | media | twitch.tv OK / IH 1314 B, G 336 B | Precio en euros (incluida España) desde mayo de 2023. No he encontrado cambios posteriores. |
| 85 | Apple One | streaming | 19.95 | MONTHLY | 19.95 | no existe | No | https://www.apple.com/es/apple-one/ (oficial, 2026-11-15) | alta | apple.com OK (aquí el logo de Apple tiene sentido) | Plan Individual. En la descripción, cambiar "Apple TV+" por "Apple TV". Faltan Familiar y Premium. |
| 86 | Mubi | streaming | 13.99 | MONTHLY | **11.99** | **95.88** | **Sí (−2,00)** | https://mubi.com/es/es/memberships (oficial, 2026-11-15) | media | mubi.com OK / IH 8158 B | Plan único "MUBI Classic". Comparaiso da 9,99 € (sin fecha). Prueba de 7 días correcta. |
| 87 | Plex Pass | streaming | 4.99 | MONTHLY | sin verificar en EUR (se deja 4.99). En USD: 6,99/mes y 69,99/año | sin verificar | ? | https://www.plex.tv/es/plans/ (no carga precios); https://www.plex.tv/nl/blog/new-lifetime-plex-pass-pricing/ (2026) | baja | plex.tv. **Logo dudoso**: IH 338 B, G 880 B. | Pase vitalicio a 749,99 USD desde el 01-07-2026. El precio actual (4,99) parece antiguo. |
| 88 | Discovery+ | streaming | 3.99 | MONTHLY | **ya no existe en España** | — | **Sí (eliminar)** | https://en.wikipedia.org/wiki/Discovery%2B (cerró en España el 09-12-2024) | alta | discoveryplus.com | El contenido pasó a Max/HBO Max. |
| 89 | Hulu | streaming | 7.99 | MONTHLY | no disponible en España | — | **Sí (eliminar)** | conocimiento general: Hulu solo opera en EE. UU. y Japón | media | hulu.com / IH 2214 B | El precio en euros es inventado. |
| 90 | Peacock Premium | streaming | 7.99 | MONTHLY | no disponible en España | — | **Sí (eliminar)** | conocimiento general: en Europa su contenido llega por SkyShowtime | media | peacocktv.com / IH 6705 B | — |
| 91 | ESPN+ | streaming | 10.99 | MONTHLY | no disponible en España | — | **Sí (eliminar)** | conocimiento general (servicio solo de EE. UU.) | media | espnplus.com redirige; el dominio real es plus.espn.com (IH 1475 B) | — |
| 92 | BritBox | streaming | 6.99 | MONTHLY | no disponible en España | — | **Sí (eliminar)** | https://rxtvinfo.com/2024/britbox-international-sold-to-bbc-what-next-for-britbox/ (opera en pocos países: EE. UU., Canadá, Australia, nórdicos…) | media | britbox.com / IH 7832 B | — |
| 93 | Shudder | streaming | 5.99 | MONTHLY | no disponible en España | — | **Sí (eliminar)** | conocimiento general (EE. UU., Canadá, Reino Unido, Irlanda, Australia y Nueva Zelanda) | baja-media | shudder.com / IH 5818 B | No he encontrado fuente explícita de 2026. |
| 94 | Nebula | streaming | 5.00 | MONTHLY | sin verificar en EUR. Cobra en USD: 6 $/mes | 60 $/año | ? | https://www.thepricer.org/how-much-does-nebula-cost/; https://macaron.im/playbook/reviews/nebula-review (nov-2026) | baja | nebula.tv / IH 5489 B | Servicio global en USD. Si se mantiene, la moneda debería ser USD 6.00 y el anual 60.00. |
| 95 | CuriosityStream | streaming | 4.99 | MONTHLY | sin verificar en EUR. 4,99 $/mes | 39,99 $/año | ? | https://selectra.com.au/streaming/providers/curiosity-stream; https://subger.com/en/service/curiositystream | baja | curiositystream.com / IH 32956 B | Cobra en USD. |
| 96 | Canal+ | streaming | 26.99 | MONTHLY | **no existe en España** | — | **Sí (eliminar)** | búsqueda: Canal+ España se fusionó en Movistar Plus+ (2015); canalplus.com es el Canal+ francés | media-alta | canalplus.com (francés) / IH 3855 B | Ese precio no corresponde a nada vendible en España. |
| 97 | Viki Pass Standard | streaming | 4.99 | MONTHLY | sin verificar (se deja 4.99) | — | ? | https://subger.com/es/service/viki-rakuten (referencia: 5,99 $ Standard y 9,99 $ Plus; datos contradictorios) | baja | viki.com OK / IH 4089 B | — |
| 98 | FloSports | streaming | 29.99 | MONTHLY | sin verificar (servicio de EE. UU.) | — | ? | — | baja | flosports.tv / IH 2665 B, G 658 B | No tiene oferta en España. Propongo eliminarlo. |
| 103 | Spotify Premium | musica | 11.99 | MONTHLY | 11.99 | no existe | No | https://www.spotify.com/es/premium/ (oficial, 2026-11-15) | alta | spotify.com OK / IH 13360 B | No hay pago anual. |
| 104 | Spotify Premium Duo | musica | 16.99 | MONTHLY | 16.99 | no existe | No | https://www.spotify.com/es/premium/ | alta | spotify.com OK | — |
| 105 | Spotify Premium Familiar | musica | 20.99 | MONTHLY | 20.99 | no existe | No | https://www.spotify.com/es/premium/ | alta | spotify.com OK | — |
| 106 | Spotify Premium Estudiantes | musica | 6.49 | MONTHLY | 6.49 | no existe | No | https://www.spotify.com/es/premium/ | alta | spotify.com OK | — |
| 107 | Apple Music | musica | 10.99 | MONTHLY | **11.99** | no existe | **Sí (+1,00)** | https://www.apple.com/es/apple-music/ (oficial, 2026-11-15) | alta | apple.com da el logo genérico. **Mejor music.apple.com** (IH 7649 B, G 2087 B). | Faltan Familiar (19,99 €) y Estudiante (6,99 €). |
| 108 | Tidal HiFi | musica | 10.99 | MONTHLY | **12.99** | sin verificar | **Sí (+2,00)** | https://basic-tutorials.com/news/tidal-price-increase-in-2026-individual-subscription-to-cost-12-99-euros-starting-in-august/ (subida en Europa desde el 03-08-2026) | media | tidal.com. **Logo dudoso**: IH 635 B, G 345 B. | El plan "HiFi" ya no existe: desde 2024 se llama "TIDAL Individual". La web oficial devuelve 403. |
| 109 | Amazon Music Unlimited | musica | 10.99 | MONTHLY | **12.99** | **129.99** | **Sí (+2,00)** | https://www.elgrupoinformatico.com/plataformas-streaming/amazon-music-unlimited-sube-precio-espana/ (04-02-2026) | media-alta | **Cambiar a music.amazon.es** (IH 36633 B, G 1517 B). amazon.com da un logo dudoso (476 B). | El precio es el mismo con o sin Prime. Por App Store cuesta 13,99 €. |
| 110 | YouTube Music Premium | musica | 10.99 | MONTHLY | sin verificar (se deja 10.99) | no existe | ? | Ninguna fuente española de 2026 da el precio. En EE. UU. pasó de 10,99 $ a 11,99 $ en abril de 2026 (https://www.elespanol.com/elandroidelibre/noticias-y-novedades/20260410/...) | baja | youtube.com funciona. **Mejor music.youtube.com** (IH 3567 B, G 1489 B). | Es probable que también subiera con YouTube Premium en agosto de 2026. Revisar a mano. |
| 111 | Deezer Premium | musica | 11.99 | MONTHLY | 11.99 | **107.99** | Sí (añadir anual) | https://www.deezer.com/es/offers (oficial, 2026-11-15) | alta | deezer.com OK / IH 3695 B | Faltan Duo, Family y Student. |
| 112 | SoundCloud Next Pro | musica | 9.99 | MONTHLY | sin verificar (se deja 9.99) | — | ? (nombre sí) | https://help.soundcloud.com/hc/en-us/articles/360051691914 (subida de Go+ desde marzo de 2026, importe no publicado); https://techcrunch.com/2024/12/17/soundcloud-introduces-a-new-cheaper-paid-plan-for-artists | baja | soundcloud.com OK / IH 18781 B | "Next Pro" ahora se llama "Artist Pro" y es un plan para creadores (8,25 $/mes con pago anual). La descripción ("escucha sin anuncios") corresponde a **SoundCloud Go+**. Propongo renombrar la fila a Go+. |
| 113 | Qobuz Sublime | musica | 14.99 | MONTHLY | **16.66 €/mes equivalente; solo pago anual** | ≈199.92 | **Sí** | https://www.qobuz.com/es-es/music/streaming/offers (oficial, "desde 16,66 €/mes"); https://europeanstack.pages.dev/software/qobuz (199,92 €/año) | media | qobuz.com OK / IH 6638 B | Sublime solo se vende con pago anual. Propongo ciclo YEARLY con 199.92, o mantener el equivalente mensual de 16.66 y priceAnnual 199.92. |
| 114 | Napster Premium | musica | 9.99 | MONTHLY | **ya no existe** | — | **Sí (eliminar)** | https://www.musicinafrica.net/node/380360; https://dynamoi.com/ko/news/2026-01-06-napster-ends-streaming-era-to-launch-ai-kiosks-after-207m-sa (streaming cerrado entre finales de 2025 y enero de 2026) | media-alta | napster.com / IH 3920 B | Ahora es "Napster AI" (en USD, sin oferta en España). |
| 115 | Pandora Premium | musica | 9.99 | MONTHLY | no disponible en España | — | **Sí (eliminar)** | conocimiento general (solo EE. UU.) | alta | pandora.com / IH 23662 B | — |
| 116 | Beatport Link | musica | 9.99 | MONTHLY | sin verificar en EUR. USD: Essential 10,99 $, Advanced 15,99 $, Professional 29,99 $ | USD 109 / 159 / 299 | ? (nombre sí) | https://www.cloudspress.com/beatport-pricing-subscriptions-and-track-purchase-faq-2026/ | baja | beatport.com. **Logo dudoso**: IH 597 B. | "Beatport Link" ahora se llama "Beatport Streaming". |
| 117 | LiveOne Premium | musica | 9.99 | MONTHLY | no disponible en España | — | **Sí (eliminar)** | conocimiento general (servicio de EE. UU.) | media | liveone.com / IH 6206 B | — |
| 118 | Mixcloud Select | musica | 4.99 | MONTHLY | sin verificar (cada creador fija el precio, desde unos 2,99) | — | ? | https://techcrunch.com/?p=1753834; https://subger.com/en/service/mixcloud-select | baja | mixcloud.com OK (IH 3891 B, G 1092 B) | Es una suscripción a un canal concreto, no un plan único. Existe también "Mixcloud Premium". |
| 119 | iHeart Radio All Access | musica | 9.99 | MONTHLY | no disponible en España | — | **Sí (eliminar)** | conocimiento general (solo EE. UU.) | media | iheart.com / IH 12322 B | — |
| 120 | Resso Premium | musica | 2.99 | MONTHLY | **ya no existe** | — | **Sí (eliminar)** | https://techcrunch.com/2024/01/11/bytedance-is-shutting-down-its-music-streaming-service-resso-in-india; https://www.socialmediatoday.com/news/tiktok-announces-shut-down-of-separate-tiktok-music-app/727985/ | alta | **Domain incorrecto**: resso.com.br; Google devuelve 404 | Nunca estuvo disponible en España. Cerró en Brasil e Indonesia en 2023, en India en enero de 2024, y su sucesor TikTok Music cerró el 28-11-2024. |
| 121 | Pocket Casts Plus | musica | 3.99 | MONTHLY | sin verificar en EUR. USD: 3,99 $/mes | 39,99 $ | ? | https://support.pocketcasts.com/knowledge-base/pocket-casts-plus-pricing/ | media-baja | pocketcasts.com OK / IH 19129 B | Cobra en USD. Si se mantiene, añadir priceAnnual 39.99. |
| 316 | Netflix Estándar | prueba | 14.99 | MONTHLY | 14.99 (copia de la L64) | — | No | igual que L64 | alta | netflix.com OK | trialDays=30 es dudoso: Netflix no ofrece prueba gratuita en España. |
| 317 | Spotify Premium | prueba | 11.99 | MONTHLY | 11.99 (copia de la L103) | — | No | igual que L103 | alta | spotify.com OK | Prueba de 1 mes confirmada en la web oficial. |
| 318 | Amazon Prime | prueba | 4.99 | MONTHLY | 4.99 (copia de la L70) | 49.90 | No (falta priceAnnual) | igual que L70 | media | **amazon.es** / logo dudoso | La prueba de 30 días es correcta. |
| 319 | Disney+ | prueba | 10.99 | MONTHLY | 10.99 (copia de la L67) | 109.90 | No (falta priceAnnual) | igual que L67 | alta | disneyplus.com OK | trialDays=7 es dudoso: Disney+ España no tiene prueba gratuita, solo promociones. |
| 320 | Apple TV+ | prueba | 9.99 | MONTHLY | 9.99 (copia de la L72) | — | No (nombre sí) | igual que L72 | alta | tv.apple.com recomendado | Prueba de 7 días confirmada. Renombrar a "Apple TV". |
| 321 | Cursor Pro | prueba | 20.00 | MONTHLY | copia de la L42 (categoría `ia`). Precio original 20.00 | — | — | fuera de mi grupo; no lo he verificado | — | cursor.com | Debe coincidir con lo que verifique el grupo de IA. |
| 322 | Adobe Creative Cloud | prueba | 79.30 | MONTHLY | copia de la L128 (`software`). Precio original 79.30 | — | — | fuera de mi grupo | — | adobe.com | Igual que la L128. |
| 323 | YouTube Premium | prueba | 13.99 | MONTHLY | **15.99** (copia de la L71) | — | **Sí (+2,00)** | igual que L71 | media-alta | youtube.com OK | — |
| 324 | Notion Plus | prueba | 10.00 | MONTHLY | copia de la L131 (`software`). Precio original 10.00 | — | — | fuera de mi grupo | — | notion.so | Igual que la L131. |
| 325 | Duolingo Super | prueba | 8.49 | MONTHLY | copia de la L411 (`educacion`). Precio original 8.49 | — | — | fuera de mi grupo | — | duolingo.com | Igual que la L411. |
| 326 | NordVPN | prueba | 15.99 | MONTHLY | copia de la L228 (`seguridad`). Precio original 15.99 | — | — | fuera de mi grupo | — | nordvpn.com | Igual que la L228. |
| 327 | Grammarly Premium | prueba | 12.00 | MONTHLY | copia de la L138 (`software`). Precio original 12.00 | — | — | fuera de mi grupo | — | grammarly.com | Igual que la L138. |
| 328 | Apple One | prueba | 19.95 | MONTHLY | 19.95 (copia de la L85) | — | No | igual que L85 | alta | apple.com | Cambiar "Apple TV+" por "Apple TV" en la descripción. |
| 329 | LinkedIn Premium | prueba | 39.90 | MONTHLY | copia de la L140 (`software`). Precio original 39.90 | — | — | fuera de mi grupo | — | linkedin.com | Igual que la L140. |
| 330 | Discovery+ | prueba | 3.99 | MONTHLY | **servicio extinto** (copia de la L88) | — | **Sí (eliminar)** | igual que L88 | alta | discoveryplus.com | Eliminarla junto con la L88. |
| 331 | GeForce Now Priority | prueba | 10.99 | MONTHLY | copia de la L210 (`gaming`). Precio original 10.99 | — | — | fuera de mi grupo | — | nvidia.com | Igual que la L210. |
| 332 | Keeper Security | prueba | 3.99 | MONTHLY | copia de la L238 (`seguridad`). Precio original 3.99 | — | — | fuera de mi grupo | — | keepersecurity.com | Igual que la L238. |
| 333 | Medium | prueba | 5.00 | MONTHLY | copia de la L261 (`noticias`). Precio original 5.00 | — | — | fuera de mi grupo | — | medium.com | Igual que la L261. |

Nota sobre `prueba`: las 18 filas son copias de filas de otras categorías. Solo cambian `categoryKey`, `trialDays` y que pierden `priceAnnual`. Lo mejor sería que se generen a partir de la fila original (referencia o lookup por nombre + domain) para que los precios no se desincronicen.

## Planes que faltan

| Nombre propuesto | Precio | Ciclo | priceAnnual | Domain | Fuente | Confianza |
|---|---|---|---|---|---|---|
| Prime Video sin anuncios (suplemento sobre Prime) | 1.99 | MONTHLY | — | primevideo.com | https://hipertextual.com/cine-television/amazon-prime-video-sin-anuncios-aumento-precio-2026/ (13-03-2026). Otra fuente dice 2,99 €. | media |
| YouTube Premium Familiar | 29.99 | MONTHLY | — | youtube.com | https://www.avpasion.com/youtube-premium-subir-de-precio-plan-familiar-30/ (21-08-2026) | media-alta |
| YouTube Premium Estudiantes | 10.49 | MONTHLY | — | youtube.com | misma | media-alta |
| YouTube Premium Lite | 8.99 | MONTHLY | — | youtube.com | https://quasa.io/es/media/youtube-premium-sube-a-15-99-en-espana-la-familia-pagara-29-99 y un hilo de Bandaancha (de 7,99 a 8,99 €) | media |
| HBO Max + DAZN (pack) | 44.99 | MONTHLY | — | hbomax.com | https://www.hbomax.com/es/es | alta |
| Crunchyroll Fan | 5.99 | MONTHLY | 47.99 | crunchyroll.com | https://www.xatakamovil.com/streaming/crunchyroll-acaba-anunciar-inevitable-subida-precio-sus-suscripciones-ves-mucho-anime-facil-elegir (04-02-2026) | media |
| DAZN Premium (mensual sin permanencia) | 44.99 | MONTHLY | — | dazn.com | https://roams.es/actualidad/telefonia/dazn-baja-futbol-premium-precios-ver-laliga-competiciones/ | media |
| DAZN Fútbol (mensual sin permanencia) | 29.99 | MONTHLY | — | dazn.com | misma | media |
| Movistar Plus+ Cine y Series | 4.99 | MONTHLY | — | movistarplus.es | https://www.movistarplus.es/ (oficial) | alta |
| SkyShowtime Estándar con anuncios | 5.99 | MONTHLY | — | skyshowtime.com | https://roams.es/actualidad/telefonia/skyshowtime-rebaja-planes-sin-anuncios-12-octubre/ (14-09-2026) | media |
| SkyShowtime Prémium | 13.99 | MONTHLY | (compromiso de 12 meses: 10,49 €/mes, 125,88 €) | skyshowtime.com | https://www.skyshowtime.com/es (oficial) | alta |
| Apple One Familiar | 27.95 | MONTHLY | — | apple.com | https://www.apple.com/es/apple-one/ (oficial) | alta |
| Apple One Premium | 36.95 | MONTHLY | — | apple.com | https://www.apple.com/es/apple-one/ | alta |
| Apple Music Familiar | 19.99 | MONTHLY | — | music.apple.com | https://www.apple.com/es/apple-music/ (oficial) | alta |
| Apple Music Estudiantes | 6.99 | MONTHLY | — | music.apple.com | https://www.apple.com/es/apple-music/ | alta |
| Amazon Music Unlimited Familiar | 22.99 | MONTHLY | 230.00 | music.amazon.es | https://www.elgrupoinformatico.com/plataformas-streaming/amazon-music-unlimited-sube-precio-espana/ (04-02-2026) | media-alta |
| TIDAL Familiar | 21.99 | MONTHLY | — | tidal.com | https://basic-tutorials.com/news/tidal-price-increase-in-2026-individual-subscription-to-cost-12-99-euros-starting-in-august/ | media |
| Deezer Duo | 15.99 | MONTHLY | 174.99 | deezer.com | https://www.deezer.com/es/offers (oficial) | alta |
| Deezer Family | 19.99 | MONTHLY | 218.99 | deezer.com | https://www.deezer.com/es/offers | alta |
| Deezer Student | 5.99 | MONTHLY | — | deezer.com | https://www.deezer.com/es/offers | alta |
| Qobuz Studio | desde 12.49 (equivalente con pago anual; el mensual puro no lo he verificado) | — | sin verificar | qobuz.com | https://www.qobuz.com/es-es/music/streaming/offers (oficial) | media-baja |
| Qobuz Studio Family | 20.83 | MONTHLY | — | qobuz.com | misma | media |
| mitele PLUS (opcional, plataforma española) | 5.99 | MONTHLY | 48.00 | mitele.es | https://www.diariodeleon.es/cultura/251205/2068562/plataformas-streaming-espanolas-precios-catalogo.html (act. 06-01-2026) | media-baja |
| FlixOlé (opcional, plataforma española) | 4.99 | MONTHLY | 49.99 | flixole.com | misma | media-baja |

Variantes de Atresplayer (con anuncios, sin anuncios, familiar): no las añado hasta confirmar precios en la web oficial (ver L81). Tampoco añado Crunchyroll Ultimate Fan: no hay fuente fiable para España.

## Planes que ya no existen (o no se venden en España)

| Línea(s) | Plan | Situación | Acción propuesta |
|---|---|---|---|
| 88, 330 | Discovery+ | Cerró en España el 09-12-2024; su contenido pasó a HBO Max | Eliminar |
| 114 | Napster Premium | El streaming cerró entre finales de 2025 y enero de 2026; ahora es Napster AI | Eliminar |
| 120 | Resso Premium | Cerrado y nunca disponible en España (TikTok Music cerró el 28-11-2024) | Eliminar |
| 96 | Canal+ | No existe en España: Canal+ España se integró en Movistar Plus+ y canalplus.com es el francés | Eliminar |
| 83 | Paramount+ | Sin suscripción independiente en España (llega por SkyShowtime y Movistar Plus+) | Eliminar o dejar sin precio |
| 89, 90, 91, 92, 93 | Hulu, Peacock Premium, ESPN+, BritBox, Shudder | No disponibles en España | Eliminar |
| 115, 117, 119 | Pandora Premium, LiveOne, iHeart All Access | Solo en EE. UU. | Eliminar |
| 98 | FloSports | Servicio de EE. UU. sin oferta para España | Eliminar (o pasarlo a USD) |
| 66 | (Netflix Básico, que ya no está en el catálogo) | Discontinuado | Nada que hacer |

Planes que siguen existiendo con otro nombre:
- Max pasa a llamarse **HBO Max** (L73-75; dominio hbomax.com).
- Apple TV+ pasa a llamarse **Apple TV** (L72, L320, y las descripciones de L85 y L328).
- Tidal HiFi pasa a llamarse **TIDAL Individual** (L108).
- SoundCloud Next Pro pasa a llamarse **Artist Pro**, que es un plan para creadores. La fila describe Go+ (L112).
- Beatport Link pasa a llamarse **Beatport Streaming** (L116).
- El plan de Movistar+ se llama ahora **Plan Libre** (L79).

Servicios que cobran en USD y no en EUR: Nebula (L94), CuriosityStream (L95), Pocket Casts (L121), Plex (L87), Beatport (L116). Si se mantienen, conviene usar la moneda USD real en lugar de inventar un precio en euros.

## Logos dudosos

| Domain | icon.horse | Google s2 | Propuesta |
|---|---|---|---|
| amazon.com / amazon.es (L70, L318) | 476 B | 658 B | Usar amazon.es. Si se quiere el logo de Prime Video, primevideo.com (2958 B). |
| amazon.com (L109) | 476 B | 658 B | music.amazon.es (36633 B) |
| plex.tv (L87) | 338 B | 880 B | Revisar a ojo o usar iconUrl |
| tidal.com (L108) | 635 B | 345 B | Revisar a ojo o usar iconUrl |
| beatport.com (L116) | 597 B | 856 B | Revisar a ojo |
| resso.com.br (L120) | 2615 B | 404 | Dominio incorrecto; eliminar la fila |
| apple.com para Apple TV+ y Apple Music (L72, L107, L320) | 2187 B (logo genérico de Apple) | 2468 B | tv.apple.com (4826 B) y music.apple.com |
| youtube.com para YouTube Music (L110) | 3081 B (logo de YouTube) | 794 B | music.youtube.com (3567 B) |
| filmin.es (L80) | 1064 B | 1126 B | Probablemente correcto; revisar a ojo |
| max.com (L73-75) | 2735 B | 1316 B | Funciona, pero cambiar a hbomax.com |
| espnplus.com (L91) | 5484 B | 704 B | Fila a eliminar; el dominio real es plus.espn.com |

Última pasada de icon.horse: mixcloud.com 3891 B (OK), music.apple.com 7649 B (OK) y resso.com.br / resso.com 2615 B. Icon.horse sí devuelve imagen para Resso, pero Google s2 da 404 y el servicio está cerrado, así que la fila hay que eliminarla igualmente.
