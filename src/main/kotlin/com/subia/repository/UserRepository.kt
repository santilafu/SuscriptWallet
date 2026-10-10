package com.subia.repository

import com.subia.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface UserRepository : JpaRepository<User, Long> {
    /** Coincidencia EXACTA. Para buscar lo que escribe el usuario, usar [findByEmailAnyCase]. */
    fun findByEmail(email: String): User?
    fun findByEmailVerificationToken(token: String): User?
    fun existsByEmail(email: String): Boolean
    fun findByGoogleId(googleId: String): User?

    /** Sin distinguir mayúsculas; si hubiera varias (cuentas antiguas), la más antigua. */
    fun findFirstByEmailIgnoreCaseOrderByIdAsc(email: String): User?

    // ── Escrituras por columna (auditoría ronda 5) ──────────────────────────────────────────
    // Antes se hacía `findByEmail` → `save(user.copy(...))`: con peticiones concurrentes se perdían
    // incrementos del contador (el bloqueo se esquivaba con ráfagas) y, como `save` reescribe la
    // fila entera, un login fallido podía pisar un passwordHash / emailVerified recién cambiado.
    // Estos UPDATE solo tocan sus columnas y son atómicos en PostgreSQL (bloqueo de fila).
    // `flushAutomatically` vuelca antes lo pendiente; `clearAutomatically` vacía la caché de
    // primer nivel para que nadie lea después una entidad User obsoleta en la misma transacción.
    // Requieren transacción: se llaman desde métodos @Transactional de UserService.

    /** Suma un intento fallido. Devuelve las filas afectadas (0 si la cuenta no existe). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.id = :id")
    fun incrementFailedAttempts(@Param("id") id: Long): Int

    /** Lectura directa del contador (tras el UPDATE, la fila está bloqueada por esta transacción). */
    @Query("SELECT u.failedAttempts FROM User u WHERE u.id = :id")
    fun findFailedAttempts(@Param("id") id: Long): Int?

    /** Fija el fin del bloqueo sin acortar nunca uno más largo ya guardado. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.lockedUntil = :until WHERE u.id = :id AND (u.lockedUntil IS NULL OR u.lockedUntil < :until)")
    fun extendLock(@Param("id") id: Long, @Param("until") until: OffsetDateTime): Int

    /** Reinicia contador y bloqueo (solo escribe si hay algo que limpiar). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.failedAttempts = 0, u.lockedUntil = NULL WHERE u.id = :id AND (u.failedAttempts <> 0 OR u.lockedUntil IS NOT NULL)")
    fun resetFailedAttempts(@Param("id") id: Long): Int

    /** Verifica el email consumiendo el token (0 si otro uso concurrente se adelantó). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "UPDATE User u SET u.emailVerified = true, u.emailVerificationToken = NULL, u.emailVerificationExpiresAt = NULL " +
            "WHERE u.id = :id AND u.emailVerificationToken = :token"
    )
    fun markEmailVerified(@Param("id") id: Long, @Param("token") token: String): Int

    /**
     * Anula el enlace de verificación pendiente (ronda 6). Al repetirse el registro de una cuenta sin
     * verificar ya no se reenvía la verificación (dejaba la cuenta con la contraseña de quien la
     * registró primero): se envía un enlace para fijar la contraseña y el anterior deja de valer.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.emailVerificationToken = NULL, u.emailVerificationExpiresAt = NULL WHERE u.id = :id")
    fun clearVerificationToken(@Param("id") id: Long): Int

    /**
     * Alta de un usuario de Google sin abortar la transacción si otro alta simultáneo del mismo
     * usuario se adelanta (ronda 6): `ON CONFLICT DO NOTHING` espera a su commit y devuelve 0.
     * El resto de columnas toman los DEFAULT de V6 (rol USER, 0 intentos, created_at = now()).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = "INSERT INTO users (email, google_id, email_verified) VALUES (:email, :googleId, TRUE) ON CONFLICT DO NOTHING",
        nativeQuery = true
    )
    fun insertGoogleUserIfAbsent(@Param("email") email: String, @Param("googleId") googleId: String): Int

    /**
     * Contraseña nueva tras un restablecimiento: el enlace llegó al buzón, así que además se
     * verifica el email y se levanta el bloqueo por intentos.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "UPDATE User u SET u.passwordHash = :hash, u.emailVerified = true, u.emailVerificationToken = NULL, " +
            "u.emailVerificationExpiresAt = NULL, u.failedAttempts = 0, u.lockedUntil = NULL WHERE u.id = :id"
    )
    fun resetPasswordAndUnlock(@Param("id") id: Long, @Param("hash") hash: String): Int

    /** Vincula Google a una cuenta ya verificada (no toca la contraseña). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.googleId = :googleId WHERE u.id = :id")
    fun linkGoogle(@Param("id") id: Long, @Param("googleId") googleId: String): Int

    /**
     * Vincula Google a una cuenta SIN verificar: la verifica y descarta su contraseña previa
     * (anti pre-hijacking, ver UserService.findOrCreateByGoogle).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        "UPDATE User u SET u.googleId = :googleId, u.emailVerified = true, u.emailVerificationToken = NULL, " +
            "u.emailVerificationExpiresAt = NULL, u.passwordHash = NULL WHERE u.id = :id"
    )
    fun linkGoogleAndVerify(@Param("id") id: Long, @Param("googleId") googleId: String): Int
}

/**
 * Busca la cuenta por el email que escribe el usuario, tolerando espacios y mayúsculas.
 *
 * Las cuentas nuevas se guardan en minúsculas, pero las antiguas pueden tener mayúsculas
 * (`Juan@Gmail.com`). Antes solo se probaba "tal cual" y "en minúsculas": quien escribía
 * `juan@gmail.com` no encontraba su cuenta antigua y el registro / Google creaban una DUPLICADA
 * (auditoría ronda 4). Orden: exacta (usa el índice único) → sin distinguir mayúsculas.
 */
fun UserRepository.findByEmailAnyCase(email: String): User? {
    val trimmed = email.trim()
    if (trimmed.isEmpty()) return null
    return findByEmail(trimmed) ?: findFirstByEmailIgnoreCaseOrderByIdAsc(trimmed)
}
