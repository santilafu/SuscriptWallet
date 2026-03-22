package com.subia.service

import com.subia.model.BillingCycle
import com.subia.model.CatalogItem
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Proporciona el catálogo de servicios de suscripción conocidos con sus precios en euros.
 *
 * Todos los precios están actualizados a marzo de 2026 y expresados en EUR.
 * El catálogo es estático y vive en memoria; no se persiste en base de datos.
 * Para actualizar un precio, edita el valor en la lista correspondiente dentro de [catalog].
 *
 * Claves de categoría disponibles:
 *   "ia", "streaming", "musica", "software", "cloud", "gaming", "seguridad",
 *   "noticias", "salud", "desarrollo", "finanzas", "educacion", "creatividad", "citas"
 */
@Service
class CatalogService {

    /**
     * Mapa clave → lista de servicios. Todos los precios están en EUR.
     */
    private val catalog: Map<String, List<CatalogItem>> = mapOf(

        // ── Inteligencia Artificial ──────────────────────────────────────────────
        "ia" to listOf(
            CatalogItem("Claude Pro",              BigDecimal("20.56"),  "EUR", BillingCycle.MONTHLY, "Anthropic Claude Pro — acceso prioritario y límites ampliados",                "ia", domain = "claude.ai",          cancelUrl = "https://claude.ai/settings/billing"),
            CatalogItem("ChatGPT Plus",            BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Plus — GPT-4o con acceso prioritario",                          "ia", domain = "openai.com",         cancelUrl = "https://chatgpt.com/settings"),
            CatalogItem("ChatGPT Pro",             BigDecimal("223.85"), "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Pro — acceso ilimitado a o1 y herramientas avanzadas",           "ia", domain = "openai.com",         cancelUrl = "https://chatgpt.com/settings"),
            CatalogItem("Cursor Pro",              BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Editor de código con IA — plan Pro mensual",                                    "ia", domain = "cursor.com",         cancelUrl = "https://www.cursor.com/settings"),
            CatalogItem("GitHub Copilot",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Asistente de código IA para IDEs — plan Individual",                            "ia", domain = "github.com",         cancelUrl = "https://github.com/settings/copilot"),
            CatalogItem("Gemini Advanced",         BigDecimal("26.61"),  "EUR", BillingCycle.MONTHLY, "Google One AI Premium — Gemini Advanced + 2 TB de almacenamiento",              "ia", domain = "gemini.google.com",  cancelUrl = "https://one.google.com/about"),
            CatalogItem("Perplexity Pro",          BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Motor de búsqueda con IA — búsquedas ilimitadas y modelos avanzados",           "ia", domain = "perplexity.ai",      cancelUrl = "https://www.perplexity.ai/settings/account"),
            CatalogItem("Midjourney Standard",     BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA — 15 horas GPU/mes",                                  "ia", domain = "midjourney.com",     cancelUrl = "https://www.midjourney.com/account"),
            CatalogItem("Midjourney Basic",        BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA — 3,3 horas GPU/mes",                                 "ia", domain = "midjourney.com",     cancelUrl = "https://www.midjourney.com/account"),
            CatalogItem("Grok SuperGrok",          BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "xAI Grok SuperGrok — acceso a Grok 3 con límites ampliados",                    "ia", domain = "x.com",             cancelUrl = "https://x.com/i/premium_sign_up"),
            CatalogItem("Notion AI",               BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "Complemento de IA para Notion — resúmenes, redacción y búsqueda semántica",     "ia", domain = "notion.so",          cancelUrl = "https://www.notion.so/profile/plans"),
            CatalogItem("Canva Pro",               BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Diseño gráfico con IA — plantillas premium, Magic Studio y marca",              "ia", domain = "canva.com",          cancelUrl = "https://www.canva.com/settings/purchase-history"),
            CatalogItem("ElevenLabs Creator",      BigDecimal("26.62"),  "EUR", BillingCycle.MONTHLY, "Síntesis de voz IA realista",                                                   "ia", 7,  "elevenlabs.io",       "https://elevenlabs.io/subscription"),
            CatalogItem("Runway Standard",         BigDecimal("18.15"),  "EUR", BillingCycle.MONTHLY, "Generación y edición de vídeo con IA",                                          "ia", domain = "runwayml.com",        cancelUrl = "https://app.runwayml.com/settings"),
            CatalogItem("Character.AI Plus",       BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Chat con personajes IA",                                                         "ia", domain = "character.ai",        cancelUrl = "https://character.ai/subscription"),
            CatalogItem("Suno Pro",                BigDecimal("12.10"),  "EUR", BillingCycle.MONTHLY, "Generación de música con IA",                                                   "ia", domain = "suno.com",             cancelUrl = "https://suno.com/account"),
            CatalogItem("Leonardo AI Artisan",     BigDecimal("29.04"),  "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA para creadores",                                      "ia", domain = "leonardo.ai",          cancelUrl = "https://app.leonardo.ai/settings"),
            CatalogItem("Poe Basic",               BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Acceso a múltiples modelos IA (Claude, GPT-4…)",                                "ia", domain = "poe.com",              cancelUrl = "https://poe.com/settings"),
            CatalogItem("Jasper AI Creator",       BigDecimal("59.29"),  "EUR", BillingCycle.MONTHLY, "Redacción de contenido con IA para marketing",                                  "ia", 7,  "jasper.ai",           "https://app.jasper.ai/settings/billing"),
            CatalogItem("Adobe Firefly Premium",   BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA integrada en Adobe",                                  "ia", 30, "firefly.adobe.com",   "https://account.adobe.com/plans"),
            CatalogItem("Krea AI Pro",             BigDecimal("42.35"),  "EUR", BillingCycle.MONTHLY, "Generación y mejora de imágenes en tiempo real",                                "ia", domain = "krea.ai",              cancelUrl = "https://www.krea.ai/settings")
        ),

        // ── Streaming de vídeo ───────────────────────────────────────────────────
        "streaming" to listOf(
            CatalogItem("Netflix Estándar",        BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD, sin anuncios",                                   "streaming", domain = "netflix.com",       cancelUrl = "https://www.netflix.com/cancelplan"),
            CatalogItem("Netflix Premium",         BigDecimal("27.82"),  "EUR", BillingCycle.MONTHLY, "Netflix — 4 pantallas Ultra HD, sin anuncios",                                  "streaming", domain = "netflix.com",       cancelUrl = "https://www.netflix.com/cancelplan"),
            CatalogItem("Netflix con anuncios",    BigDecimal("7.25"),   "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD con anuncios",                                    "streaming", domain = "netflix.com",       cancelUrl = "https://www.netflix.com/cancelplan"),
            CatalogItem("Disney+",                 BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Disney+ — Disney, Marvel, Star Wars y National Geographic",                     "streaming", domain = "disneyplus.com",    cancelUrl = "https://www.disneyplus.com/account"),
            CatalogItem("Amazon Prime",            BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Amazon Prime — envíos gratis + Prime Video + Prime Music",                      "streaming", domain = "amazon.com",        cancelUrl = "https://www.amazon.es/mc/pipelines/cancellation"),
            CatalogItem("YouTube Premium",         BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "YouTube sin anuncios, reproducción en segundo plano y YouTube Music",           "streaming", domain = "youtube.com",       cancelUrl = "https://www.youtube.com/paid_memberships"),
            CatalogItem("Apple TV+",               BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Apple TV+ — series y películas originales Apple",                               "streaming", domain = "apple.com",         cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Max",                     BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Max (HBO) — plan con anuncios, series y películas HBO y Warner",                "streaming", domain = "max.com",           cancelUrl = "https://www.max.com/es/es/account/subscription"),
            CatalogItem("Max sin anuncios",        BigDecimal("19.35"),  "EUR", BillingCycle.MONTHLY, "Max (HBO) — plan sin anuncios, descargas incluidas",                            "streaming", domain = "max.com",           cancelUrl = "https://www.max.com/es/es/account/subscription"),
            CatalogItem("Crunchyroll Mega Fan",    BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Crunchyroll — anime en simulcast sin anuncios, acceso offline",                 "streaming", domain = "crunchyroll.com",   cancelUrl = "https://www.crunchyroll.com/account/membership"),
            CatalogItem("DAZN",                    BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "DAZN — deportes en directo: LaLiga, MotoGP, Fórmula 1",                         "streaming", domain = "dazn.com",          cancelUrl = "https://www.dazn.com/es-ES/account/subscription"),
            CatalogItem("Movistar+",               BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Movistar+ — series, fútbol y entretenimiento premium",                          "streaming", domain = "movistar.es",       cancelUrl = "https://www.movistar.es/particulares/television/movistar-plus/"),
            CatalogItem("Filmin",                  BigDecimal("10.88"),  "EUR", BillingCycle.MONTHLY, "Filmin — cine independiente, clásicos y festivales",                            "streaming", domain = "filmin.es",         cancelUrl = "https://www.filmin.es/ajustes"),
            CatalogItem("SkyShowtime",             BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "SkyShowtime — Paramount+, Peacock, Universal y Nickelodeon",                    "streaming", domain = "skyshowtime.com",   cancelUrl = "https://www.skyshowtime.com/account"),
            CatalogItem("Paramount+",              BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Paramount+ — series CBS, Paramount originals y deportes",                       "streaming", domain = "paramountplus.com", cancelUrl = "https://www.paramountplus.com/account/"),
            CatalogItem("Twitch Turbo",            BigDecimal("10.27"),  "EUR", BillingCycle.MONTHLY, "Twitch Turbo — sin anuncios en todos los canales",                              "streaming", domain = "twitch.tv",         cancelUrl = "https://www.twitch.tv/settings/turbo"),
            CatalogItem("Apple One",               BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Apple One Individual — Apple TV+, Music, Arcade, iCloud+ en un plan",           "streaming", 30, "apple.com",          "https://appleid.apple.com/account/manage"),
            CatalogItem("Mubi",                    BigDecimal("13.99"),  "EUR", BillingCycle.MONTHLY, "Mubi — cine de autor, premiado y de culto con 30 películas curadas",              "streaming", 7,  "mubi.com",           "https://mubi.com/es/account"),
            CatalogItem("Plex Pass",               BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Plex Pass — streaming de tu biblioteca personal + canales premium",              "streaming", domain = "plex.tv",           cancelUrl = "https://www.plex.tv/plex-pass/"),
            CatalogItem("Discovery+",              BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Discovery+ — documentales, reality shows y contenido de Discovery",              "streaming", 7,  "discoveryplus.com",  "https://www.discoveryplus.com/es/account"),
            CatalogItem("Hulu",                    BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Hulu — series y películas con anuncios, catálogo de EE.UU.",                    "streaming", 30, "hulu.com",           "https://secure.hulu.com/account"),
            CatalogItem("Peacock Premium",         BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Streaming NBCUniversal, deportes y noticias",                                   "streaming", 7,  "peacocktv.com",      "https://www.peacocktv.com/account/cancel"),
            CatalogItem("ESPN+",                   BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Deportes en streaming — UFC, béisbol, fútbol americano",                        "streaming", domain = "espnplus.com",      cancelUrl = "https://plus.espn.com/settings"),
            CatalogItem("BritBox",                 BigDecimal("8.77"),   "EUR", BillingCycle.MONTHLY, "Mejor contenido británico — BBC, ITV, Channel 4",                               "streaming", 7,  "britbox.com",        "https://www.britbox.com/account/cancel"),
            CatalogItem("Shudder",                 BigDecimal("7.25"),   "EUR", BillingCycle.MONTHLY, "Streaming especializado en terror y suspense",                                  "streaming", 7,  "shudder.com",        "https://www.shudder.com/account/cancel"),
            CatalogItem("Nebula",                  BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Contenido original de creadores de YouTube",                                    "streaming", 30, "nebula.tv",          "https://nebula.tv/account/subscription"),
            CatalogItem("CuriosityStream",         BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Documentales científicos y de naturaleza",                                      "streaming", 7,  "curiositystream.com","https://curiositystream.com/account/subscription"),
            CatalogItem("Canal+",                  BigDecimal("32.66"),  "EUR", BillingCycle.MONTHLY, "Cine, series y deportes en español",                                            "streaming", 30, "canalplus.com",      "https://www.canalplus.com/micuenta"),
            CatalogItem("Viki Pass Standard",      BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Dramas y películas asiáticas subtituladas",                                     "streaming", 7,  "viki.com",           "https://www.viki.com/settings/subscription"),
            CatalogItem("FloSports",               BigDecimal("36.30"),  "EUR", BillingCycle.MONTHLY, "Deportes de nicho en streaming (lucha, atletismo…)",                            "streaming", 7,  "flosports.tv",       "https://www.flosports.tv/account")
        ),

        // ── Música ───────────────────────────────────────────────────────────────
        "musica" to listOf(
            CatalogItem("Spotify Premium",         BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Spotify — música y podcasts sin anuncios, escucha sin conexión",                "musica", domain = "spotify.com",    cancelUrl = "https://www.spotify.com/es/account/subscription/"),
            CatalogItem("Apple Music",             BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Apple Music — 100 millones de canciones, lossless y Dolby Atmos",               "musica", domain = "apple.com",      cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Tidal HiFi",              BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Tidal — audio lossless y HiRes FLAC para audiófilos",                           "musica", domain = "tidal.com",      cancelUrl = "https://account.tidal.com/subscription"),
            CatalogItem("Amazon Music Unlimited",  BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Amazon Music Unlimited — 100 millones de canciones en HD y Ultra HD",           "musica", domain = "amazon.com",     cancelUrl = "https://music.amazon.es/settings"),
            CatalogItem("YouTube Music Premium",   BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "YouTube Music — sin anuncios, descarga y reproducción en segundo plano",        "musica", domain = "youtube.com",    cancelUrl = "https://www.youtube.com/paid_memberships"),
            CatalogItem("Deezer Premium",          BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Deezer — música sin anuncios, modo offline y Flow personalizado",               "musica", domain = "deezer.com",     cancelUrl = "https://www.deezer.com/es/account"),
            CatalogItem("SoundCloud Next Pro",     BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "SoundCloud — escucha sin anuncios y subidas ilimitadas para creadores",         "musica", domain = "soundcloud.com", cancelUrl = "https://soundcloud.com/settings/subscription"),
            CatalogItem("Qobuz Sublime",           BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Qobuz Sublime — streaming lossless Hi-Res y descuentos en la tienda",           "musica", domain = "qobuz.com",      cancelUrl = "https://www.qobuz.com/es-es/account/subscriptions"),
            CatalogItem("Napster Premium",         BigDecimal("10.88"),  "EUR", BillingCycle.MONTHLY, "Napster Premium — música ilimitada sin anuncios y modo offline",                 "musica", domain = "napster.com",    cancelUrl = "https://account.napster.com/settings"),
            CatalogItem("Pandora Premium",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Radio y streaming de música personalizado",                                     "musica", 30, "pandora.com",     "https://www.pandora.com/account/subscription"),
            CatalogItem("Beatport Link",           BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Streaming de música electrónica para DJs",                                      "musica", 30, "beatport.com",    "https://www.beatport.com/account/subscription"),
            CatalogItem("LiveOne Premium",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Streaming de música y radio en vivo",                                           "musica", 7,  "livexlive.com",   "https://liveone.com/account"),
            CatalogItem("Mixcloud Select",         BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Acceso ilimitado a mixtapes y sets de DJ",                                      "musica", domain = "mixcloud.com",   cancelUrl = "https://www.mixcloud.com/settings/")
        ),

        // ── Software y productividad ─────────────────────────────────────────────
        "software" to listOf(
            CatalogItem("Microsoft 365 Personal",  BigDecimal("84.69"),  "EUR", BillingCycle.YEARLY,  "Microsoft 365 — Office + 1 TB OneDrive para 1 persona",                        "software", domain = "microsoft.com",      cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("Microsoft 365 Familiar",  BigDecimal("120.99"), "EUR", BillingCycle.YEARLY,  "Microsoft 365 — Office + 1 TB OneDrive para hasta 6 personas",                 "software", domain = "microsoft.com",      cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("Adobe Creative Cloud",    BigDecimal("72.59"),  "EUR", BillingCycle.MONTHLY, "Adobe CC — suite completa con Photoshop, Illustrator, Premiere y más",         "software", domain = "adobe.com",          cancelUrl = "https://account.adobe.com/plans"),
            CatalogItem("Adobe Photoshop",         BigDecimal("29.27"),  "EUR", BillingCycle.MONTHLY, "Adobe Photoshop — edición de imágenes profesional",                             "software", domain = "adobe.com",          cancelUrl = "https://account.adobe.com/plans"),
            CatalogItem("Adobe Acrobat Pro",       BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Adobe Acrobat Pro — creación y edición avanzada de PDF",                       "software", domain = "adobe.com",          cancelUrl = "https://account.adobe.com/plans"),
            CatalogItem("Notion Plus",             BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Notion Plus — bloques ilimitados, historial de 30 días",                        "software", domain = "notion.so",          cancelUrl = "https://www.notion.so/profile/plans"),
            CatalogItem("Figma Professional",      BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Figma — diseño colaborativo, proyectos ilimitados por editor",                  "software", domain = "figma.com",          cancelUrl = "https://www.figma.com/settings"),
            CatalogItem("Sketch",                  BigDecimal("10.88"),  "EUR", BillingCycle.MONTHLY, "Sketch — diseño de interfaces macOS, colaboración incluida",                    "software", domain = "sketch.com",         cancelUrl = "https://www.sketch.com/manage/"),
            CatalogItem("Todoist Pro",             BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Todoist — gestión de tareas con recordatorios y filtros avanzados",             "software", domain = "todoist.com",        cancelUrl = "https://app.todoist.com/app/settings/subscription"),
            CatalogItem("Evernote Personal",       BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Evernote — notas, documentos y tareas sincronizados",                           "software", domain = "evernote.com",       cancelUrl = "https://www.evernote.com/Settings.action"),
            CatalogItem("Obsidian Sync",           BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "Obsidian Sync — sincronización cifrada de bóvedas entre dispositivos",          "software", domain = "obsidian.md",        cancelUrl = "https://obsidian.md/account"),
            CatalogItem("Affinity Suite",          BigDecimal("20.56"),  "EUR", BillingCycle.MONTHLY, "Affinity — Photo, Designer y Publisher en un solo plan",                        "software", domain = "affinity.serif.com", cancelUrl = "https://affinity.serif.com/es/account/"),
            CatalogItem("Grammarly Premium",       BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Grammarly — corrección gramatical avanzada e inteligencia de escritura",        "software", domain = "grammarly.com",      cancelUrl = "https://account.grammarly.com/subscription"),
            CatalogItem("Duolingo Super",          BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Duolingo — aprendizaje de idiomas sin anuncios, con racha reparadora",          "software", domain = "duolingo.com",       cancelUrl = "https://www.duolingo.com/settings/super"),
            CatalogItem("Setapp",                  BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Setapp — más de 240 apps de Mac y iOS por una suscripción mensual",             "software", 7,  "setapp.com",          "https://setapp.com/manage"),
            CatalogItem("LinkedIn Premium",        BigDecimal("42.35"),  "EUR", BillingCycle.MONTHLY, "LinkedIn Premium — InMail, quién vio tu perfil y cursos LinkedIn Learning",     "software", 30, "linkedin.com",        "https://www.linkedin.com/premium/products/"),
            CatalogItem("Zoom Pro",                BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Zoom Pro — reuniones de hasta 30 horas con 100 participantes",                  "software", domain = "zoom.us",            cancelUrl = "https://zoom.us/account/billing"),
            CatalogItem("Slack Pro",               BigDecimal("9.07"),   "EUR", BillingCycle.MONTHLY, "Slack Pro — historial completo, videollamadas y apps ilimitadas",               "software", domain = "slack.com",          cancelUrl = "https://app.slack.com/plans"),
            CatalogItem("Trello Premium",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Trello Premium — vistas avanzadas, automatizaciones y dashboard ilimitado",     "software", domain = "trello.com",         cancelUrl = "https://trello.com/billing"),
            CatalogItem("Asana Starter",           BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Asana Starter — gestión de proyectos con línea de tiempo y automatizaciones",  "software", domain = "asana.com",          cancelUrl = "https://app.asana.com/-/account_api"),
            CatalogItem("Monday.com Basic",        BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Monday.com Basic — gestión visual de trabajo en equipo",                        "software", 14, "monday.com",          "https://monday.com/account-settings/billing"),
            CatalogItem("Airtable Plus",           BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Airtable Plus — base de datos visual con automatizaciones y extensiones",       "software", 14, "airtable.com",        "https://airtable.com/account"),
            CatalogItem("Zapier Starter",          BigDecimal("29.03"),  "EUR", BillingCycle.MONTHLY, "Zapier Starter — 750 tareas/mes con multi-step zaps y filtros",                 "software", 14, "zapier.com",          "https://zapier.com/app/settings/billing"),
            CatalogItem("Make Pro",                BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Make Pro — automatizaciones visuales avanzadas con 10 000 operaciones/mes",    "software", 30, "make.com",            "https://www.make.com/en/settings/billing"),
            CatalogItem("Loom Business",           BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Loom Business — grabación de pantalla y video asincrónico para equipos",       "software", 14, "loom.com",            "https://www.loom.com/settings/billing"),
            CatalogItem("Miro Team",               BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Miro Team — pizarra colaborativa infinita con plantillas avanzadas",            "software", 30, "miro.com",            "https://miro.com/app/settings/billing/"),
            CatalogItem("Webflow Basic",           BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Webflow Basic — diseño y publicación web sin código con CMS",                  "software", domain = "webflow.com",        cancelUrl = "https://webflow.com/dashboard/account/billing"),
            CatalogItem("Framer Mini",             BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Framer Mini — diseño web interactivo y publicación incluida",                   "software", 14, "framer.com",          "https://framer.com/settings/billing"),
            CatalogItem("Descript Creator",        BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Descript Creator — edición de vídeo y podcast basada en transcripción",        "software", 7,  "descript.com",        "https://www.descript.com/settings/billing"),
            CatalogItem("Bear Notes Pro",          BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "Editor de texto markdown para notas y escritura",                               "software", domain = "bear.app",           cancelUrl = "https://bear.app/faq/subscriptions"),
            CatalogItem("Craft Docs Pro",          BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Editor de documentos colaborativo con IA",                                      "software", domain = "craft.do",            cancelUrl = "https://www.craft.do/account"),
            CatalogItem("Day One Premium",         BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Diario personal con cifrado de extremo a extremo",                              "software", domain = "dayoneapp.com",       cancelUrl = "https://dayoneapp.com/account"),
            CatalogItem("Ulysses",                 BigDecimal("7.25"),   "EUR", BillingCycle.MONTHLY, "Editor de escritura creativa y long-form",                                      "software", 14, "ulysses.app",         "https://ulysses.app/support"),
            CatalogItem("PDF Expert Pro",          BigDecimal("8.47"),   "EUR", BillingCycle.MONTHLY, "Edición, anotación y firma de PDFs",                                            "software", 7,  "pdfexpert.com",       "https://pdfexpert.com/account"),
            CatalogItem("Fantastical Premium",     BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Calendario y tareas con IA y lenguaje natural",                                 "software", 14, "fantastical.app",     "https://flexibits.com/fantastical/support"),
            CatalogItem("Raycast Pro",             BigDecimal("10.89"),  "EUR", BillingCycle.MONTHLY, "Launcher y productividad avanzada para Mac",                                    "software", 14, "raycast.com",         "https://raycast.com/pricing"),
            CatalogItem("Pitch Pro",               BigDecimal("18.15"),  "EUR", BillingCycle.MONTHLY, "Presentaciones colaborativas modernas",                                         "software", domain = "pitch.com",           cancelUrl = "https://app.pitch.com/app/settings/billing"),
            CatalogItem("Jira Software Standard",  BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Gestión ágil de proyectos de software",                                         "software", 14, "atlassian.com",       "https://support.atlassian.com/billing-and-licensing/docs/cancel-your-subscription"),
            CatalogItem("Confluence Standard",     BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Wiki corporativa y gestión del conocimiento",                                   "software", 14, "atlassian.com",       "https://support.atlassian.com/billing-and-licensing/docs/cancel-your-subscription"),
            CatalogItem("CleanMyMac Business",     BigDecimal("14.52"),  "EUR", BillingCycle.MONTHLY, "Optimización y limpieza de Mac",                                                "software", 7,  "cleanmymac.com",      "https://account.macpaw.com"),
            CatalogItem("X Premium",               BigDecimal("14.52"),  "EUR", BillingCycle.MONTHLY, "Verificación, editar tweets y funciones extra en X",                            "software", domain = "x.com",              cancelUrl = "https://x.com/settings/account/subscription")
        ),

        // ── Almacenamiento en la nube ────────────────────────────────────────────
        "cloud" to listOf(
            CatalogItem("Google One 2 TB",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Google One — 2 TB compartidos entre Drive, Gmail y Fotos",                     "cloud", domain = "google.com",    cancelUrl = "https://one.google.com/about"),
            CatalogItem("Google One 5 TB",         BigDecimal("30.24"),  "EUR", BillingCycle.MONTHLY, "Google One — 5 TB compartidos entre Drive, Gmail y Fotos",                     "cloud", domain = "google.com",    cancelUrl = "https://one.google.com/about"),
            CatalogItem("iCloud+ 200 GB",          BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "iCloud+ — 200 GB para iPhone, iPad y Mac",                                      "cloud", domain = "icloud.com",    cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("iCloud+ 2 TB",            BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "iCloud+ — 2 TB con Private Relay y dominio de correo personalizado",            "cloud", domain = "icloud.com",    cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Dropbox Plus",            BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Dropbox Plus — 2 TB con sincronización inteligente",                            "cloud", domain = "dropbox.com",   cancelUrl = "https://www.dropbox.com/account/plan"),
            CatalogItem("OneDrive 100 GB",         BigDecimal("2.42"),   "EUR", BillingCycle.MONTHLY, "Microsoft OneDrive — 100 GB de almacenamiento independiente",                   "cloud", domain = "microsoft.com", cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("Backblaze Personal",      BigDecimal("10.27"),  "EUR", BillingCycle.MONTHLY, "Backblaze — copia de seguridad ilimitada del ordenador",                        "cloud", domain = "backblaze.com",  cancelUrl = "https://secure.backblaze.com/user_overview.htm"),
            CatalogItem("pCloud Premium 2 TB",     BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "pCloud — 2 TB de almacenamiento cifrado en Europa",                             "cloud", domain = "pcloud.com",    cancelUrl = "https://my.pcloud.com/#page=account&settings=subscription"),
            CatalogItem("DigitalOcean",            BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "DigitalOcean — servidores en la nube, Droplets y managed databases",             "cloud", domain = "digitalocean.com", cancelUrl = "https://cloud.digitalocean.com/account/billing"),
            CatalogItem("Cloudflare Pro",          BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Cloudflare Pro — CDN, WAF y optimización de rendimiento web",                   "cloud", domain = "cloudflare.com",  cancelUrl = "https://dash.cloudflare.com/profile/billing"),
            CatalogItem("Render Starter",          BigDecimal("7.26"),   "EUR", BillingCycle.MONTHLY, "Render Starter — despliegue de apps web con SSDs y builds automáticos",         "cloud", domain = "render.com",      cancelUrl = "https://dashboard.render.com/billing"),
            CatalogItem("Supabase Pro",            BigDecimal("27.22"),  "EUR", BillingCycle.MONTHLY, "Supabase Pro — base de datos Postgres, auth y storage gestionados",             "cloud", 14, "supabase.com",     "https://supabase.com/dashboard/account/billing"),
            CatalogItem("MongoDB Atlas",           BigDecimal("60.50"),  "EUR", BillingCycle.MONTHLY, "MongoDB Atlas — base de datos NoSQL en la nube totalmente gestionada",          "cloud", domain = "mongodb.com",     cancelUrl = "https://account.mongodb.com/account/billing/overview"),
            CatalogItem("Algolia Search",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Algolia — búsqueda como servicio con IA y analíticas avanzadas",                "cloud", domain = "algolia.com",     cancelUrl = "https://dashboard.algolia.com/account/billing"),
            CatalogItem("Mega Pro Lite",           BigDecimal("5.44"),   "EUR", BillingCycle.MONTHLY, "2TB cifrado de extremo a extremo en la nube",                                   "cloud", domain = "mega.io",          cancelUrl = "https://mega.io/cancel"),
            CatalogItem("Box Personal Pro",        BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Almacenamiento en la nube para profesionales",                                  "cloud", 14, "box.com",           "https://account.box.com/settings"),
            CatalogItem("Sync.com Solo Basic",     BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Almacenamiento seguro con cifrado zero-knowledge",                              "cloud", domain = "sync.com",         cancelUrl = "https://www.sync.com/account"),
            CatalogItem("Tresorit Personal",       BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Almacenamiento en la nube ultra-seguro y cifrado",                              "cloud", 14, "tresorit.com",      "https://app.tresorit.com/account"),
            CatalogItem("Icedrive Pro",            BigDecimal("5.44"),   "EUR", BillingCycle.MONTHLY, "Almacenamiento en la nube con cifrado cliente",                                 "cloud", domain = "icedrive.net",     cancelUrl = "https://icedrive.net/account")
        ),

        // ── Gaming ───────────────────────────────────────────────────────────────
        "gaming" to listOf(
            CatalogItem("Xbox Game Pass Ultimate", BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Xbox Game Pass Ultimate — juegos en PC, consola y la nube + EA Play",           "gaming", domain = "xbox.com",          cancelUrl = "https://account.microsoft.com/services"),
            CatalogItem("PlayStation Plus Extra",  BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "PS Plus Extra — biblioteca de 400+ juegos para PS5 y PS4",                     "gaming", domain = "playstation.com",   cancelUrl = "https://www.playstation.com/es-es/playstation-plus/"),
            CatalogItem("PlayStation Plus Essential",BigDecimal("10.88"), "EUR", BillingCycle.MONTHLY, "PS Plus Essential — juegos mensuales y multijugador online",                    "gaming", domain = "playstation.com",   cancelUrl = "https://www.playstation.com/es-es/playstation-plus/"),
            CatalogItem("PlayStation Plus Premium",BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "PS Plus Premium — biblioteca completa con juegos clásicos y streaming",         "gaming", domain = "playstation.com",   cancelUrl = "https://www.playstation.com/es-es/playstation-plus/"),
            CatalogItem("EA Play",                 BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "EA Play — acceso anticipado y biblioteca de juegos EA",                         "gaming", domain = "ea.com",            cancelUrl = "https://www.ea.com/ea-play"),
            CatalogItem("Nintendo Switch Online",  BigDecimal("24.19"),  "EUR", BillingCycle.YEARLY,  "Nintendo Switch Online — multijugador online y juegos NES/SNES/N64",            "gaming", domain = "nintendo.com",      cancelUrl = "https://accounts.nintendo.com/profile/about"),
            CatalogItem("Nintendo Switch Online+", BigDecimal("42.34"),  "EUR", BillingCycle.YEARLY,  "Nintendo Switch Online + Pack de expansión — Game Boy, N64 y DLC",              "gaming", domain = "nintendo.com",      cancelUrl = "https://accounts.nintendo.com/profile/about"),
            CatalogItem("Apple Arcade",            BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Apple Arcade — más de 200 juegos premium sin anuncios",                         "gaming", domain = "apple.com",         cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Humble Choice",           BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "Humble Choice — 9 juegos de PC al mes + descuentos en la tienda",               "gaming", domain = "humblebundle.com",  cancelUrl = "https://www.humblebundle.com/subscription"),
            CatalogItem("GeForce Now Priority",    BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "GeForce Now Priority — streaming de juegos en la nube con RTX",                 "gaming", 30, "nvidia.com",         "https://www.nvidia.com/es-es/geforce-now/account/"),
            CatalogItem("Shadow PC",               BigDecimal("36.29"),  "EUR", BillingCycle.MONTHLY, "Shadow PC — PC gaming en la nube de alta gama accesible desde cualquier lugar", "gaming", domain = "shadow.tech",      cancelUrl = "https://account.shadow.tech/billing"),
            CatalogItem("Discord Nitro",           BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Emojis custom, boosts de servidor y más en Discord",                            "gaming", domain = "discord.com",       cancelUrl = "https://discord.com/settings/subscriptions"),
            CatalogItem("Discord Nitro Basic",     BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "Emojis animados y mayor límite de archivos en Discord",                         "gaming", domain = "discord.com",       cancelUrl = "https://discord.com/settings/subscriptions"),
            CatalogItem("Ubisoft+ Essential",      BigDecimal("17.99"),  "EUR", BillingCycle.MONTHLY, "Catálogo de juegos Ubisoft en PC",                                              "gaming", 7,  "ubisoftplus.com",    "https://ubisoftconnect.com/en-US/ubisoftplus/subscription"),
            CatalogItem("Roblox Premium 1000",     BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "1000 Robux/mes y ventajas de trading en Roblox",                                "gaming", domain = "roblox.com",        cancelUrl = "https://www.roblox.com/account/membership"),
            CatalogItem("Xbox PC Game Pass",       BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Catálogo de juegos para PC + EA Play incluido",                                 "gaming", 30, "xbox.com",           "https://account.microsoft.com/services")
        ),

        // ── Seguridad y privacidad ───────────────────────────────────────────────
        "seguridad" to listOf(
            CatalogItem("NordVPN",                 BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "NordVPN — VPN con 6 000+ servidores y bloqueador de anuncios",                  "seguridad", domain = "nordvpn.com",     cancelUrl = "https://my.nordaccount.com/dashboard/nordvpn/"),
            CatalogItem("ExpressVPN",              BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "ExpressVPN — VPN rápida con servidores en 105 países",                          "seguridad", domain = "expressvpn.com",  cancelUrl = "https://www.expressvpn.com/subscriptions"),
            CatalogItem("Mullvad VPN",             BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Mullvad — VPN centrada en la privacidad, sin cuenta ni logs",                   "seguridad", domain = "mullvad.net",     cancelUrl = "https://mullvad.net/account"),
            CatalogItem("ProtonVPN Plus",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Proton VPN Plus — VPN de código abierto con Tor y alta velocidad",              "seguridad", domain = "proton.me",       cancelUrl = "https://account.proton.me/dashboard"),
            CatalogItem("1Password",               BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "1Password — gestor de contraseñas con bóvedas ilimitadas",                      "seguridad", domain = "1password.com",   cancelUrl = "https://my.1password.com/profile"),
            CatalogItem("Bitwarden Premium",       BigDecimal("11.48"),  "EUR", BillingCycle.YEARLY,  "Bitwarden — gestor de contraseñas open-source, plan Premium anual",              "seguridad", domain = "bitwarden.com",   cancelUrl = "https://vault.bitwarden.com/#/settings/subscription"),
            CatalogItem("Dashlane Premium",        BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Dashlane — gestor de contraseñas con VPN incluida",                             "seguridad", domain = "dashlane.com",    cancelUrl = "https://app.dashlane.com/settings/account-information"),
            CatalogItem("Malwarebytes Premium",    BigDecimal("4.22"),   "EUR", BillingCycle.MONTHLY, "Malwarebytes — antimalware en tiempo real para PC y Mac",                       "seguridad", domain = "malwarebytes.com", cancelUrl = "https://my.malwarebytes.com/account"),
            CatalogItem("ProtonMail Plus",         BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "Proton Mail — correo cifrado de extremo a extremo",                             "seguridad", domain = "proton.me",       cancelUrl = "https://account.proton.me/dashboard"),
            CatalogItem("Surfshark Starter",       BigDecimal("3.01"),   "EUR", BillingCycle.MONTHLY, "Surfshark — VPN con dispositivos ilimitados y bloqueador de anuncios",          "seguridad", domain = "surfshark.com",   cancelUrl = "https://my.surfshark.com/vpn/manual-setup/main/users"),
            CatalogItem("Keeper Security",         BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Keeper Security — gestor de contraseñas con bóveda segura y auditoría",         "seguridad", 30, "keepersecurity.com", "https://keepersecurity.com/vault/#account"),
            CatalogItem("Fastmail",                BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Fastmail — correo privado sin anuncios con dominio personalizado",               "seguridad", 30, "fastmail.com",       "https://www.fastmail.com/settings/billing/"),
            CatalogItem("Tutanota Premium",        BigDecimal("3.63"),   "EUR", BillingCycle.MONTHLY, "Tutanota Premium — correo cifrado de extremo a extremo y calendario privado",   "seguridad", domain = "tutanota.com",    cancelUrl = "https://mail.tutanota.com/#settings/subscription"),
            CatalogItem("ProtonDrive Plus",        BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Proton Drive Plus — almacenamiento cifrado en la nube con 200 GB",              "seguridad", domain = "proton.me",       cancelUrl = "https://account.proton.me/dashboard"),
            CatalogItem("NordPass Premium",        BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "Gestor de contraseñas del equipo de NordVPN",                                   "seguridad", 30, "nordpass.com",       "https://nordpass.com/account"),
            CatalogItem("Norton 360 Standard",     BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Antivirus, VPN y backup en la nube para 1 equipo",                              "seguridad", 7,  "norton.com",         "https://my.norton.com/account/cancel"),
            CatalogItem("CyberGhost VPN",          BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "VPN rápida con 9000+ servidores en 90 países",                                  "seguridad", 45, "cyberghostvpn.com",  "https://my.cyberghostvpn.com/account"),
            CatalogItem("Private Internet Access", BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "VPN de código abierto, sin registros verificado",                               "seguridad", 30, "privateinternetaccess.com", "https://www.privateinternetaccess.com/account/cancel"),
            CatalogItem("AdGuard Premium",         BigDecimal("3.01"),   "EUR", BillingCycle.MONTHLY, "Bloqueo de anuncios y rastreadores en todos los dispositivos",                  "seguridad", domain = "adguard.com",     cancelUrl = "https://my.adguard.com"),
            CatalogItem("Proton Pass Plus",        BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Gestor de contraseñas del ecosistema Proton",                                   "seguridad", domain = "proton.me",       cancelUrl = "https://account.proton.me/settings#subscription"),
            CatalogItem("RoboForm Premium",        BigDecimal("2.42"),   "EUR", BillingCycle.MONTHLY, "Gestor de contraseñas con autocompletado web",                                  "seguridad", 30, "roboform.com",       "https://my.roboform.com")
        ),

        // ── Noticias y Lectura ───────────────────────────────────────────────────
        "noticias" to listOf(
            CatalogItem("Kindle Unlimited",        BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Kindle Unlimited — más de 4 millones de libros digitales",                     "noticias", domain = "amazon.com",   cancelUrl = "https://www.amazon.es/kindle-dbs/hz/subscribe/ku"),
            CatalogItem("Readwise Reader",         BigDecimal("9.06"),   "EUR", BillingCycle.MONTHLY, "Readwise — lectura, subrayado y repaso de libros y artículos",                  "noticias", domain = "readwise.io",  cancelUrl = "https://readwise.io/settings"),
            CatalogItem("Scribd",                  BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Scribd — libros, audiolibros, revistas y documentos ilimitados",                "noticias", domain = "scribd.com",   cancelUrl = "https://www.scribd.com/account-settings/payments"),
            CatalogItem("El País Digital",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "El País — acceso digital ilimitado con archivo histórico",                      "noticias", domain = "elpais.com",   cancelUrl = "https://elpais.com/suscripciones/gestion.html"),
            CatalogItem("The New York Times",      BigDecimal("19.35"),  "EUR", BillingCycle.MONTHLY, "NYT — acceso completo a noticias, juegos, Cooking y Wirecutter",               "noticias", domain = "nytimes.com",  cancelUrl = "https://www.nytimes.com/subscription/cancel-subscription"),
            CatalogItem("Blinkist Premium",        BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "Blinkist — resúmenes de libros de no ficción en 15 minutos",                   "noticias", domain = "blinkist.com", cancelUrl = "https://www.blinkist.com/app/account/subscription"),
            CatalogItem("Audible",                 BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Audible — 1 audiolibro al mes + acceso a catálogo Plus",                       "noticias", domain = "audible.com",  cancelUrl = "https://www.audible.es/account/memberships"),
            CatalogItem("Medium",                  BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Medium — artículos y ensayos de miles de escritores independientes",             "noticias", 30, "medium.com",    "https://medium.com/me/membership"),
            CatalogItem("Pocket Premium",          BigDecimal("5.45"),   "EUR", BillingCycle.MONTHLY, "Pocket Premium — guarda y lee artículos sin conexión con búsqueda avanzada",    "noticias", domain = "getpocket.com", cancelUrl = "https://getpocket.com/premium/manage"),
            CatalogItem("Feedly Pro",              BigDecimal("9.06"),   "EUR", BillingCycle.MONTHLY, "Feedly Pro — agregador de noticias con IA y flujos sin límite",                 "noticias", 14, "feedly.com",    "https://feedly.com/i/subscription/feedly.pro"),
            CatalogItem("Inoreader Pro",           BigDecimal("9.06"),   "EUR", BillingCycle.MONTHLY, "Inoreader Pro — lector RSS con automatizaciones, filtros y búsqueda completa",  "noticias", domain = "inoreader.com", cancelUrl = "https://www.inoreader.com/account/subscription"),
            CatalogItem("Overcast Premium",        BigDecimal("12.09"),  "EUR", BillingCycle.YEARLY,  "Overcast Premium — el mejor cliente de podcasts para iPhone",                   "noticias", domain = "overcast.fm",  cancelUrl = "https://overcast.fm/account"),
            CatalogItem("The Wall Street Journal", BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Noticias financieras y de negocios globales",                                   "noticias", domain = "wsj.com",      cancelUrl = "https://customercenter.wsj.com"),
            CatalogItem("Financial Times",         BigDecimal("48.37"),  "EUR", BillingCycle.MONTHLY, "Noticias económicas y financieras internacionales",                             "noticias", 4,  "ft.com",        "https://help.ft.com/faq/subscribing-to-ft"),
            CatalogItem("The Economist",           BigDecimal("26.62"),  "EUR", BillingCycle.MONTHLY, "Análisis político y económico global semanal",                                  "noticias", domain = "economist.com", cancelUrl = "https://myaccount.economist.com"),
            CatalogItem("La Vanguardia Premium",   BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "Diario digital catalán con contenido exclusivo",                                "noticias", domain = "lavanguardia.com", cancelUrl = "https://suscripcion.lavanguardia.com"),
            CatalogItem("The Athletic",            BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Periodismo deportivo en profundidad",                                           "noticias", 7,  "theathletic.com", "https://theathletic.com/account/subscription"),
            CatalogItem("El Confidencial Premium", BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "Noticias y reportajes de investigación en España",                              "noticias", domain = "elconfidencial.com", cancelUrl = "https://suscripcion.elconfidencial.com"),
            CatalogItem("Le Monde Numérique",      BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Periódico de referencia francés con archivo completo",                          "noticias", domain = "lemonde.fr",   cancelUrl = "https://abo.lemonde.fr"),
            CatalogItem("Caixin Global",           BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Noticias económicas y financieras de China",                                    "noticias", domain = "caixinglobal.com", cancelUrl = "https://www.caixinglobal.com/account")
        ),

        // ── Salud y Deporte ──────────────────────────────────────────────────────
        "salud" to listOf(
            CatalogItem("Strava Summit",           BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Strava — análisis avanzado de entrenamiento, segmentos y rutas",                "salud", domain = "strava.com",        cancelUrl = "https://www.strava.com/settings/subscription"),
            CatalogItem("Whoop",                   BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "Whoop — monitor de salud 24/7: recuperación, sueño y esfuerzo",                "salud", domain = "whoop.com",         cancelUrl = "https://www.whoop.com/us/en/thelocker/cancel-whoop-membership/"),
            CatalogItem("Garmin Connect+",         BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Garmin Connect+ — estadísticas avanzadas y planes de entrenamiento",            "salud", domain = "garmin.com",        cancelUrl = "https://connect.garmin.com/modern/settings"),
            CatalogItem("MyFitnessPal Premium",    BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "MyFitnessPal — seguimiento nutricional avanzado con escáner de alimentos",     "salud", domain = "myfitnesspal.com",   cancelUrl = "https://www.myfitnesspal.com/account/my-subscription"),
            CatalogItem("Calm Premium",            BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Calm — meditación, sueño y reducción del estrés",                               "salud", domain = "calm.com",          cancelUrl = "https://www.calm.com/app/account"),
            CatalogItem("Headspace",               BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Headspace — meditación guiada y cursos de bienestar mental",                    "salud", domain = "headspace.com",     cancelUrl = "https://www.headspace.com/account"),
            CatalogItem("Noom",                    BigDecimal("66.54"),  "EUR", BillingCycle.MONTHLY, "Noom — programa de pérdida de peso basado en psicología conductual",            "salud", domain = "noom.com",          cancelUrl = "https://web.noom.com/settings"),
            CatalogItem("Apple Fitness+",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Apple Fitness+ — clases de entrenamiento con métricas de Apple Watch",          "salud", domain = "apple.com",         cancelUrl = "https://appleid.apple.com/account/manage"),
            CatalogItem("Peloton App",             BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Peloton App — clases de ciclismo, yoga, fuerza y meditación en streaming",      "salud", 30, "onepeloton.com",     "https://members.onepeloton.co.uk/profile/preferences"),
            CatalogItem("Oura Ring Membership",    BigDecimal("7.26"),   "EUR", BillingCycle.MONTHLY, "Oura Ring Membership — datos avanzados de sueño y recuperación del anillo",     "salud", domain = "ouraring.com",      cancelUrl = "https://cloud.ouraring.com/user/settings/membership"),
            CatalogItem("Sleep Cycle Premium",     BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Sleep Cycle Premium — análisis avanzado del sueño y alarma inteligente",        "salud", 7,  "sleepcycle.com",     "https://www.sleepcycle.com/account/"),
            CatalogItem("Runkeeper Go",            BigDecimal("12.09"),  "EUR", BillingCycle.YEARLY,  "Runkeeper Go — planes de entrenamiento personalizados y estadísticas avanzadas", "salud", domain = "runkeeper.com",     cancelUrl = "https://runkeeper.com/user/settings/premium"),
            CatalogItem("Fitbit Premium",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Análisis avanzado de salud y sueño con Fitbit",                                 "salud", 90, "fitbit.com",         "https://www.fitbit.com/settings/billing"),
            CatalogItem("Freeletics Coach",        BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "Entrenamientos personalizados con IA sin equipamiento",                         "salud", domain = "freeletics.com",    cancelUrl = "https://www.freeletics.com/en/account/subscriptions"),
            CatalogItem("Flo Premium",             BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Seguimiento de ciclo menstrual y salud femenina",                               "salud", 7,  "flo.health",         "https://flo.health/account"),
            CatalogItem("Zwift",                   BigDecimal("20.57"),  "EUR", BillingCycle.MONTHLY, "Ciclismo y running indoor con mundo virtual",                                   "salud", 14, "zwift.com",          "https://www.zwift.com/cancel"),
            CatalogItem("TrainerRoad",             BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Entrenamiento de ciclismo basado en datos y zonas",                             "salud", 30, "trainerroad.com",    "https://www.trainerroad.com/account"),
            CatalogItem("Sweat App",               BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Programas de fitness para mujeres (BBG, LIFT, BREATHE)",                        "salud", 7,  "sweat.com",          "https://www.sweat.com/account/membership"),
            CatalogItem("Centr Premium",           BigDecimal("36.29"),  "EUR", BillingCycle.MONTHLY, "App de fitness de Chris Hemsworth — gym y mindfulness",                         "salud", 7,  "centr.com",          "https://centr.com/account/subscription")
        ),

        // ── Pruebas gratuitas ────────────────────────────────────────────────────
        "prueba" to listOf(
            CatalogItem("Netflix Estándar",        BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD, sin anuncios",                                   "prueba", 30, "netflix.com",    "https://www.netflix.com/cancelplan"),
            CatalogItem("Spotify Premium",         BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Spotify — música y podcasts sin anuncios, escucha sin conexión",                "prueba", 30, "spotify.com",    "https://www.spotify.com/es/account/subscription/"),
            CatalogItem("Amazon Prime",            BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Amazon Prime — envíos gratis + Prime Video + Prime Music",                      "prueba", 30, "amazon.com",     "https://www.amazon.es/mc/pipelines/cancellation"),
            CatalogItem("Disney+",                 BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Disney+ — Disney, Marvel, Star Wars y National Geographic",                     "prueba", 7,  "disneyplus.com", "https://www.disneyplus.com/account"),
            CatalogItem("Apple TV+",               BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Apple TV+ — series y películas originales Apple",                               "prueba", 7,  "apple.com",      "https://appleid.apple.com/account/manage"),
            CatalogItem("Cursor Pro",              BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Editor de código con IA — plan Pro mensual",                                    "prueba", 14, "cursor.com",     "https://www.cursor.com/settings"),
            CatalogItem("Adobe Creative Cloud",    BigDecimal("72.59"),  "EUR", BillingCycle.MONTHLY, "Adobe CC — suite completa con Photoshop, Illustrator, Premiere y más",         "prueba", 7,  "adobe.com",      "https://account.adobe.com/plans"),
            CatalogItem("YouTube Premium",         BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "YouTube sin anuncios, reproducción en segundo plano y YouTube Music",           "prueba", 30, "youtube.com",    "https://www.youtube.com/paid_memberships"),
            CatalogItem("Notion Plus",             BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Notion Plus — bloques ilimitados, historial de 30 días",                        "prueba", 14, "notion.so",      "https://www.notion.so/profile/plans"),
            CatalogItem("Duolingo Super",          BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Duolingo — aprendizaje de idiomas sin anuncios, con racha reparadora",          "prueba", 14, "duolingo.com",   "https://www.duolingo.com/settings/super"),
            CatalogItem("NordVPN",                 BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "NordVPN — VPN con 6 000+ servidores y bloqueador de anuncios",                  "prueba", 30, "nordvpn.com",    "https://my.nordaccount.com/dashboard/nordvpn/"),
            CatalogItem("Grammarly Premium",       BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Grammarly — corrección gramatical avanzada e inteligencia de escritura",        "prueba", 7,  "grammarly.com",  "https://account.grammarly.com/subscription"),
            CatalogItem("Apple One",               BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Apple One Individual — Apple TV+, Music, Arcade, iCloud+ en un plan",           "prueba", 30, "apple.com",      "https://appleid.apple.com/account/manage"),
            CatalogItem("LinkedIn Premium",        BigDecimal("42.35"),  "EUR", BillingCycle.MONTHLY, "LinkedIn Premium — InMail, quién vio tu perfil y cursos LinkedIn Learning",     "prueba", 30, "linkedin.com",   "https://www.linkedin.com/premium/products/"),
            CatalogItem("Discovery+",              BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Discovery+ — documentales, reality shows y contenido de Discovery",              "prueba", 7,  "discoveryplus.com", "https://www.discoveryplus.com/es/account"),
            CatalogItem("GeForce Now Priority",    BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "GeForce Now Priority — streaming de juegos en la nube con RTX",                 "prueba", 30, "nvidia.com",     "https://www.nvidia.com/es-es/geforce-now/account/"),
            CatalogItem("Keeper Security",         BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Keeper Security — gestor de contraseñas con bóveda segura y auditoría",         "prueba", 30, "keepersecurity.com", "https://keepersecurity.com/vault/#account"),
            CatalogItem("Medium",                  BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Medium — artículos y ensayos de miles de escritores independientes",             "prueba", 30, "medium.com",     "https://medium.com/me/membership")
        ),

        // ── Desarrollo y DevOps ──────────────────────────────────────────────────
        "desarrollo" to listOf(
            CatalogItem("GitHub Pro",              BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "GitHub — repositorios privados ilimitados, wikis y Pages avanzadas",            "desarrollo", domain = "github.com",    cancelUrl = "https://github.com/settings/billing"),
            CatalogItem("GitHub Team",             BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "GitHub Team — revisiones de código, protección de ramas y CODEOWNERS",         "desarrollo", domain = "github.com",    cancelUrl = "https://github.com/settings/billing"),
            CatalogItem("GitLab Premium",          BigDecimal("32.66"),  "EUR", BillingCycle.MONTHLY, "GitLab Premium — CI/CD avanzado, revisiones de código y seguridad",             "desarrollo", domain = "gitlab.com",    cancelUrl = "https://gitlab.com/-/profile/billing"),
            CatalogItem("Vercel Pro",              BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Vercel Pro — despliegue continuo con ancho de banda y builds ampliados",        "desarrollo", domain = "vercel.com",    cancelUrl = "https://vercel.com/account/billing"),
            CatalogItem("Netlify Pro",             BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Netlify — despliegue de sitios estáticos con funciones serverless",              "desarrollo", domain = "netlify.com",   cancelUrl = "https://app.netlify.com/user/billing"),
            CatalogItem("Railway Hobby",           BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Railway — despliegue de aplicaciones con crédito mensual incluido",             "desarrollo", domain = "railway.app",   cancelUrl = "https://railway.app/account/billing"),
            CatalogItem("Sentry Team",             BigDecimal("29.03"),  "EUR", BillingCycle.MONTHLY, "Sentry — monitorización de errores y rendimiento",                              "desarrollo", domain = "sentry.io",     cancelUrl = "https://sentry.io/settings/billing/overview/"),
            CatalogItem("Linear",                  BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Linear — gestión de proyectos de software para equipos",                        "desarrollo", domain = "linear.app",    cancelUrl = "https://linear.app/settings/billing"),
            CatalogItem("JetBrains All Products",  BigDecimal("34.97"),  "EUR", BillingCycle.MONTHLY, "JetBrains — todos los IDEs: IntelliJ, WebStorm, PyCharm y más",                 "desarrollo", domain = "jetbrains.com", cancelUrl = "https://account.jetbrains.com/licenses"),
            CatalogItem("Postman Basic",           BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "Postman — plataforma de API con colecciones privadas y colaboración",           "desarrollo", domain = "postman.com",   cancelUrl = "https://go.postman.co/settings/billing"),
            CatalogItem("Datadog Pro",             BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Datadog — monitorización de infraestructura y aplicaciones",                    "desarrollo", domain = "datadoghq.com", cancelUrl = "https://app.datadoghq.com/account/billing"),
            CatalogItem("New Relic",               BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "New Relic — observabilidad full-stack con trazas, logs y métricas",              "desarrollo", domain = "newrelic.com",  cancelUrl = "https://one.newrelic.com/admin-portal/billing-billing/home"),
            CatalogItem("Grafana Cloud Pro",       BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Grafana Cloud Pro — dashboards, alertas y trazas distribuidas",                  "desarrollo", domain = "grafana.com",   cancelUrl = "https://grafana.com/orgs/account/billing"),
            CatalogItem("PagerDuty",               BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "PagerDuty — gestión de incidentes y alertas on-call para equipos",              "desarrollo", domain = "pagerduty.com", cancelUrl = "https://account.pagerduty.com/account/settings"),
            CatalogItem("Pingdom",                 BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Pingdom — monitorización de disponibilidad y rendimiento web",                   "desarrollo", domain = "pingdom.com",   cancelUrl = "https://my.pingdom.com/account/plans"),
            CatalogItem("Fly.io",                  BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Fly.io — despliegue de apps cerca de tus usuarios en la edge",                  "desarrollo", domain = "fly.io",        cancelUrl = "https://fly.io/dashboard/billing"),
            CatalogItem("Neon Postgres",           BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Neon Postgres — base de datos Postgres serverless con branching",                "desarrollo", domain = "neon.tech",     cancelUrl = "https://console.neon.tech/app/settings/billing"),
            CatalogItem("Upstash",                 BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Upstash — Redis y Kafka serverless de pago por uso",                             "desarrollo", domain = "upstash.com",   cancelUrl = "https://console.upstash.com/account/billing"),
            CatalogItem("Tabnine Pro",             BigDecimal("14.52"),  "EUR", BillingCycle.MONTHLY, "Autocompletado de código con IA (privacidad garantizada)",                      "desarrollo", 14, "tabnine.com",    "https://app.tabnine.com/settings/subscription"),
            CatalogItem("Plausible Analytics",     BigDecimal("10.89"),  "EUR", BillingCycle.MONTHLY, "Analítica web ligera y respetuosa con la privacidad",                           "desarrollo", 30, "plausible.io",   "https://plausible.io/settings"),
            CatalogItem("Fathom Analytics",        BigDecimal("16.94"),  "EUR", BillingCycle.MONTHLY, "Analytics web sin cookies, compliant con GDPR",                                 "desarrollo", 30, "usefathom.com",  "https://app.usefathom.com/settings/subscription"),
            CatalogItem("Heroku Eco",              BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Dynos económicos para apps en producción en Heroku",                            "desarrollo", domain = "heroku.com",    cancelUrl = "https://dashboard.heroku.com/account/billing"),
            CatalogItem("Warp AI",                 BigDecimal("18.15"),  "EUR", BillingCycle.MONTHLY, "Terminal con IA integrada y autocompletado de comandos",                        "desarrollo", 14, "warp.dev",       "https://app.warp.dev/account"),
            CatalogItem("Retool Cloud",            BigDecimal("12.10"),  "EUR", BillingCycle.MONTHLY, "Plataforma de apps internas con bajo código",                                   "desarrollo", 14, "retool.com",     "https://docs.retool.com/docs/cancel"),
            CatalogItem("Pieces for Developers",   BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Gestión de snippets de código con IA local",                                    "desarrollo", domain = "pieces.app",    cancelUrl = "https://pieces.app/account"),
            CatalogItem("Codeium Teams",           BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Autocompletado y chat IA para equipos",                                         "desarrollo", domain = "codeium.com",   cancelUrl = "https://codeium.com/account")
        ),

        // ── Finanzas personales ──────────────────────────────────────────────────
        "finanzas" to listOf(
            CatalogItem("YNAB",                    BigDecimal("14.52"),  "EUR", BillingCycle.MONTHLY, "Presupuesto personal con método zero-based",                                    "finanzas", 34, "ynab.com",           "https://app.youneedabudget.com/settings/subscription"),
            CatalogItem("Copilot Money",           BigDecimal("13.31"),  "EUR", BillingCycle.MONTHLY, "App de finanzas personales impulsada por IA",                                   "finanzas", 14, "copilot.money",      "https://copilot.money/account"),
            CatalogItem("Monarch Money",           BigDecimal("15.14"),  "EUR", BillingCycle.MONTHLY, "Gestión financiera familiar todo en uno",                                        "finanzas", 30, "monarchmoney.com",   "https://app.monarchmoney.com/settings/subscription"),
            CatalogItem("Revolut Premium",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Cuenta bancaria digital con seguro de viaje y cashback",                        "finanzas", domain = "revolut.com",      cancelUrl = "https://app.revolut.com/subscription"),
            CatalogItem("Revolut Metal",           BigDecimal("19.35"),  "EUR", BillingCycle.MONTHLY, "Plan Revolut premium con tarjeta de metal y concierge",                         "finanzas", domain = "revolut.com",      cancelUrl = "https://app.revolut.com/subscription"),
            CatalogItem("N26 Smart",               BigDecimal("5.93"),   "EUR", BillingCycle.MONTHLY, "Cuenta bancaria digital con hasta 10 subcuentas",                               "finanzas", domain = "n26.com",          cancelUrl = "https://n26.com/en-es/membership"),
            CatalogItem("N26 You",                 BigDecimal("11.98"),  "EUR", BillingCycle.MONTHLY, "Cuenta N26 con seguros de viaje y asistencia en viaje",                         "finanzas", domain = "n26.com",          cancelUrl = "https://n26.com/en-es/membership"),
            CatalogItem("Vivid Standard",          BigDecimal("11.98"),  "EUR", BillingCycle.MONTHLY, "Cuenta bancaria digital con cashback en cripto",                                "finanzas", 30, "vivid.money",        "https://vivid.money/account"),
            CatalogItem("Wallet Pro",              BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Control de gastos con sincronización bancaria automática",                      "finanzas", 30, "budgetbakers.com",   "https://budgetbakers.com/account"),
            CatalogItem("Toshl Finance Pro",       BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Registro de gastos e ingresos con visualizaciones",                             "finanzas", 30, "toshl.com",          "https://toshl.com/account"),
            CatalogItem("PocketGuard Plus",        BigDecimal("13.28"),  "EUR", BillingCycle.MONTHLY, "Presupuesto simplificado — cuánto puedo gastar hoy",                            "finanzas", 7,  "pocketguard.com",    "https://pocketguard.com/support"),
            CatalogItem("Spendee Premium",         BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "App de presupuesto compartido para familias y parejas",                         "finanzas", 7,  "spendee.com",        "https://spendee.com/support")
        ),

        // ── Educación y cursos ───────────────────────────────────────────────────
        "educacion" to listOf(
            CatalogItem("Coursera Plus",           BigDecimal("54.39"),  "EUR", BillingCycle.MONTHLY, "Acceso ilimitado a +7000 cursos y certificaciones",                             "educacion", 7,  "coursera.org",       "https://www.coursera.org/account-profile#cancellation"),
            CatalogItem("MasterClass",             BigDecimal("12.10"),  "EUR", BillingCycle.MONTHLY, "Clases magistrales de expertos mundiales en su campo",                          "educacion", 30, "masterclass.com",    "https://www.masterclass.com/account/membership"),
            CatalogItem("Skillshare Premium",      BigDecimal("14.52"),  "EUR", BillingCycle.MONTHLY, "Clases de diseño, negocios, tecnología y más",                                  "educacion", 7,  "skillshare.com",     "https://www.skillshare.com/en/profile/account"),
            CatalogItem("Brilliant Premium",       BigDecimal("25.66"),  "EUR", BillingCycle.MONTHLY, "Matemáticas, ciencia y programación de forma interactiva",                      "educacion", 7,  "brilliant.org",      "https://brilliant.org/account"),
            CatalogItem("Babbel Premium",          BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Aprendizaje de idiomas con lecciones breves de 15 min",                         "educacion", domain = "babbel.com",       cancelUrl = "https://account.babbel.com/en/account"),
            CatalogItem("Rosetta Stone",           BigDecimal("12.28"),  "EUR", BillingCycle.MONTHLY, "Aprendizaje de idiomas con inmersión y reconocimiento de voz",                  "educacion", 3,  "rosettastone.com",   "https://support.rosettastone.com/hc/en-us/articles/204765966"),
            CatalogItem("Domestika Plus",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Acceso ilimitado a cursos de diseño y creatividad",                             "educacion", domain = "domestika.org",    cancelUrl = "https://www.domestika.org/en/settings/subscription"),
            CatalogItem("Platzi Expert",           BigDecimal("29.71"),  "EUR", BillingCycle.MONTHLY, "Cursos de tecnología y programación en español",                                "educacion", domain = "platzi.com",       cancelUrl = "https://platzi.com/account/subscriptions"),
            CatalogItem("DataCamp Standard",       BigDecimal("25.63"),  "EUR", BillingCycle.MONTHLY, "Aprende ciencia de datos y Python/R de forma práctica",                         "educacion", domain = "datacamp.com",     cancelUrl = "https://www.datacamp.com/profile/account_settings"),
            CatalogItem("Frontend Masters",        BigDecimal("39.96"),  "EUR", BillingCycle.MONTHLY, "Cursos avanzados de desarrollo web y JavaScript",                               "educacion", domain = "frontendmasters.com", cancelUrl = "https://frontendmasters.com/settings/subscription"),
            CatalogItem("Udemy Personal Plan",     BigDecimal("19.99"),  "EUR", BillingCycle.MONTHLY, "Acceso a cursos seleccionados del catálogo Udemy",                              "educacion", domain = "udemy.com",        cancelUrl = "https://www.udemy.com/user/subscription"),
            CatalogItem("Preply Business",         BigDecimal("90.75"),  "EUR", BillingCycle.MONTHLY, "Clases particulares de idiomas con tutores nativos",                            "educacion", domain = "preply.com",       cancelUrl = "https://preply.com/en/account/settings")
        ),

        // ── Creatividad y foto ───────────────────────────────────────────────────
        "creatividad" to listOf(
            CatalogItem("VSCO Membership",         BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Edición de fotos con presets y comunidad creativa",                             "creatividad", 7,  "vsco.co",           "https://vsco.co/user/settings"),
            CatalogItem("Adobe Lightroom Plan",    BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Edición y organización profesional de fotografía",                              "creatividad", 30, "lightroom.adobe.com","https://account.adobe.com/plans"),
            CatalogItem("Facetune Premium",        BigDecimal("7.25"),   "EUR", BillingCycle.MONTHLY, "Retoque de selfies y fotos de retrato con IA",                                  "creatividad", domain = "facetuneapp.com",   cancelUrl = "https://support.facetuneapp.com"),
            CatalogItem("CapCut Pro",              BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Edición de vídeo profesional con efectos y plantillas",                         "creatividad", 7,  "capcut.com",        "https://www.capcut.com/settings"),
            CatalogItem("InShot Pro",              BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Editor de vídeo y foto para redes sociales",                                    "creatividad", 7,  "inshot.com",        "https://help.inshot.com"),
            CatalogItem("Adobe Premiere Rush",     BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Edición de vídeo multi-plataforma para creadores",                              "creatividad", 30, "adobe.com",         "https://account.adobe.com/plans"),
            CatalogItem("Splice Premium",          BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Editor de vídeo para móvil con librería de sonidos",                            "creatividad", 7,  "splice.com",        "https://splice.com/account"),
            CatalogItem("Unfold Pro",              BigDecimal("1.33"),   "EUR", BillingCycle.MONTHLY, "Plantillas premium para historias de Instagram y TikTok",                       "creatividad", 7,  "unfold.com",        "https://unfold.com/pages/faq"),
            CatalogItem("Afterlight",              BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "Edición de fotos artística con filtros únicos",                                 "creatividad", domain = "afterlight.us",     cancelUrl = "https://afterlight.us/support")
        ),

        // ── Citas y social ───────────────────────────────────────────────────────
        "citas" to listOf(
            CatalogItem("Tinder Gold",             BigDecimal("36.29"),  "EUR", BillingCycle.MONTHLY, "Me gustas, rewind y passports para ligar en Tinder",                            "citas", domain = "tinder.com",    cancelUrl = "https://www.help.tinder.com/hc/en-us/articles/115005376946"),
            CatalogItem("Tinder Platinum",         BigDecimal("48.39"),  "EUR", BillingCycle.MONTHLY, "Todo Gold más prioridad en Likes y mensajes a matches",                         "citas", domain = "tinder.com",    cancelUrl = "https://www.help.tinder.com/hc/en-us/articles/115005376946"),
            CatalogItem("Bumble Premium",          BigDecimal("39.92"),  "EUR", BillingCycle.MONTHLY, "Filtros avanzados, modo incógnito y retroceder swipes",                         "citas", domain = "bumble.com",    cancelUrl = "https://bumble.com/en-us/the-buzz/cancellation"),
            CatalogItem("Hinge+",                  BigDecimal("36.29"),  "EUR", BillingCycle.MONTHLY, "Filtros extendidos y likes ilimitados en Hinge",                                "citas", domain = "hinge.co",      cancelUrl = "https://hingeapp.zendesk.com/hc/en-us/articles/360007194833"),
            CatalogItem("Meetic Premium",          BigDecimal("36.29"),  "EUR", BillingCycle.MONTHLY, "Citas online — perfil destacado y chat sin límites",                            "citas", 3,  "meetic.es",      "https://www.meetic.es/my/account/settings/subscription"),
            CatalogItem("Badoo Premium",           BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Súper poderes en Badoo — ver quién te gustó",                                   "citas", domain = "badoo.com",     cancelUrl = "https://badoo.com/settings/premium"),
            CatalogItem("Grindr Unlimited",        BigDecimal("36.29"),  "EUR", BillingCycle.MONTHLY, "Chat sin anuncios, ver quién te visitó en Grindr",                              "citas", 7,  "grindr.com",     "https://help.grindr.com/hc/en-us/articles/115001667531"),
            CatalogItem("OkCupid A-List",          BigDecimal("30.24"),  "EUR", BillingCycle.MONTHLY, "Ver quién te gustó y sin anuncios en OkCupid",                                  "citas", domain = "okcupid.com",   cancelUrl = "https://www.okcupid.com/faq/a-list-faq")
        )
    )

    /**
     * Devuelve los servicios del catálogo que corresponden al nombre de una categoría.
     * La coincidencia es flexible: compara el nombre (en minúsculas) con palabras clave.
     * Si no hay coincidencia, devuelve todos los servicios ordenados por nombre.
     */
    fun getItemsForCategory(categoryName: String): List<CatalogItem> {
        val name = categoryName.lowercase()
        val key = when {
            name.contains("ia") || name.contains("ai") || name.contains("intelig") -> "ia"
            name.contains("stream") || name.contains("video") || name.contains("entret") -> "streaming"
            name.contains("músic") || name.contains("music") || name.contains("audio") -> "musica"
            name.contains("soft") || name.contains("produc") || name.contains("diseño")
                    || name.contains("herramient") -> "software"
            name.contains("cloud") || name.contains("nube") || name.contains("almac")
                    || name.contains("backup") -> "cloud"
            name.contains("gam") || name.contains("jueg") || name.contains("xbox")
                    || name.contains("playstation") || name.contains("nintendo") -> "gaming"
            name.contains("segur") || name.contains("privac") || name.contains("vpn")
                    || name.contains("contrase") || name.contains("antivir") -> "seguridad"
            name.contains("notici") || name.contains("lectur") || name.contains("libro")
                    || name.contains("pren") -> "noticias"
            name.contains("salud") || name.contains("deport") || name.contains("fitness")
                    || name.contains("bienest") -> "salud"
            name.contains("desarroll") || name.contains("devops") || name.contains("progr")
                    || name.contains("código") || name.contains("codigo") || name.contains("dev") -> "desarrollo"
            name.contains("prueba") || name.contains("trial") || name.contains("gratu") -> "prueba"
            name.contains("finanz") || name.contains("banco") || name.contains("presupuest") -> "finanzas"
            name.contains("educac") || name.contains("curso") || name.contains("aprendiz") -> "educacion"
            name.contains("creativid") || name.contains("foto") || name.contains("edici") -> "creatividad"
            name.contains("citas") || name.contains("dating") || name.contains("social") -> "citas"
            else -> null
        }
        return (if (key != null) catalog[key] else null) ?: catalog.values.flatten().sortedBy { it.name }
    }

    /** Devuelve todos los servicios del catálogo ordenados por nombre. */
    fun getAllItems(): List<CatalogItem> = catalog.values.flatten().sortedBy { it.name }

    /** Devuelve todos los items agrupados por clave de categoría. */
    fun getAllItemsGrouped(): Map<String, List<CatalogItem>> = catalog

    /** Devuelve un mapa nombre → dominio para mostrar logos en los templates. */
    fun getDomainMap(): Map<String, String> =
        catalog.values.flatten()
            .filter { it.domain != null }
            .associate { it.name to it.domain!! }

    /** Devuelve un mapa nombre → URL de cancelación para mostrar el enlace directo. */
    fun getCancelUrlMap(): Map<String, String> =
        catalog.values.flatten()
            .filter { it.cancelUrl != null }
            .associate { it.name to it.cancelUrl!! }
}