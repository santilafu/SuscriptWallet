# Suppress warnings for missing error-prone annotations (used by Tink/crypto libs)
-dontwarn com.google.errorprone.annotations.**

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named).
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes used for serialization
-keep class com.subia.shared.model.** { *; }
-keep class com.subia.shared.dto.** { *; }
-keepclassmembers class com.subia.shared.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-dontwarn org.slf4j.**
-keep class io.ktor.** { *; }

# Clases JVM no disponibles en Android (usadas por Ktor debug detector)
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
