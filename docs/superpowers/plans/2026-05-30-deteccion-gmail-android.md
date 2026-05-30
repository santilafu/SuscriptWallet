# Detección de suscripciones por Gmail en Android — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Llevar a la app Android la detección automática de suscripciones leyendo Gmail (ya existente en la web), exponiendo la lógica del backend como API REST y añadiendo una UI nativa de revisión en onboarding y ajustes.

**Architecture:** El backend reutiliza `GmailScanService` (intacto) y añade endpoints REST `/api/gmail/scan/*`. La identidad entre el JWT de la app y el callback OAuth (que abre un navegador sin el JWT) se resuelve con un **ticket de un solo uso** que viaja en el `state` de OAuth. Los resultados se guardan en una tabla temporal por usuario y la app los recupera con su JWT. Android abre el consentimiento en una **Custom Tab** y vuelve por **deep link** `subia://gmail/done`.

**Tech Stack:** Backend: Spring Boot, Kotlin, Spring Security (OAuth2 Resource Server + JWT), JPA/Hibernate, Flyway, JUnit5 + MockK. Android: Kotlin Multiplatform, Jetpack Compose, Koin, Ktor, Navigation Compose, androidx.browser (Custom Tabs), kotlin.test.

**Repo:** raíz en `C:\Users\santi\OneDrive\Escritorio\SubIA`. Backend en `src/`, app en `mobile/`.

---

## Mapa de archivos

### Backend (`src/main/kotlin/com/subia/`)
- **Crear** `model/GmailScanTicket.kt` — entidad JPA del ticket de un solo uso.
- **Crear** `model/GmailScanResultRow.kt` — entidad JPA de una detección temporal por usuario.
- **Crear** `repository/GmailScanTicketRepository.kt` — repo JPA del ticket.
- **Crear** `repository/GmailScanResultRepository.kt` — repo JPA de resultados.
- **Crear** `service/GmailScanTicketService.kt` — emisión y consumo (un solo uso) de tickets; persistencia/lectura/purga de resultados.
- **Crear** `controller/api/ApiGmailController.kt` — endpoints REST `/api/gmail/scan/*`.
- **Crear** `dto/api/GmailDto.kt` — DTOs de transporte (ticket, detección, alta).
- **Modificar** `controller/GmailController.kt` — el callback acepta el camino "ticket" además del web.
- **Modificar** `config/SecurityConfig.kt` — `permitAll` del callback y reglas `/api/gmail/**`.
- **Crear** `src/main/resources/db/migration/V{n}__gmail_scan_tables.sql` — Flyway de las dos tablas.
- **Crear** tests en `src/test/kotlin/com/subia/service/GmailScanTicketServiceTest.kt`.

### Android — shared (`mobile/shared/src/commonMain/kotlin/com/subia/shared/`)
- **Crear** `model/GmailScan.kt` — DTOs serializables.
- **Modificar** `network/ApiRoutes.kt` — rutas nuevas.
- **Crear** `repository/GmailScanRepository.kt`.
- **Crear** `viewmodel/GmailScanViewModel.kt`.
- **Modificar** `di/SharedModule.kt` — registro Koin.
- **Crear** test `mobile/shared/src/commonTest/kotlin/com/subia/shared/GmailScanViewModelTest.kt`.

### Android — app (`mobile/androidApp/src/androidMain/`)
- **Modificar** `../build.gradle.kts` — dependencia `androidx.browser`, `buildConfigField` de la URL web.
- **Modificar** `AndroidManifest.xml` — `<intent-filter>` del deep link.
- **Modificar** `kotlin/com/subia/android/MainActivity.kt` — captura del deep link.
- **Crear** `kotlin/com/subia/android/util/CustomTabsHelper.kt`.
- **Crear** `kotlin/com/subia/android/ui/screens/GmailScanScreen.kt`.
- **Modificar** `kotlin/com/subia/android/navigation/AppNavGraph.kt` — ruta `GmailScanRoute`.
- **Modificar** `kotlin/com/subia/android/ui/SubIAApp.kt` — `composable<GmailScanRoute>` + paso del deep link.
- **Modificar** `kotlin/com/subia/android/ui/screens/OnboardingScreen.kt` — 4ª página con acción.
- **Modificar** `kotlin/com/subia/android/ui/screens/SettingsScreen.kt` — botón de entrada.
- **Modificar** `res/values/strings.xml` y `res/values-en/strings.xml` — textos.

> **Decisión de identidad:** `scanId == ticketId` (un UUID). El ticket se crea al pedir el escaneo, viaja como `state`, y al guardar resultados se usa como `scanId`. El deep link de vuelta no necesita parámetros de identidad: la app recupera resultados con su JWT.

---

## FASE A — Backend: persistencia del ticket y resultados

### Task A1: Entidad y repositorio del ticket

**Files:**
- Create: `src/main/kotlin/com/subia/model/GmailScanTicket.kt`
- Create: `src/main/kotlin/com/subia/repository/GmailScanTicketRepository.kt`

- [ ] **Step 1: Crear la entidad del ticket**

`src/main/kotlin/com/subia/model/GmailScanTicket.kt`:
```kotlin
package com.subia.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * Ticket de un solo uso que liga el JWT de la app (userId) con el callback OAuth de Google,
 * que llega por navegador sin el JWT. Su id (UUID) viaja en el parámetro `state` de OAuth.
 */
@Entity
@Table(name = "gmail_scan_ticket")
data class GmailScanTicket(
    @Id
    @Column(length = 64)
    val id: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val months: Int,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,

    @Column(nullable = false)
    val used: Boolean = false
)
```

- [ ] **Step 2: Crear el repositorio**

`src/main/kotlin/com/subia/repository/GmailScanTicketRepository.kt`:
```kotlin
package com.subia.repository

import com.subia.model.GmailScanTicket
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface GmailScanTicketRepository : JpaRepository<GmailScanTicket, String> {
    fun deleteByExpiresAtBefore(cutoff: OffsetDateTime)
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/subia/model/GmailScanTicket.kt src/main/kotlin/com/subia/repository/GmailScanTicketRepository.kt
git commit -m "feat(backend): entidad y repo del ticket de escaneo Gmail"
```

---

### Task A2: Entidad y repositorio de resultados temporales

**Files:**
- Create: `src/main/kotlin/com/subia/model/GmailScanResultRow.kt`
- Create: `src/main/kotlin/com/subia/repository/GmailScanResultRepository.kt`

- [ ] **Step 1: Crear la entidad de resultado**

`src/main/kotlin/com/subia/model/GmailScanResultRow.kt`:
```kotlin
package com.subia.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * Una suscripción detectada en el escaneo, guardada temporalmente para que la app la revise.
 * Se purga al darla de alta o por expiración. `scanId` == id del ticket que originó el escaneo.
 */
@Entity
@Table(name = "gmail_scan_result")
data class GmailScanResultRow(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "scan_id", length = 64, nullable = false)
    val scanId: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "service_name", nullable = false)
    val serviceName: String,

    @Column(nullable = false)
    val description: String = "",

    @Column(nullable = false)
    val domain: String,

    @Column(name = "sender_email", nullable = false)
    val senderEmail: String,

    @Column(name = "last_seen", nullable = false)
    val lastSeen: String,

    @Column(nullable = false)
    val price: BigDecimal,

    @Column(nullable = false)
    val currency: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    val billingCycle: BillingCycle,

    @Column(name = "price_from_email", nullable = false)
    val priceFromEmail: Boolean,

    @Column(name = "category_key", nullable = false)
    val categoryKey: String
)
```

- [ ] **Step 2: Crear el repositorio**

`src/main/kotlin/com/subia/repository/GmailScanResultRepository.kt`:
```kotlin
package com.subia.repository

import com.subia.model.GmailScanResultRow
import org.springframework.data.jpa.repository.JpaRepository

interface GmailScanResultRepository : JpaRepository<GmailScanResultRow, Long> {
    fun findByUserId(userId: Long): List<GmailScanResultRow>
    fun findByIdInAndUserId(ids: List<Long>, userId: Long): List<GmailScanResultRow>
    fun deleteByUserId(userId: Long)
    fun deleteByIdInAndUserId(ids: List<Long>, userId: Long)
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/subia/model/GmailScanResultRow.kt src/main/kotlin/com/subia/repository/GmailScanResultRepository.kt
git commit -m "feat(backend): entidad y repo de resultados temporales de escaneo Gmail"
```

---

### Task A3: Migración Flyway de las tablas

**Files:**
- Create: `src/main/resources/db/migration/V{n}__gmail_scan_tables.sql`

- [ ] **Step 1: Averiguar el número de versión siguiente**

Run: `ls src/main/resources/db/migration/`
Expected: lista de `V1__...`, `V2__...`. Usa el siguiente número libre como `{n}`.

- [ ] **Step 2: Crear la migración**

`src/main/resources/db/migration/V{n}__gmail_scan_tables.sql` (sintaxis PostgreSQL, que es lo que usa Render; ajusta tipos si el dialecto local difiere):
```sql
CREATE TABLE gmail_scan_ticket (
    id          VARCHAR(64) PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    months      INT         NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE gmail_scan_result (
    id               BIGSERIAL    PRIMARY KEY,
    scan_id          VARCHAR(64)  NOT NULL,
    user_id          BIGINT       NOT NULL,
    service_name     VARCHAR(255) NOT NULL,
    description      VARCHAR(255) NOT NULL DEFAULT '',
    domain           VARCHAR(255) NOT NULL,
    sender_email     VARCHAR(255) NOT NULL,
    last_seen        VARCHAR(32)  NOT NULL,
    price            NUMERIC(12,2) NOT NULL,
    currency         VARCHAR(8)   NOT NULL,
    billing_cycle    VARCHAR(16)  NOT NULL,
    price_from_email BOOLEAN      NOT NULL,
    category_key     VARCHAR(64)  NOT NULL
);

CREATE INDEX idx_gmail_scan_result_user ON gmail_scan_result (user_id);
CREATE INDEX idx_gmail_scan_ticket_expires ON gmail_scan_ticket (expires_at);
```

- [ ] **Step 3: Verificar que la app arranca y aplica la migración**

Run (con el JAVA_HOME del JBR, ver memoria de build): `cd src/.. && ./gradlew :bootRun` (o el módulo backend correspondiente), arrancar y comprobar en el log que Flyway aplica `V{n}`. Parar el proceso.
Expected: log `Migrating schema ... to version {n} - gmail scan tables` sin errores.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/
git commit -m "feat(backend): migración Flyway de tablas de escaneo Gmail"
```

---

### Task A4: Servicio de tickets (TDD)

**Files:**
- Create: `src/main/kotlin/com/subia/service/GmailScanTicketService.kt`
- Test: `src/test/kotlin/com/subia/service/GmailScanTicketServiceTest.kt`

- [ ] **Step 1: Escribir el test que falla**

`src/test/kotlin/com/subia/service/GmailScanTicketServiceTest.kt`:
```kotlin
package com.subia.service

import com.subia.model.GmailScanTicket
import com.subia.repository.GmailScanResultRepository
import com.subia.repository.GmailScanTicketRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional

class GmailScanTicketServiceTest {

    private val ticketRepo = mockk<GmailScanTicketRepository>(relaxed = true)
    private val resultRepo = mockk<GmailScanResultRepository>(relaxed = true)
    private val service = GmailScanTicketService(ticketRepo, resultRepo)

    @Test
    fun `issue crea un ticket con el userId y meses dados y lo persiste`() {
        val saved = slot<GmailScanTicket>()
        every { ticketRepo.save(capture(saved)) } answers { saved.captured }

        val ticket = service.issue(userId = 7L, months = 12)

        assertEquals(7L, ticket.userId)
        assertEquals(12, ticket.months)
        assertEquals(false, ticket.used)
    }

    @Test
    fun `consume devuelve el ticket valido y lo marca usado`() {
        val t = GmailScanTicket("abc", 7L, 12, OffsetDateTime.now().plusMinutes(5), used = false)
        every { ticketRepo.findById("abc") } returns Optional.of(t)
        val saved = slot<GmailScanTicket>()
        every { ticketRepo.save(capture(saved)) } answers { saved.captured }

        val result = service.consume("abc")

        assertEquals(7L, result?.userId)
        assertEquals(true, saved.captured.used)
    }

    @Test
    fun `consume devuelve null si el ticket ya fue usado`() {
        val t = GmailScanTicket("abc", 7L, 12, OffsetDateTime.now().plusMinutes(5), used = true)
        every { ticketRepo.findById("abc") } returns Optional.of(t)
        assertNull(service.consume("abc"))
    }

    @Test
    fun `consume devuelve null si el ticket expiro`() {
        val t = GmailScanTicket("abc", 7L, 12, OffsetDateTime.now().minusMinutes(1), used = false)
        every { ticketRepo.findById("abc") } returns Optional.of(t)
        assertNull(service.consume("abc"))
    }

    @Test
    fun `consume devuelve null si el ticket no existe`() {
        every { ticketRepo.findById("nope") } returns Optional.empty()
        assertNull(service.consume("nope"))
    }
}
```

- [ ] **Step 2: Ejecutar el test y verlo fallar**

Run: `./gradlew test --tests "com.subia.service.GmailScanTicketServiceTest"`
Expected: FAIL — `GmailScanTicketService` no existe / no compila.

- [ ] **Step 3: Implementar el servicio**

`src/main/kotlin/com/subia/service/GmailScanTicketService.kt`:
```kotlin
package com.subia.service

import com.subia.model.GmailScanResultRow
import com.subia.model.GmailScanTicket
import com.subia.repository.GmailScanResultRepository
import com.subia.repository.GmailScanTicketRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

/** TTL del ticket: tiempo máximo entre que la app lo pide y Google devuelve el callback. */
private const val TICKET_TTL_MINUTES = 10L

@Service
class GmailScanTicketService(
    private val ticketRepo: GmailScanTicketRepository,
    private val resultRepo: GmailScanResultRepository
) {
    /** Emite un ticket de un solo uso para el usuario y lo persiste. */
    fun issue(userId: Long, months: Int): GmailScanTicket {
        val ticket = GmailScanTicket(
            id = UUID.randomUUID().toString(),
            userId = userId,
            months = months,
            expiresAt = OffsetDateTime.now().plusMinutes(TICKET_TTL_MINUTES),
            used = false
        )
        return ticketRepo.save(ticket)
    }

    /**
     * Valida y consume el ticket: devuelve el ticket si es válido (existe, no usado, no expirado)
     * marcándolo como usado; en cualquier otro caso devuelve null.
     */
    @Transactional
    fun consume(ticketId: String): GmailScanTicket? {
        val ticket = ticketRepo.findById(ticketId).orElse(null) ?: return null
        if (ticket.used || ticket.expiresAt.isBefore(OffsetDateTime.now())) return null
        ticketRepo.save(ticket.copy(used = true))
        return ticket
    }

    /** Reemplaza los resultados del usuario por el nuevo lote del escaneo. */
    @Transactional
    fun replaceResults(userId: Long, rows: List<GmailScanResultRow>) {
        resultRepo.deleteByUserId(userId)
        resultRepo.saveAll(rows)
    }

    fun resultsFor(userId: Long): List<GmailScanResultRow> = resultRepo.findByUserId(userId)

    @Transactional
    fun consumeResults(userId: Long, ids: List<Long>): List<GmailScanResultRow> {
        val rows = resultRepo.findByIdInAndUserId(ids, userId)
        if (rows.isNotEmpty()) resultRepo.deleteByIdInAndUserId(rows.map { it.id }, userId)
        return rows
    }
}
```

- [ ] **Step 4: Ejecutar el test y verlo pasar**

Run: `./gradlew test --tests "com.subia.service.GmailScanTicketServiceTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/subia/service/GmailScanTicketService.kt src/test/kotlin/com/subia/service/GmailScanTicketServiceTest.kt
git commit -m "feat(backend): servicio de tickets de escaneo Gmail (TDD)"
```

---

## FASE B — Backend: endpoints REST y seguridad

### Task B1: DTOs de transporte

**Files:**
- Create: `src/main/kotlin/com/subia/dto/api/GmailDto.kt`

- [ ] **Step 1: Crear los DTOs**

`src/main/kotlin/com/subia/dto/api/GmailDto.kt`:
```kotlin
package com.subia.dto.api

import java.math.BigDecimal

/** Respuesta a la petición de escaneo: URL de consentimiento que la app abre en Custom Tab. */
data class GmailScanTicketDto(val connectUrl: String)

/** Una suscripción detectada, tal como la revisa la app. `id` identifica la fila para el alta. */
data class GmailDetectedDto(
    val id: Long,
    val serviceName: String,
    val description: String,
    val domain: String,
    val senderEmail: String,
    val lastSeen: String,
    val price: BigDecimal,
    val currency: String,
    val billingCycle: String,
    val priceFromEmail: Boolean
)

/** Petición de alta: ids de las detecciones que el usuario marcó. */
data class GmailAddRequestDto(val ids: List<Long>)

/** Resultado del alta. */
data class GmailAddResultDto(val added: Int)
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/com/subia/dto/api/GmailDto.kt
git commit -m "feat(backend): DTOs REST de escaneo Gmail"
```

---

### Task B2: Adaptar el callback OAuth para el camino "ticket"

**Files:**
- Modify: `src/main/kotlin/com/subia/controller/GmailController.kt`

**Contexto:** el `callback` actual (líneas 67-95) valida el `state` contra la sesión HTTP (camino web). Añadimos: si el `state` corresponde a un ticket válido, seguimos el camino API (escanear → guardar por userId → redirigir al deep link). El camino web se conserva intacto.

- [ ] **Step 1: Inyectar dependencias nuevas en el controlador**

En la cabecera de la clase `GmailController` añade al constructor `gmailScanTicketService: GmailScanTicketService`, `catalogService: CatalogService` (si no está ya) y conserva los existentes. Añade los imports:
```kotlin
import com.subia.service.GmailScanTicketService
import com.subia.model.GmailScanResultRow
import com.subia.model.BillingCycle
```

- [ ] **Step 2: Añadir una constante con el deep link y el método del camino ticket**

Dentro de `GmailController`, añade:
```kotlin
private val androidDeepLink = "subia://gmail/done"

/**
 * Camino del callback para la app móvil: el `state` es un ticket de un solo uso.
 * Escanea, guarda los resultados ligados al userId del ticket y redirige al deep link.
 * Devuelve null si el `state` no es un ticket válido (entonces se sigue el camino web).
 */
private fun handleTicketCallback(code: String, state: String): String? {
    val ticket = gmailScanTicketService.consume(state) ?: return null
    return try {
        val token = gmailScanService.exchangeCodeForToken(code)
        val detected = gmailScanService.scan(token, ticket.months)
        val rows = detected.map { d ->
            GmailScanResultRow(
                scanId = ticket.id,
                userId = ticket.userId,
                serviceName = d.serviceName,
                description = d.catalogItem.description,
                domain = d.domain,
                senderEmail = d.senderEmail,
                lastSeen = d.lastSeen,
                price = d.effectivePrice,
                currency = d.effectiveCurrency,
                billingCycle = d.effectiveCycle,
                priceFromEmail = d.priceFromEmail,
                categoryKey = d.catalogItem.categoryKey
            )
        }
        gmailScanTicketService.replaceResults(ticket.userId, rows)
        "redirect:$androidDeepLink?status=ok"
    } catch (ex: Exception) {
        log.error("Escaneo Gmail (móvil) fallido: {}", ex.message, ex)
        "redirect:$androidDeepLink?status=error"
    }
}
```

- [ ] **Step 3: Llamar al camino ticket al principio del `callback` existente**

Justo al entrar en `callback(...)`, ANTES de leer la sesión, añade:
```kotlin
// Camino móvil: si el state es un ticket válido, lo gestionamos y salimos.
if (error == null && !code.isNullOrBlank() && !state.isNullOrBlank()) {
    handleTicketCallback(code, state)?.let { return it }
}
```
El resto del método (camino web con sesión) queda igual.

- [ ] **Step 4: Verificar que compila y los tests existentes siguen pasando**

Run: `./gradlew test --tests "com.subia.*"`
Expected: PASS (incluye `GmailScanServiceTest` y `GmailScanTicketServiceTest`).

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/subia/controller/GmailController.kt
git commit -m "feat(backend): callback OAuth admite el camino ticket (móvil) sin romper la web"
```

---

### Task B3: Controlador REST `/api/gmail/scan/*`

**Files:**
- Create: `src/main/kotlin/com/subia/controller/api/ApiGmailController.kt`

- [ ] **Step 1: Crear el controlador**

`src/main/kotlin/com/subia/controller/api/ApiGmailController.kt`:
```kotlin
package com.subia.controller.api

import com.subia.dto.api.ApiResponse
import com.subia.dto.api.GmailAddRequestDto
import com.subia.dto.api.GmailAddResultDto
import com.subia.dto.api.GmailDetectedDto
import com.subia.dto.api.GmailScanTicketDto
import com.subia.model.GmailScanResultRow
import com.subia.model.Subscription
import com.subia.repository.UserRepository
import com.subia.service.CategoryService
import com.subia.service.GmailScanService
import com.subia.service.GmailScanTicketService
import com.subia.service.SubscriptionService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

@RestController
@RequestMapping("/api/gmail")
class ApiGmailController(
    private val gmailScanService: GmailScanService,
    private val gmailScanTicketService: GmailScanTicketService,
    private val subscriptionService: SubscriptionService,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository
) {
    private fun resolveUserId(jwt: Jwt): Long =
        userRepository.findByEmail(jwt.subject)?.id
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado")

    /** Emite el ticket y devuelve la URL de consentimiento (state = ticketId). */
    @PostMapping("/scan/ticket")
    fun ticket(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false, defaultValue = "12") months: Int
    ): ApiResponse<GmailScanTicketDto> {
        val userId = resolveUserId(jwt)
        val ticket = gmailScanTicketService.issue(userId, months.coerceIn(1, GmailScanService.MAX_MONTHS))
        val url = gmailScanService.buildAuthorizationUrl(ticket.id)
        return ApiResponse(data = GmailScanTicketDto(connectUrl = url))
    }

    /** Devuelve las detecciones guardadas para el usuario. */
    @GetMapping("/scan/results")
    fun results(@AuthenticationPrincipal jwt: Jwt): ApiResponse<List<GmailDetectedDto>> {
        val userId = resolveUserId(jwt)
        return ApiResponse(data = gmailScanTicketService.resultsFor(userId).map { it.toDto() })
    }

    /** Da de alta las detecciones marcadas y las purga. */
    @PostMapping("/scan/add")
    fun add(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody req: GmailAddRequestDto
    ): ApiResponse<GmailAddResultDto> {
        val userId = resolveUserId(jwt)
        val rows = gmailScanTicketService.consumeResults(userId, req.ids)
        val keyToId = categoryKeyToId()
        var added = 0
        for (r in rows) {
            val categoryId = keyToId[r.categoryKey] ?: continue
            subscriptionService.save(
                Subscription(
                    name = r.serviceName,
                    description = r.description,
                    price = r.price,
                    currency = r.currency,
                    billingCycle = r.billingCycle,
                    renewalDate = LocalDate.now().plusMonths(1),
                    category = categoryService.findById(categoryId),
                    active = true,
                    notes = ""
                ),
                userId
            )
            added++
        }
        return ApiResponse(data = GmailAddResultDto(added = added))
    }

    /** Mapea la categoryKey del catálogo al id real de la categoría del usuario. */
    private fun categoryKeyToId(): Map<String, Long> =
        categoryService.findAll().mapNotNull { cat ->
            val key = when (cat.name) {
                "IA" -> "ia"; "Streaming" -> "streaming"; "Música" -> "musica"
                "Software" -> "software"; "Cloud" -> "cloud"; "Gaming" -> "gaming"
                "Seguridad" -> "seguridad"; "Noticias y Lectura" -> "noticias"
                "Salud y Deporte" -> "salud"; "Desarrollo" -> "desarrollo"
                "Prueba gratuita" -> "prueba"; "Finanzas" -> "finanzas"
                "Educación" -> "educacion"; "Creatividad y foto" -> "creatividad"
                "Citas y social" -> "citas"; else -> null
            }
            if (key != null) key to cat.id else null
        }.toMap()

    private fun GmailScanResultRow.toDto() = GmailDetectedDto(
        id = id, serviceName = serviceName, description = description, domain = domain,
        senderEmail = senderEmail, lastSeen = lastSeen, price = price, currency = currency,
        billingCycle = billingCycle.name, priceFromEmail = priceFromEmail
    )
}
```

> **Nota:** la tabla `categoryKeyToId` se duplica con la de `GmailController`. Si prefieres DRY, extrae el `when` a `CatalogService.categoryKeyToId(categories)` en una tarea de refactor aparte; no lo hagas aquí para no ampliar el alcance.

- [ ] **Step 2: Confirmar que `GmailScanService.MAX_MONTHS` es público**

Run: `grep -n "MAX_MONTHS" src/main/kotlin/com/subia/service/GmailScanService.kt`
Expected: una constante en el `companion object` accesible (`const val MAX_MONTHS`). Si fuera `private`, quítale el `private`.

- [ ] **Step 3: Verificar compilación**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/subia/controller/api/ApiGmailController.kt
git commit -m "feat(backend): endpoints REST /api/gmail/scan/{ticket,results,add}"
```

---

### Task B4: Reglas de seguridad

**Files:**
- Modify: `src/main/kotlin/com/subia/config/SecurityConfig.kt`

- [ ] **Step 1: Permitir el callback en la cadena web**

En `webFilterChain`, dentro de la lista `.requestMatchers(...).permitAll()` (la de `/login`, `/register`, ...), añade `"/oauth/gmail/callback"` para que el callback funcione sin sesión web (la identidad la da el ticket):
```kotlin
auth.requestMatchers(
    "/login", "/register", "/verify-email", "/verify-pending", "/verify-email-error",
    "/forgot-password", "/reset-password", "/auth/google/callback",
    "/oauth/gmail/callback",
    "/delete-account", "/account-deleted",
    "/css/**", "/js/**", "/images/**", "/error"
).permitAll()
```

- [ ] **Step 2: Confirmar que `/api/gmail/**` queda protegido**

En `apiFilterChain`, la regla final `auth.anyRequest().authenticated()` ya cubre `/api/gmail/**` con JWT. No añadas `permitAll` para esas rutas. Verifica leyendo que no exista ningún `permitAll` que las afecte.

- [ ] **Step 3: Verificar compilación y arranque**

Run: `./gradlew compileKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/subia/config/SecurityConfig.kt
git commit -m "feat(backend): seguridad para callback Gmail (permitAll) y API protegida"
```

---

### Task B5: Test de integración de los endpoints REST

**Files:**
- Test: `src/test/kotlin/com/subia/controller/api/ApiGmailControllerTest.kt`

- [ ] **Step 1: Escribir el test del flujo de alta (con mocks de servicios)**

`src/test/kotlin/com/subia/controller/api/ApiGmailControllerTest.kt`:
```kotlin
package com.subia.controller.api

import com.subia.dto.api.GmailAddRequestDto
import com.subia.model.BillingCycle
import com.subia.model.Category
import com.subia.model.GmailScanResultRow
import com.subia.model.User
import com.subia.model.UserRole
import com.subia.repository.UserRepository
import com.subia.service.CategoryService
import com.subia.service.GmailScanService
import com.subia.service.GmailScanTicketService
import com.subia.service.SubscriptionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal

class ApiGmailControllerTest {

    private val gmailScanService = mockk<GmailScanService>(relaxed = true)
    private val ticketService = mockk<GmailScanTicketService>(relaxed = true)
    private val subscriptionService = mockk<SubscriptionService>(relaxed = true)
    private val categoryService = mockk<CategoryService>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val controller = ApiGmailController(
        gmailScanService, ticketService, subscriptionService, categoryService, userRepository
    )

    private fun jwt(email: String) = mockk<Jwt> { every { subject } returns email }

    private fun row(id: Long, key: String) = GmailScanResultRow(
        id = id, scanId = "s", userId = 7L, serviceName = "Netflix", description = "",
        domain = "netflix.com", senderEmail = "billing@netflix.com", lastSeen = "2026-05-01",
        price = BigDecimal("14.99"), currency = "EUR", billingCycle = BillingCycle.MONTHLY,
        priceFromEmail = true, categoryKey = key
    )

    @Test
    fun `add da de alta solo las detecciones con categoria mapeable`() {
        every { userRepository.findByEmail("a@b.com") } returns
            User(id = 7L, email = "a@b.com", passwordHash = "x", emailVerified = true, role = UserRole.USER)
        every { ticketService.consumeResults(7L, listOf(1L, 2L)) } returns
            listOf(row(1L, "streaming"), row(2L, "desconocida"))
        every { categoryService.findAll() } returns
            listOf(Category(id = 30L, name = "Streaming", color = "#000", icon = "📺"))
        every { categoryService.findById(30L) } returns
            Category(id = 30L, name = "Streaming", color = "#000", icon = "📺")

        val resp = controller.add(jwt("a@b.com"), GmailAddRequestDto(ids = listOf(1L, 2L)))

        assertEquals(1, resp.data?.added)
        verify(exactly = 1) { subscriptionService.save(any(), 7L) }
    }
}
```

> Ajusta el constructor de `Category` y `User` a sus firmas reales si difieren (el sondeo mostró `Category(id, name, color, icon)` y `User(id, email, passwordHash, emailVerified, role, ...)`).

- [ ] **Step 2: Ejecutar y verificar**

Run: `./gradlew test --tests "com.subia.controller.api.ApiGmailControllerTest"`
Expected: PASS. Si falla por firmas de `Category`/`User`, corrige los constructores y repite.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/com/subia/controller/api/ApiGmailControllerTest.kt
git commit -m "test(backend): alta de suscripciones detectadas vía API Gmail"
```

---

## FASE C — Android shared: datos y ViewModel

### Task C1: DTOs serializables

**Files:**
- Create: `mobile/shared/src/commonMain/kotlin/com/subia/shared/model/GmailScan.kt`

- [ ] **Step 1: Crear los modelos**

`mobile/shared/src/commonMain/kotlin/com/subia/shared/model/GmailScan.kt`:
```kotlin
package com.subia.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class GmailScanTicketResponse(val connectUrl: String)

@Serializable
data class GmailDetected(
    val id: Long,
    val serviceName: String,
    val description: String = "",
    val domain: String,
    val senderEmail: String,
    val lastSeen: String,
    val price: Double,
    val currency: String,
    val billingCycle: String,
    val priceFromEmail: Boolean
)

@Serializable
data class GmailAddRequest(val ids: List<Long>)

@Serializable
data class GmailAddResult(val added: Int)
```

- [ ] **Step 2: Commit**

```bash
git add mobile/shared/src/commonMain/kotlin/com/subia/shared/model/GmailScan.kt
git commit -m "feat(android): modelos serializables de escaneo Gmail"
```

---

### Task C2: Rutas API

**Files:**
- Modify: `mobile/shared/src/commonMain/kotlin/com/subia/shared/network/ApiRoutes.kt`

- [ ] **Step 1: Añadir las rutas**

Dentro de `object ApiRoutes`, tras el bloque de catálogo, añade:
```kotlin
    // Escaneo de Gmail
    const val GMAIL_SCAN_TICKET = "$BASE/gmail/scan/ticket"
    const val GMAIL_SCAN_RESULTS = "$BASE/gmail/scan/results"
    const val GMAIL_SCAN_ADD = "$BASE/gmail/scan/add"
    fun gmailScanTicket(months: Int) = "$GMAIL_SCAN_TICKET?months=$months"
```

- [ ] **Step 2: Commit**

```bash
git add mobile/shared/src/commonMain/kotlin/com/subia/shared/network/ApiRoutes.kt
git commit -m "feat(android): rutas API de escaneo Gmail"
```

---

### Task C3: Repositorio

**Files:**
- Create: `mobile/shared/src/commonMain/kotlin/com/subia/shared/repository/GmailScanRepository.kt`

- [ ] **Step 1: Crear el repositorio**

`mobile/shared/src/commonMain/kotlin/com/subia/shared/repository/GmailScanRepository.kt`:
```kotlin
package com.subia.shared.repository

import com.subia.shared.model.GmailAddRequest
import com.subia.shared.model.GmailAddResult
import com.subia.shared.model.GmailDetected
import com.subia.shared.model.GmailScanTicketResponse
import com.subia.shared.network.ApiClient
import com.subia.shared.network.ApiRoutes

/** Acceso a los endpoints REST de detección de suscripciones por Gmail. */
class GmailScanRepository(private val apiClient: ApiClient) {

    /** Pide el ticket y devuelve la URL de consentimiento a abrir en Custom Tab. */
    suspend fun requestTicket(months: Int): Result<GmailScanTicketResponse> =
        apiClient.post(ApiRoutes.gmailScanTicket(months), EmptyBody)

    suspend fun getResults(): Result<List<GmailDetected>> =
        apiClient.get(ApiRoutes.GMAIL_SCAN_RESULTS)

    suspend fun add(ids: List<Long>): Result<GmailAddResult> =
        apiClient.post(ApiRoutes.GMAIL_SCAN_ADD, GmailAddRequest(ids))
}

/** Cuerpo vacío para el POST del ticket (los datos van en el query param). */
@kotlinx.serialization.Serializable
private object EmptyBody
```

- [ ] **Step 2: Commit**

```bash
git add mobile/shared/src/commonMain/kotlin/com/subia/shared/repository/GmailScanRepository.kt
git commit -m "feat(android): repositorio de escaneo Gmail"
```

---

### Task C4: ViewModel (TDD)

**Files:**
- Create: `mobile/shared/src/commonMain/kotlin/com/subia/shared/viewmodel/GmailScanViewModel.kt`
- Test: `mobile/shared/src/commonTest/kotlin/com/subia/shared/GmailScanViewModelTest.kt`

- [ ] **Step 1: Escribir el test que falla**

`mobile/shared/src/commonTest/kotlin/com/subia/shared/GmailScanViewModelTest.kt`:
```kotlin
package com.subia.shared

import com.subia.shared.model.GmailAddResult
import com.subia.shared.model.GmailDetected
import com.subia.shared.viewmodel.GmailScanUiState
import com.subia.shared.viewmodel.GmailScanViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GmailScanViewModelTest {

    private fun detected(id: Long) = GmailDetected(
        id = id, serviceName = "Netflix", domain = "netflix.com",
        senderEmail = "b@netflix.com", lastSeen = "2026-05-01", price = 14.99,
        currency = "EUR", billingCycle = "MONTHLY", priceFromEmail = true
    )

    @Test
    fun `onReturn carga resultados y pasa a estado Results`() = runTest {
        val vm = GmailScanViewModel(FakeRepo(results = listOf(detected(1), detected(2))))
        vm.onReturnedFromConsent("ok")
        val state = vm.uiState.value
        assertTrue(state is GmailScanUiState.Results)
        assertEquals(2, (state as GmailScanUiState.Results).items.size)
        // por defecto todas seleccionadas
        assertEquals(setOf(1L, 2L), vm.selectedIds.value)
    }

    @Test
    fun `onReturn con status error pasa a Error`() = runTest {
        val vm = GmailScanViewModel(FakeRepo())
        vm.onReturnedFromConsent("error")
        assertTrue(vm.uiState.value is GmailScanUiState.Error)
    }

    @Test
    fun `onReturn sin resultados pasa a Empty`() = runTest {
        val vm = GmailScanViewModel(FakeRepo(results = emptyList()))
        vm.onReturnedFromConsent("ok")
        assertTrue(vm.uiState.value is GmailScanUiState.Empty)
    }

    @Test
    fun `toggle cambia la seleccion`() = runTest {
        val vm = GmailScanViewModel(FakeRepo(results = listOf(detected(1), detected(2))))
        vm.onReturnedFromConsent("ok")
        vm.toggle(1L)
        assertEquals(setOf(2L), vm.selectedIds.value)
    }

    @Test
    fun `addSelected da de alta y pasa a Done`() = runTest {
        val repo = FakeRepo(results = listOf(detected(1)), addResult = GmailAddResult(added = 1))
        val vm = GmailScanViewModel(repo)
        vm.onReturnedFromConsent("ok")
        vm.addSelected()
        val state = vm.uiState.value
        assertTrue(state is GmailScanUiState.Done)
        assertEquals(1, (state as GmailScanUiState.Done).added)
    }
}
```

`FakeRepo` (añádelo al final del archivo de test):
```kotlin
private class FakeRepo(
    private val results: List<GmailDetected> = emptyList(),
    private val ticketUrl: String = "https://accounts.google.com/o/oauth2/v2/auth?x=1",
    private val addResult: GmailAddResult = GmailAddResult(added = 0)
) : com.subia.shared.repository.GmailScanRepository(apiClient = throwingClient()) {
    override suspend fun requestTicket(months: Int) =
        Result.success(com.subia.shared.model.GmailScanTicketResponse(ticketUrl))
    override suspend fun getResults() = Result.success(results)
    override suspend fun add(ids: List<Long>) = Result.success(addResult)
}
```

> **Nota de testabilidad:** para poder subclasear el repo en el test, declara los métodos de `GmailScanRepository` como `open` y la clase como `open`, **o** (preferible) define una `interface GmailScanRepository` con una impl `GmailScanRepositoryImpl(apiClient)`. Elige interface si quieres evitar el `throwingClient()` hack. Si usas interface, en Koin registra `single<GmailScanRepository> { GmailScanRepositoryImpl(get()) }` y el FakeRepo implementa la interface directamente (sin constructor con apiClient). **Implementa la variante interface** — es más limpia; ajusta Task C3 para que `GmailScanRepository` sea `interface` + `GmailScanRepositoryImpl`.

- [ ] **Step 2: Ejecutar el test y verlo fallar**

Run: `cd mobile && ./gradlew :shared:allTests --tests "com.subia.shared.GmailScanViewModelTest"`
Expected: FAIL — `GmailScanViewModel` no existe.

- [ ] **Step 3: Implementar el ViewModel**

`mobile/shared/src/commonMain/kotlin/com/subia/shared/viewmodel/GmailScanViewModel.kt`:
```kotlin
package com.subia.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subia.shared.model.GmailDetected
import com.subia.shared.repository.GmailScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GmailScanUiState {
    data object Idle : GmailScanUiState
    /** Se ha pedido el ticket; la app debe abrir [connectUrl] en Custom Tab. */
    data class LaunchConsent(val connectUrl: String) : GmailScanUiState
    /** Custom Tab abierta; esperando el deep link de vuelta. */
    data object AwaitingReturn : GmailScanUiState
    data object LoadingResults : GmailScanUiState
    data class Results(val items: List<GmailDetected>) : GmailScanUiState
    data object Empty : GmailScanUiState
    data object Adding : GmailScanUiState
    data class Done(val added: Int) : GmailScanUiState
    data class Error(val message: String) : GmailScanUiState
}

class GmailScanViewModel(
    private val repository: GmailScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GmailScanUiState>(GmailScanUiState.Idle)
    val uiState: StateFlow<GmailScanUiState> = _uiState.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    /** Pide el ticket y emite LaunchConsent para que la UI abra la Custom Tab. */
    fun startScan(months: Int = 12) {
        viewModelScope.launch {
            repository.requestTicket(months)
                .onSuccess { _uiState.value = GmailScanUiState.LaunchConsent(it.connectUrl) }
                .onFailure { _uiState.value = GmailScanUiState.Error("No se pudo iniciar la conexión") }
        }
    }

    /** La UI llama esto cuando ha lanzado la Custom Tab, para mostrar "esperando". */
    fun onConsentLaunched() { _uiState.value = GmailScanUiState.AwaitingReturn }

    /** Llamado al volver por el deep link subia://gmail/done?status=... */
    fun onReturnedFromConsent(status: String?) {
        if (status == "error") {
            _uiState.value = GmailScanUiState.Error("No se completó la conexión con Gmail")
            return
        }
        viewModelScope.launch {
            _uiState.value = GmailScanUiState.LoadingResults
            repository.getResults()
                .onSuccess { items ->
                    if (items.isEmpty()) {
                        _uiState.value = GmailScanUiState.Empty
                    } else {
                        _selectedIds.value = items.map { it.id }.toSet()
                        _uiState.value = GmailScanUiState.Results(items)
                    }
                }
                .onFailure { _uiState.value = GmailScanUiState.Error("No se pudieron cargar los resultados") }
        }
    }

    fun toggle(id: Long) {
        _selectedIds.value = _selectedIds.value.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    fun addSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = GmailScanUiState.Adding
            repository.add(ids)
                .onSuccess { _uiState.value = GmailScanUiState.Done(it.added) }
                .onFailure { _uiState.value = GmailScanUiState.Error("No se pudieron añadir") }
        }
    }
}
```

- [ ] **Step 4: Ejecutar el test y verlo pasar**

Run: `cd mobile && ./gradlew :shared:allTests --tests "com.subia.shared.GmailScanViewModelTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add mobile/shared/src/commonMain/kotlin/com/subia/shared/viewmodel/GmailScanViewModel.kt mobile/shared/src/commonTest/kotlin/com/subia/shared/GmailScanViewModelTest.kt
git commit -m "feat(android): GmailScanViewModel con estados de escaneo (TDD)"
```

---

### Task C5: Registro en Koin

**Files:**
- Modify: `mobile/shared/src/commonMain/kotlin/com/subia/shared/di/SharedModule.kt`

- [ ] **Step 1: Registrar repo y viewmodel**

En `sharedModule`, junto a los demás `single`/`viewModel`, añade (usa la variante interface decidida en C4):
```kotlin
    single<GmailScanRepository> { GmailScanRepositoryImpl(get()) }
    viewModel { GmailScanViewModel(get()) }
```
Añade los imports correspondientes al principio del archivo.

- [ ] **Step 2: Verificar compilación del shared**

Run: `cd mobile && ./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add mobile/shared/src/commonMain/kotlin/com/subia/shared/di/SharedModule.kt
git commit -m "feat(android): registro Koin de GmailScan"
```

---

## FASE D — Android app: deep link, Custom Tab y UI

### Task D1: Dependencia Custom Tabs y helper

**Files:**
- Modify: `mobile/androidApp/build.gradle.kts`
- Create: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/util/CustomTabsHelper.kt`

- [ ] **Step 1: Añadir la dependencia**

En `androidMain.dependencies { ... }` de `mobile/androidApp/build.gradle.kts`, añade:
```kotlin
            implementation("androidx.browser:browser:1.7.0")
```

- [ ] **Step 2: Crear el helper**

`mobile/androidApp/src/androidMain/kotlin/com/subia/android/util/CustomTabsHelper.kt`:
```kotlin
package com.subia.android.util

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/** Abre una URL en una pestaña de Chrome Custom Tabs (mejor UX que el navegador externo). */
fun openCustomTab(context: Context, url: String) {
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
        .launchUrl(context, url.toUri())
}
```

- [ ] **Step 3: Verificar que sincroniza/compila**

Run: `cd mobile && ./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add mobile/androidApp/build.gradle.kts mobile/androidApp/src/androidMain/kotlin/com/subia/android/util/CustomTabsHelper.kt
git commit -m "feat(android): Custom Tabs (androidx.browser) + helper"
```

---

### Task D2: Deep link en el manifest y captura en MainActivity

**Files:**
- Modify: `mobile/androidApp/src/androidMain/AndroidManifest.xml`
- Modify: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/MainActivity.kt`

- [ ] **Step 1: Añadir el intent-filter del deep link**

Dentro del `<activity android:name=".MainActivity" ...>`, tras el intent-filter MAIN/LAUNCHER, añade:
```xml
        <intent-filter android:autoVerify="false">
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="subia" android:host="gmail" />
        </intent-filter>
```

- [ ] **Step 2: Capturar el deep link y exponerlo a Compose**

En `MainActivity`, añade un `StateFlow` que emita el estado del deep link y trátalo en `onCreate`/`onNewIntent`:
```kotlin
    // Campo de la clase
    private val gmailReturnStatus = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleGmailDeepLink(intent)
    }

    private fun handleGmailDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "subia" && uri.host == "gmail") {
            gmailReturnStatus.value = uri.getQueryParameter("status") ?: "ok"
        }
    }
```
En `onCreate`, tras `setContent { ... }` no; **antes** de construir el árbol, llama `handleGmailDeepLink(intent)`. Dentro del `setContent`, recoge el flujo y pásalo a `SubIAApp`:
```kotlin
            val gmailStatus by gmailReturnStatus.collectAsState()
            SubIAApp(
                navController = navController,
                startDestination = startDestination,
                authViewModel = authViewModel,
                gmailReturnStatus = gmailStatus,
                onGmailReturnConsumed = { gmailReturnStatus.value = null }
            )
```
Añade los imports: `import kotlinx.coroutines.flow.MutableStateFlow`, `import androidx.compose.runtime.collectAsState`, `import androidx.compose.runtime.getValue`.

- [ ] **Step 3: Verificar compilación**

Run: `cd mobile && ./gradlew :androidApp:compileDebugKotlin`
Expected: FAIL esperado solo si `SubIAApp` aún no acepta los parámetros nuevos — se añaden en Task D3. Si falla por eso, continúa con D3 y vuelve a compilar al final de D3.

- [ ] **Step 4: Commit**

```bash
git add mobile/androidApp/src/androidMain/AndroidManifest.xml mobile/androidApp/src/androidMain/kotlin/com/subia/android/MainActivity.kt
git commit -m "feat(android): deep link subia://gmail/done capturado en MainActivity"
```

---

### Task D3: Pantalla de escaneo y ruta

**Files:**
- Modify: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/navigation/AppNavGraph.kt`
- Create: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/screens/GmailScanScreen.kt`
- Modify: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/SubIAApp.kt`

- [ ] **Step 1: Declarar la ruta**

En `AppNavGraph.kt`, junto a las demás rutas `@Serializable`, añade:
```kotlin
@Serializable object GmailScanRoute
```

- [ ] **Step 2: Crear la pantalla**

`mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/screens/GmailScanScreen.kt`:
```kotlin
package com.subia.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.subia.android.util.openCustomTab
import com.subia.shared.viewmodel.GmailScanUiState
import com.subia.shared.viewmodel.GmailScanViewModel

/**
 * Pantalla de detección por Gmail. Abre el consentimiento en Custom Tab y, al volver por
 * deep link, muestra la lista seleccionable de suscripciones detectadas.
 *
 * @param returnStatus estado del deep link de vuelta ("ok"/"error"), o null si no se ha vuelto.
 * @param onReturnConsumed limpia el estado del deep link tras procesarlo.
 */
@Composable
fun GmailScanScreen(
    viewModel: GmailScanViewModel,
    returnStatus: String?,
    onReturnConsumed: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val selected by viewModel.selectedIds.collectAsState()

    // Cuando el VM pide abrir el consentimiento, lanzamos la Custom Tab una vez.
    LaunchedEffect(state) {
        val s = state
        if (s is GmailScanUiState.LaunchConsent) {
            openCustomTab(context, s.connectUrl)
            viewModel.onConsentLaunched()
        }
    }

    // Cuando volvemos por el deep link, avisamos al VM y consumimos el estado.
    LaunchedEffect(returnStatus) {
        if (returnStatus != null) {
            viewModel.onReturnedFromConsent(returnStatus)
            onReturnConsumed()
        }
    }

    // Al terminar el alta, salimos.
    LaunchedEffect(state) {
        if (state is GmailScanUiState.Done) onDone()
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        when (val s = state) {
            is GmailScanUiState.Idle -> {
                Text("Detecta tus suscripciones", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text("Conecta tu Gmail y buscaremos recibos de suscripciones para que no tengas que añadirlas a mano. Solo leemos; no guardamos tus correos.")
                Spacer(Modifier.height(24.dp))
                Button(onClick = { viewModel.startScan() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Conectar con Gmail")
                }
            }
            is GmailScanUiState.LaunchConsent,
            is GmailScanUiState.AwaitingReturn -> CenterProgress("Esperando la autorización…") {
                Button(onClick = { viewModel.onReturnedFromConsent("ok") }) { Text("Ya autoricé") }
            }
            is GmailScanUiState.LoadingResults -> CenterProgress("Buscando suscripciones…")
            is GmailScanUiState.Results -> {
                Text("Hemos encontrado ${s.items.size}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(s.items, key = { it.id }) { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Checkbox(checked = item.id in selected, onCheckedChange = { viewModel.toggle(item.id) })
                            Spacer(Modifier.height(0.dp))
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(item.serviceName, style = MaterialTheme.typography.titleMedium)
                                Text("${item.price} ${item.currency} · ${cycleLabel(item.billingCycle)} · visto ${item.lastSeen}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Button(onClick = { viewModel.addSelected() }, enabled = selected.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Añadir ${selected.size} seleccionadas")
                }
            }
            is GmailScanUiState.Empty -> CenterMessage("No encontramos suscripciones en tu correo.", onBack)
            is GmailScanUiState.Adding -> CenterProgress("Añadiendo…")
            is GmailScanUiState.Done -> CenterProgress("Listo")
            is GmailScanUiState.Error -> CenterMessage(s.message, onBack) {
                Button(onClick = { viewModel.startScan() }) { Text("Reintentar") }
            }
        }
    }
}

private fun cycleLabel(cycle: String) = when (cycle) {
    "YEARLY" -> "anual"; "WEEKLY" -> "semanal"; else -> "mensual"
}

@Composable
private fun CenterProgress(text: String, extra: @Composable (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(text)
        if (extra != null) { Spacer(Modifier.height(16.dp)); extra() }
    }
}

@Composable
private fun CenterMessage(text: String, onBack: () -> Unit, extra: @Composable (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        if (extra != null) { extra(); Spacer(Modifier.height(8.dp)) }
        Button(onClick = onBack) { Text("Volver") }
    }
}
```

> Los textos van inline para no bloquear; en una tarea de pulido posterior pásalos a `strings.xml` (ES/EN). No amplíes el alcance aquí.

- [ ] **Step 3: Registrar la ruta en el NavHost y pasar el estado del deep link**

En `SubIAApp.kt`, primero amplía la firma de `SubIAApp` para aceptar los parámetros nuevos:
```kotlin
fun SubIAApp(
    navController: NavHostController,
    startDestination: Any,
    authViewModel: AuthViewModel,
    gmailReturnStatus: String? = null,
    onGmailReturnConsumed: () -> Unit = {}
) {
```
Dentro del `NavHost`, añade:
```kotlin
        composable<GmailScanRoute> {
            val vm: GmailScanViewModel = koinViewModel()
            GmailScanScreen(
                viewModel = vm,
                returnStatus = gmailReturnStatus,
                onReturnConsumed = onGmailReturnConsumed,
                onDone = {
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
```
Añade imports de `GmailScanViewModel`, `GmailScanScreen`, `GmailScanRoute` y `koinViewModel` si falta.

- [ ] **Step 4: Compilar el módulo Android (cierra D2 y D3)**

Run: `cd mobile && ./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/screens/GmailScanScreen.kt mobile/androidApp/src/androidMain/kotlin/com/subia/android/navigation/AppNavGraph.kt mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/SubIAApp.kt
git commit -m "feat(android): pantalla de detección por Gmail y ruta"
```

---

### Task D4: Entrada permanente en Ajustes

**Files:**
- Modify: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/screens/SettingsScreen.kt`
- Modify: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/SubIAApp.kt` (paso del callback de navegación a Settings)

- [ ] **Step 1: Añadir un parámetro de navegación a SettingsScreen**

Amplía la firma de `SettingsScreen` con `onDetectGmail: () -> Unit = {}` y, tras la sección "Datos", añade:
```kotlin
            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))
            Text("Detección automática", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Detecta tus suscripciones desde Gmail", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDetectGmail, modifier = Modifier.fillMaxWidth()) {
                Text("Detectar suscripciones con Gmail")
            }
```

- [ ] **Step 2: Conectar la navegación en SubIAApp**

En el `composable<SettingsRoute>` de `SubIAApp.kt`, pasa el callback:
```kotlin
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onDetectGmail = { navController.navigate(GmailScanRoute) }
            )
        }
```

- [ ] **Step 3: Compilar**

Run: `cd mobile && ./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/screens/SettingsScreen.kt mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/SubIAApp.kt
git commit -m "feat(android): entrada a detección Gmail desde Ajustes"
```

---

### Task D5: Paso de onboarding

**Files:**
- Modify: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/screens/OnboardingScreen.kt`
- Modify: `mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/SubIAApp.kt`
- Modify: `mobile/androidApp/src/androidMain/res/values/strings.xml`, `.../res/values-en/strings.xml`

- [ ] **Step 1: Añadir strings**

En `values/strings.xml`:
```xml
    <string name="onb4_title">Detecta tus suscripciones</string>
    <string name="onb4_desc">Conecta tu Gmail y las añadimos por ti, sin escribir nada.</string>
    <string name="onb4_action">Detectar con Gmail</string>
    <string name="onb4_skip">Ahora no</string>
```
En `values-en/strings.xml`:
```xml
    <string name="onb4_title">Detect your subscriptions</string>
    <string name="onb4_desc">Connect your Gmail and we\'ll add them for you, no typing.</string>
    <string name="onb4_action">Detect with Gmail</string>
    <string name="onb4_skip">Not now</string>
```

- [ ] **Step 2: Añadir la 4ª página con acción**

En `OnboardingScreen`, amplía la firma con `onDetectGmail: () -> Unit` y añade la 4ª página a la lista. Como esta página tiene un botón de acción propio (no solo "siguiente"), trátala como última y, cuando `esUltima`, muestra dos botones: el de acción (que llama `onDetectGmail` tras marcar el onboarding como completado) y el "Ahora no" (que llama `onFinish`). Implementación mínima dentro del `Column` final, sustituyendo el botón único por:
```kotlin
        if (esUltima) {
            Button(onClick = { onDetectGmail() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onb4_action))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onb4_skip))
            }
        } else {
            Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onb_next))
            }
        }
```
Y añade la página a la lista `paginas`:
```kotlin
        OnbPagina(Icons.Default.Mail, R.string.onb4_title, R.string.onb4_desc)
```

- [ ] **Step 3: Conectar el onboarding con la navegación**

En `SubIAApp.kt` (o donde se muestre `OnboardingRoute`, según el sondeo está la decisión en `SubIAApp.kt:228`), pasa el callback de modo que al pulsar "Detectar con Gmail" se marque el onboarding completado y se navegue a `GmailScanRoute`:
```kotlin
        composable<OnboardingRoute> {
            OnboardingScreen(
                onFinish = { /* marcar completado + navegar a Dashboard, como ya hace */ },
                onDetectGmail = {
                    // marcar onboarding completado (misma lógica que onFinish) y luego:
                    navController.navigate(GmailScanRoute)
                }
            )
        }
```
Reutiliza la función existente que marca `OnboardingPrefs.setCompleted(...)` para ambos caminos (no dupliques la lógica de persistencia del flag).

- [ ] **Step 4: Compilar**

Run: `cd mobile && ./gradlew :androidApp:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/screens/OnboardingScreen.kt mobile/androidApp/src/androidMain/kotlin/com/subia/android/ui/SubIAApp.kt mobile/androidApp/src/androidMain/res/values/strings.xml mobile/androidApp/src/androidMain/res/values-en/strings.xml
git commit -m "feat(android): paso de onboarding para detección por Gmail"
```

---

## FASE E — Verificación de extremo a extremo

### Task E1: Build completo y suite de tests

- [ ] **Step 1: Tests backend**

Run: `./gradlew test` (en la raíz del backend)
Expected: PASS, incluyendo `GmailScanServiceTest`, `GmailScanTicketServiceTest`, `ApiGmailControllerTest`.

- [ ] **Step 2: Tests y build Android**

Run: `cd mobile && ./gradlew :shared:allTests && ./gradlew :androidApp:assembleDebug`
Expected: PASS y APK debug generado.

- [ ] **Step 3: Prueba manual con un test user**

Requisitos: backend corriendo accesible desde el dispositivo/emulador; el `redirect_uri` de Google (`{baseUrl}/oauth/gmail/callback`) registrado en la consola de Google; el `GOOGLE_CLIENT_SECRET` configurado en el backend; tu cuenta añadida como **test user** en la pantalla de consentimiento (mientras no esté verificada).

Pasos:
1. Login en la app.
2. Ajustes → "Detectar suscripciones con Gmail".
3. Se abre la Custom Tab → consiente con una cuenta con recibos de suscripción.
4. Vuelve solo a la app por el deep link.
5. Revisa la lista, marca algunas, "Añadir".
6. Comprueba en la lista de suscripciones que aparecen las añadidas.

Expected: el flujo completa sin intervención manual de "Ya autoricé" en el caso normal; las suscripciones marcadas se crean.

- [ ] **Step 4: Commit (si hubo ajustes) y cierre**

```bash
git add -A
git commit -m "test(e2e): verificación del flujo de detección Gmail en Android"
```

---

## Notas de despliegue (fuera del código)

- **Google Cloud Console:** registrar el `redirect_uri` `{baseUrl}/oauth/gmail/callback` (ya usado por la web), mantener el scope `gmail.readonly`, y arrancar la verificación del scope restringido (proceso de semanas; ver el spec, sección 10). El mismo proyecto/consent screen cubre web y Android.
- **`GOOGLE_CLIENT_SECRET`** debe estar configurado en el backend (Render) para que `exchangeCodeForToken` funcione.
- **Deep link:** `subia://gmail/done` no requiere App Links verificados (no es `https`), por eso `autoVerify="false"`.

---

## Self-Review (hecho al escribir el plan)

- **Cobertura del spec:** ticket+identidad (A1,A4,B2) ✓ · persistencia temporal por usuario (A2,A3) ✓ · endpoints REST ticket/results/add (B1,B3) ✓ · seguridad callback/API (B4) ✓ · Custom Tab (D1) ✓ · deep link (D2) ✓ · pantalla revisión seleccionable (D3) ✓ · onboarding (D5) ✓ · ajustes (D4) ✓ · manejo de errores/vacío (C4 estados + D3 UI) ✓ · privacidad: scope intacto, purga en `consumeResults`/`replaceResults` ✓ · tests backend y ViewModel ✓.
- **Sin placeholders:** todos los pasos de código llevan el código real; los textos UI inline se marcan explícitamente como "pulir a strings después" para no fingir cobertura.
- **Consistencia de tipos:** `scanId==ticketId` (String) coherente en A1/A2/B2; `GmailDetected.id`(Long) ↔ `GmailScanResultRow.id` ↔ selección por ids en C4/B3; `billingCycle` viaja como String en DTOs Android y como enum `BillingCycle` en backend (conversión por `.name`).
- **Riesgo señalado:** duplicación de `categoryKeyToId` entre `GmailController` y `ApiGmailController` — marcada como refactor opcional posterior, no se acomete aquí (YAGNI/alcance).
