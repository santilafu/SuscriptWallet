# Suppress warnings for missing error-prone annotations (used by Tink/crypto libs)
-dontwarn com.google.errorprone.annotations.**

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }

# Keep data classes used for serialization
-keep class com.subia.shared.model.** { *; }
-keep class com.subia.shared.dto.** { *; }

# Ktor
-dontwarn org.slf4j.**
-keep class io.ktor.** { *; }

# Clases JVM no disponibles en Android (usadas por Ktor debug detector)
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
