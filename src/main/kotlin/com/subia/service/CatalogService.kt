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
 *   "noticias", "salud", "desarrollo"
 */
@Service
class CatalogService {

    /**
     * Mapa clave → lista de servicios. Todos los precios están en EUR.
     */
    private val catalog: Map<String, List<CatalogItem>> = mapOf(

        // ── Inteligencia Artificial ──────────────────────────────────────────────
        "ia" to listOf(
            CatalogItem("Claude Pro",              BigDecimal("20.56"),  "EUR", BillingCycle.MONTHLY, "Anthropic Claude Pro — acceso prioritario y límites ampliados",                "ia", domain = "claude.ai"),
            CatalogItem("ChatGPT Plus",            BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Plus — GPT-4o con acceso prioritario",                          "ia", domain = "openai.com"),
            CatalogItem("ChatGPT Pro",             BigDecimal("223.85"), "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Pro — acceso ilimitado a o1 y herramientas avanzadas",           "ia", domain = "openai.com"),
            CatalogItem("Cursor Pro",              BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Editor de código con IA — plan Pro mensual",                                    "ia", domain = "cursor.com"),
            CatalogItem("GitHub Copilot",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Asistente de código IA para IDEs — plan Individual",                            "ia", domain = "github.com"),
            CatalogItem("Gemini Advanced",         BigDecimal("26.61"),  "EUR", BillingCycle.MONTHLY, "Google One AI Premium — Gemini Advanced + 2 TB de almacenamiento",              "ia", domain = "gemini.google.com"),
            CatalogItem("Perplexity Pro",          BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Motor de búsqueda con IA — búsquedas ilimitadas y modelos avanzados",           "ia", domain = "perplexity.ai"),
            CatalogItem("Midjourney Standard",     BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA — 15 horas GPU/mes",                                  "ia", domain = "midjourney.com"),
            CatalogItem("Midjourney Basic",        BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA — 3,3 horas GPU/mes",                                 "ia", domain = "midjourney.com"),
            CatalogItem("Grok SuperGrok",          BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "xAI Grok SuperGrok — acceso a Grok 3 con límites ampliados",                    "ia", domain = "x.com"),
            CatalogItem("Notion AI",               BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "Complemento de IA para Notion — resúmenes, redacción y búsqueda semántica",     "ia", domain = "notion.so"),
            CatalogItem("Canva Pro",               BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Diseño gráfico con IA — plantillas premium, Magic Studio y marca",              "ia", domain = "canva.com")
        ),

        // ── Streaming de vídeo ───────────────────────────────────────────────────
        "streaming" to listOf(
            CatalogItem("Netflix Estándar",        BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD, sin anuncios",                                   "streaming", domain = "netflix.com"),
            CatalogItem("Netflix Premium",         BigDecimal("27.82"),  "EUR", BillingCycle.MONTHLY, "Netflix — 4 pantallas Ultra HD, sin anuncios",                                  "streaming", domain = "netflix.com"),
            CatalogItem("Netflix con anuncios",    BigDecimal("7.25"),   "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD con anuncios",                                    "streaming", domain = "netflix.com"),
            CatalogItem("Disney+",                 BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Disney+ — Disney, Marvel, Star Wars y National Geographic",                     "streaming", domain = "disneyplus.com"),
            CatalogItem("Amazon Prime",            BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Amazon Prime — envíos gratis + Prime Video + Prime Music",                      "streaming", domain = "amazon.com"),
            CatalogItem("YouTube Premium",         BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "YouTube sin anuncios, reproducción en segundo plano y YouTube Music",           "streaming", domain = "youtube.com"),
            CatalogItem("Apple TV+",               BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Apple TV+ — series y películas originales Apple",                               "streaming", domain = "apple.com"),
            CatalogItem("Max",                     BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Max (HBO) — plan con anuncios, series y películas HBO y Warner",                "streaming", domain = "max.com"),
            CatalogItem("Max sin anuncios",        BigDecimal("19.35"),  "EUR", BillingCycle.MONTHLY, "Max (HBO) — plan sin anuncios, descargas incluidas",                            "streaming", domain = "max.com"),
            CatalogItem("Crunchyroll Mega Fan",    BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Crunchyroll — anime en simulcast sin anuncios, acceso offline",                 "streaming", domain = "crunchyroll.com"),
            CatalogItem("DAZN",                    BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "DAZN — deportes en directo: LaLiga, MotoGP, Fórmula 1",                         "streaming", domain = "dazn.com"),
            CatalogItem("Movistar+",               BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Movistar+ — series, fútbol y entretenimiento premium",                          "streaming", domain = "movistar.es"),
            CatalogItem("Filmin",                  BigDecimal("10.88"),  "EUR", BillingCycle.MONTHLY, "Filmin — cine independiente, clásicos y festivales",                            "streaming", domain = "filmin.es"),
            CatalogItem("SkyShowtime",             BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "SkyShowtime — Paramount+, Peacock, Universal y Nickelodeon",                    "streaming", domain = "skyshowtime.com"),
            CatalogItem("Paramount+",              BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Paramount+ — series CBS, Paramount originals y deportes",                       "streaming", domain = "paramountplus.com"),
            CatalogItem("Twitch Turbo",            BigDecimal("10.27"),  "EUR", BillingCycle.MONTHLY, "Twitch Turbo — sin anuncios en todos los canales",                              "streaming", domain = "twitch.tv")
        ),

        // ── Música ───────────────────────────────────────────────────────────────
        "musica" to listOf(
            CatalogItem("Spotify Premium",         BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Spotify — música y podcasts sin anuncios, escucha sin conexión",                "musica", domain = "spotify.com"),
            CatalogItem("Apple Music",             BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Apple Music — 100 millones de canciones, lossless y Dolby Atmos",               "musica", domain = "apple.com"),
            CatalogItem("Tidal HiFi",              BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Tidal — audio lossless y HiRes FLAC para audiófilos",                           "musica", domain = "tidal.com"),
            CatalogItem("Amazon Music Unlimited",  BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Amazon Music Unlimited — 100 millones de canciones en HD y Ultra HD",           "musica", domain = "amazon.com"),
            CatalogItem("YouTube Music Premium",   BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "YouTube Music — sin anuncios, descarga y reproducción en segundo plano",        "musica", domain = "youtube.com"),
            CatalogItem("Deezer Premium",          BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Deezer — música sin anuncios, modo offline y Flow personalizado",               "musica", domain = "deezer.com"),
            CatalogItem("SoundCloud Next Pro",     BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "SoundCloud — escucha sin anuncios y subidas ilimitadas para creadores",         "musica", domain = "soundcloud.com")
        ),

        // ── Software y productividad ─────────────────────────────────────────────
        "software" to listOf(
            CatalogItem("Microsoft 365 Personal",  BigDecimal("84.69"),  "EUR", BillingCycle.YEARLY,  "Microsoft 365 — Office + 1 TB OneDrive para 1 persona",                        "software", domain = "microsoft.com"),
            CatalogItem("Microsoft 365 Familiar",  BigDecimal("120.99"), "EUR", BillingCycle.YEARLY,  "Microsoft 365 — Office + 1 TB OneDrive para hasta 6 personas",                 "software", domain = "microsoft.com"),
            CatalogItem("Adobe Creative Cloud",    BigDecimal("72.59"),  "EUR", BillingCycle.MONTHLY, "Adobe CC — suite completa con Photoshop, Illustrator, Premiere y más",         "software", domain = "adobe.com"),
            CatalogItem("Adobe Photoshop",         BigDecimal("29.27"),  "EUR", BillingCycle.MONTHLY, "Adobe Photoshop — edición de imágenes profesional",                             "software", domain = "adobe.com"),
            CatalogItem("Adobe Acrobat Pro",       BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Adobe Acrobat Pro — creación y edición avanzada de PDF",                       "software", domain = "adobe.com"),
            CatalogItem("Notion Plus",             BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Notion Plus — bloques ilimitados, historial de 30 días",                        "software", domain = "notion.so"),
            CatalogItem("Figma Professional",      BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Figma — diseño colaborativo, proyectos ilimitados por editor",                  "software", domain = "figma.com"),
            CatalogItem("Sketch",                  BigDecimal("10.88"),  "EUR", BillingCycle.MONTHLY, "Sketch — diseño de interfaces macOS, colaboración incluida",                    "software", domain = "sketch.com"),
            CatalogItem("Todoist Pro",             BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Todoist — gestión de tareas con recordatorios y filtros avanzados",             "software", domain = "todoist.com"),
            CatalogItem("Evernote Personal",       BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Evernote — notas, documentos y tareas sincronizados",                           "software", domain = "evernote.com"),
            CatalogItem("Obsidian Sync",           BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "Obsidian Sync — sincronización cifrada de bóvedas entre dispositivos",          "software", domain = "obsidian.md"),
            CatalogItem("Affinity Suite",          BigDecimal("20.56"),  "EUR", BillingCycle.MONTHLY, "Affinity — Photo, Designer y Publisher en un solo plan",                        "software", domain = "affinity.serif.com"),
            CatalogItem("Grammarly Premium",       BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Grammarly — corrección gramatical avanzada e inteligencia de escritura",        "software", domain = "grammarly.com"),
            CatalogItem("Duolingo Super",          BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Duolingo — aprendizaje de idiomas sin anuncios, con racha reparadora",          "software", domain = "duolingo.com")
        ),

        // ── Almacenamiento en la nube ────────────────────────────────────────────
        "cloud" to listOf(
            CatalogItem("Google One 2 TB",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Google One — 2 TB compartidos entre Drive, Gmail y Fotos",                     "cloud", domain = "google.com"),
            CatalogItem("Google One 5 TB",         BigDecimal("30.24"),  "EUR", BillingCycle.MONTHLY, "Google One — 5 TB compartidos entre Drive, Gmail y Fotos",                     "cloud", domain = "google.com"),
            CatalogItem("iCloud+ 200 GB",          BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "iCloud+ — 200 GB para iPhone, iPad y Mac",                                      "cloud", domain = "icloud.com"),
            CatalogItem("iCloud+ 2 TB",            BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "iCloud+ — 2 TB con Private Relay y dominio de correo personalizado",            "cloud", domain = "icloud.com"),
            CatalogItem("Dropbox Plus",            BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Dropbox Plus — 2 TB con sincronización inteligente",                            "cloud", domain = "dropbox.com"),
            CatalogItem("OneDrive 100 GB",         BigDecimal("2.42"),   "EUR", BillingCycle.MONTHLY, "Microsoft OneDrive — 100 GB de almacenamiento independiente",                   "cloud", domain = "microsoft.com"),
            CatalogItem("Backblaze Personal",      BigDecimal("10.27"),  "EUR", BillingCycle.MONTHLY, "Backblaze — copia de seguridad ilimitada del ordenador",                        "cloud", domain = "backblaze.com"),
            CatalogItem("pCloud Premium 2 TB",     BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "pCloud — 2 TB de almacenamiento cifrado en Europa",                             "cloud", domain = "pcloud.com")
        ),

        // ── Gaming ───────────────────────────────────────────────────────────────
        "gaming" to listOf(
            CatalogItem("Xbox Game Pass Ultimate", BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Xbox Game Pass Ultimate — juegos en PC, consola y la nube + EA Play",           "gaming", domain = "xbox.com"),
            CatalogItem("PlayStation Plus Extra",  BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "PS Plus Extra — biblioteca de 400+ juegos para PS5 y PS4",                     "gaming", domain = "playstation.com"),
            CatalogItem("PlayStation Plus Essential",BigDecimal("10.88"), "EUR", BillingCycle.MONTHLY, "PS Plus Essential — juegos mensuales y multijugador online",                    "gaming", domain = "playstation.com"),
            CatalogItem("PlayStation Plus Premium",BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "PS Plus Premium — biblioteca completa con juegos clásicos y streaming",         "gaming", domain = "playstation.com"),
            CatalogItem("EA Play",                 BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "EA Play — acceso anticipado y biblioteca de juegos EA",                         "gaming", domain = "ea.com"),
            CatalogItem("Nintendo Switch Online",  BigDecimal("24.19"),  "EUR", BillingCycle.YEARLY,  "Nintendo Switch Online — multijugador online y juegos NES/SNES/N64",            "gaming", domain = "nintendo.com"),
            CatalogItem("Nintendo Switch Online+", BigDecimal("42.34"),  "EUR", BillingCycle.YEARLY,  "Nintendo Switch Online + Pack de expansión — Game Boy, N64 y DLC",              "gaming", domain = "nintendo.com"),
            CatalogItem("Apple Arcade",            BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Apple Arcade — más de 200 juegos premium sin anuncios",                         "gaming", domain = "apple.com"),
            CatalogItem("Humble Choice",           BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "Humble Choice — 9 juegos de PC al mes + descuentos en la tienda",               "gaming", domain = "humblebundle.com")
        ),

        // ── Seguridad y privacidad ───────────────────────────────────────────────
        "seguridad" to listOf(
            CatalogItem("NordVPN",                 BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "NordVPN — VPN con 6 000+ servidores y bloqueador de anuncios",                  "seguridad", domain = "nordvpn.com"),
            CatalogItem("ExpressVPN",              BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "ExpressVPN — VPN rápida con servidores en 105 países",                          "seguridad", domain = "expressvpn.com"),
            CatalogItem("Mullvad VPN",             BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Mullvad — VPN centrada en la privacidad, sin cuenta ni logs",                   "seguridad", domain = "mullvad.net"),
            CatalogItem("ProtonVPN Plus",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Proton VPN Plus — VPN de código abierto con Tor y alta velocidad",              "seguridad", domain = "proton.me"),
            CatalogItem("1Password",               BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "1Password — gestor de contraseñas con bóvedas ilimitadas",                      "seguridad", domain = "1password.com"),
            CatalogItem("Bitwarden Premium",       BigDecimal("11.48"),  "EUR", BillingCycle.YEARLY,  "Bitwarden — gestor de contraseñas open-source, plan Premium anual",              "seguridad", domain = "bitwarden.com"),
            CatalogItem("Dashlane Premium",        BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Dashlane — gestor de contraseñas con VPN incluida",                             "seguridad", domain = "dashlane.com"),
            CatalogItem("Malwarebytes Premium",    BigDecimal("4.22"),   "EUR", BillingCycle.MONTHLY, "Malwarebytes — antimalware en tiempo real para PC y Mac",                       "seguridad", domain = "malwarebytes.com"),
            CatalogItem("ProtonMail Plus",         BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "Proton Mail — correo cifrado de extremo a extremo",                             "seguridad", domain = "proton.me"),
            CatalogItem("Surfshark Starter",       BigDecimal("3.01"),   "EUR", BillingCycle.MONTHLY, "Surfshark — VPN con dispositivos ilimitados y bloqueador de anuncios",          "seguridad", domain = "surfshark.com")
        ),

        // ── Noticias y Lectura ───────────────────────────────────────────────────
        "noticias" to listOf(
            CatalogItem("Kindle Unlimited",        BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Kindle Unlimited — más de 4 millones de libros digitales",                     "noticias", domain = "amazon.com"),
            CatalogItem("Readwise Reader",         BigDecimal("9.06"),   "EUR", BillingCycle.MONTHLY, "Readwise — lectura, subrayado y repaso de libros y artículos",                  "noticias", domain = "readwise.io"),
            CatalogItem("Scribd",                  BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Scribd — libros, audiolibros, revistas y documentos ilimitados",                "noticias", domain = "scribd.com"),
            CatalogItem("El País Digital",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "El País — acceso digital ilimitado con archivo histórico",                      "noticias", domain = "elpais.com"),
            CatalogItem("The New York Times",      BigDecimal("19.35"),  "EUR", BillingCycle.MONTHLY, "NYT — acceso completo a noticias, juegos, Cooking y Wirecutter",               "noticias", domain = "nytimes.com"),
            CatalogItem("Blinkist Premium",        BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "Blinkist — resúmenes de libros de no ficción en 15 minutos",                   "noticias", domain = "blinkist.com"),
            CatalogItem("Audible",                 BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Audible — 1 audiolibro al mes + acceso a catálogo Plus",                       "noticias", domain = "audible.com")
        ),

        // ── Salud y Deporte ──────────────────────────────────────────────────────
        "salud" to listOf(
            CatalogItem("Strava Summit",           BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Strava — análisis avanzado de entrenamiento, segmentos y rutas",                "salud", domain = "strava.com"),
            CatalogItem("Whoop",                   BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "Whoop — monitor de salud 24/7: recuperación, sueño y esfuerzo",                "salud", domain = "whoop.com"),
            CatalogItem("Garmin Connect+",         BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Garmin Connect+ — estadísticas avanzadas y planes de entrenamiento",            "salud", domain = "garmin.com"),
            CatalogItem("MyFitnessPal Premium",    BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "MyFitnessPal — seguimiento nutricional avanzado con escáner de alimentos",     "salud", domain = "myfitnesspal.com"),
            CatalogItem("Calm Premium",            BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Calm — meditación, sueño y reducción del estrés",                               "salud", domain = "calm.com"),
            CatalogItem("Headspace",               BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Headspace — meditación guiada y cursos de bienestar mental",                    "salud", domain = "headspace.com"),
            CatalogItem("Noom",                    BigDecimal("66.54"),  "EUR", BillingCycle.MONTHLY, "Noom — programa de pérdida de peso basado en psicología conductual",            "salud", domain = "noom.com"),
            CatalogItem("Apple Fitness+",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Apple Fitness+ — clases de entrenamiento con métricas de Apple Watch",          "salud", domain = "apple.com")
        ),

        // ── Pruebas gratuitas ────────────────────────────────────────────────────
        "prueba" to listOf(
            CatalogItem("Netflix Estándar",        BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD, sin anuncios",                                   "prueba", 30, "netflix.com"),
            CatalogItem("Spotify Premium",         BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Spotify — música y podcasts sin anuncios, escucha sin conexión",                "prueba", 30, "spotify.com"),
            CatalogItem("Amazon Prime",            BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Amazon Prime — envíos gratis + Prime Video + Prime Music",                      "prueba", 30, "amazon.com"),
            CatalogItem("Disney+",                 BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Disney+ — Disney, Marvel, Star Wars y National Geographic",                     "prueba", 7,  "disneyplus.com"),
            CatalogItem("Apple TV+",               BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Apple TV+ — series y películas originales Apple",                               "prueba", 7,  "apple.com"),
            CatalogItem("Cursor Pro",              BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Editor de código con IA — plan Pro mensual",                                    "prueba", 14, "cursor.com"),
            CatalogItem("Adobe Creative Cloud",    BigDecimal("72.59"),  "EUR", BillingCycle.MONTHLY, "Adobe CC — suite completa con Photoshop, Illustrator, Premiere y más",         "prueba", 7,  "adobe.com"),
            CatalogItem("YouTube Premium",         BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "YouTube sin anuncios, reproducción en segundo plano y YouTube Music",           "prueba", 30, "youtube.com"),
            CatalogItem("Notion Plus",             BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Notion Plus — bloques ilimitados, historial de 30 días",                        "prueba", 14, "notion.so"),
            CatalogItem("Duolingo Super",          BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Duolingo — aprendizaje de idiomas sin anuncios, con racha reparadora",          "prueba", 14, "duolingo.com"),
            CatalogItem("NordVPN",                 BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "NordVPN — VPN con 6 000+ servidores y bloqueador de anuncios",                  "prueba", 30, "nordvpn.com"),
            CatalogItem("Grammarly Premium",       BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Grammarly — corrección gramatical avanzada e inteligencia de escritura",        "prueba", 7,  "grammarly.com")
        ),

        // ── Desarrollo y DevOps ──────────────────────────────────────────────────
        "desarrollo" to listOf(
            CatalogItem("GitHub Pro",              BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "GitHub — repositorios privados ilimitados, wikis y Pages avanzadas",            "desarrollo", domain = "github.com"),
            CatalogItem("GitHub Team",             BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "GitHub Team — revisiones de código, protección de ramas y CODEOWNERS",         "desarrollo", domain = "github.com"),
            CatalogItem("GitLab Premium",          BigDecimal("32.66"),  "EUR", BillingCycle.MONTHLY, "GitLab Premium — CI/CD avanzado, revisiones de código y seguridad",             "desarrollo", domain = "gitlab.com"),
            CatalogItem("Vercel Pro",              BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Vercel Pro — despliegue continuo con ancho de banda y builds ampliados",        "desarrollo", domain = "vercel.com"),
            CatalogItem("Netlify Pro",             BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Netlify — despliegue de sitios estáticos con funciones serverless",              "desarrollo", domain = "netlify.com"),
            CatalogItem("Railway Hobby",           BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Railway — despliegue de aplicaciones con crédito mensual incluido",             "desarrollo", domain = "railway.app"),
            CatalogItem("Sentry Team",             BigDecimal("29.03"),  "EUR", BillingCycle.MONTHLY, "Sentry — monitorización de errores y rendimiento",                              "desarrollo", domain = "sentry.io"),
            CatalogItem("Linear",                  BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Linear — gestión de proyectos de software para equipos",                        "desarrollo", domain = "linear.app"),
            CatalogItem("JetBrains All Products",  BigDecimal("34.97"),  "EUR", BillingCycle.MONTHLY, "JetBrains — todos los IDEs: IntelliJ, WebStorm, PyCharm y más",                 "desarrollo", domain = "jetbrains.com"),
            CatalogItem("Postman Basic",           BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "Postman — plataforma de API con colecciones privadas y colaboración",           "desarrollo", domain = "postman.com"),
            CatalogItem("Datadog Pro",             BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Datadog — monitorización de infraestructura y aplicaciones",                    "desarrollo", domain = "datadoghq.com")
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
            else -> null
        }
        return (if (key != null) catalog[key] else null) ?: catalog.values.flatten().sortedBy { it.name }
    }

    /** Devuelve todos los servicios del catálogo ordenados por nombre. */
    fun getAllItems(): List<CatalogItem> = catalog.values.flatten().sortedBy { it.name }

    /** Devuelve un mapa nombre → dominio para mostrar logos en los templates. */
    fun getDomainMap(): Map<String, String> =
        catalog.values.flatten()
            .filter { it.domain != null }
            .associate { it.name to it.domain!! }
}