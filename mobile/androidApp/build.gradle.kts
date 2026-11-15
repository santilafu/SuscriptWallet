import com.android.build.api.artifact.MultipleArtifact
import com.android.build.api.artifact.SingleArtifact
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}
// `-Psubia.demo=true` (solo para capturas de Play): backend local, sin anuncios y otro
// applicationId para poder instalarla junto a la app real.
val demo: Boolean = providers.gradleProperty("subia.demo").orNull == "true"
val apiBaseUrl: String = if (demo) "http://10.0.2.2:8081" else localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8081")
val googleWebClientId: String = localProps.getProperty("SUBIA_GOOGLE_WEB_CLIENT_ID", "")
val keystorePath: String = localProps.getProperty("KEYSTORE_PATH", "")
val keystorePass: String = localProps.getProperty("KEYSTORE_PASSWORD", "")
val releaseKeyAlias: String = localProps.getProperty("KEY_ALIAS", "")
val keyPass: String = localProps.getProperty("KEY_PASSWORD", "")

android {
    namespace = "com.subia.android"
    compileSdk = libs.versions.compileSdk.get().toInt()
    // NDK instalado en el SDK. Sin fijarlo, AGP 8.13 busca su versión por defecto (27.0.12077973),
    // que no está, y no puede ni hacer strip de las .so ni extraer sus símbolos.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.subia.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 38
        versionName = "2.18.1"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "SUBIA_GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("boolean", "DEMO", demo.toString())
        if (demo) applicationIdSuffix = ".demo"
    }

    signingConfigs {
        create("release") {
            if (keystorePath.isNotEmpty()) {
                storeFile = file(keystorePath)
                storePassword = keystorePass
                keyAlias = releaseKeyAlias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Reducción de recursos (recomendación de Play "R8: resource shrinking"): quita los
            // recursos que nadie referencia. La app no carga recursos por nombre (getIdentifier);
            // si algún día lo hace, protegerlos en res/raw/keep.xml con tools:keep.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Símbolos nativos dentro del AAB (BUNDLE-METADATA/com.android.tools.build.debugsymbols)
            // para que Play no avise de que faltan. SYMBOL_TABLE y no FULL: la única .so de la app
            // viene ya sin DWARF, así que solo hay tabla de símbolos que aportar (ver más abajo).
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    // El idioma se elige dentro de la app (Ajustes > Idioma). Si el AAB se parte por idioma,
    // Play solo instala los recursos de los idiomas del sistema del móvil: en un teléfono en
    // español, elegir English/Français/Português no cambiaba nada porque values-en/-fr/-pt no
    // estaban instalados. Son unos pocos KB de strings, así que van todos en el APK base.
    bundle {
        language {
            enableSplit = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // La demo habla con el backend local en claro (http://10.0.2.2:8081). El motor HTTP respeta la
    // NetworkSecurityPolicy, y la release de producción la prohíbe, así que solo la demo recibe una
    // network_security_config.xml que permite cleartext hacia 10.0.2.2 (y nada más). El directorio
    // se añade al source set release únicamente con -Psubia.demo=true: en la release normal no existe.
    if (demo) {
        sourceSets.getByName("release").res.srcDir("src/androidDemo/res")
    }
}

/**
 * Tabla de símbolos de las .so que llegan ya "stripped" desde las dependencias.
 *
 * AGP solo extrae símbolos de una .so si el strip la encoge; si ya viene sin `.symtab` ni
 * `.debug_*` la descarta ("the native debug metadata has already been stripped") y el AAB
 * sale sin símbolos aunque `debugSymbolLevel` esté puesto. Es lo que pasa con la única .so de
 * la app, libandroidx.graphics.path.so (androidx.graphics:graphics-path, vía Compose), y por
 * eso Play avisaba. Para esas .so la tabla de símbolos es la propia .so (solo le queda
 * `.dynsym`), que es lo mismo que daría `objcopy --strip-debug`: se publica como
 * `<abi>/<lib>.so.sym` en el artefacto que AGP mete en el AAB y en native-debug-symbols.zip.
 * Las .so que sí traen símbolos se dejan a AGP, para no duplicar entradas.
 */
abstract class TablaSimbolosSoPrecompiladasTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val librerias: DirectoryProperty

    @get:OutputDirectory
    abstract val salida: DirectoryProperty

    @TaskAction
    fun generar() {
        val destino = salida.get().asFile
        destino.deleteRecursively()
        destino.mkdirs()
        librerias.get().asFile.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".so") && !traeSimbolos(it) }
            .forEach { so -> so.copyTo(File(destino, "${so.parentFile.name}/${so.name}.sym")) }
    }

    /** true si el ELF tiene `.symtab` o `.debug_*` (entonces AGP sí extrae sus símbolos). */
    private fun traeSimbolos(so: File): Boolean {
        val b = ByteBuffer.wrap(so.readBytes())
        if (b.limit() < 0x40 || b.getInt(0) != 0x7F454C46) return true // no es ELF: que decida AGP
        val es64 = b.get(4).toInt() == 2
        b.order(if (b.get(5).toInt() == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
        val inicioSecciones = if (es64) b.getLong(0x28).toInt() else b.getInt(0x20)
        val tamSeccion = b.getShort(if (es64) 0x3A else 0x2E).toInt() and 0xFFFF
        val numSecciones = b.getShort(if (es64) 0x3C else 0x30).toInt() and 0xFFFF
        val indiceNombres = b.getShort(if (es64) 0x3E else 0x32).toInt() and 0xFFFF
        val cabeceraNombres = inicioSecciones + indiceNombres * tamSeccion
        val tablaNombres = if (es64) b.getLong(cabeceraNombres + 0x18).toInt() else b.getInt(cabeceraNombres + 0x10)
        return (0 until numSecciones).any { i ->
            var p = tablaNombres + b.getInt(inicioSecciones + i * tamSeccion)
            val nombre = StringBuilder()
            while (b.get(p).toInt() != 0) nombre.append(b.get(p++).toInt().toChar())
            nombre.toString().let { it == ".symtab" || it.startsWith(".debug") || it.startsWith(".zdebug") }
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val nombreVariante = variant.name.replaceFirstChar { it.uppercase() }
        val tarea = tasks.register<TablaSimbolosSoPrecompiladasTask>("tablaSimbolosSoPrecompiladas$nombreVariante") {
            librerias.set(variant.artifacts.get(SingleArtifact.MERGED_NATIVE_LIBS))
        }
        variant.artifacts.use(tarea)
            .wiredWith(TablaSimbolosSoPrecompiladasTask::salida)
            .toAppendTo(MultipleArtifact.NATIVE_SYMBOL_TABLES)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

kotlin {
    androidTarget()
    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.androidx.work.runtime.ktx)
            // Splash screen de Android 12+ con compat para APIs anteriores (sin flash de fondo)
            implementation(libs.androidx.core.splashscreen)
            // play-services-ads 25.2.0: la última que compila con Kotlin 2.1.20 (25.3+ trae metadatos de Kotlin 2.3).
            // Sigue tocando layoutInDisplayCutoutMode en su AdActivity (aviso de Play de APIs
            // obsoletas de edge-to-edge): es código del SDK de AdMob, no evitable desde la app.
            implementation(libs.play.services.ads)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.androidx.appcompat)
            // Widget de pantalla de inicio (Jetpack Glance)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)
            // Chrome Custom Tabs para el consentimiento OAuth de Gmail
            implementation(libs.androidx.browser)
        }
    }
}
