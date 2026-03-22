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
            CatalogItem("Claude Pro",              BigDecimal("20.56"),  "EUR", BillingCycle.MONTHLY, "Anthropic Claude Pro — acceso prioritario y límites ampliados",                "ia"),
            CatalogItem("ChatGPT Plus",            BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Plus — GPT-4o con acceso prioritario",                          "ia"),
            CatalogItem("ChatGPT Pro",             BigDecimal("223.85"), "EUR", BillingCycle.MONTHLY, "OpenAI ChatGPT Pro — acceso ilimitado a o1 y herramientas avanzadas",           "ia"),
            CatalogItem("Cursor Pro",              BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Editor de código con IA — plan Pro mensual",                                    "ia"),
            CatalogItem("GitHub Copilot",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Asistente de código IA para IDEs — plan Individual",                            "ia"),
            CatalogItem("Gemini Advanced",         BigDecimal("26.61"),  "EUR", BillingCycle.MONTHLY, "Google One AI Premium — Gemini Advanced + 2 TB de almacenamiento",              "ia"),
            CatalogItem("Perplexity Pro",          BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Motor de búsqueda con IA — búsquedas ilimitadas y modelos avanzados",           "ia"),
            CatalogItem("Midjourney Standard",     BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA — 15 horas GPU/mes",                                  "ia"),
            CatalogItem("Midjourney Basic",        BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Generación de imágenes IA — 3,3 horas GPU/mes",                                 "ia"),
            CatalogItem("Grok SuperGrok",          BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "xAI Grok SuperGrok — acceso a Grok 3 con límites ampliados",                    "ia"),
            CatalogItem("Notion AI",               BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "Complemento de IA para Notion — resúmenes, redacción y búsqueda semántica",     "ia"),
            CatalogItem("Canva Pro",               BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Diseño gráfico con IA — plantillas premium, Magic Studio y marca",              "ia")
        ),

        // ── Streaming de vídeo ───────────────────────────────────────────────────
        "streaming" to listOf(
            CatalogItem("Netflix Estándar",        BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD, sin anuncios",                                   "streaming"),
            CatalogItem("Netflix Premium",         BigDecimal("27.82"),  "EUR", BillingCycle.MONTHLY, "Netflix — 4 pantallas Ultra HD, sin anuncios",                                  "streaming"),
            CatalogItem("Netflix con anuncios",    BigDecimal("7.25"),   "EUR", BillingCycle.MONTHLY, "Netflix — 2 pantallas Full HD con anuncios",                                    "streaming"),
            CatalogItem("Disney+",                 BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Disney+ — Disney, Marvel, Star Wars y National Geographic",                     "streaming"),
            CatalogItem("Amazon Prime",            BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Amazon Prime — envíos gratis + Prime Video + Prime Music",                      "streaming"),
            CatalogItem("YouTube Premium",         BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "YouTube sin anuncios, reproducción en segundo plano y YouTube Music",           "streaming"),
            CatalogItem("Apple TV+",               BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Apple TV+ — series y películas originales Apple",                               "streaming"),
            CatalogItem("Max",                     BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Max (HBO) — plan con anuncios, series y películas HBO y Warner",                "streaming"),
            CatalogItem("Max sin anuncios",        BigDecimal("19.35"),  "EUR", BillingCycle.MONTHLY, "Max (HBO) — plan sin anuncios, descargas incluidas",                            "streaming"),
            CatalogItem("Crunchyroll Mega Fan",    BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Crunchyroll — anime en simulcast sin anuncios, acceso offline",                 "streaming"),
            CatalogItem("DAZN",                    BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "DAZN — deportes en directo: LaLiga, MotoGP, Fórmula 1",                         "streaming"),
            CatalogItem("Movistar+",               BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Movistar+ — series, fútbol y entretenimiento premium",                          "streaming"),
            CatalogItem("Filmin",                  BigDecimal("10.88"),  "EUR", BillingCycle.MONTHLY, "Filmin — cine independiente, clásicos y festivales",                            "streaming"),
            CatalogItem("SkyShowtime",             BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "SkyShowtime — Paramount+, Peacock, Universal y Nickelodeon",                    "streaming"),
            CatalogItem("Paramount+",              BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Paramount+ — series CBS, Paramount originals y deportes",                       "streaming"),
            CatalogItem("Twitch Turbo",            BigDecimal("10.27"),  "EUR", BillingCycle.MONTHLY, "Twitch Turbo — sin anuncios en todos los canales",                              "streaming")
        ),

        // ── Música ───────────────────────────────────────────────────────────────
        "musica" to listOf(
            CatalogItem("Spotify Premium",         BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Spotify — música y podcasts sin anuncios, escucha sin conexión",                "musica"),
            CatalogItem("Apple Music",             BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Apple Music — 100 millones de canciones, lossless y Dolby Atmos",               "musica"),
            CatalogItem("Tidal HiFi",              BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Tidal — audio lossless y HiRes FLAC para audiófilos",                           "musica"),
            CatalogItem("Amazon Music Unlimited",  BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Amazon Music Unlimited — 100 millones de canciones en HD y Ultra HD",           "musica"),
            CatalogItem("YouTube Music Premium",   BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "YouTube Music — sin anuncios, descarga y reproducción en segundo plano",        "musica"),
            CatalogItem("Deezer Premium",          BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Deezer — música sin anuncios, modo offline y Flow personalizado",               "musica"),
            CatalogItem("SoundCloud Next Pro",     BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "SoundCloud — escucha sin anuncios y subidas ilimitadas para creadores",         "musica")
        ),

        // ── Software y productividad ─────────────────────────────────────────────
        "software" to listOf(
            CatalogItem("Microsoft 365 Personal",  BigDecimal("84.69"),  "EUR", BillingCycle.YEARLY,  "Microsoft 365 — Office + 1 TB OneDrive para 1 persona",                        "software"),
            CatalogItem("Microsoft 365 Familiar",  BigDecimal("120.99"), "EUR", BillingCycle.YEARLY,  "Microsoft 365 — Office + 1 TB OneDrive para hasta 6 personas",                 "software"),
            CatalogItem("Adobe Creative Cloud",    BigDecimal("72.59"),  "EUR", BillingCycle.MONTHLY, "Adobe CC — suite completa con Photoshop, Illustrator, Premiere y más",         "software"),
            CatalogItem("Adobe Photoshop",         BigDecimal("29.27"),  "EUR", BillingCycle.MONTHLY, "Adobe Photoshop — edición de imágenes profesional",                             "software"),
            CatalogItem("Adobe Acrobat Pro",       BigDecimal("24.19"),  "EUR", BillingCycle.MONTHLY, "Adobe Acrobat Pro — creación y edición avanzada de PDF",                       "software"),
            CatalogItem("Notion Plus",             BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "Notion Plus — bloques ilimitados, historial de 30 días",                        "software"),
            CatalogItem("Figma Professional",      BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Figma — diseño colaborativo, proyectos ilimitados por editor",                  "software"),
            CatalogItem("Sketch",                  BigDecimal("10.88"),  "EUR", BillingCycle.MONTHLY, "Sketch — diseño de interfaces macOS, colaboración incluida",                    "software"),
            CatalogItem("Todoist Pro",             BigDecimal("4.84"),   "EUR", BillingCycle.MONTHLY, "Todoist — gestión de tareas con recordatorios y filtros avanzados",             "software"),
            CatalogItem("Evernote Personal",       BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Evernote — notas, documentos y tareas sincronizados",                           "software"),
            CatalogItem("Obsidian Sync",           BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "Obsidian Sync — sincronización cifrada de bóvedas entre dispositivos",          "software"),
            CatalogItem("Affinity Suite",          BigDecimal("20.56"),  "EUR", BillingCycle.MONTHLY, "Affinity — Photo, Designer y Publisher en un solo plan",                        "software"),
            CatalogItem("Grammarly Premium",       BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Grammarly — corrección gramatical avanzada e inteligencia de escritura",        "software"),
            CatalogItem("Duolingo Super",          BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Duolingo — aprendizaje de idiomas sin anuncios, con racha reparadora",          "software")
        ),

        // ── Almacenamiento en la nube ────────────────────────────────────────────
        "cloud" to listOf(
            CatalogItem("Google One 2 TB",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Google One — 2 TB compartidos entre Drive, Gmail y Fotos",                     "cloud"),
            CatalogItem("Google One 5 TB",         BigDecimal("30.24"),  "EUR", BillingCycle.MONTHLY, "Google One — 5 TB compartidos entre Drive, Gmail y Fotos",                     "cloud"),
            CatalogItem("iCloud+ 200 GB",          BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "iCloud+ — 200 GB para iPhone, iPad y Mac",                                      "cloud"),
            CatalogItem("iCloud+ 2 TB",            BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "iCloud+ — 2 TB con Private Relay y dominio de correo personalizado",            "cloud"),
            CatalogItem("Dropbox Plus",            BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Dropbox Plus — 2 TB con sincronización inteligente",                            "cloud"),
            CatalogItem("OneDrive 100 GB",         BigDecimal("2.42"),   "EUR", BillingCycle.MONTHLY, "Microsoft OneDrive — 100 GB de almacenamiento independiente",                   "cloud"),
            CatalogItem("Backblaze Personal",      BigDecimal("10.27"),  "EUR", BillingCycle.MONTHLY, "Backblaze — copia de seguridad ilimitada del ordenador",                        "cloud"),
            CatalogItem("pCloud Premium 2 TB",     BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "pCloud — 2 TB de almacenamiento cifrado en Europa",                             "cloud")
        ),

        // ── Gaming ───────────────────────────────────────────────────────────────
        "gaming" to listOf(
            CatalogItem("Xbox Game Pass Ultimate", BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Xbox Game Pass Ultimate — juegos en PC, consola y la nube + EA Play",           "gaming"),
            CatalogItem("PlayStation Plus Extra",  BigDecimal("18.14"),  "EUR", BillingCycle.MONTHLY, "PS Plus Extra — biblioteca de 400+ juegos para PS5 y PS4",                     "gaming"),
            CatalogItem("PlayStation Plus Essential",BigDecimal("10.88"), "EUR", BillingCycle.MONTHLY, "PS Plus Essential — juegos mensuales y multijugador online",                    "gaming"),
            CatalogItem("PlayStation Plus Premium",BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "PS Plus Premium — biblioteca completa con juegos clásicos y streaming",         "gaming"),
            CatalogItem("EA Play",                 BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "EA Play — acceso anticipado y biblioteca de juegos EA",                         "gaming"),
            CatalogItem("Nintendo Switch Online",  BigDecimal("24.19"),  "EUR", BillingCycle.YEARLY,  "Nintendo Switch Online — multijugador online y juegos NES/SNES/N64",            "gaming"),
            CatalogItem("Nintendo Switch Online+", BigDecimal("42.34"),  "EUR", BillingCycle.YEARLY,  "Nintendo Switch Online + Pack de expansión — Game Boy, N64 y DLC",              "gaming"),
            CatalogItem("Apple Arcade",            BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Apple Arcade — más de 200 juegos premium sin anuncios",                         "gaming"),
            CatalogItem("Humble Choice",           BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "Humble Choice — 9 juegos de PC al mes + descuentos en la tienda",               "gaming")
        ),

        // ── Seguridad y privacidad ───────────────────────────────────────────────
        "seguridad" to listOf(
            CatalogItem("NordVPN",                 BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "NordVPN — VPN con 6 000+ servidores y bloqueador de anuncios",                  "seguridad"),
            CatalogItem("ExpressVPN",              BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "ExpressVPN — VPN rápida con servidores en 105 países",                          "seguridad"),
            CatalogItem("Mullvad VPN",             BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Mullvad — VPN centrada en la privacidad, sin cuenta ni logs",                   "seguridad"),
            CatalogItem("ProtonVPN Plus",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Proton VPN Plus — VPN de código abierto con Tor y alta velocidad",              "seguridad"),
            CatalogItem("1Password",               BigDecimal("3.62"),   "EUR", BillingCycle.MONTHLY, "1Password — gestor de contraseñas con bóvedas ilimitadas",                      "seguridad"),
            CatalogItem("Bitwarden Premium",       BigDecimal("11.48"),  "EUR", BillingCycle.YEARLY,  "Bitwarden — gestor de contraseñas open-source, plan Premium anual",              "seguridad"),
            CatalogItem("Dashlane Premium",        BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Dashlane — gestor de contraseñas con VPN incluida",                             "seguridad"),
            CatalogItem("Malwarebytes Premium",    BigDecimal("4.22"),   "EUR", BillingCycle.MONTHLY, "Malwarebytes — antimalware en tiempo real para PC y Mac",                       "seguridad"),
            CatalogItem("ProtonMail Plus",         BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "Proton Mail — correo cifrado de extremo a extremo",                             "seguridad"),
            CatalogItem("Surfshark Starter",       BigDecimal("3.01"),   "EUR", BillingCycle.MONTHLY, "Surfshark — VPN con dispositivos ilimitados y bloqueador de anuncios",          "seguridad")
        ),

        // ── Noticias y Lectura ───────────────────────────────────────────────────
        "noticias" to listOf(
            CatalogItem("Kindle Unlimited",        BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Kindle Unlimited — más de 4 millones de libros digitales",                     "noticias"),
            CatalogItem("Readwise Reader",         BigDecimal("9.06"),   "EUR", BillingCycle.MONTHLY, "Readwise — lectura, subrayado y repaso de libros y artículos",                  "noticias"),
            CatalogItem("Scribd",                  BigDecimal("13.30"),  "EUR", BillingCycle.MONTHLY, "Scribd — libros, audiolibros, revistas y documentos ilimitados",                "noticias"),
            CatalogItem("El País Digital",         BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "El País — acceso digital ilimitado con archivo histórico",                      "noticias"),
            CatalogItem("The New York Times",      BigDecimal("19.35"),  "EUR", BillingCycle.MONTHLY, "NYT — acceso completo a noticias, juegos, Cooking y Wirecutter",               "noticias"),
            CatalogItem("Blinkist Premium",        BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "Blinkist — resúmenes de libros de no ficción en 15 minutos",                   "noticias"),
            CatalogItem("Audible",                 BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Audible — 1 audiolibro al mes + acceso a catálogo Plus",                       "noticias")
        ),

        // ── Salud y Deporte ──────────────────────────────────────────────────────
        "salud" to listOf(
            CatalogItem("Strava Summit",           BigDecimal("6.05"),   "EUR", BillingCycle.MONTHLY, "Strava — análisis avanzado de entrenamiento, segmentos y rutas",                "salud"),
            CatalogItem("Whoop",                   BigDecimal("33.87"),  "EUR", BillingCycle.MONTHLY, "Whoop — monitor de salud 24/7: recuperación, sueño y esfuerzo",                "salud"),
            CatalogItem("Garmin Connect+",         BigDecimal("8.46"),   "EUR", BillingCycle.MONTHLY, "Garmin Connect+ — estadísticas avanzadas y planes de entrenamiento",            "salud"),
            CatalogItem("MyFitnessPal Premium",    BigDecimal("11.48"),  "EUR", BillingCycle.MONTHLY, "MyFitnessPal — seguimiento nutricional avanzado con escáner de alimentos",     "salud"),
            CatalogItem("Calm Premium",            BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Calm — meditación, sueño y reducción del estrés",                               "salud"),
            CatalogItem("Headspace",               BigDecimal("14.51"),  "EUR", BillingCycle.MONTHLY, "Headspace — meditación guiada y cursos de bienestar mental",                    "salud"),
            CatalogItem("Noom",                    BigDecimal("66.54"),  "EUR", BillingCycle.MONTHLY, "Noom — programa de pérdida de peso basado en psicología conductual",            "salud"),
            CatalogItem("Apple Fitness+",          BigDecimal("12.09"),  "EUR", BillingCycle.MONTHLY, "Apple Fitness+ — clases de entrenamiento con métricas de Apple Watch",          "salud")
        ),

        // ── Desarrollo y DevOps ──────────────────────────────────────────────────
        "desarrollo" to listOf(
            CatalogItem("GitHub Pro",              BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "GitHub — repositorios privados ilimitados, wikis y Pages avanzadas",            "desarrollo"),
            CatalogItem("GitHub Team",             BigDecimal("4.83"),   "EUR", BillingCycle.MONTHLY, "GitHub Team — revisiones de código, protección de ramas y CODEOWNERS",         "desarrollo"),
            CatalogItem("GitLab Premium",          BigDecimal("32.66"),  "EUR", BillingCycle.MONTHLY, "GitLab Premium — CI/CD avanzado, revisiones de código y seguridad",             "desarrollo"),
            CatalogItem("Vercel Pro",              BigDecimal("22.98"),  "EUR", BillingCycle.MONTHLY, "Vercel Pro — despliegue continuo con ancho de banda y builds ampliados",        "desarrollo"),
            CatalogItem("Netlify Pro",             BigDecimal("21.77"),  "EUR", BillingCycle.MONTHLY, "Netlify — despliegue de sitios estáticos con funciones serverless",              "desarrollo"),
            CatalogItem("Railway Hobby",           BigDecimal("6.04"),   "EUR", BillingCycle.MONTHLY, "Railway — despliegue de aplicaciones con crédito mensual incluido",             "desarrollo"),
            CatalogItem("Sentry Team",             BigDecimal("29.03"),  "EUR", BillingCycle.MONTHLY, "Sentry — monitorización de errores y rendimiento",                              "desarrollo"),
            CatalogItem("Linear",                  BigDecimal("9.67"),   "EUR", BillingCycle.MONTHLY, "Linear — gestión de proyectos de software para equipos",                        "desarrollo"),
            CatalogItem("JetBrains All Products",  BigDecimal("34.97"),  "EUR", BillingCycle.MONTHLY, "JetBrains — todos los IDEs: IntelliJ, WebStorm, PyCharm y más",                 "desarrollo"),
            CatalogItem("Postman Basic",           BigDecimal("15.72"),  "EUR", BillingCycle.MONTHLY, "Postman — plataforma de API con colecciones privadas y colaboración",           "desarrollo"),
            CatalogItem("Datadog Pro",             BigDecimal("16.93"),  "EUR", BillingCycle.MONTHLY, "Datadog — monitorización de infraestructura y aplicaciones",                    "desarrollo")
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
            else -> null
        }
        return (if (key != null) catalog[key] else null) ?: catalog.values.flatten().sortedBy { it.name }
    }

    /** Devuelve todos los servicios del catálogo ordenados por nombre. */
    fun getAllItems(): List<CatalogItem> = catalog.values.flatten().sortedBy { it.name }
}