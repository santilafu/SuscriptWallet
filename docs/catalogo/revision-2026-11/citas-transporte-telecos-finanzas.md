# Revisión de precios del catálogo: citas, transporte, telecos y finanzas

- Fecha de consulta: 2026-11-15 (todas las fuentes se consultaron ese día salvo que se indique otra fecha de publicación).
- Fichero revisado: `src/main/kotlin/com/subia/service/CatalogService.kt`, filas de las categorías `finanzas` (L368-390), `citas` (L444-459), `telecos` (L492-506) y `transporte` (L540-547). Son 61 filas.
- Criterio: precio normal en España, en EUR y con IVA incluido. Las promociones van en Notas. En telecos el precio es el de la tarifa convergente más representativa (fibra de unos 600 Mb y 1 línea móvil) fuera de promoción cuando la fuente lo distingue.
- Logos: se comprobó `https://icon.horse/icon/<domain>` y `https://www.google.com/s2/favicons?domain=<domain>&sz=128` (siguiendo redirecciones). Cuando icon.horse no encuentra el logo, devuelve un PNG genérico de 256 px con la inicial sobre fondo gris (unos 2.600 bytes). Ese caso se marca como **logo dudoso (placeholder)**. Se marca **logo pequeño** cuando el icono mide 33 px o menos.
- Limitación: la cuota de búsquedas web de la sesión (compartida con otros agentes) se agotó a mitad de la revisión. Quedan como **sin verificar** Euskaltel, Avatel, BiciMAD, Parking, PocketGuard, Robinhood Gold y la duración exacta de varios planes de citas. Se puede completar en otra sesión.

## Aviso sobre las apps de citas

Tinder, Bumble, Badoo, Grindr, OkCupid y las demás apps de citas cobran precios dinámicos que dependen de la edad, de la duración (1 semana, 1, 3, 6 o 12 meses) y de las ofertas del momento. Ninguna publica una tabla oficial de precios para España. La fuente usada es la sección "Compras dentro de la app" del App Store de España (apps.apple.com/es), que es oficial y está en euros con IVA, aunque muchas veces no dice la duración de cada artículo. Solo se propone cambiar el precio cuando el App Store indica la duración de forma explícita ("1 mes", "1 Month"). En el resto de casos se da el rango observado, con confianza baja.

## Tabla

| Línea | Nombre actual | Categoría | Precio actual | Ciclo | Precio verificado | Precio anual verificado | ¿Cambia? | Fuente (URL) | Confianza | Domain OK / logo | Notas |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 368 | YNAB | finanzas | 10.00 | MONTHLY | **15.49** | **119.00** | Sí | https://apps.apple.com/es/app/id1010865877 | alta | ynab.com OK / logo OK | App Store ES: "YNAB Subscription" 15,49 € y 119,00 €. En la web, en USD: 14,99 $/mes y 109 $/año. Prueba gratis de 34 días (correcta). |
| 369 | Copilot Money | finanzas | 10.99 | MONTHLY | 13 USD (≈11-12 €) | 95 USD | No disponible en España | https://getfinny.app/blog/copilot-money-pricing-2026 | media | copilot.money OK / logo OK | Solo iOS/Mac y conecta solo con bancos de EE. UU. Se recomienda quitarlo o marcarlo como no disponible en España. |
| 370 | Monarch Money | finanzas | 12.49 | MONTHLY | 14.99 USD | 99.99 USD | No disponible en España | https://getfinny.app/blog/monarch-money-pricing-2026 | media | monarchmoney.com OK / logo OK | Solo bancos de EE. UU./Canadá. Se recomienda quitarlo o marcarlo como no disponible en España. |
| 371 | Revolut Premium | finanzas | 8.99 | MONTHLY | **9.99** | — | Sí | https://www.revolut.com/es/pricing (403 al descargarla; cifra tomada del extracto del buscador) · https://finantresnoticias.com/bancos/revolut-cambio-planes-pago-plus-premium-metal-ultra/ | media | revolut.com OK / logo OK | Página oficial ES: Plus 3,99; Premium 9,99; Metal 15,99; Ultra 55. Algunas fuentes antiguas aún dan 8,99. Los servicios financieros están exentos de IVA. |
| 372 | Revolut Metal | finanzas | 16.99 | MONTHLY | **15.99** | — | Sí | ídem | media | revolut.com OK / logo OK | Baja 1 €. |
| 373 | N26 Smart | finanzas | 4.90 | MONTHLY | 4.90 | — | No | https://n26.com/es-es/cuentas | alta | n26.com OK / logo OK | Página oficial ES. |
| 374 | N26 You | finanzas | 9.90 | MONTHLY | 9.90 (ahora **N26 Go**) | — | Sí (renombrar) | https://n26.com/es-es/cuentas | alta | n26.com OK / logo OK | El plan "You" ya no aparece. La web oficial lista Estándar (gratis), Smart 4,90, **Go 9,90** y Metal 16,90. |
| 375 | Vivid Standard | finanzas | 9.90 | MONTHLY | Standard 0 € (3,90 € si inactiva); **Prime desde 7,90**; Plus 6,90 | — | Sí | https://roams.es/finanzas/entidades-financieras/vivid-money/ (abril 2026) | baja | vivid.money OK / logo OK | Standard es gratuita. Los planes de pago son Plus y Prime. Recomendación: renombrar la fila a "Vivid Prime" con 7,90. La prueba de 30 días no está confirmada (Prime anuncia "1 mes gratis"). |
| 376 | Wallet Pro | finanzas | 4.99 | MONTHLY | **5.99** | **29.99** | Sí | https://apps.apple.com/es/app/id1032467659 | media | budgetbakers.com OK / logo OK | App Store ES ("Wallet - Finanzas personales"): Premium 5,99 / 17,99 / 29,99, 3 años 59,99, vitalicio 49,99. Se interpreta 5,99 como mensual y 29,99 como anual. Nombre comercial actual: "Wallet Premium". |
| 377 | Toshl Finance Pro | finanzas | 3.99 | MONTHLY | **2.99** | **19.49** | Sí | https://finantres.com/?p=527022 (cita el App Store ES) | media | toshl.com OK / logo pequeño (32 px) | Existe también el plan Toshl Medici, más caro (precio sin verificar). |
| 378 | PocketGuard Plus | finanzas | 9.99 | MONTHLY | sin verificar | — | No (sin verificar) | — | baja | pocketguard.com OK / logo OK | Solo conecta con bancos de EE. UU./Canadá. Se recomienda quitarlo o marcarlo como no disponible en España. |
| 379 | Spendee Premium | finanzas | 3.99 | MONTHLY | **6.99** | **39.99** | Sí | https://apps.apple.com/es/app/id635861140 | media | spendee.com OK / logo OK | App Store ES: "Spendee Premium – Monthly 6,99 €" y "Spendee Premium – Yearly 39,99 €". El 3,99 € actual es el de **Spendee Plus**. |
| 380 | Splitwise Pro | finanzas | 3.99 | MONTHLY | 2.99-3.99 (ambiguo) | 29.99-47.99 (ambiguo) | No (sin verificar) | https://apps.apple.com/es/app/id458023433 | baja | splitwise.com OK / logo OK | El App Store ES lista "Splitwise Pro" a 2,99, 3,99, 29,99 y 47,99 € sin decir la duración. Se mantiene 3,99. |
| 381 | Bunq Easy Bank | finanzas | 8.99 | MONTHLY | Plan desaparecido. Core **3.99**, Pro **9.99**, Elite **18.99** | — | Sí (renombrar) | https://finantresnoticias.com/bancos/planes-bunq-espana-free-core-pro-elite/ (28/07/2026; cita la hoja de precios oficial del 29/01/2026) · https://www.bunq.com/es/pricing | media | bunq.com OK / logo OK (64 px) | La web oficial confirma los nombres Free/Core/Pro/Elite. Recomendación: sustituir por "bunq Pro" 9,99 y añadir Core y Elite. |
| 382 | Fintonic Premium | finanzas | 2.99 | MONTHLY | No existe | — | Sí (eliminar) | https://apps.apple.com/es/app/id672220319 | media | fintonic.com OK / logo OK | El App Store ES no tiene compras dentro de la app y la ficha dice "Somos una aplicación gratuita". |
| 383 | Money Manager Pro | finanzas | 4.99 | MONTHLY | **2.49** | **21.49** | Sí | https://apps.apple.com/es/app/id560481810 | media | **Domain incorrecto: moneymanager.com no es de la app. Logo dudoso (placeholder "M")** | La app es de Realbyte Inc. (en el App Store ES se llama "Registro Contable: Gastos"). Su web es realbyteapps.com (icon.horse 200, icono de 683 bytes; no se verificó el aspecto). |
| 384 | Tricount Premium | finanzas | 1.99 | MONTHLY | Descatalogado | — | Sí (eliminar) | https://help.tricount.com/articles/what-happened-with-tricount-premium | alta | tricount.com OK / logo OK (64 px) | Tricount es gratuita desde la app nueva (pertenece a bunq). Las suscripciones vigentes duran hasta que caducan. |
| 385 | Robinhood Gold | finanzas | 5.99 | MONTHLY | sin verificar | — | No (sin verificar) | https://robinhood.com/eu/en/invest/ | baja | robinhood.com OK / logo OK (60 px) | Robinhood Europa (entidad lituana) ofrece cripto, tokens de acciones y perpetuos. No hay constancia de que Gold esté disponible en la UE. Candidato a quitar. |
| 386 | Revolut Ultra | finanzas | 45.00 | MONTHLY | **55.00** | — | Sí | https://www.revolut.com/es/pricing (extracto del buscador) · https://sincomisiones.org/cuentas/revolut/ultra | media | revolut.com OK / logo OK | La web oficial lo marca como "oferta inicial". 45 € es una cifra antigua. |
| 388 | Comisión de mantenimiento bancaria (variable) | finanzas | 5.00 | MONTHLY | **≈13.92** (167 €/año) | 167 | Sí | https://www.helpmycash.com/cuentas/comisiones-bancarias/ (cita a ASUFIN, 2026) · https://finantresnoticias.com/bancos/perder-la-nomina-puede-reactivar-240-e-en-comisiones/ | media | sin domain (correcto) | Según ASUFIN, la media de una cuenta no bonificada es de 167 €/año. Muchos bancos cobran 0 € si se domicilia la nómina, y el techo habitual es de 240 €/año (20 €/mes). |
| 389 | Cuota anual de tarjeta de crédito (variable) | finanzas | 36.00 | YEARLY | **≈40.00** | 40 | Sí | https://cuantomecuesta.com/es/tarjeta-credito/ (abril 2026) · https://finantresnoticias.com/bancos/wizink-cobra-48-euros-ano-en-tarjeta-on-fija-tae-23-17-por-ciento/ | baja | sin domain (correcto) | Rango de 0 € (neobancos) a 300 € (Amex Platinum). WiZink On cobra 48 €/año. |
| 390 | Cuota de autónomo (RETA) (variable) | finanzas | 230.00 | MONTHLY | **299.56** (tramo general 1) | — | Sí | https://www.oie.es/cuotas-de-autonomos/ (Orden PJC/297/2026, BOE 31-03-2026) | alta | seg-social.es OK / logo OK en icon.horse (48 px); Google devuelve el globo genérico (404) | En 2026 el tipo es del 31,50 %. La cuota mínima va de 205,88 € (rendimientos hasta 670 €) a 607,35 € (más de 6.000 €). Con rendimientos de 1.166,70 a 1.300 € es de 299,56 €. Tarifa plana: 80 €/mes. Si se prefiere el mínimo, poner 205,88. |
| 444 | Tinder Gold | citas | 16.49 | MONTHLY | 16.49 (rango App Store 13,99-27,49) | — | No | https://apps.apple.com/es/app/tinder-app-de-citas-y-ligar/id547702041 | baja | tinder.com OK / logo OK | Precio por edad y duración. 16,49 aparece en el App Store ES. |
| 445 | Tinder Platinum | citas | 21.99 | MONTHLY | rango 16,99-32,99 | — | No (sin verificar) | ídem | baja | tinder.com OK / logo OK | El App Store ES lista 16,99 y 32,99 € sin duración. Es probable que 32,99 sea el mensual, pero no está confirmado. |
| 446 | Bumble Premium | citas | 16.99 | MONTHLY | **29.99** | — | Sí | https://apps.apple.com/es/app/bumble-chat-ligar-citas/id930441707 | media | bumble.com OK / logo OK | El App Store ES lista "1 Month Bumble Premium 29,99 €" (7 días: 13,99 €). Alemania y Finlandia también cobran 29,99 €/mes. |
| 447 | Hinge+ | citas | 29.99 | MONTHLY | rango desde 10,99 | — | No (sin verificar) | https://apps.apple.com/es/app/hinge-app-de-citas-y-ligar/id595287172 | baja | hinge.co OK / logo OK (Google: 308 bytes) | El App Store ES lista "Hinge+ Subscription 10,99 €" varias veces, sin duración. Puede ser semanal. |
| 448 | Meetic Premium | citas | 29.99 | MONTHLY | 29.99 | — | No | https://apps.apple.com/es/app/id416973595 | media | meetic.es OK / logo pequeño (32 px) | App Store ES: "Premium 1 mes 29,99 €" (1 semana: 19,99 €). En la web hay planes de 3 y 6 meses mucho más baratos por mes (unos 10 €/mes con 6 meses). |
| 449 | Badoo Premium | citas | 12.99 | MONTHLY | rango 5,99-7,99 (sin duración) | — | No (sin verificar) | https://apps.apple.com/es/app/badoo-ligar-citas-y-amistad/id351331194 | baja | badoo.com OK / logo OK | El App Store ES lista "7 Days Badoo Premium 7,99 €" y "Badoo Premium" 5,99/6,99 €. El mensual no aparece explícito. |
| 450 | Grindr Unlimited | citas | 24.99 | MONTHLY | rango 23,99-39,99 | — | No (sin verificar) | https://apps.apple.com/es/app/grindr-citas-y-chats-para-gays/id319881193 | baja | grindr.com OK / logo OK | El App Store ES lista "Unlimited" a 23,99 y 39,99 € sin duración. La prueba de 7 días no está confirmada. |
| 451 | OkCupid A-List | citas | 24.99 | MONTHLY | Plan renombrado: **OkCupid Basic 8,99** / **Premium 15,99-43,99** | — | Sí (renombrar) | https://apps.apple.com/es/app/id338701294 | baja | okcupid.com OK / logo OK | Ahora los planes son Basic y Premium. El App Store aún lista un artículo heredado "A-List 9,99 €". |
| 452 | Happn Premium | citas | 24.99 | MONTHLY | **19.99** | — | Sí | https://apps.apple.com/es/app/id489185828 | media | happn.com OK / logo OK | App Store ES: "happn 1 Month subscription 19,99 €". Otras suscripciones a 22,99 y 29,99 € sin duración. |
| 453 | Lovoo VIP | citas | 19.99 | MONTHLY | 19.99 (ahora **LOVOO Premium**, rango 11,99-24,99) | — | Sí (renombrar) | https://apps.apple.com/es/app/id445338486 | baja | lovoo.com OK / logo OK (48 px) | El App Store ES ya no usa "VIP". |
| 454 | Coffee Meets Bagel | citas | 29.99 | MONTHLY | sin verificar en ES (Francia/Italia: 37,99-39,99) | — | No (sin verificar) | https://apps.apple.com/fr/app/id630119301 | baja | coffeemeetsbagel.com OK / logo OK | No se confirmó que esté disponible en España. |
| 455 | Zoosk Premium | citas | 24.99 | MONTHLY | sin verificar en ES (Alemania: 29,99) | — | No (sin verificar) | https://apps.apple.com/de/app/zoosk-dating-singles-treffen/id349160522 | baja | zoosk.com OK / logo OK | — |
| 456 | eHarmony Premium | citas | 45.99 | MONTHLY | sin verificar (App Store ES: Premium light 149 €, Premium PLUS 239,99 €, sin duración) | — | No (sin verificar) | https://itunes.apple.com/es/app/eharmony/id458272450 | baja | eharmony.com (no se verificó si en España se usa eharmony.es) / logo OK | eHarmony vende planes de 6, 12 o 24 meses y no tiene mensual suelto. Habría que valorar el ciclo YEARLY. |
| 457 | Pairs Premium | citas | 29.99 | MONTHLY | No aplica en España | — | Sí (eliminar) | — | media | pairs.lv (web japonesa) / logo OK | Pairs funciona en Japón y Taiwán. No se encontró en el mercado español. |
| 458 | Hinge Preferred | citas | 19.99 | MONTHLY | Plan desaparecido (ahora Hinge+ / HingeX) | — | Sí (eliminar o renombrar a **HingeX**, App Store desde 15,99) | https://apps.apple.com/es/app/hinge-app-de-citas-y-ligar/id595287172 | media | hinge.co OK / logo OK | "Preferred" es el nombre antiguo de Hinge+. Hoy duplica la fila 447. |
| 459 | MiuMeet Premium | citas | 14.99 | MONTHLY | sin verificar | — | No (sin verificar) | — | baja | **miumeet.com: logo dudoso (placeholder "M")** | No se encontró ninguna fuente. Candidato a quitar. |
| 492 | Movistar (variable) | telecos | 60.00 | MONTHLY | **53.00** | — | Sí | https://www.xatakamovil.com/comparativa-de-tarifas/mejores-tarifas-baratas-fibra-movil (26/08/2026) · https://www.kelisto.es/internet/mejor-compra/las-mejores-tarifas-de-movistar-5074-5074 (jul 2026) | media | movistar.es OK / logo OK | miMovistar con fibra de 600 Mb y 60 GB. El paquete con TV básica cuesta 67 €. |
| 493 | Vodafone (variable) | telecos | 50.00 | MONTHLY | **43.00** | — | Sí | https://www.vodafone.es/c/particulares/es/legales_tarifas_vodafone/ · Xataka 26/08/2026 | media | vodafone.es OK / logo OK | Fibra de 600 Mb y 60 GB fuera de promoción: 43 € (con móvil ilimitado, 53 €). Hay promociones desde 38 € (600 Mb y 30 GB). En enero de 2026 los paquetes subieron 3 €. |
| 494 | Orange (variable) | telecos | 45.00 | MONTHLY | **50.00** | — | Sí | https://roams.es/companias-telefonicas/orange/tarifas/movil-internet-tv/ (jul 2026) · Xataka 26/08/2026 | media | orange.es OK / logo pequeño (48 px; favicon de Google de solo 174 bytes) | Fibra de 600 Mb y 60 GB. Con datos ilimitados, 64 €. En enero de 2026 subió entre 5 y 6 €. |
| 495 | Digi (variable) | telecos | 25.00 | MONTHLY | **15.00** | — | Sí | https://www.digimobil.es/tarifa-digicombo.php | alta | digimobil.es OK / logo OK | Fibra Smart a 10 € más una línea de 50 GB con fibra a 5 € = 15 €/mes (suma propia con precios oficiales). Permanencia de 3 meses en la fibra. Digi anuncia "Seguimos sin subir nuestros precios en 2026". |
| 496 | MásMóvil (variable) | telecos | 35.00 | MONTHLY | **34.90** | — | Sí (mínimo) | Xataka 26/08/2026 | media | masmovil.es / **logo dudoso (placeholder "M" en icon.horse; Google sí da favicon)** | Fibra de 500 Mb y 50 GB. |
| 497 | Yoigo (variable) | telecos | 40.00 | MONTHLY | **49.00** | — | Sí | Xataka 26/08/2026 | media | yoigo.com OK / logo OK | Fibra de 600 Mb y 50 GB. |
| 498 | Pepephone (variable) | telecos | 30.00 | MONTHLY | **35.00** | — | Sí | Xataka 26/08/2026 | media | pepephone.com OK / logo OK | Fibra de 600 Mb y 60 GB. |
| 499 | Lowi (variable) | telecos | 25.00 | MONTHLY | **30.00** | — | Sí | Xataka 26/08/2026 | media | lowi.es OK / logo OK (50 px) | Fibra de 600 Mb y 50 GB. |
| 500 | O2 (variable) | telecos | 35.00 | MONTHLY | 35.00 | — | No | Xataka 26/08/2026 | media | o2online.es OK / logo OK | Fibra de 600 Mb y 60 GB. |
| 501 | Simyo (variable) | telecos | 15.00 | MONTHLY | **33.99** (fibra y móvil) | — | Sí (según el uso de la fila) | Xataka 26/08/2026 | media | simyo.es OK / logo OK | Fibra de 600 Mb y 50 GB a 33,99 €. Si la fila quiere representar solo móvil, 15 € es plausible, pero no está verificado. La descripción dice "Móvil y fibra". |
| 502 | Finetwork (variable) | telecos | 25.00 | MONTHLY | **23.90** | — | Sí (mínimo) | Xataka 26/08/2026 | media | finetwork.com OK / logo OK | Fibra de 600 Mb y 30 GB. |
| 503 | Jazztel (variable) | telecos | 40.00 | MONTHLY | **43.95** | — | Sí | Xataka 26/08/2026 | media | jazztel.com OK / logo OK (Google: 319 bytes) | Fibra de 600 Mb y 60 GB. |
| 504 | Euskaltel (variable) | telecos | 45.00 | MONTHLY | sin verificar | — | No (sin verificar) | https://www.euskaltel.com/ (no muestra precios) | baja | euskaltel.com OK / logo OK | La web muestra paquetes de 500 Mb + 50 GB, 500 Mb + ilimitados con TV y 1 Gb + ilimitados con TV, pero sin precio. |
| 505 | Avatel (variable) | telecos | 30.00 | MONTHLY | sin verificar | — | No (sin verificar) | https://www.avatel.es/ (no muestra precios) | baja | avatel.es OK / logo OK | Paquetes de 1 Gb + 100 GB acumulables. El precio solo aparece en el configurador. |
| 506 | Adamo (variable) | telecos | 35.00 | MONTHLY | **24.00** | — | Sí | https://www.adamo.es/ | alta | adamo.es OK / logo pequeño (32 px) | Fibra de 1 Gb y móvil con GB ilimitados a 24 €/mes con "precio para siempre". Con 2 móviles, 30 €. Cada línea extra cuesta 6 €. |
| 540 | Abono Transporte Madrid (CRTM) (variable) | transporte | 40.00 | MONTHLY | **32.70** | — | Sí | https://www.merca2.es/2026/06/04/madrid-crtm-abono-transporte-2389320/ · https://www.merca2.es/2026/07/19/madrid-consorcio-regional-2419587/ | media | crtm.es OK / logo OK | Zona A con la bonificación del 40 %, ampliada a todo 2026. Sin bonificación costaría 54,60 €. El Abono Joven (hasta 25 años) cuesta 10 € para todas las zonas. |
| 541 | T-usual / T-mobilitat (ATM Barcelona) (variable) | transporte | 22.00 | MONTHLY | **22.80** | — | Sí | https://barcelonasecreta.com/rebaja-50-tusual-barcelona/ · https://www.cronista.com/espana/actualidad-es/estos-seran-los-nuevos-precios-del-transporte-publico-para-2026-en-barcelona | media | atm.cat OK / logo OK en icon.horse (48 px); Google da el globo genérico (404) | 1 zona, desde el 15/01/2026, con el 50 % de descuento (prorrogado hasta 2028). El soporte de cartón cuesta 1 €. |
| 542 | Bicimad | transporte | 25.00 | YEARLY | sin verificar | — | No (sin verificar) | https://www.bicimad.com/ (no muestra precios) | baja | bicimad.com OK / logo pequeño (33 px, solo una "b") | Tarifa histórica: 25 €/año (15 € con abono de transporte). Una nota del Ayuntamiento habla de un modelo sin cuota anual, pero no tiene fecha. Conviene revisarlo. |
| 543 | Bicing | transporte | 50.00 | YEARLY | 50.00 | 50.00 | No | https://www.bicing.barcelona/es/tarifas | alta | bicing.barcelona OK / logo OK | Tarifa Plana a 50 €/año. Falta la tarifa por uso (35 €/año). En 2026 solo subió el precio por trayecto. |
| 544 | Uber One | transporte | 4.99 | MONTHLY | 4.99 | 59.99 (no oficial) | No | https://www.uber.com/es/es-es/u/uber-one | alta | uber.com OK / logo OK | El anual de 59,99 € solo aparece en un tercero (Spliiit). Estudiantes: 2,50 €/mes. |
| 545 | Bip&Drive (Via-T) (variable) | transporte | 1.45 | MONTHLY | **1.51** (18,15 €/año) | **18.15** | Sí (pasar a YEARLY) | https://www.bipdrive.com/en/free-toll-road-system-motorcycle · https://www.consumoteca.com/motor/bip-and-drive-via-t/ | media | bipdrive.com OK / logo OK | Cuota anual de 15 € + IVA = 18,15 €. Pago por uso: 1,99 € + IVA (2,41 €) por mes de uso. Los peajes se cobran aparte. |
| 546 | Pagatelia (Via-T) (variable) | transporte | 1.50 | MONTHLY | **1.21** por mes de uso (Tiempo Libre) o **6,15 €/año** (Fórmula Fácil/Frecuente) | 6.15 | Sí | https://www.pagatelia.com/en/plans · https://www.pagatelia.com/en/ausol | media | pagatelia.com OK / logo OK | Tiempo Libre: 1,00 € + IVA por mes de uso. Desde el 13/05/2026 las recargas tienen comisión (0,30 € con cuenta española). |
| 547 | Parking / garaje (variable) | transporte | 90.00 | MONTHLY | sin verificar | — | No (sin verificar) | — | baja | sin domain (correcto) | No dio tiempo a buscar fuentes. Se mantiene el orientativo. |

## Planes que faltan

| Nombre | Precio | Ciclo | Domain | Fuente |
|---|---|---|---|---|
| Revolut Plus | 3,99 € | MONTHLY | revolut.com | https://www.revolut.com/es/pricing (extracto del buscador) |
| N26 Metal | 16,90 € | MONTHLY | n26.com | https://n26.com/es-es/cuentas |
| N26 Go (sustituye a "You") | 9,90 € | MONTHLY | n26.com | https://n26.com/es-es/cuentas |
| bunq Core | 3,99 € | MONTHLY | bunq.com | https://finantresnoticias.com/bancos/planes-bunq-espana-free-core-pro-elite/ |
| bunq Pro | 9,99 € | MONTHLY | bunq.com | ídem |
| bunq Elite | 18,99 € | MONTHLY | bunq.com | ídem |
| Vivid Plus | 6,90 € | MONTHLY | vivid.money | https://roams.es/finanzas/entidades-financieras/vivid-money/ (confianza baja) |
| Vivid Prime | 7,90 € | MONTHLY | vivid.money | ídem (confianza baja) |
| Spendee Plus | 3,99 € | MONTHLY | spendee.com | https://apps.apple.com/es/app/id635861140 (duración no explícita) |
| Tinder Plus | 8,99 € | MONTHLY (duración no explícita) | tinder.com | https://apps.apple.com/es/app/tinder-app-de-citas-y-ligar/id547702041 (confianza baja) |
| HingeX | desde 15,99 € | MONTHLY (duración no explícita) | hinge.co | https://apps.apple.com/es/app/hinge-app-de-citas-y-ligar/id595287172 (confianza baja) |
| OkCupid Basic | 8,99 € | MONTHLY (duración no explícita) | okcupid.com | https://apps.apple.com/es/app/id338701294 (confianza baja) |
| Grindr XTRA | 11,49-16,99 € ("1 MES") | MONTHLY | grindr.com | https://apps.apple.com/es/app/grindr-citas-y-chats-para-gays/id319881193 |
| Bicing tarifa por uso | 35 € | YEARLY | bicing.barcelona | https://www.bicing.barcelona/es/tarifas |
| Abono Joven Madrid (CRTM) | 10 € | MONTHLY | crtm.es | https://www.merca2.es/2026/06/04/madrid-crtm-abono-transporte-2389320/ |

## Planes que ya no existen o no aplican en España

| Línea | Plan | Situación | Fuente |
|---|---|---|---|
| 374 | N26 You | Renombrado o sustituido por **N26 Go** (9,90 €) | https://n26.com/es-es/cuentas |
| 375 | Vivid Standard (como plan de pago) | Standard es gratuita. Los de pago son Plus y Prime | https://roams.es/finanzas/entidades-financieras/vivid-money/ |
| 381 | Bunq Easy Bank | Ya no existe. Ahora hay Free/Core/Pro/Elite | https://www.bunq.com/es/pricing |
| 382 | Fintonic Premium | No existe: la app es gratuita y no tiene compras dentro de la app | https://apps.apple.com/es/app/id672220319 |
| 384 | Tricount Premium | Descatalogado tras la integración en bunq | https://help.tricount.com/articles/what-happened-with-tricount-premium |
| 451 | OkCupid A-List | Sustituido por OkCupid Basic y Premium | https://apps.apple.com/es/app/id338701294 |
| 453 | Lovoo VIP | Ahora se llama LOVOO Premium | https://apps.apple.com/es/app/id445338486 |
| 458 | Hinge Preferred | Nombre antiguo de Hinge+. Duplica la fila 447 | https://apps.apple.com/es/app/hinge-app-de-citas-y-ligar/id595287172 |
| 457 | Pairs Premium | App del mercado japonés y taiwanés, no está en España | — |
| 369 / 370 / 378 | Copilot Money, Monarch Money, PocketGuard | Solo bancos de EE. UU. (y Canadá); no sirven en España | https://getfinny.app/blog/copilot-money-vs-monarch-money-2026 |
| 385 | Robinhood Gold | No hay constancia de Gold en la UE (sin verificar) | https://robinhood.com/eu/en/invest/ |
| 459 | MiuMeet Premium | No se encontró rastro de la app (sin verificar) | — |

## Logos dudosos

- **masmovil.es**: icon.horse devuelve el placeholder gris con una "M". El favicon de Google sí responde (921 bytes).
- **moneymanager.com**: placeholder "M". Además, el domain no es de la app (Realbyte, realbyteapps.com).
- **miumeet.com**: placeholder "M".
- **bicimad.com**: 33 px, solo una "b".
- **meetic.es, toshl.com, adamo.es**: 32 px (pequeños pero válidos).
- **orange.es**: 48 px en icon.horse y favicon de Google de 174 bytes (muy pequeño).
- **seg-social.es y atm.cat**: icon.horse da un logo de 48 px, pero Google devuelve el globo genérico (404).
