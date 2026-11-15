# Revisión del catálogo: seguros, hogar y salud (2026-11-15)

Alcance: filas de `src/main/kotlin/com/subia/service/CatalogService.kt` con `categoryKey` `salud` (L277-311), `hogar` (L464-487) y `seguros` (L511-535). Son 85 filas. No se ha tocado código.

## Cómo leer la tabla

- **Precio verificado**: precio público actual en España (EUR, IVA incluido) cuando la marca lo publica. En recibos y primas (`variablePrice=true`) es un **importe orientativo** con su base explicada.
- **Ciclos**: se respetan los que exigen los tests. Los seguros generales siguen anuales y los de salud y mascotas, mensuales.
- **Logos**: se comprobaron con `icon.horse/icon/<domain>` y `google.com/s2/favicons?domain=<domain>&sz=128`. icon.horse siempre responde 200, incluso con dominios inexistentes, porque genera una **inicial de relleno**. Por eso se comparó el hash de cada icono con el de esas iniciales (A=3022 B, C=3877 B, E=1101 B, G=4038 B, I=946 B, L=995 B, M=2599 B, O=4262 B, R=2615 B, S=4267 B). Si coincide, la tabla marca **logo dudoso (relleno)**. Los favicons de menos de 500 B se marcan como **logo pequeño**. "G404" significa que Google no tiene favicon (devuelve su globo genérico de 726 B).
- **Límite de la investigación**: se agotó el cupo de búsquedas web de la sesión. Muchas apps de salud quedan **sin verificar** y conservan su precio actual (no se inventan cifras).

### Bases de los importes orientativos

| Concepto | Dato 2026 | Fuente |
|---|---|---|
| Luz, hogar medio PVPC | 55,89 €/mes (abril), 64,12 €/mes (mayo), 79,75 €/mes (agosto) | OCU: https://www.ocu.org/organizacion/prensa/notas-de-prensa/2026/precioluz050526 · https://www.ocu.org/organizacion/prensa/notas-de-prensa/2026/luzagosto310826 |
| Luz, octubre | 88,1 €/mes (290 kWh) | Papernest: https://www.papernest.es/wp-content/uploads/sites/27/2026/11/nota_prensa_termometro_octubre-2.pdf |
| Gas, hogar medio TUR | ≈52-59 €/mes en octubre 2026 (IVA al 21 % desde junio) | https://www.papernest.es/estudios/tur-gas-octubre-2026/ · https://roams.es/actualidad/energia/tur-gas-octubre-kwh-sube-22-factura-alcanza-52-hogar-660-kwh/ |
| Agua (150 m³/año) | Barcelona 516 €, Sevilla 483 €, Palma 395 €, Valencia 338 € al año. Madrid no aparece en lo consultado | OCU (estudio de finales de 2025): https://www.ocu.org/alimentacion/agua/noticias/precios-agua-grifo-2025 · tabla en https://www.bankinter.com/blog/finanzas-personales/precio-agua-grifo-ciudades |
| Seguro de coche | Prima media 390-399 €/año (CHECK24, mar-may 2026). Terceros ampliado 566,89 € (Kelisto, 2.º trim. 2026) | https://seguros.check24.es/wp-content/uploads/2026/06/NdP_La-prima-media-del-seguro-de-coche-baja-en-mayo-hasta-los-390-euros-un-53-menos-que-hace-un-ano.pdf · https://www.kelisto.es/seguros-coche/actualidad/indice-seguros-coche-kelisto |
| Seguro de hogar | Prima media 281 €/año (Codeoscopic, agosto 2026). Otra fuente da ≈265 € | https://roams.es/actualidad/seguros/seguros-coche-hogar-frenan-precios-agosto-2026-salud-vida-disparan/ |
| Seguro de decesos | Prima media de pólizas nuevas 222 €/año (julio 2026) | https://roams.es/actualidad/seguros/salud-decesos-se-disparan-seguro-coche-abarata-julio-2026/ |
| Seguro de salud | Media 61 €/mes. Por producto, a 48 años y con precios de nov. 2026 | https://selectra.es/seguros/seguros-salud/precio |
| Seguro de mascotas (perro) | Media ≈11 €/mes (incluye pólizas solo de RC). Barkibu "desde 33,49 €/mes", sin fecha clara | https://selectra.es/seguros/seguros-mascotas/precio-seguro-perro · https://selectra.es/seguros/aseguradoras/barkibu |

**Criterio para la luz**: no hay datos fiables de factura media por comercializadora, así que diferenciarlas (55-70 €) no tiene base. Se propone un único orientativo de **70 €/mes**, que es el punto medio de las cifras OCU y Papernest de 2026 (56-88 €/mes). Las descripciones dicen "luz y gas", pero ese importe cubre solo la luz. Si se quiere representar luz y gas juntos, habría que sumar unos 50-59 €/mes de gas (TUR). Recomendación: cambiar la descripción a "Luz (y gas si lo tienes contratado)…" o añadir filas de gas aparte.

## Tabla de revisión

| Línea | Nombre actual | Categoría | Precio actual | Ciclo | Precio verificado | Precio anual verificado | ¿Cambia? | Fuente (URL) | Confianza (alta/media/baja) | Domain OK / logo | Notas |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 277 | Strava Summit | salud | 9,99 € | MONTHLY | 7,99 €/mes | 49,99 €/año | Sí | https://www.strava.com/pricing?hl=es-ES (consultado 2026-11-15) | alta | strava.com OK / logo OK | "Summit" ya no existe: renombrar a "Strava (suscripción)". Hay también el pack Strava + Runna (ver Servicios que faltan) |
| 278 | Whoop | salud | 30,00 € | MONTHLY | — | 199 €/año (One). Peak 264 €, Life 399 € | Sí (pasar a YEARLY 199 €) | https://www.whoop.com/ax/es/one/ · https://secure.businesswire.com/news/home/20250508809930/es (tabla EUR, mayo 2025) | media | whoop.com OK / logo OK | Desde 2025 la membresía es anual y por niveles. No hay cuota mensual |
| 279 | Garmin Connect+ | salud | 6,99 € | MONTHLY | 8,99 €/mes | 89,99 €/año | Sí | https://global.techradar.com/es-es/health-fitness/garmin-anade-el-nivel-premium-garmin-connect-con-funciones-de-ia-pero-promete-que-su-experiencia-gratuita-no-desaparecera | media | garmin.com OK / logo OK | Precio de lanzamiento en España (marzo 2025). 6,99 es el precio en USD. Prueba de 30 días |
| 280 | MyFitnessPal Premium | salud | 9,99 € | MONTHLY | sin verificar | sin verificar (79,99-99,99 € según usuarios) | No | https://www.chollometro.com/thread/1507575 (comentarios de usuarios) | baja | myfitnesspal.com OK / logo OK | En EE. UU. cuesta 19,99 $/mes o 79,99 $/año. Es probable que 9,99 €/mes esté por debajo del real. Confirmar en la app |
| 281 | Calm Premium | salud | 5,99 € | MONTHLY | sin verificar | sin verificar | No | — | baja | calm.com OK / logo OK | Referencia de EE. UU.: 14,99 $/mes o 69,99 $/año. 5,99 €/mes equivale al anual prorrateado. Mejor YEARLY ≈69,99 si se confirma |
| 282 | Headspace | salud | 12,99 € | MONTHLY | sin verificar | sin verificar | No | https://www.headspace.com/es/subscriptions (no muestra precio) | baja | headspace.com OK / logo OK | Referencia de EE. UU.: 12,99 $/mes o 69,99 $/año, coherente con el valor actual |
| 283 | Noom | salud | 54,99 € | MONTHLY | sin verificar | — | No | — | baja | noom.com OK / logo OK | El precio depende del plan y la duración |
| 284 | Apple Fitness+ | salud | 9,99 € | MONTHLY | 9,99 €/mes | 79,99 €/año | No | https://www.apple.com/es/apple-fitness-plus/ | alta | apple.com OK / logo OK | Confirmado. El 99,99 €/año que citan algunos medios no es oficial |
| 285 | Peloton App | salud | 12,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → proponer onepeloton.com (logo OK) | Peloton no opera en España (onepeloton.es no resuelve). Plantearse retirarla |
| 286 | Oura Ring Membership | salud | 5,99 € | MONTHLY | 5,99 €/mes | 69,99 €/año | Sí (añadir priceAnnual) | https://ouraring.com/es/membership | alta | ouraring.com OK / logo OK | Precio UE con impuestos incluidos. Primer mes gratis |
| 287 | Sleep Cycle Premium | salud | 3,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → sleepcycle.com (logo OK) | — |
| 288 | Runkeeper Go | salud | 39,99 € | YEARLY | sin verificar | — | No | — | baja | runkeeper.com OK / logo OK | — |
| 289 | Fitbit Premium | salud | 9,99 € | MONTHLY | 8,99 €/mes | sin verificar | Sí | https://arkaiacorp.com/blog/google-fitbit-edge-precio-espana-fecha-caracteristicas | media | sin domain → fitbit.com (logo OK) | Desde el 7 de mayo de 2026 se llama **Google Health Premium**. También va incluido en Google AI Pro y Ultra |
| 290 | Freeletics Coach | salud | 12,99 € | MONTHLY | sin verificar | — | No | — | baja | freeletics.com OK / logo OK | Se vende sobre todo en planes de 3, 6 o 12 meses |
| 291 | Flo Premium | salud | 9,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → flo.health (logo OK) | — |
| 292 | Zwift | salud | 17,99 € | MONTHLY | 19,99 €/mes | 199,99 €/año | Sí | https://www.zwift.com/uk/news/33635-how-much-does-zwift-cost-and-whats-included-everything-you-need-to-know | media | sin domain → zwift.com (logo OK) | Precio "para Europa". La FAQ oficial (junio 2026) da 19,99 $ |
| 293 | TrainerRoad | salud | 19,99 € | MONTHLY | 21,99 USD/mes | 209,99 USD/año | Sí (moneda USD) | https://www.trainerroad.com/pricing | alta | sin domain → trainerroad.com (logo OK) | Solo cobra en USD. Poner `currency="USD"` |
| 294 | Sweat App | salud | 19,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → sweat.com (logo OK) | — |
| 295 | Centr Premium | salud | 29,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → centr.com (logo OK) | 29,99 € parece alto para una app. Revisar |
| 296 | Lifesum Premium | salud | 4,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → lifesum.com (logo OK) | — |
| 297 | Cronometer Gold | salud | 9,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → cronometer.com (logo OK) | Probablemente cobra en USD |
| 298 | BetterSleep Premium | salud | 6,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → bettersleep.com (logo OK) | — |
| 299 | Lumosity Premium | salud | 11,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → lumosity.com (logo OK) | — |
| 300 | Waking Up Premium | salud | 11,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → wakingup.com (logo OK) | — |
| 301 | Meditopia Premium | salud | 7,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → meditopia.com (logo OK) | — |
| 302 | 8fit Pro | salud | 9,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → 8fit.com (logo OK) | Comprobar si la app sigue activa |
| 303 | Daily Yoga Premium | salud | 9,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → dailyyoga.com (logo OK) | — |
| 304 | Endel Premium | salud | 5,99 € | MONTHLY | sin verificar | — | No | — | baja | sin domain → endel.io (logo OK) | — |
| 305 | Basic-Fit | salud | 24,99 € | MONTHLY | Comfort 24,99 € **cada 4 semanas** (≈27,07 €/mes). Premium 29,99 €, Ultimate 34,99 € | ≈324,87 €/año (13 cobros) | Opcional (equivalente mensual 27,07) | https://www.basic-fit.com/es-es/home | alta | basic-fit.com OK / logo OK | Matrícula 19,99 €. Promoción: 9,99 € las primeras 4 semanas. Se cobra cada 4 semanas, no cada mes |
| 306 | VivaGym | salud | 29,90 € | MONTHLY | One 22,90 €, Prime 29,90 € (según club) | — | No (cambiar domain) | https://www.vivagym.com/es-es/entrena-en-vivagym-torre-picasso/ | media | vivagym.es redirige a **vivagym.com**: cambiar domain (logo OK) | Lema "desde 1 € al día". El precio varía por club |
| 307 | Synergym | salud | 29,90 € | MONTHLY | sin verificar (reclamaciones citan 36,99 €) | — | No | https://www.provequity.com/news/vivagym-acquires-synergym-accelerate-its-long-term-growth-iberia | baja | synergym.es OK / **logo dudoso (relleno S)** | VivaGym acordó comprar Synergym en abril de 2026. De momento la marca sigue |
| 308 | McFit | salud | 29,90 € | MONTHLY | — | — | Sí (eliminar) | https://www.basic-fit.com/es-es/mcfit · https://www.reasonwhy.es/actualidad/mcfit-sale-de-espana-compra-gimnasios-basic-fit | alta | mcfit.com/es redirige a basic-fit.com | **Ya no existe en España**: Basic-Fit absorbió sus 42 clubes en 2024 |
| 309 | Anytime Fitness | salud | 49,00 € | MONTHLY | Gama media, 45-70 €/mes (comparador) | — | No | https://cuantomecuesta.com/es/gimnasio/ | baja | anytimefitness.es redirige a anytimefitness.com/es-es / logo OK | Es franquicia y el precio depende del club |
| 310 | Go fit | salud | 49,00 € | MONTHLY | sin verificar | — | No | — | baja | go-fit.es OK / logo OK | — |
| 311 | Holmes Place | salud | 80,00 € | MONTHLY | sin verificar | — | No | https://leisureopportunities.co.uk/news/Basic-Fit-selling-Spanish-Holmes-Place-clubs-to-Weon/353365 | baja | holmesplace.com OK / logo OK | Los 5 clubes españoles (3 en Madrid, 2 en Barcelona) pasaron a WeMet en 2024. holmesplace.com/es sigue activa |
| 464 | Iberdrola | hogar | 70,00 € | MONTHLY | ≈70 €/mes (orientativo, luz) | ≈840 €/año | No | OCU (ver tabla de bases) | media | iberdrola.es OK / logo pequeño (427 B) | — |
| 465 | Endesa | hogar | 70,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | No | OCU | media | endesa.com OK / logo OK | — |
| 466 | Naturgy | hogar | 65,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU | media | naturgy.es OK / logo OK | — |
| 467 | Repsol Luz y Gas | hogar | 65,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU | media | repsol.es OK / logo pequeño (496 B) | — |
| 468 | TotalEnergies | hogar | 65,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU | media | totalenergies.es OK / logo OK | Se quedó con los clientes de hogar de EDP |
| 469 | Holaluz | hogar | 60,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU · https://corporate.holaluz.com/downloads/press-notes/ndp_cierre_expediente_agosto2026.pdf | media | holaluz.com OK / logo OK | El MITECO archivó el 28 de agosto de 2026 el expediente de inhabilitación. Sigue operando, con una situación financiera frágil |
| 470 | Octopus Energy | hogar | 60,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU | media | octopusenergy.es OK / logo OK | — |
| 471 | EDP | hogar | 65,00 € | MONTHLY | — | — | Sí (eliminar) | https://roams.es/energia/companias-energeticas/edp/ | media | edpenergia.es redirige a /empresas | **Ya no vende a hogares**: la cartera residencial pasó a TotalEnergies (2020) y EDP solo atiende a empresas |
| 472 | Plenitude | hogar | 60,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU | media | cambiar a **eniplenitude.es** (web española, logo OK). eniplenitude.com es la global | — |
| 473 | Factor Energía | hogar | 60,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU | media | factorenergia.com OK / logo OK (Google solo 215 B) | — |
| 474 | Audax Renovables | hogar | 60,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU | media | audaxrenovables.com resuelve / **logo dudoso (relleno A)**. audaxrenovables.es también da relleno | Usar el favicon de Google (2388 B), que sí es real |
| 475 | Gana Energía | hogar | 55,00 € | MONTHLY | ≈70 €/mes | ≈840 €/año | Sí | OCU | media | ganaenergia.com OK (la web devuelve 403 a bots) / logo OK | — |
| 476 | Canal de Isabel II | hogar | 25,00 € | MONTHLY | sin verificar (Madrid no aparece en lo consultado) | — | No | — | baja | canaldeisabelsegunda.es OK / logo OK (G404) | Madrid está entre las ciudades más baratas según OCU, pero no hay cifra |
| 477 | Aigües de Barcelona | hogar | 30,00 € | MONTHLY | ≈43,00 €/mes | 516 €/año | Sí | OCU vía https://www.bankinter.com/blog/finanzas-personales/precio-agua-grifo-ciudades | media | aiguesdebarcelona.cat OK / logo OK | Barcelona es la ciudad más cara del estudio (150 m³/año) |
| 478 | Emasesa | hogar | 25,00 € | MONTHLY | ≈40,25 €/mes | 483 €/año | Sí | OCU vía Bankinter | media | emasesa.com OK / logo OK (G404) | Sevilla |
| 479 | Global Omnium (Aguas de Valencia) | hogar | 25,00 € | MONTHLY | ≈28,17 €/mes | 338 €/año | Sí | OCU vía Bankinter | media | globalomnium.com OK / **logo dudoso (relleno G)** | Valencia. Google da un favicon real de 3542 B |
| 480 | Emaya | hogar | 25,00 € | MONTHLY | ≈32,92 €/mes | 395 €/año | Sí | OCU vía Bankinter | media | emaya.es OK / **logo dudoso (relleno E, G404)** | Palma |
| 481 | Aqualia | hogar | 25,00 € | MONTHLY | sin verificar (varía por municipio) | — | No | — | baja | aqualia.com OK / **logo dudoso (relleno A)** | Google da 914 B (jpeg) |
| 482 | Hidralia | hogar | 25,00 € | MONTHLY | sin verificar | — | No | — | baja | hidralia-sa.es OK / logo pequeño (356 B; Google 248 B) | — |
| 483 | Securitas Direct | hogar | 45,00 € | MONTHLY | 54,99 €/mes (kit Residencial). Rango citado: 46,50-54,99 € | ≈660 €/año | Sí | https://selectra.es/alarmas/companias/securitas-direct · https://forbes.es/economia/908254/securitas-direct-cambia-su-nombre-en-espana-a-verisure-marca-con-la-que-opera-a-nivel-global/ | media | Proponer **verisure.es** (logo OK). securitasdirect.es sigue activa | **Se llama Verisure desde el 17 de abril de 2026**: renombrar a "Verisure (antes Securitas Direct)". Equipo desde 149 €. La cuota se revisa con el IPC cada enero. Mantener `variablePrice=false` (test) |
| 484 | Prosegur Alarmas | hogar | 40,00 € | MONTHLY | 48,90 €/mes | ≈586,80 €/año | Sí | https://selectra.es/alarmas/companias/prosegur/precio | media | Proponer **movistarproseguralarmas.es** (logo OK). prosegur.es es la web corporativa | La marca comercial es "Movistar Prosegur Alarmas". Equipo en alquiler incluido. Permanencia de 24 meses |
| 485 | ADT Alarmas | hogar | 35,00 € | MONTHLY | 42,35 €/mes (kit Basic). Standard 44,77 €, Advance 48,40 € | ≈508 €/año | Sí | https://selectra.es/alarmas/companias/johnson-controls (actualizado 19/10/2026) | media | adt.es OK / **logo dudoso (relleno A, G404)** | Promoción: 19,90 € los 4 primeros meses. Desde el 1 de abril de 2026 ADT España pertenece a Sector Alarm |
| 486 | Tyco Alarmas | hogar | 35,00 € | MONTHLY | — | — | Sí (eliminar) | misma fuente que ADT | media | tyco.es no resuelve (DNS) | **Ya no existe**: la marca Tyco pasó a llamarse ADT en España en 2020. La fila duplica ADT |
| 487 | Comunidad de propietarios | hogar | 80,00 € | MONTHLY | sin verificar (rangos regionales de 50-150 €/mes) | — | No | https://www.mallorca.com/es/consejos/inmobiliaria/impuestos-derecho/comunidad-de-propietarios-espana | baja | sin domain (correcto) | No hay una media nacional publicada |
| 511 | Mapfre | seguros | 450,00 € | YEARLY | 390-570 €/año en coche. Hogar 281 € | — | No (dentro del rango) | CHECK24, Kelisto, Codeoscopic (ver bases) | media | mapfre.es OK / logo OK | Aseguradora multirramo. No hay prima media por compañía |
| 512 | AXA | seguros | 420,00 € | YEARLY | ídem | — | No | ídem | media | axa.es OK / logo OK (991 B) | — |
| 513 | Allianz | seguros | 430,00 € | YEARLY | ídem | — | No | ídem | media | allianz.es OK / **logo dudoso (relleno A)** | Google da un favicon real de 4614 B |
| 514 | Mutua Madrileña | seguros | 400,00 € | YEARLY | ídem | — | No | ídem | media | mutua.es OK / logo OK | — |
| 515 | Línea Directa | seguros | 350,00 € | YEARLY | ≈390 €/año (coche) | 390 € | Sí | CHECK24 (mayo 2026) | media | lineadirecta.com OK / logo OK | Se usa la prima media de coche porque la marca se centra en motor |
| 516 | Génesis | seguros | 330,00 € | YEARLY | ≈390 €/año | 390 € | Sí | CHECK24 | media | genesis.es OK / logo OK | — |
| 517 | Pelayo | seguros | 380,00 € | YEARLY | 390-570 € coche. Hogar 281 € | — | No | ídem | media | pelayo.com OK / logo OK | — |
| 518 | Reale | seguros | 400,00 € | YEARLY | ídem | — | No | ídem | media | reale.es OK / logo OK (641 B, G404) | — |
| 519 | Generali | seguros | 420,00 € | YEARLY | ídem | — | No | ídem | media | generali.es OK / logo OK | — |
| 520 | Zurich | seguros | 430,00 € | YEARLY | ídem | — | No | ídem | media | zurich.es OK / logo OK | — |
| 521 | Caser | seguros | 400,00 € | YEARLY | ídem | — | No | ídem | media | caser.es OK / logo pequeño (367 B) | Caser también vende salud (ver Servicios que faltan) |
| 522 | Catalana Occidente | seguros | 420,00 € | YEARLY | ídem | — | No (renombrar y cambiar domain) | https://www.gco.com/eng/press/nortehispana-seguros-se-integra-occident | alta (marca) / media (precio) | catalanaoccidente.com **no resuelve**. Proponer occident.com, aunque **ambos dan relleno (C y O)**: logo dudoso | Desde el 1 de enero de 2024 la marca es **Occident** (fusión de Seguros Catalana Occidente, Plus Ultra y Seguros Bilbao; NorteHispana se sumó en 2025) |
| 523 | Ocaso | seguros | 300,00 € | YEARLY | ≈280 €/año | 280 € | Sí | Codeoscopic: hogar 281 €, decesos 222 € (ver bases) | baja | ocaso.es OK / logo OK | Se toma la prima media de hogar como referencia (decesos sale más barato) |
| 524 | Santalucía | seguros | 300,00 € | YEARLY | ≈280 €/año | 280 € | Sí | ídem | baja | santalucia.es OK / **logo dudoso (relleno S; Google 101 B)** | — |
| 525 | Fiatc | seguros | 380,00 € | YEARLY | 390-570 € coche. Hogar 281 € | — | No | ídem | media | fiatc.es OK / logo pequeño (492 B) | Fiatc también vende salud (ver Servicios que faltan) |
| 526 | Verti | seguros | 320,00 € | YEARLY | ≈390 €/año | 390 € | Sí | CHECK24 | media | verti.es OK / logo OK (G404) | Marca online de Mapfre centrada en motor |
| 527 | Qualitas Auto | seguros | 340,00 € | YEARLY | ≈390 €/año | 390 € | Sí | CHECK24 | media | qualitasauto.com: solo responde con www / logo OK | — |
| 528 | Balumba | seguros | 300,00 € | YEARLY | ≈390 €/año | 390 € | Sí | CHECK24 | media | balumba.es OK / logo pequeño (242 B) | — |
| 529 | Sanitas | seguros | 60,00 € | MONTHLY | ≈60 €/mes (Accede 31,30; Más Salud Plus 78,15; Más Salud 93,51) | — | No | https://selectra.es/seguros/seguros-salud/precio | media | sanitas.es OK / logo pequeño (264 B) | Perfil de 48 años. Media de mercado 61 €/mes |
| 530 | Adeslas | seguros | 55,00 € | MONTHLY | Plena Vital 61,50 €/mes (Go 21 €, Plena Plus 92 €) | — | No (opcional: 61,50) | ídem | media | adeslas.es redirige a **segurcaixaadeslas.es**: cambiar domain (mismo logo, OK) | — |
| 531 | DKV | seguros | 55,00 € | MONTHLY | Integral Plus 52,03 €/mes (Élite 94,80 €) | — | No | ídem | media | dkv.es OK / logo OK | — |
| 532 | Asisa | seguros | 50,00 € | MONTHLY | sin verificar (media de mercado 61 €) | — | No | ídem (Asisa no aparece) | baja | asisa.es OK / logo pequeño (407 B) | — |
| 533 | Cosalud | seguros | 45,00 € | MONTHLY | sin verificar | — | Revisar (marca integrada) | https://roams.es/seguros/aseguradoras/plus-ultra-seguros/seguro-salud | baja | cosalud.com redirige a profesionalesmedicos.occident.com: **domain dudoso** | Cosalud era un producto de salud de Plus Ultra, hoy Occident. Plantearse renombrar a "Occident Salud" o retirarla |
| 534 | Barkibu | seguros | 25,00 € | MONTHLY | sin verificar ("desde 33,49 €/mes", sin fecha) | — | No | https://selectra.es/seguros/aseguradoras/barkibu | baja | barkibu.com OK (redirige a /es/) / logo OK | Depende de la edad, el peso y el sexo del animal |
| 535 | Santévet | seguros | 30,00 € | MONTHLY | sin verificar | — | No | https://selectra.es/seguros/seguros-mascotas/precio-seguro-perro (media de perro ≈11 €/mes) | baja | santevet.es OK / logo OK | — |

## Servicios que faltan

| Nombre | Precio | Ciclo | domain | Fuente | Notas |
|---|---|---|---|---|---|
| Sector Alarm | 39,99 €/mes (Kit Smart Home) | MONTHLY | sectoralarm.es (logo OK) | https://selectra.es/alarmas/comparador | Cuota fija (`variablePrice=false`). El equipo puede costar hasta 699 € según la oferta. Permanencia de 24 meses. Desde abril de 2026 también es dueña de ADT |
| Strava + Runna | 129,99 €/año | YEARLY | strava.com | https://www.strava.com/pricing?hl=es-ES | Pack oficial, solo anual |
| Caser Salud (Integral con copago) | 49,44 €/mes | MONTHLY | caser.es | https://selectra.es/seguros/seguros-salud/precio | Perfil de 48 años. `variablePrice=true` |
| FIATC Salud (Medifiatc P15) | 52,21 €/mes | MONTHLY | fiatc.es | ídem | Perfil de 48 años. `variablePrice=true` |
| Curenergía (PVPC, grupo Iberdrola) | ≈70 €/mes (orientativo) | MONTHLY | curenergia.es (logo OK) | OCU (factura PVPC 2026) | Comercializadora de referencia (PVPC/TUR) |
| Energía XXI (PVPC, grupo Endesa) | ≈70 €/mes | MONTHLY | energiaxxi.com (logo OK) | OCU | — |
| Comercializadora Regulada (PVPC, grupo Naturgy) | ≈70 €/mes | MONTHLY | comercializadoraregulada.es (icon.horse OK; G404) | OCU | — |
| Som Energia | ≈70 €/mes | MONTHLY | somenergia.coop (logo OK) | OCU (orientativo genérico) | Cooperativa muy conocida |
| Pepeenergy | ≈70 €/mes | MONTHLY | pepeenergy.com (logo OK) | OCU (orientativo genérico) | — |
| Gas natural TUR (filas de gas aparte) | ≈52-59 €/mes | MONTHLY | según comercializadora | Papernest y Roams (octubre 2026) | Hoy el gas está mezclado en las filas de luz |
| Altafit | sin verificar | MONTHLY | altafitgymclub.com (logo OK) | — | Cadena low-cost con mucha presencia |
| Dreamfit | sin verificar | MONTHLY | dreamfit.es (logo OK) | — | — |
| Fitness Park | sin verificar | MONTHLY | fitnesspark.es (logo OK) | — | — |
| Helvetia Seguros | sin verificar (usar la base multirramo de 390-570 €) | YEARLY | helvetia.es (logo OK) | — | — |
| Direct Seguros | sin verificar (base de coche ≈390 €) | YEARLY | directseguros.es (logo pequeño, 539 B) | — | — |
| Cigna / Aegon (salud) | sin verificar (media de mercado 61 €/mes) | MONTHLY | cigna.es / aegon.es (logo OK) | https://selectra.es/seguros/seguros-salud/precio (media) | — |
| Kozoo (mascotas) | sin verificar | MONTHLY | kozoo.eu (logo OK) | — | — |

Se descartan como dudosos: libertyseguros.es (logo de relleno L, G404), regalseguros.es (relleno R, G404), imqseguros.es (relleno I, G404), mutuageneral.es y metropolitan.es (relleno M, G404). Sus dominios no se pudieron confirmar como oficiales.

## Servicios que ya no existen o han cambiado de marca

| Fila | Estado | Acción propuesta |
|---|---|---|
| McFit (L308) | Ya no existe en España: absorbido por Basic-Fit (2024). mcfit.com/es redirige a basic-fit.com | Eliminar |
| EDP (L471) | No vende a hogares: los clientes residenciales pasaron a TotalEnergies | Eliminar |
| Tyco Alarmas (L486) | La marca desapareció en 2020 (pasó a ADT) y tyco.es no resuelve | Eliminar (duplica ADT) |
| Securitas Direct (L483) | Se llama Verisure desde el 17 de abril de 2026 | Renombrar a "Verisure (antes Securitas Direct)" y usar domain verisure.es |
| Catalana Occidente (L522) | Se llama Occident desde el 1 de enero de 2024 | Renombrar y usar domain occident.com |
| Cosalud (L533) | Producto de Plus Ultra integrado en Occident. El dominio redirige al portal de profesionales | Renombrar o retirar |
| Fitbit Premium (L289) | Se llama Google Health Premium desde el 7 de mayo de 2026 | Renombrar |
| Strava Summit (L277) | El nombre "Summit" ya no se usa | Renombrar a "Strava" |
| Prosegur Alarmas (L484) | La marca comercial es Movistar Prosegur Alarmas | Renombrar y usar domain movistarproseguralarmas.es |
| Holmes Place (L311) | Los clubes españoles pasaron a WeMet (2024). La web sigue activa | Mantener y vigilar |
| Synergym (L307) | En proceso de compra por VivaGym (abril 2026). La marca se mantiene por ahora | Mantener y vigilar |
| Peloton App (L285) | Peloton no opera en España | Plantearse retirarla |

## Resumen

- Filas revisadas: **85** (35 de salud, 24 de hogar y 25 de seguros).
- Cambian de precio, ciclo o moneda: **30**. Hay que eliminar **3** (McFit, EDP, Tyco). Solo cambian nombre o domain: Catalana Occidente→Occident, VivaGym (vivagym.com), Adeslas (segurcaixaadeslas.es) y Cosalud (revisar). Basic-Fit queda como opcional, por el cobro cada 4 semanas.
- Quedan **sin verificar** 31 filas: 20 apps de salud, 3 gimnasios, 4 recibos de hogar (Canal de Isabel II, Aqualia, Hidralia y comunidad de propietarios) y 4 seguros (Asisa, Cosalud, Barkibu y Santévet). Se mantiene su precio.
- Se respetan los ciclos de los tests: Mapfre y Línea Directa siguen YEARLY, y Sanitas y Adeslas siguen MONTHLY. Todas las filas variables conservan `variablePrice=true` y Securitas/Verisure sigue en `false`.
