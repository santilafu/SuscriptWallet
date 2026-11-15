# Segunda pasada (B): filas sin verificar o de confianza baja

> Nota (2026-11-15): los precios con fuente App Store/iOS NO se aplicaron; la app es solo Android. Ver tercera-pasada-web.md.

Fecha: 2026-11-15. Informes de origen: `gaming-noticias-educacion-creatividad.md`, `citas-transporte-telecos-finanzas.md` y `seguros-hogar-salud.md`.
No se ha modificado código. Las líneas son las que dan los informes de la primera pasada; son aproximadas porque `CatalogService.kt` se está editando en paralelo.

## Método y avisos

- No había búsquedas web disponibles. Solo se leyeron páginas públicas con WebFetch.
- **Fuente principal: la ficha de la App Store de España** (`https://apps.apple.com/es/app/id<ID>`, sección "Compras dentro de la app"). Los IDs se comprobaron con la API pública `itunes.apple.com/search` o `lookup` (`country=es`).
- Los precios de la App Store de España **están en euros con IVA incluido**.
- **Limitaciones de la App Store:**
  - La ficha muestra como mucho 10 compras.
  - A menudo no indica la duración de cada una.
  - Puede mezclar precios antiguos, promociones y planes regionales.
  - Por eso, cuando la compra no dice si es mensual o anual, la confianza baja a "baja" o "media".
  - El precio dentro de la app (iOS) puede ser más alto que el de la web, porque Apple cobra comisión.
- Se respetaron los bloqueos: 403, dominios que la herramienta no puede leer y páginas vacías. No se intentó saltar ninguno.
  - Bloqueados o vacíos: calm.com, nytimes.com, store.wsj.com, lavanguardia.com, abo.lemonde.fr, udemy.com, support.humblebundle.com, synergym.es, cosalud.es.
  - canaldeisabelsegunda.es dio un error de certificado.
- **Confianza:**
  - **alta**: web oficial con precio claro, o App Store con la duración indicada y sin ambigüedad.
  - **media**: App Store con nombre del plan pero con varias tarifas, o web oficial con alguna incoherencia.
  - **baja**: precios sin duración o contradictorios.

## Tabla

| Línea (aprox.) | Nombre | Categoría | Precio en catálogo | Precio verificado | Ciclo | Precio anual | Moneda/IVA | Fuente (URL) | Confianza | Notas |
|---|---|---|---|---|---|---|---|---|---|---|
| 209 | Humble Choice | gaming | 11.99 | sin verificar | MONTHLY | — | — | humblebundle.com y support.humblebundle.com bloquean la lectura; no tiene app iOS | — | Se mantiene sin verificar. |
| 215 | Roblox Premium 1000 | gaming | 9.99 | **9,99 €** | MONTHLY | — | EUR, IVA incl. | https://apps.apple.com/es/app/id431946152 | media | La App Store ES sigue mostrando "Roblox Premium 1000 One Month 9,99 €", además de Premium 450 a 5,99 € y Premium 2200 a 22,99 €. El precio del catálogo coincide. La primera pasada indicaba que no admite altas nuevas desde el 30-04-2026; la ficha puede reflejar planes heredados. |
| 218 | Chess.com Diamond | gaming | 9.99 | 13,99 € (probablemente mensual) | MONTHLY | 99,99 € en iOS (124,99 € en la web, según la primera pasada) | EUR, IVA incl. | https://apps.apple.com/es/app/id329218549 | baja | Diamond aparece a 13,99, 99,99, 71,99 y 8,99 €, sin duración. 8,99 y 71,99 pueden ser promociones. |
| 219 | Clash of Clans Gold Pass | gaming | 4.99 | **5,99 €** (también hay una entrada de 7,99 €) | MONTHLY (por temporada) | — | EUR, IVA incl. | https://apps.apple.com/es/app/id529479190 | media | Hay dos entradas "Gold Pass": 5,99 y 7,99 €. 4,99 € está desfasado. |
| 220 | Pokémon GO Adventure | gaming | 4.99 | sin verificar | MONTHLY | — | — | https://apps.apple.com/es/app/id1094591345 | baja | No aparece ninguna suscripción en las compras de la app. Solo hay Pokémonedas, entradas de evento y "Event Pass 3 Deluxe" (8,99 €). Conviene revisar si este plan existe. |
| 221 | Clash Royale Pass | gaming | 4.99 | **14,99 €** ("Pase de diamante"; también hay una entrada de 11,49 €) | MONTHLY (por temporada) | — | EUR, IVA incl. | https://apps.apple.com/es/app/id1053012308 | media | El pase de temporada ahora figura como "Pase de diamante". Conviene renombrarlo. |
| 222 | Brawl Stars Brawl Pass | gaming | 4.99 | **6,49 €** (Brawl Pass); Brawl Pass Plus 9,99 € | MONTHLY (por temporada) | — | EUR (no USD), IVA incl. | https://apps.apple.com/es/app/id1229016807 | media | También aparecen "Season Pass" a 7,99 €, "Season Pass Plus" a 11,99 € y "Season Pass Basic" a 3,49 €. La primera pasada daba 8,99 USD; en la tienda española el precio está en euros. |
| 255 | Scribd | noticias | 11.99 | **10,99 €** | MONTHLY | — | EUR | https://www.scribd.com/subscribe | alta | Web oficial: 10,99 €/mes tras 30 días gratis. En iOS (app Everand, https://apps.apple.com/es/app/id542557212) hay entradas entre 7,99 y 11,99 € y "Everand Plus" a 14,99 €. |
| 257 | The New York Times | noticias | 17.00 | **11,99 €** (All Access mensual) | MONTHLY | 109,99 € (All Access anual) | EUR, IVA incl. | https://apps.apple.com/es/app/id284862083 | media | Hay entradas "News - Monthly" a 7,99, 11,99 y 12,99 €, y "Basic" a 12,99 y 14,99 €. nytimes.com no se puede leer. |
| 262 | Feedly Pro | noticias | 6.00 | **7,99 €** (también hay una entrada de 8,99 €) | MONTHLY | — | EUR, IVA incl. | https://apps.apple.com/es/app/id396069556 | media | Pro+ cuesta 17,49 €/mes. La web (feedly.com/i/pro) no carga sin JavaScript. |
| 264 | Overcast Premium | noticias | 9.99 | 19,99–22,99 € (sin duración) | YEARLY | 19,99–22,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id888422857 | baja | Hay tres entradas "Overcast Premium": 19,99, 22,99 y 34,99 €. En cualquier caso, 9,99 está muy desfasado. |
| 265 | The Wall Street Journal | noticias | 9.99 | 22,99 € ("WSJ Europe"); otras entradas entre 32,99 y 44,99 € | MONTHLY (probable) | "WSJ Twelve Month Subscription" 43,99 € (incoherente) | EUR, IVA incl. | https://apps.apple.com/es/app/id364387007 | baja | Precios sin duración y contradictorios. store.wsj.com no se puede leer. 9,99 parece bajo. |
| 266 | Financial Times | noticias | 39.99 | **45,00 €** (Standard Digital) | MONTHLY | 429 € (Standard); Premium 69 €/mes, 659 €/año | EUR, IVA incl. | https://apps.apple.com/es/app/id1200842933 | media | FT Edit: 5,49 €/mes y 54 €/año. ft.com bloquea la lectura. |
| 267 | The Economist | noticias | 21.99 | **38,90 €** (Premium mensual) | MONTHLY | 389 € (Premium anual); "One year subscription" 349 € | EUR, IVA incl. | https://apps.apple.com/es/app/id1239397626 | media | En iOS no hay plan digital mensual básico; también aparece "Three month subscription" a 107,99 €. Podcasts: 6,90 €/mes y 69 €/año. 21,99 está desfasado. |
| 268 | La Vanguardia Premium | noticias | 3.49 | **14,99 €** (Premium); Básica/Plus 9,99 € | MONTHLY | 149,99 € (Premium); 99,90 € (Básica) | EUR, IVA incl. | https://apps.apple.com/es/app/id364587804 | media | La web no se puede leer. 3,49 € es, como mucho, una promoción. |
| 270 | El Confidencial Premium | noticias | 2.99 | **8,99 €** ("Acceso mensual sin oferta") | MONTHLY | **88,99 €** ("Acceso anual sin oferta") | EUR, IVA incl. | https://apps.apple.com/es/app/id324458663 | alta | Promociones: 4,99 o 5,99 €/mes y 64,99 €/año. 2,99 € no es el precio normal. |
| 271 | Le Monde Numérique | noticias | 9.99 | **13,99 €** ("Offre Essentiel") | MONTHLY | 152,99 € (Essentiel, probablemente anual) | EUR, IVA incl. | https://apps.apple.com/es/app/id294047850 | media | Otras ofertas: "100% numérique" a 17,99 € y "Duo" a 20,99 €. abo.lemonde.fr no se puede leer. |
| — | El Mundo Premium y otros periódicos españoles | noticias | — | sin verificar | — | — | — | — | — | No dio tiempo a consultarlos. |
| 396 | MasterClass | educacion | 10.00 MONTHLY | **129,99 €/año** (Individual) | YEARLY | 129,99 €; Duo 199,99 €; Family 249,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id1273867416 | media | Solo se vende por años; no hay plan mensual. Conviene cambiar el ciclo a YEARLY. En la web puede costar menos (la primera pasada daba unos 110 €). |
| 397 | Skillshare Premium | educacion | 8.00 | **13,99 €** (Monthly Premium) | MONTHLY | **95,99 €** (Annual Premium) | EUR, IVA incl. | https://apps.apple.com/es/app/id916819843 | media | También hay entradas a 15,99, 24,99 y 59,99 €/mes y 199,99 €/año; probablemente son planes antiguos. skillshare.com bloquea la lectura. |
| 399 | Babbel Premium | educacion | 13.95 | **13,49 €** (1 idioma, 1 mes) | MONTHLY | 83,99 € (1 idioma, 1 año); 95,99 € (todos los idiomas, 12 meses) | EUR, IVA incl. | https://apps.apple.com/es/app/id829587759 | media | Hay plan de 3 meses a 41,99 €. babbel.com/es/prices da 404. |
| 400 | Rosetta Stone | educacion | 11.99 MONTHLY | sin plan mensual: 3 meses 48,99 €; 12 meses 106,99 € (1 idioma) | QUARTERLY / YEARLY | 106,99 € (1 idioma); Unlimited 119,99 €; de por vida 229,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id435588892 | media | App "Rosetta Stone Classic (2024)". No aparece ningún plan mensual. |
| 401 | Domestika Plus | educacion | 9.99 | **19,90 €** (suscripción mensual) | MONTHLY | **99,50 €** (8,29 €/mes) | EUR | https://www.domestika.org/es/plus | media | Web oficial. La página dice que se ahorra un 58 % en un sitio y un 17 % en otro (incoherente). 9,99 € no coincide con ninguna opción. |
| 405 | Udemy Personal Plan | educacion | 19.99 | sin verificar | MONTHLY | — | — | udemy.com/es/pricing da 403; en iOS solo hay cursos sueltos (8,99–19,99 €) | — | — |
| 406 | Preply Business | educacion | 74.99 | sin verificar | — | — | — | — | — | Es un plan para empresas; no se consultó. |
| 407 | Photomath Plus | educacion | 7.99 | **10,99 €** ("Photomath Plus (1 month)") | MONTHLY | — | EUR, IVA incl. | https://apps.apple.com/es/app/id919087726 | media | Otras entradas entre 9,99 y 11,99 €, sin duración. |
| 408 | SoloLearn Pro | educacion | 9.99 | **7,49 €** ("PRO Mensual") | MONTHLY | 49,99 € (también hay entradas de 38,99 y 77,99 €) | EUR, IVA incl. | https://apps.apple.com/es/app/id1210079064 | media | También hay planes mensuales a 5,99 y 13,49 €. |
| 409 | Mimo Premium | educacion | 9.99 | **9,99 €** ("Mimo Pro: Un mes programando") | MONTHLY | **79,99 €** ("Un año programando") | EUR, IVA incl. | https://apps.apple.com/es/app/id1133960732 | alta | El precio mensual coincide. Conviene añadir el precio anual. |
| 411 | Duolingo Super | educacion | 8.49 | 10,49 € (probablemente mensual) | MONTHLY | 89,99–125,99 € (sin duración; 125,99 € puede ser el plan familiar) | EUR, IVA incl. | https://apps.apple.com/es/app/id570060128 | baja | Las entradas de "Super Duolingo" no indican la duración. |
| 412 | Lingoda Sprint | educacion | 99.00 | Sprint: sin precio. Plan de grupo: 12 clases por 114,99 € (precio sin descuento 134,99 €) | cada 4 semanas | — | EUR | https://www.lingoda.com/es/pricing/ | media | No se publica precio para Sprint. Plan de grupo: 5 clases 69,99 €, 20 clases 169,99 €, 40 clases 318,99 €. Particulares: desde 149,99 € (5 clases). |
| 414 | Pimsleur Premium | educacion | 19.99 | **21,49–22,49 €** (All Access) | MONTHLY | 149,99–199,99 € (All Access anual) | EUR, IVA incl. | https://apps.apple.com/es/app/id1405735469 | media | Los planes de un solo idioma cuestan 21,99 €. pimsleur.com no muestra precios. |
| 423 | VSCO Membership | creatividad | 7.99 | **9,99 €** ("Mensual Plus") | MONTHLY | **34,99 €** ("Anual Plus") | EUR, IVA incl. | https://apps.apple.com/es/app/id588013838 | alta | — |
| 425 | Facetune Premium | creatividad | 5.99 | 5,99–9,99 € ("Unlimited access", sin duración) | MONTHLY (probable) | 34,99–89,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id1149994032 | baja | Las entradas son contradictorias. |
| 427 | InShot Pro | creatividad | 3.99 | **5,99 €** ("InShot Pro - Mensual") | MONTHLY | **19,99 €** ("Anual") | EUR, IVA incl. | https://apps.apple.com/es/app/id997362197 | alta | De por vida: 59,99 €. Quitar anuncios: 3,99 € (pago único; probablemente es de ahí de donde sale el precio actual del catálogo). |
| 429 | Splice Premium | creatividad | 4.99 | **9,99 €/semana** ("Splice Weekly") | WEEKLY | **124,99 €** ("Splice Yearly") | EUR, IVA incl. | https://apps.apple.com/es/app/id409838725 | media | Se cobra por semanas. Si el catálogo no admite ciclo semanal, usar el anual (124,99 €). |
| 430 | Unfold Pro | creatividad | 1.99 | **5,99 €** (Pro mensual); Plus mensual 2,99 € | MONTHLY | 17,49 € (Plus anual) | EUR, IVA incl. | https://apps.apple.com/es/app/id1247275033 | alta | — |
| 431 | Afterlight | creatividad | 2.99 | **3,49–3,99 €** (Monthly PRO) | MONTHLY | 19,49–24,99 € (Yearly PRO) | EUR, IVA incl. | https://apps.apple.com/es/app/id1293122457 | media | Hay dos tarifas activas; también semanal (1,99 €) y de por vida (22,99 o 44,99 €). |
| 434 | Remini Pro | creatividad | 7.99 | **9,99 €/semana** ("Remini Pro Weekly") | WEEKLY | — | EUR, IVA incl. | https://apps.apple.com/es/app/id1470373330 | media | Lite semanal: 4,99–7,99 €. Hay una entrada "1 Month" a 9,99 €. Se cobra sobre todo por semanas. |
| 435 | Prequel Premium | creatividad | 7.99 | **5,49–5,99 €/semana** ("Prequel Gold Weekly") | WEEKLY | 14,99–29,99 € ("Gold Yearly") | EUR, IVA incl. | https://apps.apple.com/es/app/id1325756279 | media | No hay plan mensual. |
| 436 | YouCam Perfect Premium | creatividad | 4.99 | sin verificar | — | — | — | https://apps.apple.com/es/app/id768469908 (404) | — | No aparece en la App Store de España; la búsqueda solo devuelve YouCam Makeup, Video y Enhance. Puede que ya no esté disponible. |
| 437 | Polarr Pro | creatividad | 7.99 | **4,99 €** ("Polarr Mensual") | MONTHLY | **28,99 €** ("Polarr Anual") | EUR, IVA incl. | https://apps.apple.com/es/app/id988173374 | media | También hay niveles Lite (3,99–14,49 €) y Studio (9,99–29,49 €). |
| 438 | Veed Pro | creatividad | 24.00 | sin verificar | — | — | — | https://www.veed.io/pricing (el HTML no incluye precios) | — | — |
| 439 | Adobe Express Premium | creatividad | 12.09 | **12,09 €** | MONTHLY | No hay plan anual para Premium | EUR, IVA incl. | https://www.adobe.com/es/express/pricing | alta | El precio coincide. La página dice "Sin compromiso anual, facturación mensual". |
| — | Canva Pro (extra) | creatividad | — | **12,00 €** ("Canva Pro - Mensual"; también hay una entrada de 11,99 €) | MONTHLY | **109,99 €** | EUR, IVA incl. | https://apps.apple.com/es/app/id897446215 | media | Canva Business: 17 €/mes y 169,99 €/año. Equipos: 27 €/mes. canva.com bloquea la lectura. |
| 444 | Tinder Gold | citas | 16.49 | 16,49 € (entradas de 13,99, 16,49 y 27,49 €) | MONTHLY (probable) | — | EUR, IVA incl. | https://apps.apple.com/es/app/id547702041 | baja | Coincide con la primera pasada. Tinder no publica precios fijos. |
| 445 | Tinder Platinum | citas | 21.99 | 16,99 € o 32,99 € (sin duración) | MONTHLY (probable) | — | EUR, IVA incl. | https://apps.apple.com/es/app/id547702041 | baja | Solo hay dos entradas. Tinder Plus cuesta 8,99 €. |
| 447 | Hinge+ | citas | 29.99 | **10,99 €** ("Hinge+ Subscription"); HingeX 15,99 € | sin duración | — | EUR, IVA incl. | https://apps.apple.com/es/app/id595287172 | baja | Todas las entradas tienen el mismo precio. No se sabe si 10,99 € es semanal o mensual; 29,99 € no aparece. |
| 449 | Badoo Premium | citas | 12.99 | 5,99–6,99 € ("Badoo Premium"); "7 Days Badoo Premium" 7,99 € | probablemente semanal | — | EUR, IVA incl. | https://apps.apple.com/es/app/id351331194 | baja | La ficha deja claro que hay un plan de 7 días a 7,99 €. No aparece ningún precio mensual de 12,99 €. |
| 450 | Grindr Unlimited | citas | 24.99 | 23,99 € o 39,99 € ("Unlimited", sin duración) | MONTHLY (probable) | — | EUR, IVA incl. | https://apps.apple.com/es/app/id319881193 | baja | XTRA a 1 mes: 11,49–16,99 €; a 3 meses: 26,49 €. |
| 454 | Coffee Meets Bagel | citas | 29.99 | 30,99–39,99 € ("CMB Premium Subscription" / "Premium") | MONTHLY (probable) | 79,99 € (posible plan de varios meses) | EUR, IVA incl. | https://apps.apple.com/es/app/id630119301 | baja | Ahora se ha consultado la App Store de España (la primera pasada usó Francia e Italia). |
| 455 | Zoosk Premium | citas | 24.99 | sin verificar | — | — | — | zoosk.com/es da 404; la búsqueda en la App Store de España no la encuentra | — | Puede que no esté disponible en España. Conviene revisarlo. |
| 456 | eHarmony Premium | citas | 45.99 | sin precio mensual: paquetes de Premium light (124,99–149 €), Premium PLUS (239,99–399,99 €) y Basic (29,99–219,99 €) | paquetes de varios meses | — | EUR, IVA incl. | https://apps.apple.com/es/app/id458272450 | baja | La ficha no da la duración de los paquetes. |
| 459 | MiuMeet Premium | citas | 14.99 | sin verificar | — | — | — | La búsqueda en la App Store de España no la encuentra | — | Puede que ya no exista. |
| 504 | Euskaltel (variable) | telecos | 45.00 | **40,90 €** (fibra 500 Mb + móvil 100 GB); 45,90 € con Fan TV | MONTHLY | — | EUR (precio para particulares; la página no dice si incluye IVA) | https://www.euskaltel.com/tarifas/fibra-y-movil | media | Rango de 40,90 a 68,90 €. Fibra 1 Gb + móvil: 50,90 € (promoción de 40,90 € durante 3 meses). 45 € sirve como importe orientativo. |
| 505 | Avatel (variable) | telecos | 30.00 | sin verificar | MONTHLY | — | — | https://www.avatel.es/fibra-y-movil/ (el HTML no incluye precios) | — | Solo aparece "desde 4,99 €/mes" para líneas adicionales, y el texto está mal formateado. |
| 542 | BiciMAD | transporte | 25.00 YEARLY | **10 €** (tarifa plana de 30 días) | MONTHLY (30 días) | Sin abono anual en la web oficial | EUR | https://www.bicimad.com/bicimad | media | Pago por uso: 0,50 € por cada uno de los dos primeros tramos de 30 minutos y 3 € por cada tramo siguiente. No aparece ningún abono anual de 25 €. Conviene cambiarlo a 10 €/mes. |
| 547 | Parking / garaje (variable) | transporte | 90.00 | sin verificar | — | — | — | — | — | No se consultó: depende de la ciudad. |
| 375 | Vivid Standard | finanzas | 9.90 | sin verificar (sin cambios respecto a la primera pasada) | — | — | — | vivid.money/es-es/pricing/ da 404 | — | — |
| 378 | PocketGuard Plus | finanzas | 9.99 | **12,99 USD** (PocketGuard Premium) | MONTHLY | **74,99 USD** (6,25 USD/mes) | **USD** | https://pocketguard.com/pricing/ | alta | De por vida: 149,99 USD. La web usa "Plus" y "Premium" como sinónimos. Cobra en dólares. |
| 380 | Splitwise Pro | finanzas | 3.99 | 2,99 € (la mayoría de entradas; probablemente mensual) | MONTHLY | 29,99 € (probable) | EUR, IVA incl. | https://apps.apple.com/es/app/id458023433 | baja | Otras entradas: 3,99 y 47,99 €. splitwise.com/pro no muestra precios. |
| 385 | Robinhood Gold | finanzas | 5.99 | sin verificar | — | — | — | https://robinhood.com/eu/en/ | — | La web europea no menciona Gold (solo Invest, Crypto, Futures, Staking y Wallet). Probablemente no se ofrece en España. |
| 280 | MyFitnessPal Premium | salud | 9,99 € | 9,99 € ("Premium") o 21,99 € ("Monthly Premium") | MONTHLY | 49,99 € o 87,99 € ("Yearly Premium") | EUR, IVA incl. | https://apps.apple.com/es/app/id341232718 | baja | Parece que hay dos niveles (Premium y Premium+), pero la ficha no lo aclara. |
| 281 | Calm Premium | salud | 5,99 € | 12,99 € (probablemente mensual) | MONTHLY | 49,99 € o 64,99 € (probable) | EUR, IVA incl. | https://apps.apple.com/es/app/id571800810 | baja | Las 10 entradas se llaman "Calm Premium" y no indican duración. calm.com da 403. 5,99 € parece ser el precio anual dividido entre 12. |
| 282 | Headspace | salud | 12,99 € | **12,99 €** ("Monthly Subscription") | MONTHLY | **57,99 €** ("Annual Subscription"; también hay una entrada de 69,99 €) | EUR, IVA incl. | https://apps.apple.com/es/app/id493145008 | alta | El precio coincide. Conviene añadir el anual. |
| 283 | Noom | salud | 54,99 € | 47,99–224,99 € (sin duración) | — | — | EUR, IVA incl. | https://apps.apple.com/es/app/id634598719 | baja | Programas de varios meses. noom.com/es no muestra contenido. |
| 285 | Peloton App | salud | 12,99 € | sin verificar | — | — | — | La búsqueda en la App Store de España no la encuentra | — | Peloton no parece tener app en España. Conviene revisar si se mantiene. |
| 287 | Sleep Cycle Premium | salud | 3,99 € | 2,99–43,99 € (sin duración) | — | probablemente 34,49 o 34,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id320606217 | baja | — |
| 288 | Runkeeper Go | salud | 39,99 € YEARLY | **39,99 €** ("Runkeeper Go Yearly") | YEARLY | 39,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id300235330 | alta | El precio coincide. Mensual: 9,99 €; trimestral: 19,99 €. Hay tarifas de 24,99 y 41,99 €. |
| 290 | Freeletics Coach | salud | 12,99 € | 24,99–84,99 € (sin duración) | probablemente de 3 a 12 meses | — | EUR, IVA incl. | https://apps.apple.com/es/app/id654810212 | baja | No hay ninguna entrada mensual clara. |
| 291 | Flo Premium | salud | 9,99 € | 11,49–39,99 € (sin duración) | — | probablemente 39,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id1038369065 | baja | — |
| 294 | Sweat App | salud | 19,99 € | **24,99 €** ("Suscripción SWEAT mensual") | MONTHLY | **134,99 €** ("anual") | EUR, IVA incl. | https://apps.apple.com/es/app/id1049234587 | media | También hay planes mensuales a 19,99, 13,99, 10,99 y 9,99 €. |
| 295 | Centr Premium | salud | 29,99 € | **24,99 €** ("1 Month (Standard)") | MONTHLY | 103,99 € o 124,99 € ("12 Months") | EUR, IVA incl. | https://apps.apple.com/es/app/id1382530817 | media | 3 meses: 49,99 €. Promoción Black Friday: 77,99 €/año. |
| 296 | Lifesum Premium | salud | 4,99 € | **14,99 €** ("1 mes") | MONTHLY | **99,99 €** ("1 año") | EUR, IVA incl. | https://apps.apple.com/es/app/id286906691 | media | 3 meses: 39,99 €. Hay otra tarifa mensual de 10,99 €. 4,99 € está muy desfasado. |
| 297 | Cronometer Gold | salud | 9,99 € | **11,99 €** ("Gold mensual") | MONTHLY | **69,99 €** ("Gold anual") | EUR, IVA incl. | https://apps.apple.com/es/app/id1145935738 | media | Hay otra tarifa: 8,99 €/mes y 49,99 €/año. |
| 298 | BetterSleep Premium | salud | 6,99 € | **9,99 €** ("Acceso por un mes") | MONTHLY | 59,99 € (probable) | EUR, IVA incl. | https://apps.apple.com/es/app/id314498713 | media | 3 meses: 29,99 €. De por vida: 59,99 o 99,99 €. |
| 299 | Lumosity Premium | salud | 11,99 € | 11,99 € (probablemente mensual) | MONTHLY | 59,99–99,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id577232024 | media | El precio coincide. Las entradas no indican la duración. |
| 300 | Waking Up Premium | salud | 11,99 € | **22,99 €** ("Monthly Subscription") | MONTHLY | **179,99 €** ("Yearly"; también hay una entrada de 229,99 €) | EUR, IVA incl. | https://apps.apple.com/es/app/id1307736395 | media | Plan familiar: 249,99 €/año. 11,99 € está muy desfasado. |
| 301 | Meditopia Premium | salud | 7,99 € | **7,99 €** ("Premium Mensual") | MONTHLY | **47,99 €** ("Premium Anual") | EUR, IVA incl. | https://apps.apple.com/es/app/id1190294015 | alta | El precio coincide. Conviene añadir el anual. |
| 302 | 8fit Pro | salud | 9,99 € | sin verificar | — | — | — | https://apps.apple.com/es/app/id866617777 (404); la búsqueda "8fit" en España no la encuentra | — | Puede que se haya retirado. Conviene revisar si se mantiene. |
| 303 | Daily Yoga Premium | salud | 9,99 € | 12,49 € ("Silver Pro Monthly"); 17,99 € ("1 Month Premium") | MONTHLY | 24,99–74,99 € | EUR, IVA incl. | https://apps.apple.com/es/app/id545849922 | baja | Hay varios niveles (Silver, Gold y All Pros). |
| 304 | Endel Premium | salud | 5,99 € | **12,99 €** ("1 mes") | MONTHLY | **59,99 €** ("12 meses") | EUR, IVA incl. | https://apps.apple.com/es/app/id1346247457 | media | Promociones: 1,49 €/mes, 10,99 €/mes y 17,99 o 29,99 €/año. |
| 307 | Synergym | salud | 29,90 € | sin verificar | — | — | — | synergym.es y synergym.es/gimnasios devuelven contenido vacío | — | — |
| 309 | Anytime Fitness | salud | 49,00 € | sin verificar | — | — | — | https://www.anytimefitness.com/es-es (no muestra precios) | — | Los precios dependen de cada club. |
| 310 | Go fit | salud | 49,00 € | sin verificar (la cuota base no aparece) | — | — | — | https://go-fit.es/ | — | Solo aparecen el Plan Plus (suplemento desde 6,50 €/mes), entrenamiento personal (desde 29 €/sesión) y grupos pequeños (desde 15,50 €/sesión). |
| 311 | Holmes Place | salud | 80,00 € | sin verificar | — | — | — | https://www.holmesplace.com/es/es | — | La web española dice que "Holmes Place ahora es We/On" (weonclub.com, sin precios). Conviene renombrar o revisar la entrada. |
| — | Altafit (extra) | salud | — | **37,90 €** (mensual, más 36 € de matrícula) | MONTHLY | 346,90 € (anual, 28,91 €/mes, sin matrícula) | EUR | https://altafitgymclub.com/gimnasios/madrid-generalricardos/ | media | Dato de un solo club (Madrid, General Ricardos). Semestral: 185,90 €. El precio cambia según el club. |
| — | Fitness Park (extra) | salud | — | sin verificar (cuota base) | — | — | — | https://www.fitnesspark.es/ | — | Solo aparecen la promoción "4 primeras semanas por 17 €" y el plan Ultimate (20 € más). |
| — | Dreamfit (extra) | salud | — | sin verificar | — | — | — | https://www.dreamfit.es/ (no muestra precios) | — | — |
| 476 | Canal de Isabel II | hogar | 25,00 € | sin verificar | — | — | — | canaldeisabelsegunda.es (error de certificado) | — | — |
| 481 / 482 | Aqualia / Hidralia | hogar | 25,00 € | sin verificar | — | — | — | — | — | No se consultaron: dependen del municipio. |
| 487 | Comunidad de propietarios | hogar | 80,00 € | sin verificar | — | — | — | — | — | No se consultó. |
| 523 / 524 | Ocaso / Santalucía | seguros | 300,00 € | sin verificar (se mantiene la base de unos 280 € de la primera pasada) | — | — | — | — | — | No se consultaron. |
| 532 | Asisa | seguros | 50,00 € | sin verificar | — | — | — | https://www.asisa.es/seguros-de-salud (solo calculadora) | — | — |
| 533 | Cosalud | seguros | 45,00 € | sin verificar | — | — | — | cosalud.es (contenido vacío) | — | — |
| 534 | Barkibu | seguros | 25,00 € | **desde unos 20 €/mes** | MONTHLY | — | EUR | https://www.barkibu.com/es | media | Importe orientativo de la web oficial. Incluye plan de salud y plan de prevención; depende de la especie, la raza, el sexo y la edad. La primera pasada daba "desde 33,49 €". |
| 535 | Santévet | seguros | 30,00 € | sin verificar | — | — | — | https://www.santevet.es/ (solo presupuesto) | — | — |
| — | Kozoo (extra) | seguros | — | sin verificar | — | — | — | kozoo.eu/es da 404 | — | — |
| — | Helvetia, Direct Seguros, Cigna, Aegon (extra) | seguros | — | sin verificar | — | — | — | — | — | No se consultaron. |

## Resumen

- **Con precio comprobado: 69 filas de la tabla.** Dos son extras que no están en el catálogo con línea propia: Canva Pro y Altafit.
  - 11 con confianza alta.
  - 37 con confianza media.
  - 21 con confianza baja: hay precio, pero la App Store no indica la duración.
- **Siguen sin verificar: 29 filas de la tabla.**
  - Algunas agrupan varios servicios: los periódicos españoles, Aqualia/Hidralia, Ocaso/Santalucía y Helvetia/Direct/Cigna/Aegon.
  - Motivos: bloqueo, sin precio público, no disponibles en España o no consultadas.

### Diferencias más grandes respecto al catálogo

| Servicio | Catálogo | Verificado |
|---|---|---|
| El Confidencial | 2,99 | 8,99 €/mes (88,99 €/año) |
| La Vanguardia | 3,49 | 9,99 € (Básica) / 14,99 € (Premium) |
| Domestika Plus | 9,99 | 19,90 €/mes (99,50 €/año) |
| Lifesum | 4,99 | 14,99 €/mes (99,99 €/año) |
| Waking Up | 11,99 | 22,99 €/mes (179,99 €/año) |
| The Economist | 21,99 | 38,90 €/mes Premium |
| Hinge+ | 29,99 | 10,99 € (duración sin confirmar) |
| Clash Royale | 4,99 | Pase de diamante 14,99 € |
| Overcast | 9,99/año | 19,99–22,99 €/año |
| Endel | 5,99 | 12,99 €/mes |
| Calm | 5,99 | 12,99 € (probablemente mensual) |
| MasterClass | 10 €/mes | solo anual, 129,99 € |
| BiciMAD | 25 €/año | sin abono anual; tarifa plana de 30 días a 10 € |

### Cambios de ciclo o de producto que conviene revisar

- **Pago semanal:** Splice, Remini y Prequel.
- **Solo anual:** MasterClass.
- **Sin plan mensual:** Rosetta Stone (3 o 12 meses).
- **Posiblemente no disponibles en España:** Peloton, 8fit, Zoosk, MiuMeet y YouCam Perfect.
- **Holmes Place** ahora se llama We/On.
- **Pokémon GO Adventure:** no aparece ninguna suscripción.
- **Cobra en USD:** PocketGuard (12,99 USD/mes).
