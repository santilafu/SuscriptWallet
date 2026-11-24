package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.CatalogItem
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Proporciona el catálogo de servicios de suscripción conocidos con sus precios en euros.
 *
 * Todos los precios están revisados a noviembre de 2026 y expresados en EUR (precios reales
 * del mercado español, IVA incluido donde corresponda). Las fuentes de cada precio están en
 * `docs/catalogo/revision-2026-11/`.
 * El catálogo es estático y vive en memoria; no se persiste en base de datos.
 * Para actualizar un precio, edita el valor en la lista correspondiente dentro de [baseCatalog].
 *
 * Claves de categoría disponibles (ver [CategoryMappingService.NAME_TO_KEY]):
 *   "ia", "streaming", "musica", "software", "cloud", "gaming", "seguridad",
 *   "noticias", "salud", "desarrollo", "finanzas", "educacion", "creatividad", "citas",
 *   "hogar", "telecos", "seguros", "transporte", "belleza", "prestamos" y "prueba" (derivada, ver [TRIALS]).
 *
 * Préstamos y financiaciones: `price = null` porque la cuota depende del contrato de cada usuario.
 *
 * Recibos recurrentes (luz, gas, agua, telecos, seguros…): el precio es un importe
 * orientativo y el item lleva `variablePrice = true` para que la UI lo indique.
 *
 * Servicios que cobran en dólares: el catálogo solo guarda euros, así que el precio publicado
 * en USD (sin IVA) se convierte con `EUR = USD / 1,1052 × 1,21`, redondeado a 2 decimales
 * (HALF_UP). 1,1052 USD/EUR es el tipo de referencia del BCE del 13-11-2026 y 1,21 añade el
 * IVA español, que se suma al facturar a un particular. Como el cargo real depende del cambio
 * del día, esas filas llevan `variablePrice = true` (la app las muestra con "≈") y la
 * descripción termina en "Se cobra en USD; importe aproximado". Si un precio en dólares ya
 * incluye impuestos (Setapp), solo se aplica el cambio de moneda.
 */
@Service
class CatalogService {

    /**
     * Mapa clave → lista de servicios (sin la categoría derivada "prueba"). Todos los precios
     * están en EUR.
     */
    private val baseCatalog: Map<String, List<CatalogItem>> = mapOf(

        // ── Inteligencia Artificial ──────────────────────────────────────────────
        "ia" to listOf(
            CatalogItem("Claude Pro",              BigDecimal("21.78"),  "EUR", BillingCycle.MONTHLY, "Anthropic Claude Pro — 18 € + IVA; acceso prioritario, Claude Code y límites ampliados",                "ia", domain = "claude.ai",          cancelUrl = "https://claude.ai/settings/billing"),
            CatalogItem("Claude Max", BigDecimal("108.90"), "EUR", BillingCycle.MONTHLY, "Anthropic Claude Max — 5× el uso de Pro e incluye créditos de API (90 € + IVA)", "ia", domain = "claude.ai", cancelUrl = "https://claude.ai/settings/billing"),
            CatalogItem("Claude Ultra", BigDecimal("217.80"), "EUR", BillingCycle.MONTHLY, "Anthropic Claude Ultra (antes Max 20x) — 10× el uso semanal de Pro y sin límite de 5 h (180 € + IVA)", "ia", domain = "claude.ai", cancelUrl = "https://claude.ai/settings/billing"),
            CatalogItem("Claude Team", BigDecimal("27.37"), "EUR", BillingCycle.MONTHLY, "Anthropic Claude Team Standard — por asiento/mes, colaboración y administración — Se cobra en USD; importe aproximado", "ia", domain = "claude.ai", cancelUrl = "https://claude.ai/settings/billing", priceAnnual = BigDecimal("262.76"), variablePrice = true),
            CatalogItem("ChatGPT Plus", BigDecimal("23.00"), "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Plus — modelos avanzados con límites ampliados", "ia", domain = "chatgpt.com", cancelUrl = "https://chatgpt.com/settings"),
            CatalogItem("ChatGPT Pro 200", BigDecimal("229.00"), "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Pro 200 — 10× el uso de Plus y acceso a los modelos Pro", "ia", domain = "chatgpt.com", cancelUrl = "https://chatgpt.com/settings"),
            CatalogItem("ChatGPT Go", BigDecimal("8.00"), "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Go — plan económico con más mensajes e imágenes que el gratuito", "ia", domain = "chatgpt.com", cancelUrl = "https://chatgpt.com/settings"),
            CatalogItem("ChatGPT Pro 100", BigDecimal("103.00"), "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Pro 100 — 5× el uso de Plus y acceso a los modelos Pro", "ia", domain = "chatgpt.com", cancelUrl = "https://chatgpt.com/settings"),
            CatalogItem("ChatGPT Pro 500", BigDecimal("547.41"), "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Pro 500 — el máximo uso de los modelos Pro — Se cobra en USD; importe aproximado", "ia", domain = "chatgpt.com", cancelUrl = "https://chatgpt.com/settings", variablePrice = true),
            CatalogItem("Cursor Premium", BigDecimal("32.84"), "EUR", BillingCycle.MONTHLY, "Editor de código con IA — plan Premium (sustituye al antiguo Pro) — Se cobra en USD; importe aproximado", "ia", domain = "cursor.com", cancelUrl = "https://www.cursor.com/settings", priceAnnual = BigDecimal("328.45"), variablePrice = true),
            CatalogItem("GitHub Copilot Pro", BigDecimal("10.95"), "EUR", BillingCycle.MONTHLY, "Asistente de código IA para IDEs — plan Copilot Pro — Se cobra en USD; importe aproximado", "ia", domain = "github.com", cancelUrl = "https://github.com/settings/copilot", variablePrice = true),
            CatalogItem("Google AI Pro", BigDecimal("21.99"), "EUR", BillingCycle.MONTHLY, "Google AI Pro (antes Gemini Advanced) — Gemini, Veo y 5 TB compartidos entre Drive, Gmail y Fotos; se comparte con hasta 5 personas", "ia", domain = "gemini.google.com", cancelUrl = "https://one.google.com/about", priceAnnual = BigDecimal("219.99")),
            CatalogItem("Perplexity Pro", BigDecimal("21.90"), "EUR", BillingCycle.MONTHLY, "Motor de búsqueda con IA — búsquedas ilimitadas y modelos avanzados — Se cobra en USD; importe aproximado", "ia", domain = "perplexity.ai", cancelUrl = "https://www.perplexity.ai/settings/account", priceAnnual = BigDecimal("218.96"), variablePrice = true),
            CatalogItem("Midjourney Standard", BigDecimal("32.84"), "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA — 15 horas GPU/mes — Se cobra en USD; importe aproximado", "ia", domain = "midjourney.com", cancelUrl = "https://www.midjourney.com/account", priceAnnual = BigDecimal("315.31"), iconUrl = "https://www.google.com/s2/favicons?domain=midjourney.com&sz=128", variablePrice = true),
            CatalogItem("Midjourney Basic", BigDecimal("10.95"), "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA — 3,3 horas GPU/mes — Se cobra en USD; importe aproximado", "ia", domain = "midjourney.com", cancelUrl = "https://www.midjourney.com/account", priceAnnual = BigDecimal("105.10"), iconUrl = "https://www.google.com/s2/favicons?domain=midjourney.com&sz=128", variablePrice = true),
            CatalogItem("Grok SuperGrok", BigDecimal("32.84"), "EUR", BillingCycle.MONTHLY, "xAI SuperGrok — acceso ampliado a los modelos Grok — Se cobra en USD; importe aproximado", "ia", domain = "grok.com", cancelUrl = "https://grok.com/", variablePrice = true),
            CatalogItem("Canva Pro",               BigDecimal("12.00"),  "EUR", BillingCycle.MONTHLY, "Diseño gráfico con IA — plantillas premium, Magic Studio y marca",              "ia", domain = "canva.com",          cancelUrl = "https://www.canva.com/settings/purchase-history", priceAnnual = BigDecimal("110.00")),
            CatalogItem("ElevenLabs Creator", BigDecimal("24.09"), "EUR", BillingCycle.MONTHLY, "Síntesis de voz IA realista — plan Creator — Se cobra en USD; importe aproximado", "ia", domain = "elevenlabs.io", cancelUrl = "https://elevenlabs.io/subscription", priceAnnual = BigDecimal("240.86"), variablePrice = true),
            CatalogItem("Runway Standard", BigDecimal("16.42"), "EUR", BillingCycle.MONTHLY, "Generación y edición de vídeo con IA — Se cobra en USD; importe aproximado", "ia", domain = "runwayml.com", cancelUrl = "https://app.runwayml.com/settings", priceAnnual = BigDecimal("157.65"), variablePrice = true),
            CatalogItem("Character.AI Plus", BigDecimal("10.94"), "EUR", BillingCycle.MONTHLY, "Chat con personajes IA — c.ai+ — Se cobra en USD; importe aproximado", "ia", domain = "character.ai", cancelUrl = "https://character.ai/subscription", priceAnnual = BigDecimal("104.00"), variablePrice = true),
            CatalogItem("Suno Pro", BigDecimal("10.95"), "EUR", BillingCycle.MONTHLY, "Generación de música con IA — Se cobra en USD; importe aproximado", "ia", domain = "suno.com", cancelUrl = "https://suno.com/account", priceAnnual = BigDecimal("105.10"), variablePrice = true),
            CatalogItem("Leonardo AI Artisan", BigDecimal("32.84"), "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA para creadores — Se cobra en USD; importe aproximado", "ia", domain = "leonardo.ai", cancelUrl = "https://app.leonardo.ai/settings", priceAnnual = BigDecimal("315.31"), iconUrl = "https://www.google.com/s2/favicons?domain=leonardo.ai&sz=128", variablePrice = true),
            CatalogItem("Poe Basic",               BigDecimal("19.99"),  "EUR", BillingCycle.MONTHLY, "Acceso a múltiples modelos IA (Claude, GPT-4…)",                                "ia", domain = "poe.com",              cancelUrl = "https://poe.com/settings"),
            CatalogItem("Jasper Pro", BigDecimal("75.54"), "EUR", BillingCycle.MONTHLY, "Jasper Pro — redacción de contenido con IA para marketing (por asiento) — Se cobra en USD; importe aproximado", "ia", trialDays = 7, domain = "jasper.ai", cancelUrl = "https://app.jasper.ai/settings/billing", priceAnnual = BigDecimal("775.14"), variablePrice = true),
            CatalogItem("Adobe Firefly Standard", BigDecimal("11.17"), "EUR", BillingCycle.MONTHLY, "Adobe Firefly Standard — generación de imágenes y vídeo IA con 2 000 créditos/mes", "ia", trialDays = 30, domain = "firefly.adobe.com", cancelUrl = "https://account.adobe.com/plans"),
            CatalogItem("Krea AI Pro", BigDecimal("38.32"), "EUR", BillingCycle.MONTHLY, "Generación y mejora de imágenes en tiempo real — Se cobra en USD; importe aproximado", "ia", domain = "krea.ai", cancelUrl = "https://www.krea.ai/settings", variablePrice = true)
        ),

        // ── Streaming de vídeo ───────────────────────────────────────────────────
        "streaming" to listOf(
            CatalogItem("Netflix Estándar",        BigDecimal("14.99"),  "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD, sin anuncios",                                   "streaming", domain = "netflix.com",       cancelUrl = "https://www.netflix.com/cancelplan"),
            CatalogItem("Netflix Premium",         BigDecimal("21.99"),  "EUR", BillingCycle.MONTHLY, "Netflix — 4 pantallas Ultra HD, sin anuncios",                                  "streaming", domain = "netflix.com",       cancelUrl = "https://www.netflix.com/cancelplan"),
            CatalogItem("Netflix con anuncios",    BigDecimal("8.99"),   "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD con anuncios",                                    "streaming", domain = "netflix.com",       cancelUrl = "https://www.netflix.com/cancelplan"),
            CatalogItem("Disney+",                 BigDecimal("10.99"),  "EUR", BillingCycle.MONTHLY, "Disney+ Estándar — Disney, Marvel, Star Wars y National Geographic",             "streaming", domain = "disneyplus.com",    cancelUrl = "https://www.disneyplus.com/account", priceAnnual = BigDecimal("109.90")),
            CatalogItem("Disney+ con anuncios",  BigDecimal("6.99"),   "EUR", BillingCycle.MONTHLY, "Disney+ Estándar con anuncios — Full HD y 2 pantallas", "streaming", domain = "disneyplus.com", cancelUrl = "https://www.disneyplus.com/account"),
            CatalogItem("Disney+ Premium",       BigDecimal("15.99"),  "EUR", BillingCycle.MONTHLY, "Disney+ Premium — 4K HDR, Dolby Atmos y 4 pantallas", "streaming", domain = "disneyplus.com", cancelUrl = "https://www.disneyplus.com/account", priceAnnual = BigDecimal("159.90")),
            CatalogItem("Amazon Prime", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Amazon Prime — envíos gratis + Prime Video + Prime Music", "streaming", domain = "amazon.es", cancelUrl = "https://www.amazon.es/mc/pipelines/cancellation", priceAnnual = BigDecimal("49.90")),
            CatalogItem("YouTube Premium", BigDecimal("15.99"), "EUR", BillingCycle.MONTHLY, "YouTube sin anuncios, reproducción en segundo plano y YouTube Music", "streaming", domain = "youtube.com", cancelUrl = "https://www.youtube.com/paid_memberships"),
            CatalogItem("YouTube Premium Familiar", BigDecimal("29.99"), "EUR", BillingCycle.MONTHLY, "YouTube Premium para hasta 6 miembros del hogar", "streaming", domain = "youtube.com", cancelUrl = "https://www.youtube.com/paid_memberships"),
            CatalogItem("YouTube Premium Estudiantes", BigDecimal("10.49"), "EUR", BillingCycle.MONTHLY, "YouTube Premium con descuento para estudiantes verificados", "streaming", domain = "youtube.com", cancelUrl = "https://www.youtube.com/paid_memberships"),
            CatalogItem("YouTube Premium Lite", BigDecimal("8.99"), "EUR", BillingCycle.MONTHLY, "YouTube Premium Lite — la mayoría de vídeos sin anuncios, sin YouTube Music", "streaming", domain = "youtube.com", cancelUrl = "https://www.youtube.com/paid_memberships"),
            CatalogItem("Apple TV", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Apple TV (antes Apple TV+) — series y películas originales de Apple", "streaming", domain = "tv.apple.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("HBO Max Básico con anuncios", BigDecimal("6.99"), "EUR", BillingCycle.MONTHLY, "HBO Max — plan Básico con anuncios, series y películas HBO y Warner", "streaming", domain = "hbomax.com", cancelUrl = "https://www.hbomax.com/es/es/account/subscription", priceAnnual = BigDecimal("69.90")),
            CatalogItem("HBO Max Estándar", BigDecimal("10.99"), "EUR", BillingCycle.MONTHLY, "HBO Max Estándar — sin anuncios, Full HD, 2 pantallas y descargas", "streaming", domain = "hbomax.com", cancelUrl = "https://www.hbomax.com/es/es/account/subscription", priceAnnual = BigDecimal("109.00")),
            CatalogItem("HBO Max Premium", BigDecimal("15.99"), "EUR", BillingCycle.MONTHLY, "HBO Max Premium — 4K, Dolby Atmos, 4 pantallas y 100 descargas", "streaming", domain = "hbomax.com", cancelUrl = "https://www.hbomax.com/es/es/account/subscription", priceAnnual = BigDecimal("159.00")),
            CatalogItem("Crunchyroll Mega Fan",    BigDecimal("7.49"),   "EUR", BillingCycle.MONTHLY, "Crunchyroll — anime en simulcast sin anuncios, acceso offline",                 "streaming", domain = "crunchyroll.com",   cancelUrl = "https://www.crunchyroll.com/account/membership", priceAnnual = BigDecimal("59.99")),
            CatalogItem("Crunchyroll Fan", BigDecimal("5.99"), "EUR", BillingCycle.MONTHLY, "Crunchyroll Fan — anime en simulcast sin anuncios", "streaming", domain = "crunchyroll.com", cancelUrl = "https://www.crunchyroll.com/account/membership", priceAnnual = BigDecimal("47.99")),
            CatalogItem("DAZN",                    BigDecimal("25.99"),  "EUR", BillingCycle.MONTHLY, "DAZN Premium — todo el deporte (pago mensual con compromiso anual)",                         "streaming", domain = "dazn.com",          cancelUrl = "https://www.dazn.com/es-ES/account/subscription", priceAnnual = BigDecimal("285.99")),
            CatalogItem("DAZN Fútbol",           BigDecimal("14.99"),  "EUR", BillingCycle.MONTHLY, "DAZN Plan Fútbol — LaLiga EA Sports, Hypermotion, Premier League y Liga F (compromiso anual)", "streaming", domain = "dazn.com", cancelUrl = "https://www.dazn.com/es-ES/account/subscription", priceAnnual = BigDecimal("164.99")),
            CatalogItem("DAZN sin permanencia", BigDecimal("44.99"), "EUR", BillingCycle.MONTHLY, "DAZN Premium — todo el deporte, pago mensual sin permanencia", "streaming", domain = "dazn.com", cancelUrl = "https://www.dazn.com/es-ES/account/subscription"),
            CatalogItem("DAZN Fútbol sin permanencia", BigDecimal("29.99"), "EUR", BillingCycle.MONTHLY, "DAZN Plan Fútbol — pago mensual sin permanencia", "streaming", domain = "dazn.com", cancelUrl = "https://www.dazn.com/es-ES/account/subscription"),
            CatalogItem("Movistar+", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Movistar Plus+ Plan Libre — cine, series y deportes sin permanencia ni fibra Movistar", "streaming", domain = "movistar.es", cancelUrl = "https://www.movistar.es/particulares/television/movistar-plus/", priceAnnual = BigDecimal("99.90")),
            CatalogItem("Movistar Plus+ Cine y Series", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Movistar Plus+ — plan de cine y series sin permanencia", "streaming", domain = "movistar.es", cancelUrl = "https://www.movistar.es/particulares/television/movistar-plus/"),
            CatalogItem("Filmin",                  BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Filmin — cine independiente, clásicos y festivales",                            "streaming", domain = "filmin.es",         cancelUrl = "https://www.filmin.es/ajustes", priceAnnual = BigDecimal("99.00")),
            CatalogItem("Atresplayer Premium",   BigDecimal("5.99"),   "EUR", BillingCycle.MONTHLY, "Atresplayer Premium (con anuncios) — series y programas de Antena 3 y laSexta en avance", "streaming", domain = "atresplayer.com", cancelUrl = "https://www.atresplayer.com/usuario/suscripciones/", priceAnnual = BigDecimal("59.99")),
            CatalogItem("SkyShowtime", BigDecimal("8.99"), "EUR", BillingCycle.MONTHLY, "SkyShowtime Estándar — Paramount+, Peacock, Universal y Nickelodeon sin anuncios", "streaming", domain = "skyshowtime.com", cancelUrl = "https://www.skyshowtime.com/account"),
            CatalogItem("SkyShowtime con anuncios", BigDecimal("5.99"), "EUR", BillingCycle.MONTHLY, "SkyShowtime Estándar con anuncios", "streaming", domain = "skyshowtime.com", cancelUrl = "https://www.skyshowtime.com/account"),
            CatalogItem("SkyShowtime Prémium", BigDecimal("13.99"), "EUR", BillingCycle.MONTHLY, "SkyShowtime Prémium — sin anuncios, 4K y más pantallas", "streaming", domain = "skyshowtime.com", cancelUrl = "https://www.skyshowtime.com/account"),
            CatalogItem("Twitch Turbo", BigDecimal("11.99"), "EUR", BillingCycle.MONTHLY, "Twitch Turbo — sin anuncios en todos los canales", "streaming", domain = "twitch.tv", cancelUrl = "https://www.twitch.tv/settings/turbo"),
            CatalogItem("Apple One", BigDecimal("19.95"), "EUR", BillingCycle.MONTHLY, "Apple One Individual — Apple TV, Music, Arcade e iCloud+ en un plan", "streaming", trialDays = 30, domain = "apple.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Apple One Familiar", BigDecimal("27.95"), "EUR", BillingCycle.MONTHLY, "Apple One Familiar — Apple TV, Music, Arcade e iCloud+ para hasta 6 personas", "streaming", domain = "apple.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Apple One Premium", BigDecimal("36.95"), "EUR", BillingCycle.MONTHLY, "Apple One Premium — Familiar más Fitness+ y 2 TB de iCloud+", "streaming", domain = "apple.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Mubi", BigDecimal("11.99"), "EUR", BillingCycle.MONTHLY, "MUBI — cine de autor, premiado y de culto", "streaming", trialDays = 7, domain = "mubi.com", cancelUrl = "https://mubi.com/es/account", priceAnnual = BigDecimal("95.88")),
            CatalogItem("Plex Pass",               BigDecimal("4.99"),   "EUR", BillingCycle.MONTHLY, "Plex Pass — streaming de tu biblioteca personal + canales premium",              "streaming", domain = "plex.tv",           cancelUrl = "https://www.plex.tv/plex-pass/"),
            CatalogItem("Nebula",                  BigDecimal("7.00"),   "EUR", BillingCycle.MONTHLY, "Contenido original de creadores de YouTube",                                    "streaming", 30, "nebula.tv",          "https://nebula.tv/account/subscription", priceAnnual = BigDecimal("70.00")),
            CatalogItem("CuriosityStream",         BigDecimal("4.99"),   "EUR", BillingCycle.MONTHLY, "Documentales científicos y de naturaleza",                                      "streaming", 7,  "curiositystream.com","https://curiositystream.com/account/subscription"),
            CatalogItem("Viki Pass Standard",      BigDecimal("5.99"),   "EUR", BillingCycle.MONTHLY, "Dramas y películas asiáticas subtituladas",                                     "streaming", 7,  "viki.com",           "https://www.viki.com/settings/subscription", priceAnnual = BigDecimal("59.99"))
        ),

        // ── Música ───────────────────────────────────────────────────────────────
        "musica" to listOf(
            CatalogItem("Spotify Premium",         BigDecimal("11.99"),  "EUR", BillingCycle.MONTHLY, "Spotify — música y podcasts sin anuncios, escucha sin conexión",                "musica", domain = "spotify.com",    cancelUrl = "https://www.spotify.com/es/account/subscription/"),
            CatalogItem("Spotify Premium Duo",   BigDecimal("16.99"),  "EUR", BillingCycle.MONTHLY, "Spotify Duo — 2 cuentas Premium para dos personas que viven juntas", "musica", domain = "spotify.com", cancelUrl = "https://www.spotify.com/es/account/subscription/"),
            CatalogItem("Spotify Premium Familiar",BigDecimal("20.99"),  "EUR", BillingCycle.MONTHLY, "Spotify Familiar — hasta 6 cuentas Premium con control parental", "musica", domain = "spotify.com", cancelUrl = "https://www.spotify.com/es/account/subscription/"),
            CatalogItem("Spotify Premium Estudiantes",BigDecimal("6.49"),   "EUR", BillingCycle.MONTHLY, "Spotify para estudiantes verificados — Premium con descuento", "musica", domain = "spotify.com", cancelUrl = "https://www.spotify.com/es/account/subscription/"),
            CatalogItem("Apple Music", BigDecimal("11.99"), "EUR", BillingCycle.MONTHLY, "Apple Music — 100 millones de canciones, lossless y Dolby Atmos", "musica", domain = "music.apple.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Apple Music Familiar", BigDecimal("19.99"), "EUR", BillingCycle.MONTHLY, "Apple Music para hasta 6 personas", "musica", domain = "music.apple.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Apple Music Estudiantes", BigDecimal("6.99"), "EUR", BillingCycle.MONTHLY, "Apple Music con descuento para estudiantes", "musica", domain = "music.apple.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("TIDAL Individual", BigDecimal("12.99"), "EUR", BillingCycle.MONTHLY, "TIDAL Individual (antes HiFi) — audio lossless y Hi-Res para audiófilos", "musica", domain = "tidal.com", cancelUrl = "https://account.tidal.com/subscription"),
            CatalogItem("TIDAL Familiar", BigDecimal("21.99"), "EUR", BillingCycle.MONTHLY, "TIDAL para hasta 6 cuentas", "musica", domain = "tidal.com", cancelUrl = "https://account.tidal.com/subscription"),
            CatalogItem("Amazon Music Unlimited", BigDecimal("12.99"), "EUR", BillingCycle.MONTHLY, "Amazon Music Unlimited — 100 millones de canciones en HD y Ultra HD", "musica", domain = "music.amazon.es", cancelUrl = "https://music.amazon.es/settings", priceAnnual = BigDecimal("129.99")),
            CatalogItem("Amazon Music Unlimited Familiar", BigDecimal("22.99"), "EUR", BillingCycle.MONTHLY, "Amazon Music Unlimited para hasta 6 miembros", "musica", domain = "music.amazon.es", cancelUrl = "https://music.amazon.es/settings", priceAnnual = BigDecimal("230.00")),
            CatalogItem("YouTube Music Premium",   BigDecimal("11.99"),  "EUR", BillingCycle.MONTHLY, "YouTube Music — sin anuncios, descarga y reproducción en segundo plano",        "musica", domain = "youtube.com",    cancelUrl = "https://www.youtube.com/paid_memberships"),
            CatalogItem("Deezer Premium", BigDecimal("11.99"), "EUR", BillingCycle.MONTHLY, "Deezer — música sin anuncios, modo offline y Flow personalizado", "musica", domain = "deezer.com", cancelUrl = "https://www.deezer.com/es/account", priceAnnual = BigDecimal("107.99")),
            CatalogItem("Deezer Duo", BigDecimal("15.99"), "EUR", BillingCycle.MONTHLY, "Deezer Premium para 2 personas", "musica", domain = "deezer.com", cancelUrl = "https://www.deezer.com/es/account", priceAnnual = BigDecimal("174.99")),
            CatalogItem("Deezer Family", BigDecimal("19.99"), "EUR", BillingCycle.MONTHLY, "Deezer Premium para hasta 6 perfiles", "musica", domain = "deezer.com", cancelUrl = "https://www.deezer.com/es/account", priceAnnual = BigDecimal("218.99")),
            CatalogItem("Deezer Student", BigDecimal("5.99"), "EUR", BillingCycle.MONTHLY, "Deezer Premium con descuento para estudiantes", "musica", domain = "deezer.com", cancelUrl = "https://www.deezer.com/es/account"),
            CatalogItem("SoundCloud Go+",          BigDecimal("11.99"),  "EUR", BillingCycle.MONTHLY, "SoundCloud Go+ — escucha sin anuncios, sin conexión y en alta calidad",          "musica", domain = "soundcloud.com", cancelUrl = "https://soundcloud.com/settings/subscription"),
            CatalogItem("Qobuz Sublime", BigDecimal("199.92"), "EUR", BillingCycle.YEARLY, "Qobuz Sublime — streaming lossless Hi-Res y descuentos en la tienda (solo pago anual)", "musica", domain = "qobuz.com", cancelUrl = "https://www.qobuz.com/es-es/account/subscriptions"),
            CatalogItem("Beatport Streaming Essential", BigDecimal("10.99"), "EUR", BillingCycle.MONTHLY, "Beatport Streaming Essential (antes Link) — streaming de música electrónica para DJs",                                   "musica", 30, "beatport.com",    "https://www.beatport.com/account/subscription"),
            CatalogItem("Mixcloud Premium",        BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Mixcloud Premium — mixes y sets de DJ sin anuncios y sin conexión (6,99 € + IVA)", "musica", domain = "mixcloud.com",   cancelUrl = "https://www.mixcloud.com/settings/"),
            CatalogItem("Pocket Casts Plus",       BigDecimal("3.99"),   "EUR", BillingCycle.MONTHLY, "El mejor cliente de podcasts para Android e iOS",                               "musica", domain = "pocketcasts.com", cancelUrl = "https://pocketcasts.com/account")
        ),

        // ── Software y productividad ─────────────────────────────────────────────
        "software" to listOf(
            CatalogItem("Microsoft 365 Personal",  BigDecimal("99.00"),  "EUR", BillingCycle.YEARLY,  "Microsoft 365 Personal — Office con Copilot + 1 TB OneDrive para 1 persona",                        "software", domain = "microsoft.com",      cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("Microsoft 365 Familia", BigDecimal("129.00"), "EUR", BillingCycle.YEARLY, "Microsoft 365 Familia — Office con Copilot + 1 TB OneDrive por persona, hasta 6", "software", domain = "microsoft.com", cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("Adobe Creative Cloud",    BigDecimal("79.30"),  "EUR", BillingCycle.MONTHLY, "Adobe Creative Cloud Pro — más de 20 apps: Photoshop, Illustrator, Premiere, Acrobat… (plan anual, pago mensual)",         "software", domain = "adobe.com",          cancelUrl = "https://account.adobe.com/plans"),
            CatalogItem("Adobe Photoshop",         BigDecimal("26.43"),  "EUR", BillingCycle.MONTHLY, "Adobe Photoshop — edición de imágenes profesional",                             "software", domain = "adobe.com",          cancelUrl = "https://account.adobe.com/plans"),
            CatalogItem("Adobe Acrobat Pro", BigDecimal("24.19"), "EUR", BillingCycle.MONTHLY, "Adobe Acrobat Pro — creación y edición avanzada de PDF", "software", domain = "adobe.com", cancelUrl = "https://account.adobe.com/plans"),
            CatalogItem("Notion Plus", BigDecimal("11.50"), "EUR", BillingCycle.MONTHLY, "Notion Plus — bloques ilimitados, historial de 30 días", "software", domain = "notion.com", cancelUrl = "https://www.notion.so/profile/plans", priceAnnual = BigDecimal("114.00")),
            CatalogItem("Figma Professional", BigDecimal("21.90"), "EUR", BillingCycle.MONTHLY, "Figma — asiento Full: diseño colaborativo, proyectos ilimitados — Se cobra en USD; importe aproximado", "software", domain = "figma.com", cancelUrl = "https://www.figma.com/settings", priceAnnual = BigDecimal("210.21"), variablePrice = true),
            CatalogItem("Sketch Standard", BigDecimal("157.65"), "EUR", BillingCycle.YEARLY, "Sketch Standard — diseño de interfaces macOS por editor, facturación anual — Se cobra en USD; importe aproximado", "software", domain = "sketch.com", cancelUrl = "https://www.sketch.com/manage/", variablePrice = true),
            CatalogItem("Todoist Pro", BigDecimal("7.66"), "EUR", BillingCycle.MONTHLY, "Todoist — gestión de tareas con recordatorios y filtros avanzados — Se cobra en USD; importe aproximado", "software", domain = "todoist.com", cancelUrl = "https://app.todoist.com/app/settings/subscription", priceAnnual = BigDecimal("65.69"), variablePrice = true),
            CatalogItem("Evernote Starter",        BigDecimal("79.90"),  "EUR", BillingCycle.YEARLY,  "Evernote Starter (antes Personal) — notas, documentos y tareas sincronizados (6,65 €/mes con pago anual)", "software", domain = "evernote.com",       cancelUrl = "https://www.evernote.com/Settings.action"),
            CatalogItem("Obsidian Sync", BigDecimal("5.47"), "EUR", BillingCycle.MONTHLY, "Obsidian Sync — sincronización cifrada de bóvedas entre dispositivos — Se cobra en USD; importe aproximado", "software", domain = "obsidian.md", cancelUrl = "https://obsidian.md/account", priceAnnual = BigDecimal("52.55"), variablePrice = true),
            CatalogItem("Grammarly Pro", BigDecimal("12.00"), "EUR", BillingCycle.MONTHLY, "Grammarly Pro (antes Premium) — corrección e inteligencia de escritura; 12 €/mes con pago anual", "software", domain = "grammarly.com", cancelUrl = "https://account.grammarly.com/subscription"),
            CatalogItem("Setapp", BigDecimal("16.41"), "EUR", BillingCycle.MONTHLY, "Setapp — más de 240 apps de Mac por una suscripción mensual (14,99 $ + impuestos) — Se cobra en USD; importe aproximado", "software", trialDays = 7, domain = "setapp.com", cancelUrl = "https://setapp.com/manage", variablePrice = true),
            CatalogItem("LinkedIn Premium", BigDecimal("39.66"), "EUR", BillingCycle.MONTHLY, "LinkedIn Premium Career — InMail, quién vio tu perfil y cursos LinkedIn Learning", "software", trialDays = 30, domain = "linkedin.com", cancelUrl = "https://www.linkedin.com/premium/products/", priceAnnual = BigDecimal("237.84")),
            CatalogItem("Zoom Pro", BigDecimal("18.60"), "EUR", BillingCycle.MONTHLY, "Zoom Pro — reuniones de hasta 30 horas con 100 participantes — Se cobra en USD; importe aproximado", "software", domain = "zoom.us", cancelUrl = "https://zoom.us/account/billing", priceAnnual = BigDecimal("186.03"), variablePrice = true),
            CatalogItem("Slack Pro", BigDecimal("8.25"), "EUR", BillingCycle.MONTHLY, "Slack Pro — historial completo, videollamadas y apps ilimitadas", "software", domain = "slack.com", cancelUrl = "https://app.slack.com/plans", priceAnnual = BigDecimal("81.00")),
            CatalogItem("Trello Premium", BigDecimal("13.69"), "EUR", BillingCycle.MONTHLY, "Trello Premium — vistas avanzadas, automatizaciones y tableros ilimitados — Se cobra en USD; importe aproximado", "software", domain = "trello.com", cancelUrl = "https://trello.com/billing", priceAnnual = BigDecimal("131.38"), variablePrice = true),
            CatalogItem("Asana Starter", BigDecimal("14.77"), "EUR", BillingCycle.MONTHLY, "Asana Starter — gestión de proyectos con línea de tiempo y automatizaciones — Se cobra en USD; importe aproximado", "software", domain = "asana.com", cancelUrl = "https://app.asana.com/-/account_api", priceAnnual = BigDecimal("144.39"), variablePrice = true),
            CatalogItem("Monday.com Basic", BigDecimal("9.00"), "EUR", BillingCycle.MONTHLY, "Monday.com Basic — gestión visual de trabajo en equipo (por asiento, mínimo 3 asientos)", "software", trialDays = 14, domain = "monday.com", cancelUrl = "https://monday.com/account-settings/billing"),
            CatalogItem("Airtable Team", BigDecimal("262.76"), "EUR", BillingCycle.YEARLY, "Airtable Team — base de datos visual con automatizaciones (por usuario, facturación anual) — Se cobra en USD; importe aproximado", "software", trialDays = 14, domain = "airtable.com", cancelUrl = "https://airtable.com/account", variablePrice = true),
            CatalogItem("Zapier Professional", BigDecimal("32.83"), "EUR", BillingCycle.MONTHLY, "Zapier Professional — 750 tareas/mes con zaps de varios pasos y filtros — Se cobra en USD; importe aproximado", "software", trialDays = 14, domain = "zapier.com", cancelUrl = "https://zapier.com/app/settings/billing", priceAnnual = BigDecimal("262.63"), variablePrice = true),
            CatalogItem("Make Pro",                BigDecimal("20.60"),  "EUR", BillingCycle.MONTHLY, "Make Pro — automatizaciones visuales avanzadas con 10 000 créditos/mes — Se cobra en USD; importe aproximado", "software", 30, "make.com",            "https://www.make.com/en/settings/billing", priceAnnual = BigDecimal("210.21"), variablePrice = true),
            CatalogItem("Loom Business", BigDecimal("19.71"), "EUR", BillingCycle.MONTHLY, "Loom Business — grabación de pantalla y vídeo asíncrono para equipos — Se cobra en USD; importe aproximado", "software", trialDays = 14, domain = "loom.com", cancelUrl = "https://www.loom.com/settings/billing", variablePrice = true),
            CatalogItem("Miro Starter", BigDecimal("105.10"), "EUR", BillingCycle.YEARLY, "Miro Starter (antes Team) — pizarra colaborativa, por miembro y facturación anual — Se cobra en USD; importe aproximado", "software", trialDays = 30, domain = "miro.com", cancelUrl = "https://miro.com/app/settings/billing/", variablePrice = true),
            CatalogItem("Webflow Basic",           BigDecimal("27.37"),  "EUR", BillingCycle.MONTHLY, "Webflow Basic — diseño y publicación web sin código (por sitio) — Se cobra en USD; importe aproximado", "software", domain = "webflow.com",        cancelUrl = "https://webflow.com/dashboard/account/billing", priceAnnual = BigDecimal("197.07"), variablePrice = true),
            CatalogItem("Framer Basic", BigDecimal("10.95"), "EUR", BillingCycle.MONTHLY, "Framer Basic — diseño web interactivo y publicación incluida — Se cobra en USD; importe aproximado", "software", trialDays = 14, domain = "framer.com", cancelUrl = "https://framer.com/settings/billing", variablePrice = true),
            CatalogItem("Descript Creator", BigDecimal("38.32"), "EUR", BillingCycle.MONTHLY, "Descript Creator — edición de vídeo y podcast basada en transcripción — Se cobra en USD; importe aproximado", "software", trialDays = 7, domain = "descript.com", cancelUrl = "https://www.descript.com/settings/billing", priceAnnual = BigDecimal("315.31"), variablePrice = true),
            CatalogItem("Bear Notes Pro", BigDecimal("3.27"), "EUR", BillingCycle.MONTHLY, "Editor de texto markdown para notas y escritura — Se cobra en USD; importe aproximado", "software", domain = "bear.app", cancelUrl = "https://bear.app/faq/subscriptions", priceAnnual = BigDecimal("32.83"), variablePrice = true),
            CatalogItem("Craft Plus", BigDecimal("8.00"), "EUR", BillingCycle.MONTHLY, "Craft Plus — editor de documentos colaborativo con IA", "software", domain = "craft.do", cancelUrl = "https://www.craft.do/account", priceAnnual = BigDecimal("57.60")),
            CatalogItem("Day One Silver", BigDecimal("54.73"), "EUR", BillingCycle.YEARLY, "Day One Silver — diario personal con cifrado de extremo a extremo (solo anual) — Se cobra en USD; importe aproximado", "software", domain = "dayoneapp.com", cancelUrl = "https://dayoneapp.com/account", variablePrice = true),
            CatalogItem("Ulysses", BigDecimal("6.56"), "EUR", BillingCycle.MONTHLY, "Editor de escritura creativa y textos largos — Se cobra en USD; importe aproximado", "software", trialDays = 14, domain = "ulysses.app", cancelUrl = "https://ulysses.app/support", priceAnnual = BigDecimal("43.78"), variablePrice = true),
            CatalogItem("PDF Expert Premium", BigDecimal("84.99"), "EUR", BillingCycle.YEARLY, "PDF Expert Premium — edición, anotación y firma de PDFs (solo pago anual)", "software", trialDays = 7, domain = "pdfexpert.com", cancelUrl = "https://pdfexpert.com/account"),
            CatalogItem("Fantastical Premium",     BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Calendario y tareas con IA y lenguaje natural (Flexibits Premium Individual)",   "software", 14, "fantastical.app",     "https://flexibits.com/fantastical/support", priceAnnual = BigDecimal("70.08")),
            CatalogItem("Raycast Pro", BigDecimal("10.95"), "EUR", BillingCycle.MONTHLY, "Launcher y productividad avanzada para Mac — Se cobra en USD; importe aproximado", "software", trialDays = 14, domain = "raycast.com", cancelUrl = "https://raycast.com/pricing", priceAnnual = BigDecimal("105.10"), variablePrice = true),
            CatalogItem("Pitch Plus", BigDecimal("120.00"), "EUR", BillingCycle.YEARLY, "Pitch Plus — presentaciones colaborativas (10 €/mes con facturación anual)", "software", domain = "pitch.com", cancelUrl = "https://app.pitch.com/app/settings/billing"),
            CatalogItem("Jira Software Standard",  BigDecimal("9.28"),   "EUR", BillingCycle.MONTHLY, "Gestión ágil de proyectos de software (por usuario) — Se cobra en USD; importe aproximado", "software", 14, "atlassian.com",       "https://support.atlassian.com/billing-and-licensing/docs/cancel-your-subscription", variablePrice = true),
            CatalogItem("Confluence Standard",     BigDecimal("6.33"),   "EUR", BillingCycle.MONTHLY, "Wiki corporativa y gestión del conocimiento (por usuario) — Se cobra en USD; importe aproximado", "software", 14, "atlassian.com",       "https://support.atlassian.com/billing-and-licensing/docs/cancel-your-subscription", variablePrice = true),
            CatalogItem("CleanMyMac",              BigDecimal("9.95"),   "EUR", BillingCycle.MONTHLY, "CleanMyMac — optimización y limpieza de Mac (1 Mac)",                           "software", 7,  "cleanmymac.com",      "https://account.macpaw.com", priceAnnual = BigDecimal("39.95")),
            CatalogItem("X Premium",               BigDecimal("9.68"),   "EUR", BillingCycle.MONTHLY, "Verificación, editar posts y funciones extra en X (8 € + IVA)",                 "software", domain = "x.com",              cancelUrl = "https://x.com/settings/account/subscription", priceAnnual = BigDecimal("101.64")),
            CatalogItem("Telegram Premium",        BigDecimal("4.99"),   "EUR", BillingCycle.MONTHLY, "Sin límites de tamaño, stickers exclusivos, traducción y velocidad extra",       "software", domain = "telegram.org",       cancelUrl = "https://t.me/premiumbot"),
            CatalogItem("TickTick Premium", BigDecimal("54.73"), "EUR", BillingCycle.YEARLY, "Gestor de tareas con Pomodoro, hábitos, calendario y widgets (solo pago anual) — Se cobra en USD; importe aproximado", "software", domain = "ticktick.com", cancelUrl = "https://ticktick.com/home#settings/subscription", variablePrice = true),
            CatalogItem("Any.do Premium", BigDecimal("8.75"), "EUR", BillingCycle.MONTHLY, "App de tareas, recordatorios de ubicación y planificador diario — Se cobra en USD; importe aproximado", "software", trialDays = 7, domain = "any.do", cancelUrl = "https://app.any.do/account", priceAnnual = BigDecimal("65.56"), variablePrice = true),
            CatalogItem("Basecamp Pro Plus", BigDecimal("108.39"), "EUR", BillingCycle.MONTHLY, "Basecamp Pro Plus — gestión de proyectos: mensajes, tareas, docs y chats — Se cobra en USD; importe aproximado", "software", trialDays = 30, domain = "basecamp.com", cancelUrl = "https://basecamp.com/account/billing", variablePrice = true),
            CatalogItem("Notion Business", BigDecimal("234.00"), "EUR", BillingCycle.YEARLY, "Notion Business — equipos con Notion AI completa (19,50 €/miembro/mes con facturación anual)", "software", domain = "notion.com", cancelUrl = "https://www.notion.so/profile/plans"),
            CatalogItem("Quizlet Plus",            BigDecimal("5.99"),   "EUR", BillingCycle.MONTHLY, "Flashcards y herramientas de estudio con IA y sin anuncios",                     "software", 7, "quizlet.com",           "https://quizlet.com/settings/account")
        ),

        // ── Almacenamiento en la nube ────────────────────────────────────────────
        "cloud" to listOf(
            CatalogItem("Google AI Plus 2 TB", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Google AI Plus (antes Google One 2 TB) — 2 TB compartidos entre Drive, Gmail y Fotos", "cloud", domain = "google.com", cancelUrl = "https://one.google.com/about", priceAnnual = BigDecimal("99.99")),
            CatalogItem("iCloud+ 200 GB",          BigDecimal("2.99"),   "EUR", BillingCycle.MONTHLY, "iCloud+ — 200 GB para iPhone, iPad y Mac",                                      "cloud", domain = "icloud.com",    cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("iCloud+ 2 TB",            BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "iCloud+ — 2 TB con Private Relay y dominio de correo personalizado",            "cloud", domain = "icloud.com",    cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("iCloud+ 50 GB", BigDecimal("0.99"), "EUR", BillingCycle.MONTHLY, "iCloud+ — 50 GB para iPhone, iPad y Mac", "cloud", domain = "icloud.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("iCloud+ 6 TB", BigDecimal("29.99"), "EUR", BillingCycle.MONTHLY, "iCloud+ — 6 TB compartibles en familia", "cloud", domain = "icloud.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("iCloud+ 12 TB", BigDecimal("59.99"), "EUR", BillingCycle.MONTHLY, "iCloud+ — 12 TB compartibles en familia", "cloud", domain = "icloud.com", cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Dropbox Plus",            BigDecimal("11.99"),  "EUR", BillingCycle.MONTHLY, "Dropbox Plus — 2 TB con sincronización inteligente",                            "cloud", domain = "dropbox.com",   cancelUrl = "https://www.dropbox.com/account/plan", priceAnnual = BigDecimal("119.88")),
            CatalogItem("Microsoft 365 Básico", BigDecimal("2.00"), "EUR", BillingCycle.MONTHLY, "Microsoft 365 Básico — 100 GB de OneDrive sin las apps de Office", "cloud", domain = "microsoft.com", cancelUrl = "https://account.microsoft.com/services", priceAnnual = BigDecimal("20.00")),
            CatalogItem("Backblaze Personal", BigDecimal("9.85"), "EUR", BillingCycle.MONTHLY, "Backblaze — copia de seguridad ilimitada del ordenador — Se cobra en USD; importe aproximado", "cloud", domain = "backblaze.com", cancelUrl = "https://secure.backblaze.com/user_overview.htm", priceAnnual = BigDecimal("108.39"), variablePrice = true),
            CatalogItem("pCloud Premium Plus 2 TB", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "pCloud Premium Plus — 2 TB de almacenamiento cifrado en Europa", "cloud", domain = "pcloud.com", cancelUrl = "https://my.pcloud.com/#page=account&settings=subscription", priceAnnual = BigDecimal("99.99")),
            CatalogItem("DigitalOcean", BigDecimal("4.38"), "EUR", BillingCycle.MONTHLY, "DigitalOcean — Droplets desde 4 $/mes, pago por uso — Se cobra en USD; importe aproximado", "cloud", domain = "digitalocean.com", cancelUrl = "https://cloud.digitalocean.com/account/billing", variablePrice = true),
            CatalogItem("Cloudflare Pro", BigDecimal("27.37"), "EUR", BillingCycle.MONTHLY, "Cloudflare Pro — CDN, WAF y optimización de rendimiento web (por dominio) — Se cobra en USD; importe aproximado", "cloud", domain = "cloudflare.com", cancelUrl = "https://dash.cloudflare.com/profile/billing", priceAnnual = BigDecimal("262.76"), variablePrice = true),
            CatalogItem("Render Starter", BigDecimal("7.66"), "EUR", BillingCycle.MONTHLY, "Render Starter — instancia para desplegar apps web con builds automáticos — Se cobra en USD; importe aproximado", "cloud", domain = "render.com", cancelUrl = "https://dashboard.render.com/billing", variablePrice = true),
            CatalogItem("Supabase Pro", BigDecimal("27.37"), "EUR", BillingCycle.MONTHLY, "Supabase Pro — Postgres, auth y storage gestionados (desde, más uso) — Se cobra en USD; importe aproximado", "cloud", trialDays = 14, domain = "supabase.com", cancelUrl = "https://supabase.com/dashboard/account/billing", variablePrice = true),
            CatalogItem("MongoDB Atlas", BigDecimal("62.34"), "EUR", BillingCycle.MONTHLY, "MongoDB Atlas — clúster dedicado M10 desde, pago por uso — Se cobra en USD; importe aproximado", "cloud", domain = "mongodb.com", cancelUrl = "https://account.mongodb.com/account/billing/overview", variablePrice = true),
            CatalogItem("Algolia Search", BigDecimal("10.00"), "EUR", BillingCycle.MONTHLY, "Algolia — búsqueda como servicio; sin cuota fija, pago por uso (importe orientativo)", "cloud", domain = "algolia.com", cancelUrl = "https://dashboard.algolia.com/account/billing", variablePrice = true),
            CatalogItem("Mega Pro Lite", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "MEGA Pro Lite — 400 GB cifrados de extremo a extremo en la nube", "cloud", domain = "mega.io", cancelUrl = "https://mega.io/cancel"),
            CatalogItem("Box Personal Pro",        BigDecimal("12.00"),  "EUR", BillingCycle.MONTHLY, "Almacenamiento en la nube para profesionales",                                  "cloud", 14, "box.com",           "https://account.box.com/settings", priceAnnual = BigDecimal("108.00")),
            CatalogItem("Sync.com Personal 1 TB", BigDecimal("17.52"), "EUR", BillingCycle.MONTHLY, "Sync.com Personal — 1 TB con cifrado zero-knowledge — Se cobra en USD; importe aproximado", "cloud", domain = "sync.com", cancelUrl = "https://www.sync.com/account", priceAnnual = BigDecimal("157.65"), variablePrice = true),
            CatalogItem("Tresorit Personal Essential", BigDecimal("13.99"), "EUR", BillingCycle.MONTHLY, "Tresorit Personal Essential — 1 TB en la nube con cifrado de extremo a extremo", "cloud", 14, "tresorit.com",      "https://app.tresorit.com/account", priceAnnual = BigDecimal("134.28")),
            CatalogItem("Icedrive Pro", BigDecimal("7.99"), "EUR", BillingCycle.MONTHLY, "Icedrive Pro — 2 TB en la nube con cifrado en el cliente", "cloud", domain = "icedrive.net", cancelUrl = "https://icedrive.net/account")
        ),

        // ── Gaming ───────────────────────────────────────────────────────────────
        "gaming" to listOf(
            CatalogItem("Xbox Game Pass Ultimate", BigDecimal("20.99"), "EUR", BillingCycle.MONTHLY, "Xbox Game Pass Ultimate — 400+ juegos en consola, PC y nube + EA Play y Ubisoft+ Classics", "gaming", domain = "xbox.com", cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("Xbox Game Pass Premium",BigDecimal("12.99"),  "EUR", BillingCycle.MONTHLY, "Game Pass Premium — 200+ juegos de consola y PC, novedades Xbox en su primer año", "gaming", domain = "xbox.com", cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("Xbox Game Pass Essential",BigDecimal("8.99"),   "EUR", BillingCycle.MONTHLY, "Game Pass Essential — multijugador online y 50+ juegos", "gaming", domain = "xbox.com", cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("PlayStation Plus Extra",  BigDecimal("15.99"),  "EUR", BillingCycle.MONTHLY, "PS Plus Extra — biblioteca de 400+ juegos para PS5 y PS4",                     "gaming", domain = "playstation.com",   cancelUrl = "https://www.playstation.com/es-es/playstation-plus/", priceAnnual = BigDecimal("125.99")),
            CatalogItem("PlayStation Plus Essential",BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "PS Plus Essential — juegos mensuales y multijugador online",                    "gaming", domain = "playstation.com",   cancelUrl = "https://www.playstation.com/es-es/playstation-plus/", priceAnnual = BigDecimal("71.99")),
            CatalogItem("PlayStation Plus Premium",BigDecimal("18.99"),  "EUR", BillingCycle.MONTHLY, "PS Plus Premium — biblioteca completa con juegos clásicos y streaming",         "gaming", domain = "playstation.com",   cancelUrl = "https://www.playstation.com/es-es/playstation-plus/", priceAnnual = BigDecimal("151.99")),
            CatalogItem("EA Play",                 BigDecimal("5.99"),   "EUR", BillingCycle.MONTHLY, "EA Play — acceso anticipado y biblioteca de juegos EA",                         "gaming", domain = "ea.com",            cancelUrl = "https://www.ea.com/ea-play", priceAnnual = BigDecimal("39.99")),
            CatalogItem("EA Play Pro", BigDecimal("16.99"), "EUR", BillingCycle.MONTHLY, "EA Play Pro — juegos nuevos de EA en PC desde el lanzamiento", "gaming", domain = "ea.com", cancelUrl = "https://www.ea.com/ea-play", priceAnnual = BigDecimal("119.99")),
            CatalogItem("Nintendo Switch Online",  BigDecimal("19.99"),  "EUR", BillingCycle.YEARLY,  "Nintendo Switch Online — multijugador online y juegos NES/SNES/N64",            "gaming", domain = "nintendo.com",      cancelUrl = "https://accounts.nintendo.com/profile/about"),
            CatalogItem("Nintendo Switch Online+", BigDecimal("39.99"),  "EUR", BillingCycle.YEARLY,  "Nintendo Switch Online + Pack de expansión — Game Boy, N64 y DLC",              "gaming", domain = "nintendo.com",      cancelUrl = "https://accounts.nintendo.com/profile/about"),
            CatalogItem("Nintendo Switch Online Familiar", BigDecimal("34.99"), "EUR", BillingCycle.YEARLY, "Nintendo Switch Online — plan familiar de 12 meses (hasta 8 cuentas)", "gaming", domain = "nintendo.com", cancelUrl = "https://accounts.nintendo.com/profile/about"),
            CatalogItem("Nintendo Switch Online+ Familiar", BigDecimal("69.99"), "EUR", BillingCycle.YEARLY, "Nintendo Switch Online + Paquete de expansión — plan familiar de 12 meses", "gaming", domain = "nintendo.com", cancelUrl = "https://accounts.nintendo.com/profile/about"),
            CatalogItem("Apple Arcade", BigDecimal("6.99"), "EUR", BillingCycle.MONTHLY, "Apple Arcade — más de 200 juegos premium sin anuncios", "gaming", domain = "apple.com", cancelUrl = "https://appleid.apple.com/account/manage", priceAnnual = BigDecimal("49.99")),
            CatalogItem("Humble Choice",           BigDecimal("11.99"),  "EUR", BillingCycle.MONTHLY, "Humble Choice — 9 juegos de PC al mes + descuentos en la tienda",               "gaming", domain = "humblebundle.com",  cancelUrl = "https://www.humblebundle.com/subscription"),
            CatalogItem("GeForce Now Performance", BigDecimal("10.99"), "EUR", BillingCycle.MONTHLY, "GeForce Now Performance (antes Priority) — juegos en la nube con RTX, 100 h/mes", "gaming", trialDays = 30, domain = "nvidia.com", cancelUrl = "https://www.nvidia.com/es-es/geforce-now/account/"),
            CatalogItem("GeForce Now Ultimate", BigDecimal("21.99"), "EUR", BillingCycle.MONTHLY, "GeForce Now Ultimate — juegos en la nube con la gama RTX más alta", "gaming", domain = "nvidia.com", cancelUrl = "https://www.nvidia.com/es-es/geforce-now/account/"),
            CatalogItem("Shadow PC Neo", BigDecimal("32.99"), "EUR", BillingCycle.MONTHLY, "Shadow PC Neo — PC gaming en la nube sin permanencia", "gaming", domain = "shadow.tech", cancelUrl = "https://account.shadow.tech/billing"),
            CatalogItem("Discord Nitro",           BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Emojis custom, boosts de servidor y más en Discord",                            "gaming", domain = "discord.com",       cancelUrl = "https://discord.com/settings/subscriptions"),
            CatalogItem("Discord Nitro Basic",     BigDecimal("2.99"),   "EUR", BillingCycle.MONTHLY, "Emojis animados y mayor límite de archivos en Discord",                         "gaming", domain = "discord.com",       cancelUrl = "https://discord.com/settings/subscriptions"),
            CatalogItem("Ubisoft+ Premium", BigDecimal("17.99"), "EUR", BillingCycle.MONTHLY, "Ubisoft+ Premium — catálogo de juegos Ubisoft, incluidos los estrenos", "gaming", trialDays = 7, domain = "ubisoft.com", cancelUrl = "https://ubisoftconnect.com/en-US/ubisoftplus/subscription"),
            CatalogItem("Ubisoft+ Classics", BigDecimal("7.99"), "EUR", BillingCycle.MONTHLY, "Ubisoft+ Classics — catálogo de juegos clásicos de Ubisoft", "gaming", domain = "ubisoft.com", cancelUrl = "https://ubisoftconnect.com/en-US/ubisoftplus/subscription"),
            CatalogItem("Roblox Premium 1000", BigDecimal("11.99"), "EUR", BillingCycle.MONTHLY, "1000 Robux/mes — sin altas nuevas desde el 30-04-2026 (lo sustituye Roblox Plus)", "gaming", domain = "roblox.com", cancelUrl = "https://www.roblox.com/account/membership"),
            CatalogItem("Xbox PC Game Pass", BigDecimal("12.99"), "EUR", BillingCycle.MONTHLY, "Catálogo de juegos para PC + EA Play incluido", "gaming", trialDays = 30, domain = "xbox.com", cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("Google Play Pass", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Acceso a 800+ apps y juegos premium sin anuncios ni compras integradas", "gaming", trialDays = 10, domain = "play.google.com", cancelUrl = "https://play.google.com/store/account/subscriptions", priceAnnual = BigDecimal("29.99")),
            CatalogItem("Chess.com Diamond",       BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Lecciones, análisis ilimitados y partidas clasificadas sin anuncios",            "gaming", 7,  "chess.com",          "https://www.chess.com/settings/subscription"),
            CatalogItem("Clash of Clans Gold Pass",BigDecimal("4.99"),   "EUR", BillingCycle.MONTHLY, "Misiones exclusivas, cosméticos y recompensas mensuales en Clash of Clans",     "gaming", domain = "supercell.com",    cancelUrl = "https://support.supercell.com"),
            CatalogItem("Pokémon GO Adventure", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Pase de aventura mensual con incursiones remotas y elementos exclusivos", "gaming", domain = "pokemongo.com", cancelUrl = "https://niantic.helpshift.com"),
            CatalogItem("Clash Royale Pass",       BigDecimal("4.99"),   "EUR", BillingCycle.MONTHLY, "Pase Royale mensual con recompensas, mazo y cosméticos exclusivos",              "gaming", domain = "clashroyale.com",  cancelUrl = "https://support.supercell.com"),
            CatalogItem("Brawl Stars Brawl Pass",  BigDecimal("4.99"),   "EUR", BillingCycle.MONTHLY, "Pase de temporada con Brawlers, pins y recompensas exclusivos",                 "gaming", domain = "brawlstars.com",   cancelUrl = "https://support.supercell.com"),
            CatalogItem("Minecraft Realms",        BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Servidor privado de Minecraft para jugar con hasta 10 amigos",                  "gaming", domain = "minecraft.net",    cancelUrl = "https://www.minecraft.net/realms")
        ),

        // ── Seguridad y privacidad ───────────────────────────────────────────────
        "seguridad" to listOf(
            CatalogItem("NordVPN",                 BigDecimal("14.99"),  "EUR", BillingCycle.MONTHLY, "NordVPN Básico — VPN con 6 000+ servidores y bloqueador de anuncios",           "seguridad", domain = "nordvpn.com",     cancelUrl = "https://my.nordaccount.com/dashboard/nordvpn/", priceAnnual = BigDecimal("139.08")),
            CatalogItem("ExpressVPN",              BigDecimal("12.99"),  "EUR", BillingCycle.MONTHLY, "ExpressVPN Basic — VPN rápida con servidores en 105 países",                    "seguridad", domain = "expressvpn.com",  cancelUrl = "https://www.expressvpn.com/subscriptions", priceAnnual = BigDecimal("79.95")),
            CatalogItem("Mullvad VPN",             BigDecimal("5.00"),   "EUR", BillingCycle.MONTHLY, "Mullvad — VPN centrada en la privacidad, sin cuenta ni logs",                   "seguridad", domain = "mullvad.net",     cancelUrl = "https://mullvad.net/account"),
            CatalogItem("ProtonVPN Plus", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Proton VPN Plus — VPN de código abierto con Tor y alta velocidad", "seguridad", domain = "proton.me", cancelUrl = "https://account.proton.me/dashboard", priceAnnual = BigDecimal("83.88")),
            CatalogItem("1Password",               BigDecimal("5.31"),   "EUR", BillingCycle.MONTHLY, "1Password — gestor de contraseñas con bóvedas ilimitadas",                      "seguridad", domain = "1password.com",   cancelUrl = "https://my.1password.com/profile", priceAnnual = BigDecimal("53.00")),
            CatalogItem("Bitwarden Premium", BigDecimal("21.68"), "EUR", BillingCycle.YEARLY, "Bitwarden — gestor de contraseñas open-source, plan Premium anual — Se cobra en USD; importe aproximado", "seguridad", domain = "bitwarden.com", cancelUrl = "https://vault.bitwarden.com/#/settings/subscription", variablePrice = true),
            CatalogItem("Dashlane Premium",        BigDecimal("57.50"),  "EUR", BillingCycle.YEARLY,  "Dashlane — gestor de contraseñas con VPN incluida (47,52 € + IVA, solo pago anual)", "seguridad", domain = "dashlane.com",    cancelUrl = "https://app.dashlane.com/settings/account-information"),
            CatalogItem("Malwarebytes Standard",   BigDecimal("59.99"),  "EUR", BillingCycle.YEARLY,  "Malwarebytes Standard (antes Premium) — antimalware en tiempo real, 3 dispositivos (solo pago anual)", "seguridad", domain = "malwarebytes.com", cancelUrl = "https://my.malwarebytes.com/account"),
            CatalogItem("ProtonMail Plus", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Proton Mail — correo cifrado de extremo a extremo", "seguridad", domain = "proton.me", cancelUrl = "https://account.proton.me/dashboard", priceAnnual = BigDecimal("47.88")),
            CatalogItem("Surfshark Starter", BigDecimal("2.49"), "EUR", BillingCycle.MONTHLY, "Surfshark — VPN con dispositivos ilimitados; 2,49 €/mes es el precio del plan de 24 meses", "seguridad", domain = "surfshark.com", cancelUrl = "https://my.surfshark.com/vpn/manual-setup/main/users"),
            CatalogItem("Keeper Security",         BigDecimal("51.16"),  "EUR", BillingCycle.YEARLY,  "Keeper Security — gestor de contraseñas con bóveda segura y auditoría (solo pago anual)", "seguridad", 30, "keepersecurity.com", "https://keepersecurity.com/vault/#account"),
            CatalogItem("Fastmail", BigDecimal("6.00"), "EUR", BillingCycle.MONTHLY, "Fastmail Individual — correo privado sin anuncios con dominio personalizado", "seguridad", trialDays = 30, domain = "fastmail.com", cancelUrl = "https://www.fastmail.com/settings/billing/", priceAnnual = BigDecimal("60.00")),
            CatalogItem("Fastmail Duo", BigDecimal("10.00"), "EUR", BillingCycle.MONTHLY, "Fastmail para 2 personas — correo privado sin anuncios", "seguridad", domain = "fastmail.com", cancelUrl = "https://www.fastmail.com/settings/billing/", priceAnnual = BigDecimal("96.00")),
            CatalogItem("Fastmail Family", BigDecimal("14.00"), "EUR", BillingCycle.MONTHLY, "Fastmail para familias — correo privado sin anuncios", "seguridad", domain = "fastmail.com", cancelUrl = "https://www.fastmail.com/settings/billing/", priceAnnual = BigDecimal("132.00")),
            CatalogItem("Tuta Revolutionary", BigDecimal("3.60"), "EUR", BillingCycle.MONTHLY, "Tuta (antes Tutanota) — correo cifrado de extremo a extremo y calendario privado", "seguridad", domain = "tuta.com", cancelUrl = "https://mail.tutanota.com/#settings/subscription", priceAnnual = BigDecimal("36.00")),
            CatalogItem("ProtonDrive Plus", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Proton Drive Plus — almacenamiento cifrado en la nube con 200 GB", "seguridad", domain = "proton.me", cancelUrl = "https://account.proton.me/dashboard", priceAnnual = BigDecimal("47.88")),
            CatalogItem("NordPass Premium",        BigDecimal("43.41"),  "EUR", BillingCycle.YEARLY,  "Gestor de contraseñas del equipo de NordVPN (35,88 € + IVA, solo pago anual)",  "seguridad", 30, "nordpass.com",       "https://nordpass.com/account"),
            CatalogItem("Norton 360 Standard",     BigDecimal("79.99"),  "EUR", BillingCycle.YEARLY,  "Antivirus, VPN y backup en la nube para 1 equipo (solo pago anual; el primer año suele estar en oferta)", "seguridad", 7,  "norton.com",         "https://my.norton.com/account/cancel"),
            CatalogItem("CyberGhost VPN", BigDecimal("11.99"), "EUR", BillingCycle.MONTHLY, "VPN rápida con 9000+ servidores en 90 países", "seguridad", trialDays = 45, domain = "cyberghostvpn.com", cancelUrl = "https://my.cyberghostvpn.com/account"),
            CatalogItem("Private Internet Access", BigDecimal("11.69"),  "EUR", BillingCycle.MONTHLY, "VPN de código abierto, sin registros verificado",                               "seguridad", 30, "privateinternetaccess.com", "https://www.privateinternetaccess.com/account/cancel", priceAnnual = BigDecimal("41.88")),
            CatalogItem("AdGuard Premium",         BigDecimal("29.88"),  "EUR", BillingCycle.YEARLY,  "Bloqueo de anuncios y rastreadores, hasta 3 dispositivos (solo pago anual)",   "seguridad", domain = "adguard.com",     cancelUrl = "https://my.adguard.com"),
            CatalogItem("Proton Pass Plus", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Gestor de contraseñas del ecosistema Proton", "seguridad", domain = "proton.me", cancelUrl = "https://account.proton.me/settings#subscription", priceAnnual = BigDecimal("35.88")),
            CatalogItem("RoboForm Premium", BigDecimal("32.71"), "EUR", BillingCycle.YEARLY, "Gestor de contraseñas con autocompletado web (solo pago anual) — Se cobra en USD; importe aproximado", "seguridad", trialDays = 30, domain = "roboform.com", cancelUrl = "https://my.roboform.com", variablePrice = true)
        ),

        // ── Noticias y Lectura ───────────────────────────────────────────────────
        "noticias" to listOf(
            CatalogItem("Kindle Unlimited", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Kindle Unlimited — más de 4 millones de libros digitales", "noticias", domain = "amazon.es", cancelUrl = "https://www.amazon.es/kindle-dbs/hz/subscribe/ku"),
            CatalogItem("Readwise Full", BigDecimal("14.22"), "EUR", BillingCycle.MONTHLY, "Readwise Full — Reader y Highlights: lectura, subrayado y repaso — Se cobra en USD; importe aproximado", "noticias", domain = "readwise.io", cancelUrl = "https://readwise.io/settings", priceAnnual = BigDecimal("131.25"), variablePrice = true),
            CatalogItem("Scribd",                  BigDecimal("10.99"),  "EUR", BillingCycle.MONTHLY, "Scribd — libros, audiolibros, revistas y documentos ilimitados",                "noticias", domain = "scribd.com",   cancelUrl = "https://www.scribd.com/account-settings/payments"),
            CatalogItem("El País Digital", BigDecimal("12.90"), "EUR", BillingCycle.MONTHLY, "El País Básica — acceso digital ilimitado con archivo histórico", "noticias", domain = "elpais.com", cancelUrl = "https://elpais.com/suscripciones/gestion.html", priceAnnual = BigDecimal("119.90")),
            CatalogItem("The New York Times",      BigDecimal("17.00"),  "EUR", BillingCycle.MONTHLY, "NYT — acceso completo a noticias, juegos, Cooking y Wirecutter",               "noticias", domain = "nytimes.com",  cancelUrl = "https://www.nytimes.com/subscription/cancel-subscription"),
            CatalogItem("Blinkist Premium",        BigDecimal("12.99"),  "EUR", BillingCycle.MONTHLY, "Blinkist — resúmenes de libros de no ficción en 15 minutos",                   "noticias", domain = "blinkist.com", cancelUrl = "https://www.blinkist.com/app/account/subscription"),
            CatalogItem("Audible", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Audible — 1 audiolibro al mes + acceso a catálogo Plus", "noticias", domain = "audible.es", cancelUrl = "https://www.audible.es/account/memberships"),
            CatalogItem("Storytel",              BigDecimal("8.99"),   "EUR", BillingCycle.MONTHLY, "Storytel Unlimited — audiolibros y ebooks ilimitados", "noticias", domain = "storytel.com", cancelUrl = "https://www.storytel.com/es/account"),
            CatalogItem("Storytel Family", BigDecimal("15.99"), "EUR", BillingCycle.MONTHLY, "Storytel Family — audiolibros y ebooks para 2 cuentas", "noticias", domain = "storytel.com", cancelUrl = "https://www.storytel.com/es/account"),
            CatalogItem("Medium", BigDecimal("5.47"), "EUR", BillingCycle.MONTHLY, "Medium — artículos y ensayos de miles de escritores independientes — Se cobra en USD; importe aproximado", "noticias", trialDays = 30, domain = "medium.com", cancelUrl = "https://medium.com/me/membership", priceAnnual = BigDecimal("54.74"), variablePrice = true),
            CatalogItem("Feedly Pro",              BigDecimal("6.00"),   "EUR", BillingCycle.MONTHLY, "Feedly Pro — agregador de noticias con IA y flujos sin límite",                 "noticias", 14, "feedly.com",    "https://feedly.com/i/subscription/feedly.pro"),
            CatalogItem("Inoreader Pro", BigDecimal("10.88"), "EUR", BillingCycle.MONTHLY, "Inoreader Pro — lector RSS con automatizaciones y filtros (8,99 € + IVA)", "noticias", domain = "inoreader.com", cancelUrl = "https://www.inoreader.com/account/subscription", priceAnnual = BigDecimal("96.85")),
            CatalogItem("Overcast Premium", BigDecimal("16.41"), "EUR", BillingCycle.YEARLY, "Overcast Premium — cliente de podcasts para iPhone (pago anual) — Se cobra en USD; importe aproximado", "noticias", domain = "overcast.fm", cancelUrl = "https://overcast.fm/account", variablePrice = true),
            CatalogItem("The Wall Street Journal", BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Noticias financieras y de negocios globales",                                   "noticias", domain = "wsj.com",      cancelUrl = "https://customercenter.wsj.com"),
            CatalogItem("Financial Times",         BigDecimal("45.00"),  "EUR", BillingCycle.MONTHLY, "Financial Times Standard Digital — noticias económicas y financieras internacionales",                          "noticias", 4,  "ft.com",        "https://help.ft.com/faq/subscribing-to-ft"),
            CatalogItem("The Economist",           BigDecimal("21.99"),  "EUR", BillingCycle.MONTHLY, "Análisis político y económico global semanal",                                  "noticias", domain = "economist.com", cancelUrl = "https://myaccount.economist.com"),
            CatalogItem("La Vanguardia Premium",   BigDecimal("3.49"),   "EUR", BillingCycle.MONTHLY, "Diario digital catalán con contenido exclusivo",                                "noticias", domain = "lavanguardia.com", cancelUrl = "https://suscripcion.lavanguardia.com"),
            CatalogItem("The Athletic",            BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Periodismo deportivo en profundidad",                                           "noticias", 7,  "theathletic.com", "https://theathletic.com/account/subscription"),
            CatalogItem("El Confidencial Premium", BigDecimal("2.99"),   "EUR", BillingCycle.MONTHLY, "Noticias y reportajes de investigación en España",                              "noticias", domain = "elconfidencial.com", cancelUrl = "https://suscripcion.elconfidencial.com"),
            CatalogItem("Le Monde Essentiel",      BigDecimal("13.99"),  "EUR", BillingCycle.MONTHLY, "Le Monde Essentiel (antes Numérique) — periódico de referencia francés, 1 cuenta",                      "noticias", domain = "lemonde.fr",   cancelUrl = "https://abo.lemonde.fr"),
            CatalogItem("Caixin Global",           BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Noticias económicas y financieras de China",                                    "noticias", domain = "caixinglobal.com", cancelUrl = "https://www.caixinglobal.com/account")
        ),

        // ── Salud y Deporte ──────────────────────────────────────────────────────
        "salud" to listOf(
            CatalogItem("Strava", BigDecimal("7.99"), "EUR", BillingCycle.MONTHLY, "Strava — análisis avanzado de entrenamiento, segmentos y rutas", "salud", domain = "strava.com", cancelUrl = "https://www.strava.com/settings/subscription", priceAnnual = BigDecimal("49.99")),
            CatalogItem("Whoop One", BigDecimal("199.00"), "EUR", BillingCycle.YEARLY, "Whoop One — monitor de salud 24/7: recuperación, sueño y esfuerzo (membresía anual)", "salud", domain = "whoop.com", cancelUrl = "https://www.whoop.com/us/en/thelocker/cancel-whoop-membership/"),
            CatalogItem("Garmin Connect+", BigDecimal("8.99"), "EUR", BillingCycle.MONTHLY, "Garmin Connect+ — estadísticas avanzadas y planes de entrenamiento", "salud", domain = "garmin.com", cancelUrl = "https://connect.garmin.com/modern/settings", priceAnnual = BigDecimal("89.99")),
            CatalogItem("MyFitnessPal Premium",    BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "MyFitnessPal — seguimiento nutricional avanzado con escáner de alimentos",     "salud", domain = "myfitnesspal.com",   cancelUrl = "https://www.myfitnesspal.com/account/my-subscription", priceAnnual = BigDecimal("49.99")),
            CatalogItem("Calm Premium",            BigDecimal("5.99"),   "EUR", BillingCycle.MONTHLY, "Calm — meditación, sueño y reducción del estrés",                               "salud", domain = "calm.com",          cancelUrl = "https://www.calm.com/app/account"),
            CatalogItem("Headspace",               BigDecimal("12.99"),  "EUR", BillingCycle.MONTHLY, "Headspace — meditación guiada y cursos de bienestar mental",                    "salud", domain = "headspace.com",     cancelUrl = "https://www.headspace.com/account", priceAnnual = BigDecimal("57.99")),
            CatalogItem("Noom",                    BigDecimal("76.64"),  "EUR", BillingCycle.MONTHLY, "Noom — programa de pérdida de peso basado en psicología conductual; el plan final se personaliza — Se cobra en USD; importe aproximado", "salud", domain = "noom.com",          cancelUrl = "https://web.noom.com/settings", priceAnnual = BigDecimal("228.82"), variablePrice = true),
            CatalogItem("Apple Fitness+",          BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Apple Fitness+ — clases de entrenamiento con métricas de Apple Watch",          "salud", domain = "apple.com",         cancelUrl = "https://appleid.apple.com/account/manage", priceAnnual = BigDecimal("79.99")),
            CatalogItem("Peloton App",             BigDecimal("12.99"),  "EUR", BillingCycle.MONTHLY, "Peloton App — clases de ciclismo, yoga, fuerza y meditación en streaming",      "salud", 30, "onepeloton.com",     "https://members.onepeloton.co.uk/profile/preferences"),
            CatalogItem("Oura Ring Membership", BigDecimal("5.99"), "EUR", BillingCycle.MONTHLY, "Oura Ring Membership — datos avanzados de sueño y recuperación del anillo", "salud", domain = "ouraring.com", cancelUrl = "https://cloud.ouraring.com/user/settings/membership", priceAnnual = BigDecimal("69.99")),
            CatalogItem("Sleep Cycle Premium",     BigDecimal("3.99"),   "EUR", BillingCycle.MONTHLY, "Sleep Cycle Premium — análisis avanzado del sueño y alarma inteligente",        "salud", 7,  "sleepcycle.com",     "https://www.sleepcycle.com/account/"),
            CatalogItem("Runkeeper Go",            BigDecimal("39.99"),   "EUR", BillingCycle.YEARLY,  "Runkeeper Go — planes de entrenamiento personalizados y estadísticas avanzadas", "salud", domain = "runkeeper.com",     cancelUrl = "https://runkeeper.com/user/settings/premium"),
            CatalogItem("Google Health Premium", BigDecimal("8.99"), "EUR", BillingCycle.MONTHLY, "Google Health Premium (antes Fitbit Premium) — análisis avanzado de salud y sueño", "salud", trialDays = 90, domain = "fitbit.com", cancelUrl = "https://www.fitbit.com/settings/billing"),
            CatalogItem("Freeletics Coach",        BigDecimal("12.99"),  "EUR", BillingCycle.MONTHLY, "Entrenamientos personalizados con IA sin equipamiento",                         "salud", domain = "freeletics.com",    cancelUrl = "https://www.freeletics.com/en/account/subscriptions"),
            CatalogItem("Flo Premium",             BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Seguimiento de ciclo menstrual y salud femenina",                               "salud", 7,  "flo.health",         "https://flo.health/account"),
            CatalogItem("Zwift", BigDecimal("19.99"), "EUR", BillingCycle.MONTHLY, "Ciclismo y running indoor con mundo virtual", "salud", trialDays = 14, domain = "zwift.com", cancelUrl = "https://www.zwift.com/cancel", priceAnnual = BigDecimal("199.99")),
            CatalogItem("TrainerRoad", BigDecimal("24.08"), "EUR", BillingCycle.MONTHLY, "Entrenamiento de ciclismo basado en datos y zonas — Se cobra en USD; importe aproximado", "salud", trialDays = 30, domain = "trainerroad.com", cancelUrl = "https://www.trainerroad.com/account", priceAnnual = BigDecimal("229.90"), variablePrice = true),
            CatalogItem("Sweat App",               BigDecimal("19.99"),  "EUR", BillingCycle.MONTHLY, "Programas de fitness para mujeres (BBG, LIFT, BREATHE)",                        "salud", 7,  "sweat.com",          "https://www.sweat.com/account/membership"),
            CatalogItem("Centr Premium",           BigDecimal("29.99"),  "EUR", BillingCycle.MONTHLY, "App de fitness de Chris Hemsworth — gym y mindfulness",                         "salud", 7,  "centr.com",          "https://centr.com/account/subscription"),
            CatalogItem("Lifesum Premium",         BigDecimal("99.99"),  "EUR", BillingCycle.YEARLY,  "App de dieta y nutrición con planes personalizados y seguimiento calórico (desde 8,33 €/mes con pago anual)", "salud", 7,  "lifesum.com",        "https://app.lifesum.com/settings/subscription"),
            CatalogItem("Cronometer Gold",         BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Seguimiento nutricional detallado con micronutrientes y análisis avanzado",      "salud", 7,  "cronometer.com",     "https://cronometer.com/account/"),
            CatalogItem("BetterSleep Premium",     BigDecimal("6.99"),   "EUR", BillingCycle.MONTHLY, "Mejora tu sueño con sonidos relajantes, meditaciones y seguimiento nocturno",    "salud", 7,  "bettersleep.com",    "https://www.bettersleep.com/account"),
            CatalogItem("Lumosity Premium",        BigDecimal("11.99"),  "EUR", BillingCycle.MONTHLY, "Entrenamiento cerebral científico con +60 juegos cognitivos",                    "salud", 7,  "lumosity.com",       "https://www.lumosity.com/account/subscription"),
            CatalogItem("Waking Up Premium",       BigDecimal("21.89"),  "EUR", BillingCycle.MONTHLY, "Meditación guiada por Sam Harris — mindfulness y filosofía — Se cobra en USD; importe aproximado", "salud", 30, "wakingup.com",       "https://app.wakingup.com/settings", priceAnnual = BigDecimal("142.32"), variablePrice = true),
            CatalogItem("Meditopia Premium",       BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Meditación en español con sesiones guiadas para dormir y reducir el estrés",    "salud", 7,  "meditopia.com",      "https://meditopia.com/account"),
            CatalogItem("8fit Pro",                BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Rutinas de ejercicio en casa y planes de nutrición personalizados",              "salud", 7,  "8fit.com",           "https://8fit.com/settings"),
            CatalogItem("Daily Yoga Premium",      BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Más de 500 clases de yoga, meditación y pilates para todos los niveles",        "salud", 7,  "dailyyoga.com",      "https://www.dailyyoga.com/account"),
            CatalogItem("Endel Premium",           BigDecimal("5.99"),   "EUR", BillingCycle.MONTHLY, "Paisajes sonoros adaptativos con IA para concentración y sueño",                 "salud", 7,  "endel.io",           "https://endel.io/account"),
            // 13 cobros al año de 24,99 € equivalen a 27,07 € al mes (24,99 × 13 / 12).
            CatalogItem("Basic-Fit",             BigDecimal("27.07"),  "EUR", BillingCycle.MONTHLY, "Basic-Fit Comfort — Se cobra cada 4 semanas (24,99 €); importe mensual equivalente", "salud", domain = "basic-fit.com", variablePrice = true),
            CatalogItem("VivaGym", BigDecimal("29.90"), "EUR", BillingCycle.MONTHLY, "VivaGym — cuota mensual sin permanencia, varía por centro y plan — importe orientativo", "salud", domain = "vivagym.com", variablePrice = true),
            CatalogItem("Synergym", BigDecimal("29.90"), "EUR", BillingCycle.MONTHLY, "Synergym — cuota mensual low cost, varía por centro — importe orientativo", "salud", domain = "synergym.es", iconUrl = "https://www.google.com/s2/favicons?domain=synergym.es&sz=128", variablePrice = true),
            CatalogItem("Anytime Fitness",       BigDecimal("49.00"),  "EUR", BillingCycle.MONTHLY, "Anytime Fitness — gimnasio 24 h, cuota según club — importe orientativo", "salud", domain = "anytimefitness.es", variablePrice = true),
            CatalogItem("Go fit",                BigDecimal("49.00"),  "EUR", BillingCycle.MONTHLY, "Go fit — centros deportivos con piscina, cuota según centro y edad — importe orientativo", "salud", domain = "go-fit.es", variablePrice = true),
            CatalogItem("Holmes Place",          BigDecimal("80.00"),  "EUR", BillingCycle.MONTHLY, "Holmes Place — club premium con spa y clases, cuota según club — importe orientativo", "salud", domain = "holmesplace.com", variablePrice = true)
        ),

        // ── Desarrollo y DevOps ──────────────────────────────────────────────────
        "desarrollo" to listOf(
            CatalogItem("GitHub Pro", BigDecimal("4.38"), "EUR", BillingCycle.MONTHLY, "GitHub — repositorios privados ilimitados, wikis y Pages avanzadas — Se cobra en USD; importe aproximado", "desarrollo", domain = "github.com", cancelUrl = "https://github.com/settings/billing", variablePrice = true),
            CatalogItem("GitHub Team", BigDecimal("4.38"), "EUR", BillingCycle.MONTHLY, "GitHub Team — revisiones de código, protección de ramas y CODEOWNERS (por usuario) — Se cobra en USD; importe aproximado", "desarrollo", domain = "github.com", cancelUrl = "https://github.com/settings/billing", variablePrice = true),
            CatalogItem("GitLab Premium", BigDecimal("381.00"), "EUR", BillingCycle.YEARLY, "GitLab Premium — CI/CD avanzado y seguridad (por usuario, solo pago anual) — Se cobra en USD; importe aproximado", "desarrollo", domain = "gitlab.com", cancelUrl = "https://gitlab.com/-/profile/billing", variablePrice = true),
            CatalogItem("Vercel Pro", BigDecimal("21.90"), "EUR", BillingCycle.MONTHLY, "Vercel Pro — despliegue continuo por asiento de desarrollador, con 20 $ de crédito — Se cobra en USD; importe aproximado", "desarrollo", domain = "vercel.com", cancelUrl = "https://vercel.com/account/billing", variablePrice = true),
            CatalogItem("Netlify Pro", BigDecimal("21.90"), "EUR", BillingCycle.MONTHLY, "Netlify Pro — sitios estáticos y funciones serverless con miembros ilimitados — Se cobra en USD; importe aproximado", "desarrollo", domain = "netlify.com", cancelUrl = "https://app.netlify.com/user/billing", variablePrice = true),
            CatalogItem("Railway Hobby", BigDecimal("5.47"), "EUR", BillingCycle.MONTHLY, "Railway — despliegue de aplicaciones con 5 $ de uso incluido — Se cobra en USD; importe aproximado", "desarrollo", domain = "railway.com", cancelUrl = "https://railway.com/account/billing", variablePrice = true),
            CatalogItem("Sentry Team", BigDecimal("31.75"), "EUR", BillingCycle.MONTHLY, "Sentry — monitorización de errores y rendimiento — Se cobra en USD; importe aproximado", "desarrollo", domain = "sentry.io", cancelUrl = "https://sentry.io/settings/billing/overview/", priceAnnual = BigDecimal("341.59"), variablePrice = true),
            CatalogItem("Linear Basic", BigDecimal("131.38"), "EUR", BillingCycle.YEARLY, "Linear Basic — gestión de proyectos de software (por usuario, facturación anual) — Se cobra en USD; importe aproximado", "desarrollo", domain = "linear.app", cancelUrl = "https://linear.app/settings/billing", variablePrice = true),
            CatalogItem("JetBrains All Products",  BigDecimal("36.18"),  "EUR", BillingCycle.MONTHLY, "JetBrains — todos los IDEs para uso individual (29,90 € + IVA); el anual baja desde el segundo año", "desarrollo", domain = "jetbrains.com", cancelUrl = "https://account.jetbrains.com/licenses", priceAnnual = BigDecimal("361.79")),
            CatalogItem("Postman Solo", BigDecimal("118.24"), "EUR", BillingCycle.YEARLY, "Postman Solo — plataforma de API (sustituye a Basic; facturación anual) — Se cobra en USD; importe aproximado", "desarrollo", domain = "postman.com", cancelUrl = "https://go.postman.co/settings/billing", variablePrice = true),
            CatalogItem("Datadog Pro", BigDecimal("19.71"), "EUR", BillingCycle.MONTHLY, "Datadog — monitorización de infraestructura, por host y mes — Se cobra en USD; importe aproximado", "desarrollo", domain = "datadoghq.com", cancelUrl = "https://app.datadoghq.com/account/billing", priceAnnual = BigDecimal("197.07"), variablePrice = true),
            CatalogItem("New Relic", BigDecimal("10.95"), "EUR", BillingCycle.MONTHLY, "New Relic — observabilidad full-stack; primer usuario completo más uso — Se cobra en USD; importe aproximado", "desarrollo", domain = "newrelic.com", cancelUrl = "https://one.newrelic.com/admin-portal/billing-billing/home", variablePrice = true),
            CatalogItem("Grafana Cloud Pro", BigDecimal("20.80"), "EUR", BillingCycle.MONTHLY, "Grafana Cloud Pro — cuota de plataforma más uso — Se cobra en USD; importe aproximado", "desarrollo", domain = "grafana.com", cancelUrl = "https://grafana.com/orgs/account/billing", variablePrice = true),
            CatalogItem("PagerDuty", BigDecimal("27.37"), "EUR", BillingCycle.MONTHLY, "PagerDuty Professional — gestión de incidentes y guardias, por usuario — Se cobra en USD; importe aproximado", "desarrollo", domain = "pagerduty.com", cancelUrl = "https://account.pagerduty.com/account/settings", priceAnnual = BigDecimal("275.90"), variablePrice = true),
            CatalogItem("Pingdom",                 BigDecimal("171.96"), "EUR", BillingCycle.YEARLY,  "Pingdom Synthetic Monitoring — disponibilidad y rendimiento web (14,33 €/mes con pago anual)", "desarrollo", domain = "pingdom.com",   cancelUrl = "https://my.pingdom.com/account/plans"),
            CatalogItem("Fly.io", BigDecimal("2.40"), "EUR", BillingCycle.MONTHLY, "Fly.io — pago por uso, sin cuota; la máquina más barata desde 2,19 $/mes — Se cobra en USD; importe aproximado", "desarrollo", domain = "fly.io", cancelUrl = "https://fly.io/dashboard/billing", variablePrice = true),
            CatalogItem("Neon Postgres", BigDecimal("16.42"), "EUR", BillingCycle.MONTHLY, "Neon — Postgres serverless con branching; pago por uso sin mínimo (uso intermitente típico) — Se cobra en USD; importe aproximado", "desarrollo", domain = "neon.com", cancelUrl = "https://console.neon.tech/app/settings/billing", variablePrice = true),
            CatalogItem("Upstash", BigDecimal("10.95"), "EUR", BillingCycle.MONTHLY, "Upstash — Redis Fixed 250 MB — Se cobra en USD; importe aproximado", "desarrollo", domain = "upstash.com", cancelUrl = "https://console.upstash.com/account/billing", variablePrice = true),
            CatalogItem("Tabnine Pro",             BigDecimal("12.00"),  "EUR", BillingCycle.MONTHLY, "Autocompletado de código con IA (privacidad garantizada)",                      "desarrollo", 14, "tabnine.com",    "https://app.tabnine.com/settings/subscription"),
            CatalogItem("Plausible Analytics", BigDecimal("9.85"), "EUR", BillingCycle.MONTHLY, "Analítica web ligera y respetuosa con la privacidad — plan Starter — Se cobra en USD; importe aproximado", "desarrollo", trialDays = 30, domain = "plausible.io", cancelUrl = "https://plausible.io/settings", priceAnnual = BigDecimal("98.53"), variablePrice = true),
            CatalogItem("Fathom Analytics", BigDecimal("16.42"), "EUR", BillingCycle.MONTHLY, "Analítica web sin cookies, conforme al RGPD — 100 000 páginas vistas — Se cobra en USD; importe aproximado", "desarrollo", trialDays = 30, domain = "usefathom.com", cancelUrl = "https://app.usefathom.com/settings/subscription", priceAnnual = BigDecimal("164.22"), variablePrice = true),
            CatalogItem("Heroku Eco", BigDecimal("5.47"), "EUR", BillingCycle.MONTHLY, "Dynos económicos para apps en Heroku — Se cobra en USD; importe aproximado", "desarrollo", domain = "heroku.com", cancelUrl = "https://dashboard.heroku.com/account/billing", variablePrice = true),
            CatalogItem("Warp Build", BigDecimal("21.90"), "EUR", BillingCycle.MONTHLY, "Warp Build — terminal con IA integrada y agentes — Se cobra en USD; importe aproximado", "desarrollo", trialDays = 14, domain = "warp.dev", cancelUrl = "https://app.warp.dev/account", priceAnnual = BigDecimal("236.48"), variablePrice = true),
            CatalogItem("Retool Team", BigDecimal("11.00"), "EUR", BillingCycle.MONTHLY, "Retool Team — apps internas con bajo código (por creador)", "desarrollo", trialDays = 14, domain = "retool.com", cancelUrl = "https://docs.retool.com/docs/cancel", priceAnnual = BigDecimal("108.00")),
            CatalogItem("Pieces for Developers", BigDecimal("20.79"), "EUR", BillingCycle.MONTHLY, "Pieces Pro — gestión de snippets de código con IA local — Se cobra en USD; importe aproximado", "desarrollo", domain = "pieces.app", cancelUrl = "https://pieces.app/account", variablePrice = true)
        ),

        // ── Finanzas personales ──────────────────────────────────────────────────
        "finanzas" to listOf(
            CatalogItem("YNAB",                    BigDecimal("10.00"),  "EUR", BillingCycle.MONTHLY, "Presupuesto personal con método zero-based",                                    "finanzas", 34, "ynab.com",           "https://app.youneedabudget.com/settings/subscription"),
            CatalogItem("Revolut Premium", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Cuenta bancaria digital con seguro de viaje y cashback", "finanzas", domain = "revolut.com", cancelUrl = "https://app.revolut.com/subscription"),
            CatalogItem("Revolut Plus", BigDecimal("3.99"), "EUR", BillingCycle.MONTHLY, "Plan Revolut Plus — protección de compras y ventajas básicas", "finanzas", domain = "revolut.com", cancelUrl = "https://app.revolut.com/subscription"),
            CatalogItem("Revolut Metal", BigDecimal("15.99"), "EUR", BillingCycle.MONTHLY, "Plan Revolut premium con tarjeta de metal y concierge", "finanzas", domain = "revolut.com", cancelUrl = "https://app.revolut.com/subscription"),
            CatalogItem("N26 Smart",               BigDecimal("4.90"),   "EUR", BillingCycle.MONTHLY, "Cuenta bancaria digital con hasta 10 subcuentas",                               "finanzas", domain = "n26.com",          cancelUrl = "https://n26.com/en-es/membership"),
            CatalogItem("N26 Go", BigDecimal("9.90"), "EUR", BillingCycle.MONTHLY, "Cuenta N26 Go (antes You) con seguros y asistencia en viaje", "finanzas", domain = "n26.com", cancelUrl = "https://n26.com/en-es/membership"),
            CatalogItem("N26 Metal", BigDecimal("16.90"), "EUR", BillingCycle.MONTHLY, "Cuenta N26 Metal con tarjeta metálica y seguros ampliados", "finanzas", domain = "n26.com", cancelUrl = "https://n26.com/en-es/membership"),
            CatalogItem("Vivid Standard",          BigDecimal("9.90"),   "EUR", BillingCycle.MONTHLY, "Cuenta bancaria digital con cashback en cripto",                                "finanzas", 30, "vivid.money",        "https://vivid.money/account"),
            CatalogItem("Wallet Premium", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Wallet Premium — control de gastos con sincronización bancaria automática", "finanzas", trialDays = 30, domain = "budgetbakers.com", cancelUrl = "https://budgetbakers.com/account"),
            CatalogItem("Toshl Finance Pro",       BigDecimal("3.99"),   "EUR", BillingCycle.MONTHLY, "Registro de gastos e ingresos con visualizaciones",                             "finanzas", 30, "toshl.com",          "https://toshl.com/account"),
            CatalogItem("Spendee Premium",         BigDecimal("3.99"),   "EUR", BillingCycle.MONTHLY, "App de presupuesto compartido para familias y parejas",                         "finanzas", 7,  "spendee.com",        "https://spendee.com/support"),
            CatalogItem("Splitwise Pro",           BigDecimal("3.99"),   "EUR", BillingCycle.MONTHLY, "Divide gastos con amigos y roommates sin discusiones",                           "finanzas", domain = "splitwise.com",    cancelUrl = "https://splitwise.com/account/settings"),
            CatalogItem("bunq Pro", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Neobanco europeo con cuentas múltiples, tarjetas y cashback — plan Pro", "finanzas", domain = "bunq.com", cancelUrl = "https://www.bunq.com/card/subscription"),
            CatalogItem("bunq Core", BigDecimal("3.99"), "EUR", BillingCycle.MONTHLY, "Neobanco europeo — plan Core", "finanzas", domain = "bunq.com", cancelUrl = "https://www.bunq.com/card/subscription"),
            CatalogItem("bunq Elite", BigDecimal("18.99"), "EUR", BillingCycle.MONTHLY, "Neobanco europeo — plan Elite con seguros y más ventajas", "finanzas", domain = "bunq.com", cancelUrl = "https://www.bunq.com/card/subscription"),
            CatalogItem("Money Manager Pro", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Registro de ingresos y gastos con gráficos y balance detallado", "finanzas", domain = "realbyteapps.com", cancelUrl = "https://www.realbyteapps.com/"),
            CatalogItem("Robinhood Gold",          BigDecimal("5.99"),   "EUR", BillingCycle.MONTHLY, "Inversiones y trading con tasas reducidas y cuenta margen",                     "finanzas", domain = "robinhood.com",    cancelUrl = "https://robinhood.com/settings/gold"),
            CatalogItem("Revolut Ultra", BigDecimal("55.00"), "EUR", BillingCycle.MONTHLY, "Plan Revolut máximo — seguro Revolut, concierge 24h y cashback top", "finanzas", domain = "revolut.com", cancelUrl = "https://app.revolut.com/subscription"),
            // Recibos recurrentes (V13)
            CatalogItem("Comisión de mantenimiento bancaria", BigDecimal("13.92"), "EUR", BillingCycle.MONTHLY, "Comisión mensual de cuenta corriente no bonificada (media 167 €/año según ASUFIN) — importe orientativo", "finanzas", variablePrice = true),
            CatalogItem("Cuota anual de tarjeta de crédito",BigDecimal("36.00"),  "EUR", BillingCycle.YEARLY,  "Cuota anual de tarjeta de crédito — importe orientativo según entidad",                                      "finanzas", variablePrice = true),
            CatalogItem("Cuota de autónomo (RETA)", BigDecimal("299.56"), "EUR", BillingCycle.MONTHLY, "Cuota mensual de autónomo a la Seguridad Social (tramo general 1 de 2026) — tramo según ingresos reales", "finanzas", domain = "seg-social.es", cancelUrl = "https://portal.seg-social.gob.es/", variablePrice = true)
        ),

        // ── Educación y cursos ───────────────────────────────────────────────────
        "educacion" to listOf(
            CatalogItem("Coursera Plus", BigDecimal("52.00"), "EUR", BillingCycle.MONTHLY, "Acceso ilimitado a +7000 cursos y certificaciones", "educacion", trialDays = 7, domain = "coursera.org", cancelUrl = "https://www.coursera.org/account-profile#cancellation", priceAnnual = BigDecimal("355.00")),
            CatalogItem("MasterClass",             BigDecimal("120.00"), "EUR", BillingCycle.YEARLY,  "MasterClass Standard — clases magistrales de expertos (10 €/mes, solo pago anual)", "educacion", 30, "masterclass.com",    "https://www.masterclass.com/account/membership"),
            CatalogItem("Skillshare Premium",      BigDecimal("8.00"),   "EUR", BillingCycle.MONTHLY, "Clases de diseño, negocios, tecnología y más",                                  "educacion", 7,  "skillshare.com",     "https://www.skillshare.com/en/profile/account"),
            CatalogItem("Brilliant Premium", BigDecimal("13.75"), "EUR", BillingCycle.MONTHLY, "Matemáticas, ciencia y programación de forma interactiva (precio mensual con pago anual)", "educacion", trialDays = 7, domain = "brilliant.org", cancelUrl = "https://brilliant.org/account"),
            CatalogItem("Brilliant Family", BigDecimal("28.00"), "EUR", BillingCycle.MONTHLY, "Brilliant para hasta 6 personas (precio mensual con pago anual)", "educacion", domain = "brilliant.org", cancelUrl = "https://brilliant.org/account"),
            CatalogItem("Babbel Premium",          BigDecimal("13.95"),  "EUR", BillingCycle.MONTHLY, "Aprendizaje de idiomas con lecciones breves de 15 min",                         "educacion", domain = "babbel.com",       cancelUrl = "https://account.babbel.com/en/account", priceAnnual = BigDecimal("83.40")),
            CatalogItem("Rosetta Stone",           BigDecimal("11.99"),  "EUR", BillingCycle.MONTHLY, "Aprendizaje de idiomas con inmersión y reconocimiento de voz",                  "educacion", 3,  "rosettastone.com",   "https://support.rosettastone.com/hc/en-us/articles/204765966"),
            CatalogItem("Domestika Plus",          BigDecimal("19.90"),  "EUR", BillingCycle.MONTHLY, "Acceso ilimitado a cursos de diseño y creatividad",                             "educacion", domain = "domestika.org",    cancelUrl = "https://www.domestika.org/en/settings/subscription", priceAnnual = BigDecimal("99.50")),
            CatalogItem("Platzi Expert", BigDecimal("239.00"), "EUR", BillingCycle.YEARLY, "Cursos de tecnología y programación en español — plan Expert anual", "educacion", domain = "platzi.com", cancelUrl = "https://platzi.com/account/subscriptions"),
            CatalogItem("Platzi Expert Duo", BigDecimal("319.00"), "EUR", BillingCycle.YEARLY, "Platzi Expert para 2 personas — plan anual", "educacion", domain = "platzi.com", cancelUrl = "https://platzi.com/account/subscriptions"),
            CatalogItem("DataCamp Premium", BigDecimal("144.00"), "EUR", BillingCycle.YEARLY, "DataCamp Premium — ciencia de datos, Python y R (12 €/mes con pago anual)", "educacion", domain = "datacamp.com", cancelUrl = "https://www.datacamp.com/profile/account_settings"),
            CatalogItem("Frontend Masters", BigDecimal("42.70"), "EUR", BillingCycle.MONTHLY, "Cursos avanzados de desarrollo web y JavaScript (ahora master.dev) — Se cobra en USD; importe aproximado", "educacion", domain = "master.dev", cancelUrl = "https://frontendmasters.com/settings/subscription", priceAnnual = BigDecimal("426.98"), variablePrice = true),
            CatalogItem("Udemy Personal Plan",     BigDecimal("25.99"),  "EUR", BillingCycle.MONTHLY, "Acceso a cursos seleccionados del catálogo Udemy",                              "educacion", domain = "udemy.com",        cancelUrl = "https://www.udemy.com/user/subscription"),
            CatalogItem("Preply Business",         BigDecimal("74.99"),  "EUR", BillingCycle.MONTHLY, "Clases particulares de idiomas con tutores nativos",                            "educacion", domain = "preply.com",       cancelUrl = "https://preply.com/en/account/settings"),
            CatalogItem("Photomath Plus",          BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Resuelve problemas matemáticos paso a paso con la cámara del móvil",             "educacion", 7,  "photomath.com",      "https://photomath.com/account"),
            CatalogItem("SoloLearn Pro",           BigDecimal("9.99"),   "EUR", BillingCycle.MONTHLY, "Aprende programación con rutas de aprendizaje, proyectos y comunidad",           "educacion", 7,  "sololearn.com",      "https://www.sololearn.com/settings"),
            CatalogItem("Mimo Premium", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Aprende a programar en 5 minutos al día con proyectos reales", "educacion", trialDays = 7, domain = "mimo.org", cancelUrl = "https://getmimo.com/settings"),
            CatalogItem("Duolingo Super",          BigDecimal("8.49"),   "EUR", BillingCycle.MONTHLY, "Duolingo sin anuncios, vidas ilimitadas y racha reparadora",                     "educacion", 14, "duolingo.com",       "https://www.duolingo.com/settings/super", priceAnnual = BigDecimal("87.96")),
            CatalogItem("Lingoda Sprint",          BigDecimal("99.00"),  "EUR", BillingCycle.MONTHLY, "Clases de idiomas en vivo con profesores nativos — sprints con reembolso",       "educacion", domain = "lingoda.com",      cancelUrl = "https://learn.lingoda.com/profile/subscription"),
            CatalogItem("italki Credits",          BigDecimal("20.00"),  "EUR", BillingCycle.MONTHLY, "Clases con profesores de idiomas de todo el mundo a tu ritmo",                   "educacion", domain = "italki.com",       cancelUrl = "https://www.italki.com/settings"),
            CatalogItem("Pimsleur Premium",        BigDecimal("19.99"),  "EUR", BillingCycle.MONTHLY, "Método de idiomas basado en audio — aprende escuchando sin leer",                "educacion", 7,  "pimsleur.com",       "https://www.pimsleur.com/account"),
            // Recibos recurrentes (V13)
            CatalogItem("Cuota de colegio",       BigDecimal("350.00"), "EUR", BillingCycle.MONTHLY, "Mensualidad de colegio concertado o privado — importe orientativo",                                          "educacion", variablePrice = true),
            CatalogItem("Matrícula universitaria",BigDecimal("1200.00"),"EUR", BillingCycle.YEARLY,  "Matrícula anual de universidad pública — importe orientativo, admite pago fraccionado",                      "educacion", variablePrice = true),
            CatalogItem("Academia",               BigDecimal("90.00"),  "EUR", BillingCycle.MONTHLY, "Academia de idiomas, oposiciones o refuerzo — cuota mensual orientativa",                                    "educacion", variablePrice = true)
        ),

        // ── Creatividad y foto ───────────────────────────────────────────────────
        "creatividad" to listOf(
            CatalogItem("VSCO Membership",         BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Edición de fotos con presets y comunidad creativa",                             "creatividad", 7,  "vsco.co",           "https://vsco.co/user/settings"),
            CatalogItem("Adobe Lightroom Plan", BigDecimal("14.74"), "EUR", BillingCycle.MONTHLY, "Edición y organización profesional de fotografía", "creatividad", trialDays = 30, domain = "lightroom.adobe.com", cancelUrl = "https://account.adobe.com/plans", priceAnnual = BigDecimal("144.33")),
            CatalogItem("Facetune Premium",        BigDecimal("5.99"),   "EUR", BillingCycle.MONTHLY, "Retoque de selfies y fotos de retrato con IA",                                  "creatividad", domain = "facetuneapp.com",   cancelUrl = "https://support.facetuneapp.com"),
            CatalogItem("CapCut Pro", BigDecimal("29.99"), "EUR", BillingCycle.MONTHLY, "CapCut Pro — edición de vídeo con efectos y plantillas (precio web de referencia)", "creatividad", trialDays = 7, domain = "capcut.com", cancelUrl = "https://www.capcut.com/settings", priceAnnual = BigDecimal("259.99")),
            CatalogItem("InShot Pro",              BigDecimal("3.99"),   "EUR", BillingCycle.MONTHLY, "Editor de vídeo y foto para redes sociales",                                    "creatividad", 7,  "inshot.com",        "https://help.inshot.com"),
            CatalogItem("Splice Premium", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Editor de vídeo para móvil con librería de sonidos", "creatividad", trialDays = 7, domain = "spliceapp.com", cancelUrl = "https://spliceapp.com/"),
            CatalogItem("Unfold Pro",              BigDecimal("1.99"),   "EUR", BillingCycle.MONTHLY, "Plantillas premium para historias de Instagram y TikTok",                       "creatividad", 7,  "unfold.com",        "https://unfold.com/pages/faq"),
            CatalogItem("Afterlight",              BigDecimal("2.99"),   "EUR", BillingCycle.MONTHLY, "Edición de fotos artística con filtros únicos",                                 "creatividad", domain = "afterlight.us",     cancelUrl = "https://afterlight.us/support"),
            CatalogItem("Picsart Pro", BigDecimal("12.99"), "EUR", BillingCycle.MONTHLY, "Picsart Pro (antes Gold) — editor de fotos y vídeos con herramientas de IA", "creatividad", trialDays = 7, domain = "picsart.com", cancelUrl = "https://picsart.com/account/subscriptions", priceAnnual = BigDecimal("109.92")),
            CatalogItem("Pixlr Premium", BigDecimal("9.99"), "EUR", BillingCycle.MONTHLY, "Editor de fotos y diseño gráfico con IA online y en móvil", "creatividad", domain = "pixlr.com", cancelUrl = "https://pixlr.com/account", priceAnnual = BigDecimal("95.88")),
            CatalogItem("Remini Pro",              BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Mejora la calidad de fotos antiguas y borrosas con IA",                          "creatividad", 7,  "remini.ai",         "https://remini.ai/settings"),
            CatalogItem("Prequel Premium",         BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Filtros y efectos de vídeo analógicos y retro para Instagram y TikTok",         "creatividad", 7,  "prequel.app",       "https://prequel.app/premium"),
            CatalogItem("YouCam Perfect Premium", BigDecimal("4.99"), "EUR", BillingCycle.MONTHLY, "Edición de selfies con retoques de piel, maquillaje y stickers AR", "creatividad", trialDays = 7, domain = "perfectcorp.com", cancelUrl = "https://www.perfectcorp.com/"),
            CatalogItem("Polarr Pro",              BigDecimal("7.99"),   "EUR", BillingCycle.MONTHLY, "Editor de fotos avanzado con filtros propios y edición por lotes",              "creatividad", 7,  "polarr.com",        "https://www.polarr.com/account"),
            CatalogItem("Veed Pro",                BigDecimal("24.00"),  "EUR", BillingCycle.MONTHLY, "Editor de vídeo online con subtítulos automáticos y grabación de pantalla",     "creatividad", 7,  "veed.io",           "https://www.veed.io/account/subscription"),
            CatalogItem("Adobe Express Premium",   BigDecimal("12.09"),   "EUR", BillingCycle.MONTHLY, "Diseño gráfico rápido — plantillas, vídeo y redes sociales con Adobe",         "creatividad", 30, "express.adobe.com", "https://account.adobe.com/plans")
        ),

        // ── Citas y social ───────────────────────────────────────────────────────
        "citas" to listOf(
            CatalogItem("Tinder Gold",             BigDecimal("16.49"),  "EUR", BillingCycle.MONTHLY, "Me gustas, rewind y passports para ligar en Tinder",                            "citas", domain = "tinder.com",    cancelUrl = "https://www.help.tinder.com/hc/en-us/articles/115005376946"),
            CatalogItem("Tinder Platinum",         BigDecimal("21.99"),  "EUR", BillingCycle.MONTHLY, "Todo Gold más prioridad en Likes y mensajes a matches",                         "citas", domain = "tinder.com",    cancelUrl = "https://www.help.tinder.com/hc/en-us/articles/115005376946"),
            CatalogItem("Bumble Premium",          BigDecimal("16.99"),  "EUR", BillingCycle.MONTHLY, "Filtros avanzados, modo incógnito y retroceder swipes",                         "citas", domain = "bumble.com",    cancelUrl = "https://bumble.com/en-us/the-buzz/cancellation"),
            CatalogItem("Hinge+",                  BigDecimal("29.99"),  "EUR", BillingCycle.MONTHLY, "Filtros extendidos y likes ilimitados en Hinge",                                "citas", domain = "hinge.co",      cancelUrl = "https://hingeapp.zendesk.com/hc/en-us/articles/360007194833"),
            CatalogItem("Meetic Premium",          BigDecimal("29.99"),  "EUR", BillingCycle.MONTHLY, "Citas online — perfil destacado y chat sin límites",                            "citas", 3,  "meetic.es",      "https://www.meetic.es/my/account/settings/subscription"),
            CatalogItem("Badoo Premium",           BigDecimal("12.99"),  "EUR", BillingCycle.MONTHLY, "Súper poderes en Badoo — ver quién te gustó",                                   "citas", domain = "badoo.com",     cancelUrl = "https://badoo.com/settings/premium"),
            CatalogItem("Grindr Unlimited",        BigDecimal("24.99"),  "EUR", BillingCycle.MONTHLY, "Chat sin anuncios, ver quién te visitó en Grindr",                              "citas", 7,  "grindr.com",     "https://help.grindr.com/hc/en-us/articles/115001667531"),
            CatalogItem("OkCupid A-List",          BigDecimal("24.99"),  "EUR", BillingCycle.MONTHLY, "Ver quién te gustó y sin anuncios en OkCupid",                                  "citas", domain = "okcupid.com",   cancelUrl = "https://www.okcupid.com/faq/a-list-faq"),
            CatalogItem("Happn Premium",           BigDecimal("24.99"),  "EUR", BillingCycle.MONTHLY, "Descubre y conecta con personas que cruzaste en la calle",                       "citas", 7,  "happn.com",          "https://www.happn.com/en/subscribe"),
            CatalogItem("Lovoo VIP",               BigDecimal("19.99"),  "EUR", BillingCycle.MONTHLY, "App de citas con IA matchmaking, perfiles destacados y chat ilimitado",          "citas", domain = "lovoo.com",      cancelUrl = "https://www.lovoo.com/vip"),
            CatalogItem("Coffee Meets Bagel",      BigDecimal("29.99"),  "EUR", BillingCycle.MONTHLY, "Citas de calidad — 1 match al día para relaciones más serias",                  "citas", 7,  "coffeemeetsbagel.com","https://coffeemeetsbagel.com/settings/subscription"),
            CatalogItem("Zoosk Premium",           BigDecimal("24.99"),  "EUR", BillingCycle.MONTHLY, "Red social de citas con algoritmo de aprendizaje y SmartPick",                   "citas", domain = "zoosk.com",      cancelUrl = "https://www.zoosk.com/account"),
            CatalogItem("eHarmony Premium",        BigDecimal("45.99"),  "EUR", BillingCycle.MONTHLY, "Citas serias con test de compatibilidad y matches profundos",                    "citas", domain = "eharmony.com",   cancelUrl = "https://www.eharmony.com/settings/billing"),
            CatalogItem("MiuMeet Premium",         BigDecimal("14.99"),  "EUR", BillingCycle.MONTHLY, "App de citas en tiempo real con mapa de personas cercanas",                     "citas", domain = "miumeet.com",    cancelUrl = "https://www.miumeet.com/settings")
        ),

        // ── Hogar y suministros (luz, gas, agua, alarmas) — importes orientativos ──
        "hogar" to listOf(
            CatalogItem("Iberdrola", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "iberdrola.es", cancelUrl = "https://www.iberdrola.es/clientes", variablePrice = true),
            CatalogItem("Endesa", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "endesa.com", cancelUrl = "https://www.endesa.com/es/clientes", variablePrice = true),
            CatalogItem("Naturgy", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "naturgy.es", cancelUrl = "https://www.naturgy.es/hogar", variablePrice = true),
            CatalogItem("Repsol Luz y Gas", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "repsol.es", cancelUrl = "https://www.repsol.es/particulares/", variablePrice = true),
            CatalogItem("TotalEnergies", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "totalenergies.es", cancelUrl = "https://www.totalenergies.es/es/hogares", variablePrice = true),
            CatalogItem("Holaluz", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "holaluz.com", cancelUrl = "https://www.holaluz.com/", variablePrice = true),
            CatalogItem("Octopus Energy", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "octopusenergy.es", cancelUrl = "https://octopusenergy.es/", variablePrice = true),
            CatalogItem("Plenitude", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz (Eni Plenitude)", "hogar", domain = "eniplenitude.es", cancelUrl = "https://eniplenitude.es/", variablePrice = true),
            CatalogItem("Factor Energía", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "factorenergia.com", cancelUrl = "https://www.factorenergia.com/", variablePrice = true),
            CatalogItem("Audax Renovables", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "audaxrenovables.com", cancelUrl = "https://www.audaxrenovables.com/", iconUrl = "https://www.google.com/s2/favicons?domain=audaxrenovables.com&sz=128", variablePrice = true),
            CatalogItem("Gana Energía", BigDecimal("70.00"), "EUR", BillingCycle.MONTHLY, "Luz (y gas si lo tienes contratado) — factura mensual según consumo; importe orientativo de la luz", "hogar", domain = "ganaenergia.com", cancelUrl = "https://ganaenergia.com/", variablePrice = true),
            CatalogItem("Canal de Isabel II",     BigDecimal("25.00"),  "EUR", BillingCycle.MONTHLY, "Agua en la Comunidad de Madrid — factura bimestral, equivalente mensual orientativo",                        "hogar", domain = "canaldeisabelsegunda.es", cancelUrl = "https://oficinavirtual.canaldeisabelsegunda.es/", variablePrice = true),
            CatalogItem("Aigües de Barcelona", BigDecimal("43.00"), "EUR", BillingCycle.MONTHLY, "Agua en el área de Barcelona — factura bimestral, equivalente mensual orientativo", "hogar", domain = "aiguesdebarcelona.cat", cancelUrl = "https://www.aiguesdebarcelona.cat/", variablePrice = true),
            CatalogItem("Emasesa", BigDecimal("40.25"), "EUR", BillingCycle.MONTHLY, "Agua en Sevilla — factura bimestral, equivalente mensual orientativo", "hogar", domain = "emasesa.com", cancelUrl = "https://www.emasesa.com/", variablePrice = true),
            CatalogItem("Global Omnium (Aguas de Valencia)", BigDecimal("28.17"), "EUR", BillingCycle.MONTHLY, "Agua en Valencia — factura bimestral, equivalente mensual orientativo", "hogar", domain = "globalomnium.com", cancelUrl = "https://www.globalomnium.com/", iconUrl = "https://www.google.com/s2/favicons?domain=globalomnium.com&sz=128", variablePrice = true),
            CatalogItem("Emaya", BigDecimal("32.92"), "EUR", BillingCycle.MONTHLY, "Agua en Palma — factura bimestral, equivalente mensual orientativo", "hogar", domain = "emaya.es", cancelUrl = "https://www.emaya.es/", variablePrice = true),
            CatalogItem("Aqualia", BigDecimal("25.00"), "EUR", BillingCycle.MONTHLY, "Agua en más de 1.000 municipios — factura periódica, equivalente mensual orientativo", "hogar", domain = "aqualia.com", cancelUrl = "https://www.aqualia.com/", iconUrl = "https://www.google.com/s2/favicons?domain=aqualia.com&sz=128", variablePrice = true),
            CatalogItem("Hidralia",               BigDecimal("25.00"),  "EUR", BillingCycle.MONTHLY, "Agua en Andalucía — factura periódica, equivalente mensual orientativo",                                     "hogar", domain = "hidralia-sa.es", cancelUrl = "https://www.hidralia-sa.es/", variablePrice = true),
            CatalogItem("Verisure (antes Securitas Direct)", BigDecimal("54.99"), "EUR", BillingCycle.MONTHLY, "Alarma para el hogar con conexión a central receptora (Verisure desde abril de 2026) — cuota mensual", "hogar", domain = "verisure.es", cancelUrl = "https://www.verisure.es/"),
            CatalogItem("Movistar Prosegur Alarmas", BigDecimal("48.90"), "EUR", BillingCycle.MONTHLY, "Alarma Movistar Prosegur para el hogar — cuota mensual", "hogar", domain = "movistarproseguralarmas.es", cancelUrl = "https://www.movistarproseguralarmas.es/"),
            CatalogItem("ADT Alarmas", BigDecimal("42.35"), "EUR", BillingCycle.MONTHLY, "Alarma ADT para el hogar (kit Basic) — cuota mensual", "hogar", domain = "adt.es", cancelUrl = "https://www.adt.es/"),
            CatalogItem("Comunidad de propietarios",BigDecimal("80.00"),  "EUR", BillingCycle.MONTHLY, "Cuota mensual de la comunidad de vecinos (derramas aparte) — importe orientativo",                           "hogar", variablePrice = true)
        ),

        // ── Telecomunicaciones (fibra y móvil) — importes orientativos ──────────
        "telecos" to listOf(
            CatalogItem("Movistar", BigDecimal("53.00"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil (miMovistar) — factura mensual con consumos extra (importe orientativo)", "telecos", domain = "movistar.es", cancelUrl = "https://www.movistar.es/particulares/atencion-cliente/", variablePrice = true),
            CatalogItem("Vodafone", BigDecimal("43.00"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil — factura mensual con consumos extra (importe orientativo)", "telecos", domain = "vodafone.es", cancelUrl = "https://www.vodafone.es/c/particulares/es/ayuda/", variablePrice = true),
            CatalogItem("Orange", BigDecimal("50.00"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil — factura mensual con consumos extra (importe orientativo)", "telecos", domain = "orange.es", cancelUrl = "https://www.orange.es/ayuda", variablePrice = true),
            CatalogItem("Digi", BigDecimal("15.00"), "EUR", BillingCycle.MONTHLY, "Fibra y móvil low-cost — factura mensual (importe orientativo)", "telecos", domain = "digimobil.es", cancelUrl = "https://www.digimobil.es/", variablePrice = true),
            CatalogItem("MásMóvil", BigDecimal("34.90"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil — factura mensual (importe orientativo)", "telecos", domain = "masmovil.es", cancelUrl = "https://www.masmovil.es/", iconUrl = "https://www.google.com/s2/favicons?domain=masmovil.es&sz=128", variablePrice = true),
            CatalogItem("Yoigo", BigDecimal("49.00"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil — factura mensual (importe orientativo)", "telecos", domain = "yoigo.com", cancelUrl = "https://www.yoigo.com/", variablePrice = true),
            CatalogItem("Pepephone", BigDecimal("35.00"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil — factura mensual (importe orientativo)", "telecos", domain = "pepephone.com", cancelUrl = "https://www.pepephone.com/", variablePrice = true),
            CatalogItem("Lowi", BigDecimal("30.00"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil (Vodafone) — factura mensual (importe orientativo)", "telecos", domain = "lowi.es", cancelUrl = "https://www.lowi.es/", variablePrice = true),
            CatalogItem("O2",                     BigDecimal("35.00"),  "EUR", BillingCycle.MONTHLY, "Fibra + móvil (Telefónica) — factura mensual (importe orientativo)",                                         "telecos", domain = "o2online.es", cancelUrl = "https://o2online.es/", variablePrice = true),
            CatalogItem("Simyo", BigDecimal("33.99"), "EUR", BillingCycle.MONTHLY, "Fibra y móvil low-cost (Orange) — factura mensual (importe orientativo)", "telecos", domain = "simyo.es", cancelUrl = "https://www.simyo.es/", variablePrice = true),
            CatalogItem("Finetwork", BigDecimal("23.90"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil — factura mensual (importe orientativo)", "telecos", domain = "finetwork.com", cancelUrl = "https://www.finetwork.com/", variablePrice = true),
            CatalogItem("Jazztel", BigDecimal("43.95"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil (Orange) — factura mensual (importe orientativo)", "telecos", domain = "jazztel.com", cancelUrl = "https://www.jazztel.com/", variablePrice = true),
            CatalogItem("Euskaltel",              BigDecimal("45.00"),  "EUR", BillingCycle.MONTHLY, "Fibra + móvil + TV — factura mensual (importe orientativo)",                                                 "telecos", domain = "euskaltel.com", cancelUrl = "https://www.euskaltel.com/", variablePrice = true),
            CatalogItem("Avatel",                 BigDecimal("30.00"),  "EUR", BillingCycle.MONTHLY, "Fibra + móvil — factura mensual (importe orientativo)",                                                      "telecos", domain = "avatel.es", cancelUrl = "https://www.avatel.es/", variablePrice = true),
            CatalogItem("Adamo", BigDecimal("24.00"), "EUR", BillingCycle.MONTHLY, "Fibra + móvil — factura mensual (importe orientativo)", "telecos", domain = "adamo.es", cancelUrl = "https://www.adamo.es/", variablePrice = true)
        ),

        // ── Seguros — prima anual orientativa (admite fraccionado) o cuota mensual ──
        "seguros" to listOf(
            CatalogItem("Mapfre",                 BigDecimal("450.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "mapfre.es", cancelUrl = "https://www.mapfre.es/", variablePrice = true),
            CatalogItem("AXA",                    BigDecimal("420.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "axa.es", cancelUrl = "https://www.axa.es/", variablePrice = true),
            CatalogItem("Allianz", BigDecimal("430.00"), "EUR", BillingCycle.YEARLY, "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado", "seguros", domain = "allianz.es", cancelUrl = "https://www.allianz.es/", iconUrl = "https://www.google.com/s2/favicons?domain=allianz.es&sz=128", variablePrice = true),
            CatalogItem("Mutua Madrileña",        BigDecimal("400.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, moto, hogar o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "mutua.es", cancelUrl = "https://www.mutua.es/", variablePrice = true),
            CatalogItem("Línea Directa", BigDecimal("390.00"), "EUR", BillingCycle.YEARLY, "Seguro de coche, moto u hogar — prima anual orientativa, admite pago fraccionado", "seguros", domain = "lineadirecta.com", cancelUrl = "https://www.lineadirecta.com/", variablePrice = true),
            CatalogItem("Génesis", BigDecimal("390.00"), "EUR", BillingCycle.YEARLY, "Seguro de coche, moto u hogar — prima anual orientativa, admite pago fraccionado", "seguros", domain = "genesis.es", cancelUrl = "https://www.genesis.es/", variablePrice = true),
            CatalogItem("Pelayo",                 BigDecimal("380.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "pelayo.com", cancelUrl = "https://www.pelayo.com/", variablePrice = true),
            CatalogItem("Reale",                  BigDecimal("400.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "reale.es", cancelUrl = "https://www.reale.es/", variablePrice = true),
            CatalogItem("Generali",               BigDecimal("420.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "generali.es", cancelUrl = "https://www.generali.es/", variablePrice = true),
            CatalogItem("Zurich",                 BigDecimal("430.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "zurich.es", cancelUrl = "https://www.zurich.es/", variablePrice = true),
            CatalogItem("Caser",                  BigDecimal("400.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "caser.es", cancelUrl = "https://www.caser.es/", variablePrice = true),
            CatalogItem("Occident", BigDecimal("420.00"), "EUR", BillingCycle.YEARLY, "Occident (antes Catalana Occidente) — seguro de coche, hogar, vida o salud; prima anual orientativa, admite pago fraccionado", "seguros", domain = "occident.com", cancelUrl = "https://www.occident.com/", iconUrl = "https://www.google.com/s2/favicons?domain=occident.com&sz=128", variablePrice = true),
            CatalogItem("Ocaso",                  BigDecimal("300.00"), "EUR", BillingCycle.YEARLY,  "Seguro de decesos u hogar — prima anual orientativa, admite pago fraccionado",                               "seguros", domain = "ocaso.es", cancelUrl = "https://www.ocaso.es/", variablePrice = true),
            CatalogItem("Santalucía",             BigDecimal("300.00"), "EUR", BillingCycle.YEARLY,  "Seguro de decesos, hogar o vida — prima anual orientativa, admite pago fraccionado",                         "seguros", domain = "santalucia.es", cancelUrl = "https://www.santalucia.es/", variablePrice = true),
            CatalogItem("Fiatc",                  BigDecimal("380.00"), "EUR", BillingCycle.YEARLY,  "Seguro de coche, hogar, vida o salud — prima anual orientativa, admite pago fraccionado",                    "seguros", domain = "fiatc.es", cancelUrl = "https://www.fiatc.es/", variablePrice = true),
            CatalogItem("Verti", BigDecimal("390.00"), "EUR", BillingCycle.YEARLY, "Seguro de coche o moto — prima anual orientativa, admite pago fraccionado", "seguros", domain = "verti.es", cancelUrl = "https://www.verti.es/", variablePrice = true),
            CatalogItem("Qualitas Auto", BigDecimal("390.00"), "EUR", BillingCycle.YEARLY, "Seguro de coche o moto — prima anual orientativa, admite pago fraccionado", "seguros", domain = "qualitasauto.com", cancelUrl = "https://www.qualitasauto.com/", variablePrice = true),
            CatalogItem("Balumba", BigDecimal("390.00"), "EUR", BillingCycle.YEARLY, "Seguro de coche o moto — prima anual orientativa, admite pago fraccionado", "seguros", domain = "balumba.es", cancelUrl = "https://www.balumba.es/", variablePrice = true),
            CatalogItem("Sanitas",                BigDecimal("60.00"),  "EUR", BillingCycle.MONTHLY, "Seguro de salud — cuota mensual según póliza (importe orientativo)",                                         "seguros", domain = "sanitas.es", cancelUrl = "https://www.sanitas.es/", variablePrice = true),
            CatalogItem("Adeslas", BigDecimal("55.00"), "EUR", BillingCycle.MONTHLY, "Seguro de salud (SegurCaixa Adeslas) — cuota mensual según póliza (importe orientativo)", "seguros", domain = "segurcaixaadeslas.es", cancelUrl = "https://www.segurcaixaadeslas.es/", variablePrice = true),
            CatalogItem("DKV",                    BigDecimal("55.00"),  "EUR", BillingCycle.MONTHLY, "Seguro de salud — cuota mensual según póliza (importe orientativo)",                                         "seguros", domain = "dkv.es", cancelUrl = "https://dkv.es/", variablePrice = true),
            CatalogItem("Asisa",                  BigDecimal("50.00"),  "EUR", BillingCycle.MONTHLY, "Seguro de salud — cuota mensual según póliza (importe orientativo)",                                         "seguros", domain = "asisa.es", cancelUrl = "https://www.asisa.es/", variablePrice = true),
            CatalogItem("Cosalud",                BigDecimal("45.00"),  "EUR", BillingCycle.MONTHLY, "Seguro de salud — cuota mensual según póliza (importe orientativo)",                                         "seguros", domain = "cosalud.com", cancelUrl = "https://www.cosalud.com/", variablePrice = true),
            CatalogItem("Barkibu",                BigDecimal("25.00"),  "EUR", BillingCycle.MONTHLY, "Seguro de mascotas — cuota mensual según raza y edad (importe orientativo)",                                 "seguros", domain = "barkibu.com", cancelUrl = "https://www.barkibu.com/", variablePrice = true),
            CatalogItem("Santévet",               BigDecimal("30.00"),  "EUR", BillingCycle.MONTHLY, "Seguro de mascotas — cuota mensual según raza y edad (importe orientativo)",                                 "seguros", domain = "santevet.es", cancelUrl = "https://www.santevet.es/", variablePrice = true)
        ),

        // ── Transporte (abonos, bici pública, peajes, parking) ──────────────────
        "transporte" to listOf(
            CatalogItem("Abono Transporte Madrid (CRTM)", BigDecimal("32.70"), "EUR", BillingCycle.MONTHLY, "Abono mensual de transporte público de Madrid (zona A con bonificación) — importe según zona y perfil", "transporte", domain = "crtm.es", cancelUrl = "https://www.crtm.es/", variablePrice = true),
            CatalogItem("Abono Joven Madrid (CRTM)", BigDecimal("10.00"), "EUR", BillingCycle.MONTHLY, "Abono Transporte Joven de Madrid (hasta 25 años), todas las zonas", "transporte", domain = "crtm.es", cancelUrl = "https://www.crtm.es/"),
            CatalogItem("T-usual / T-mobilitat (ATM Barcelona)", BigDecimal("22.80"), "EUR", BillingCycle.MONTHLY, "Abono mensual de transporte del área de Barcelona — importe según zona", "transporte", domain = "atm.cat", cancelUrl = "https://www.atm.cat/", variablePrice = true),
            CatalogItem("Bicimad",                BigDecimal("10.00"),  "EUR", BillingCycle.MONTHLY, "Tarifa plana de 30 días de la bicicleta pública de Madrid (ya no hay abono anual)",                       "transporte", domain = "bicimad.com", cancelUrl = "https://www.bicimad.com/"),
            CatalogItem("Bicing",                 BigDecimal("50.00"),  "EUR", BillingCycle.YEARLY,  "Abono anual de bicicleta pública de Barcelona",                                                              "transporte", domain = "bicing.barcelona", cancelUrl = "https://www.bicing.barcelona/"),
            CatalogItem("Bicing tarifa por uso", BigDecimal("35.00"), "EUR", BillingCycle.YEARLY, "Abono anual de Bicing con pago por trayecto", "transporte", domain = "bicing.barcelona", cancelUrl = "https://www.bicing.barcelona/"),
            CatalogItem("Uber One",               BigDecimal("4.99"),   "EUR", BillingCycle.MONTHLY, "Uber One — descuentos en viajes y envíos sin gastos en Uber Eats",                                           "transporte", domain = "uber.com", cancelUrl = "https://www.uber.com/es/es/u/uber-one/"),
            CatalogItem("Bip&Drive (Via-T)", BigDecimal("18.15"), "EUR", BillingCycle.YEARLY, "Cuota anual del dispositivo Via-T para peajes (15 € + IVA) — los peajes se cobran aparte", "transporte", domain = "bipdrive.com", cancelUrl = "https://www.bipdrive.com/", variablePrice = true),
            CatalogItem("Pagatelia (Via-T)", BigDecimal("6.15"), "EUR", BillingCycle.YEARLY, "Cuota anual del dispositivo Via-T para peajes (Fórmula Fácil/Frecuente) — los peajes se cobran aparte", "transporte", domain = "pagatelia.com", cancelUrl = "https://www.pagatelia.com/", variablePrice = true),
            CatalogItem("Parking / garaje",       BigDecimal("90.00"),  "EUR", BillingCycle.MONTHLY, "Plaza de garaje en alquiler o abono mensual de parking — importe orientativo",                               "transporte", variablePrice = true)
        ),

        // ── Belleza (cajas de cosmética por suscripción) ────────────────────────
        // Fuentes en docs/catalogo/revision-2026-11/belleza.md. Solo planes mensuales y anuales:
        // los de 3 y 6 meses de LOOKFANTASTIC se dejan fuera a propósito.
        // Blissim lleva iconUrl porque icon.horse solo devuelve su favicon de 32 px; el de
        // Google es el mismo logo a 128 px (y está permitido por la CSP de la web).
        "belleza" to listOf(
            CatalogItem("Blissim", BigDecimal("16.90"), "EUR", BillingCycle.MONTHLY, "Blissim — caja mensual de 5 productos de belleza, envío incluido en España peninsular y sin permanencia", "belleza", domain = "blissim.es", cancelUrl = "https://www.blissim.es/", iconUrl = "https://www.google.com/s2/favicons?domain=blissim.es&sz=128"),
            CatalogItem("LOOKFANTASTIC Beauty Box", BigDecimal("20.00"), "EUR", BillingCycle.MONTHLY, "LOOKFANTASTIC Beauty Box — caja mensual de belleza, pago mensual sin permanencia", "belleza", domain = "lookfantastic.es", cancelUrl = "https://www.lookfantastic.es/"),
            CatalogItem("LOOKFANTASTIC Beauty Box 12 meses", BigDecimal("17.00"), "EUR", BillingCycle.MONTHLY, "LOOKFANTASTIC Beauty Box — caja mensual de belleza, pago mensual con compromiso de 12 meses", "belleza", domain = "lookfantastic.es", cancelUrl = "https://www.lookfantastic.es/"),
            CatalogItem("LOOKFANTASTIC Beauty Box Anual", BigDecimal("200.00"), "EUR", BillingCycle.YEARLY, "LOOKFANTASTIC Beauty Box — 12 cajas mensuales de belleza con un único pago anual", "belleza", domain = "lookfantastic.es", cancelUrl = "https://www.lookfantastic.es/")
        ),

        // ── Préstamos y financiaciones (V18) ─────────────────────────────────────
        // Sin precio (`price = null`): cada usuario pone su cuota. Fuentes y logos en
        // docs/catalogo/revision-2026-11/prestamos.md. Sin microcréditos, por decisión del usuario.
        // iconUrl solo donde icon.horse da una inicial genérica o un logo peor (ver el informe).
        "prestamos" to listOf(
            // Bancos
            CatalogItem("Banco Santander", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche del Banco Santander — pon la cuota de tu contrato", "prestamos", domain = "bancosantander.es", cancelUrl = "https://www.bancosantander.es/", iconUrl = "https://www.google.com/s2/favicons?domain=bancosantander.es&sz=128"),
            CatalogItem("BBVA", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de BBVA — pon la cuota de tu contrato", "prestamos", domain = "bbva.es", cancelUrl = "https://www.bbva.es/"),
            CatalogItem("CaixaBank", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de CaixaBank — pon la cuota de tu contrato", "prestamos", domain = "caixabank.es", cancelUrl = "https://www.caixabank.es/"),
            CatalogItem("Banco Sabadell", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche del Banco Sabadell — pon la cuota de tu contrato", "prestamos", domain = "bancsabadell.com", cancelUrl = "https://www.bancsabadell.com/"),
            CatalogItem("Bankinter", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de Bankinter (incluye los antiguos clientes de EVO Banco) — pon la cuota de tu contrato", "prestamos", domain = "bankinter.com", cancelUrl = "https://www.bankinter.com/"),
            CatalogItem("Unicaja", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de Unicaja — pon la cuota de tu contrato", "prestamos", domain = "unicajabanco.es", cancelUrl = "https://www.unicajabanco.es/"),
            CatalogItem("Abanca", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de Abanca — pon la cuota de tu contrato", "prestamos", domain = "abanca.com", cancelUrl = "https://www.abanca.com/"),
            CatalogItem("Kutxabank", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de Kutxabank — pon la cuota de tu contrato", "prestamos", domain = "kutxabank.es", cancelUrl = "https://www.kutxabank.es/"),
            CatalogItem("Ibercaja", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de Ibercaja — pon la cuota de tu contrato", "prestamos", domain = "ibercaja.es", cancelUrl = "https://www.ibercaja.es/"),
            CatalogItem("ING", null, "EUR", BillingCycle.MONTHLY, "Hipoteca Naranja o préstamo de ING — pon la cuota de tu contrato", "prestamos", domain = "ing.es", cancelUrl = "https://www.ing.es/"),
            CatalogItem("Openbank", null, "EUR", BillingCycle.MONTHLY, "Hipoteca o préstamo de Openbank — pon la cuota de tu contrato", "prestamos", domain = "openbank.es", cancelUrl = "https://www.openbank.es/"),
            CatalogItem("Cajamar", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de Cajamar — pon la cuota de tu contrato", "prestamos", domain = "cajamar.es", cancelUrl = "https://www.cajamar.es/"),
            CatalogItem("Caja Rural", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de tu Caja Rural (Ruralvía) — pon la cuota de tu contrato", "prestamos", domain = "ruralvia.com", cancelUrl = "https://www.grupocajarural.es/es"),
            CatalogItem("Laboral Kutxa", null, "EUR", BillingCycle.MONTHLY, "Hipoteca, préstamo personal o de coche de Laboral Kutxa — pon la cuota de tu contrato", "prestamos", domain = "laboralkutxa.com", cancelUrl = "https://www.laboralkutxa.com/"),
            // Financieras de consumo y pago a plazos
            CatalogItem("Cetelem", null, "EUR", BillingCycle.MONTHLY, "Préstamo personal, financiación de compras o tarjeta de Cetelem — pon la cuota de tu contrato", "prestamos", domain = "cetelem.es", cancelUrl = "https://www.cetelem.es/", iconUrl = "https://www.google.com/s2/favicons?domain=cetelem.fr&sz=128"),
            CatalogItem("Cofidis", null, "EUR", BillingCycle.MONTHLY, "Préstamo personal, crédito o financiación de compras de Cofidis — pon la cuota de tu contrato", "prestamos", domain = "cofidis.es", cancelUrl = "https://www.cofidis.es/"),
            CatalogItem("Santander Consumer Finance", null, "EUR", BillingCycle.MONTHLY, "Financiación de coche o de compras con Santander Consumer Finance — pon la cuota de tu contrato", "prestamos", domain = "santanderconsumer.es", cancelUrl = "https://www.santanderconsumer.es/"),
            CatalogItem("Sabadell Consumer Finance", null, "EUR", BillingCycle.MONTHLY, "Financiación de compras en tienda con Sabadell Consumer Finance — pon la cuota de tu contrato", "prestamos", domain = "sabadellconsumer.com", cancelUrl = "https://www.sabadellconsumer.com/"),
            CatalogItem("Bankinter Consumer Finance", null, "EUR", BillingCycle.MONTHLY, "Préstamo, tarjeta o financiación de Bankinter Consumer Finance — pon la cuota de tu contrato", "prestamos", domain = "bankinterconsumerfinance.com", cancelUrl = "https://www.bankinterconsumerfinance.com/"),
            CatalogItem("CaixaBank Payments & Consumer", null, "EUR", BillingCycle.MONTHLY, "Financiación de compras o tarjeta de CaixaBank Payments & Consumer — pon la cuota de tu contrato", "prestamos", domain = "caixabankpc.com", cancelUrl = "https://www.caixabankpc.com/"),
            CatalogItem("Oney", null, "EUR", BillingCycle.MONTHLY, "Financiación de compras en tienda o pago en 3 y 4 veces con Oney — pon la cuota de tu contrato", "prestamos", domain = "oney.es", cancelUrl = "https://www.oney.es/", iconUrl = "https://www.google.com/s2/favicons?domain=oney.com&sz=128"),
            CatalogItem("WiZink", null, "EUR", BillingCycle.MONTHLY, "Tarjeta de crédito o préstamo de WiZink — pon la cuota de tu contrato", "prestamos", domain = "wizink.es", cancelUrl = "https://www.wizink.es/"),
            CatalogItem("Younited", null, "EUR", BillingCycle.MONTHLY, "Préstamo personal online de Younited — pon la cuota de tu contrato", "prestamos", domain = "younited.com", cancelUrl = "https://younited.com/es/"),
            CatalogItem("Financiera El Corte Inglés", null, "EUR", BillingCycle.MONTHLY, "Financiación de compras o tarjeta de El Corte Inglés — pon la cuota de tu contrato", "prestamos", domain = "elcorteingles.es", cancelUrl = "https://www.elcorteingles.es/"),
            CatalogItem("Volkswagen Financial Services", null, "EUR", BillingCycle.MONTHLY, "Financiación o renting de coche con Volkswagen Financial Services — pon la cuota de tu contrato", "prestamos", domain = "vwfs.es", cancelUrl = "https://www.vwfs.es/"),
            CatalogItem("Klarna", null, "EUR", BillingCycle.MONTHLY, "Pago a plazos con Klarna — pon la cuota de tu contrato", "prestamos", domain = "klarna.com", cancelUrl = "https://www.klarna.com/es/", iconUrl = "https://www.google.com/s2/favicons?domain=klarna.com&sz=128"),
            CatalogItem("PayPal Paga en 3 plazos", null, "EUR", BillingCycle.MONTHLY, "Compra pagada en 3 plazos con PayPal — pon la cuota de tu contrato", "prestamos", domain = "paypal.com", cancelUrl = "https://www.paypal.com/es/"),
            CatalogItem("seQura", null, "EUR", BillingCycle.MONTHLY, "Pago a plazos con seQura — pon la cuota de tu contrato", "prestamos", domain = "sequra.com", cancelUrl = "https://www.sequra.com/"),
            CatalogItem("Aplazame", null, "EUR", BillingCycle.MONTHLY, "Pago a plazos con Aplazame — pon la cuota de tu contrato", "prestamos", domain = "aplazame.com", cancelUrl = "https://aplazame.com/"),
            // Clínicas
            CatalogItem("Vitaldent (financiación)", null, "EUR", BillingCycle.MONTHLY, "Tratamiento dental financiado en Vitaldent — pon la cuota de tu contrato", "prestamos", domain = "vitaldent.com", cancelUrl = "https://www.vitaldent.com/"),
            // Genéricos sin marca
            CatalogItem("Hipoteca", null, "EUR", BillingCycle.MONTHLY, "Cuota mensual de la hipoteca — pon la cuota de tu contrato", "prestamos"),
            CatalogItem("Préstamo personal", null, "EUR", BillingCycle.MONTHLY, "Cuota mensual de un préstamo personal — pon la cuota de tu contrato", "prestamos"),
            CatalogItem("Préstamo de coche", null, "EUR", BillingCycle.MONTHLY, "Cuota mensual de la financiación del coche — pon la cuota de tu contrato", "prestamos"),
            CatalogItem("Financiación de compra a plazos", null, "EUR", BillingCycle.MONTHLY, "Cuota de una compra financiada (móvil, electrodoméstico, muebles…) — pon la cuota de tu contrato", "prestamos"),
            CatalogItem("Tarjeta revolving", null, "EUR", BillingCycle.MONTHLY, "Cuota mensual de una tarjeta de crédito revolving — pon la cuota de tu contrato", "prestamos")
        )
    )

    /**
     * Catálogo completo: [baseCatalog] más la categoría "prueba", que se genera a partir de las
     * filas originales (ver [TRIALS]). Antes "prueba" eran copias a mano y se quedaban con
     * precios y dominios viejos cuando se actualizaba el original.
     */
    private val catalog: Map<String, List<CatalogItem>> = buildMap {
        for ((key, items) in baseCatalog) {
            put(key, items)
            // Se inserta tras "salud" para conservar el orden de siempre en getAllItemsGrouped().
            if (key == "salud") put("prueba", trialItems())
        }
    }

    /**
     * Copia cada fila original de [TRIALS] con la clave "prueba" y sus días de prueba.
     * Falla al arrancar si una prueba apunta a una fila que ya no existe, para que el
     * despiste se vea en los tests y no como una tarjeta que falta en la web.
     */
    private fun trialItems(): List<CatalogItem> = TRIALS.map { (originKey, name, days) ->
        val original = baseCatalog[originKey]?.firstOrNull { it.name == name }
            ?: error("Prueba gratuita sin fila original: '$name' en '$originKey'")
        original.copy(categoryKey = "prueba", trialDays = days)
    }

    /**
     * Devuelve los servicios del catálogo que corresponden al nombre de una categoría.
     * El nombre se traduce a clave con [CategoryMappingService.keyFor] (único punto de verdad).
     * Si no hay coincidencia, devuelve todos los servicios ordenados por nombre.
     */
    fun getItemsForCategory(categoryName: String): List<CatalogItem> {
        val key = CategoryMappingService.keyFor(categoryName)
        return (if (key != null) catalog[key] else null) ?: catalog.values.flatten().sortedWith(POR_NOMBRE)
    }

    /** Devuelve todos los servicios del catálogo ordenados por nombre (sin distinguir mayúsculas ni acentos). */
    fun getAllItems(): List<CatalogItem> = catalog.values.flatten().sortedWith(POR_NOMBRE)

    /** Devuelve todos los items agrupados por clave de categoría. */
    fun getAllItemsGrouped(): Map<String, List<CatalogItem>> = catalog

    /** Devuelve un mapa nombre → dominio para mostrar logos en los templates. */
    fun getDomainMap(): Map<String, String> =
        withRenamedNames(
            catalog.values.flatten()
                .filter { it.domain != null }
                .associate { it.name to it.domain!! }
        )

    /** Devuelve un mapa nombre → URL de cancelación para mostrar el enlace directo. */
    fun getCancelUrlMap(): Map<String, String> =
        withRenamedNames(
            catalog.values.flatten()
                .filter { it.cancelUrl != null }
                .associate { it.name to it.cancelUrl!! }
        )

    /**
     * Devuelve un mapa nombre → URL directa del icono, solo para los servicios que la tienen
     * (aquellos en los que icon.horse devuelve una inicial genérica en vez del logo real).
     * La web la prueba antes que icon.horse, igual que la app Android.
     */
    fun getIconUrlMap(): Map<String, String> =
        withRenamedNames(
            catalog.values.flatten()
                .filter { it.iconUrl != null }
                .associate { it.name to it.iconUrl!! }
        )

    /**
     * Añade las entradas de los nombres antiguos de [RENAMED_SERVICES] para que las suscripciones
     * que el usuario guardó con el nombre de antes sigan encontrando su logo y su enlace de baja.
     * Si un nombre antiguo coincide con uno actual, gana el actual.
     */
    private fun withRenamedNames(byName: Map<String, String>): Map<String, String> =
        RENAMED_SERVICES.mapNotNull { (old, current) -> byName[current]?.let { old to it } }.toMap() + byName

    companion object {
        /** Orden alfabético español sin distinguir mayúsculas ni acentos ("Ibercaja" antes que "ING"). */
        private val POR_NOMBRE: Comparator<CatalogItem> =
            compareBy(java.text.Collator.getInstance(java.util.Locale.forLanguageTag("es")).apply { strength = java.text.Collator.PRIMARY }) { it.name }

        /**
         * Servicios con prueba gratuita: (categoría de origen, nombre en esa categoría, días).
         * Si se renombra o elimina la fila original, hay que actualizar aquí (lo comprueba el test).
         */
        internal val TRIALS: List<Triple<String, String, Int>> = listOf(
            Triple("streaming", "Netflix Estándar", 30),
            Triple("musica", "Spotify Premium", 30),
            Triple("streaming", "Amazon Prime", 30),
            Triple("streaming", "Disney+", 7),
            Triple("streaming", "Apple TV", 7),
            Triple("ia", "Cursor Premium", 14),
            Triple("software", "Adobe Creative Cloud", 7),
            Triple("streaming", "YouTube Premium", 30),
            Triple("software", "Notion Plus", 14),
            Triple("educacion", "Duolingo Super", 14),
            Triple("seguridad", "NordVPN", 30),
            Triple("software", "Grammarly Pro", 7),
            Triple("streaming", "Apple One", 30),
            Triple("software", "LinkedIn Premium", 30),
            Triple("gaming", "GeForce Now Performance", 30),
            Triple("seguridad", "Keeper Security", 30),
            Triple("noticias", "Medium", 30)
        )

        /** Nombre anterior → nombre actual de los planes renombrados en la revisión de noviembre de 2026. */
        internal val RENAMED_SERVICES: Map<String, String> = mapOf(
            "Claude Max 5x" to "Claude Max",
            "Claude Max 20x" to "Claude Ultra",
            "ChatGPT Pro" to "ChatGPT Pro 200",
            "ChatGPT Pro 5x" to "ChatGPT Pro 100",
            "Cursor Pro" to "Cursor Premium",
            "GitHub Copilot" to "GitHub Copilot Pro",
            "Gemini Advanced" to "Google AI Pro",
            "Notion AI" to "Notion Business",
            "Jasper AI Creator" to "Jasper Pro",
            "Adobe Firefly Premium" to "Adobe Firefly Standard",
            "Apple TV+" to "Apple TV",
            "Max" to "HBO Max Básico con anuncios",
            "Max sin anuncios" to "HBO Max Estándar",
            "Max Premium" to "HBO Max Premium",
            "Tidal HiFi" to "TIDAL Individual",
            "Microsoft 365 Familiar" to "Microsoft 365 Familia",
            "Sketch" to "Sketch Standard",
            "Grammarly Premium" to "Grammarly Pro",
            "Airtable Plus" to "Airtable Team",
            "Zapier Starter" to "Zapier Professional",
            "Miro Team" to "Miro Starter",
            "Framer Mini" to "Framer Basic",
            "Craft Docs Pro" to "Craft Plus",
            "Day One Premium" to "Day One Silver",
            "PDF Expert Pro" to "PDF Expert Premium",
            "Pitch Pro" to "Pitch Plus",
            "Basecamp Business" to "Basecamp Pro Plus",
            "Notion Team" to "Notion Business",
            "Google One 2 TB" to "Google AI Plus 2 TB",
            "Google One 5 TB" to "Google AI Pro",
            // Era una segunda fila del mismo plan en "cloud" (D8): queda solo la de "ia".
            "Google AI Pro 5 TB" to "Google AI Pro",
            "OneDrive 100 GB" to "Microsoft 365 Básico",
            "pCloud Premium 2 TB" to "pCloud Premium Plus 2 TB",
            "Sync.com Solo Basic" to "Sync.com Personal 1 TB",
            "GeForce Now Priority" to "GeForce Now Performance",
            "Shadow PC" to "Shadow PC Neo",
            "Ubisoft+ Essential" to "Ubisoft+ Premium",
            "Tutanota Premium" to "Tuta Revolutionary",
            "Readwise Reader" to "Readwise Full",
            "Strava Summit" to "Strava",
            "Whoop" to "Whoop One",
            "Fitbit Premium" to "Google Health Premium",
            "Linear" to "Linear Basic",
            "Postman Basic" to "Postman Solo",
            "Warp AI" to "Warp Build",
            "Retool Cloud" to "Retool Team",
            "N26 You" to "N26 Go",
            "Wallet Pro" to "Wallet Premium",
            "Bunq Easy Bank" to "bunq Pro",
            "DataCamp Standard" to "DataCamp Premium",
            "PicsArt Gold" to "Picsart Pro",
            "Securitas Direct" to "Verisure (antes Securitas Direct)",
            "Prosegur Alarmas" to "Movistar Prosegur Alarmas",
            "Catalana Occidente" to "Occident",
            // Segunda y tercera pasada (precios web verificados)
            "Evernote Personal" to "Evernote Starter",
            "CleanMyMac Business" to "CleanMyMac",
            "Tresorit Personal" to "Tresorit Personal Essential",
            "Malwarebytes Premium" to "Malwarebytes Standard",
            "SoundCloud Next Pro" to "SoundCloud Go+",
            "Beatport Link" to "Beatport Streaming Essential",
            "Le Monde Numérique" to "Le Monde Essentiel",
            "Mixcloud Select" to "Mixcloud Premium"
        )
    }
}
