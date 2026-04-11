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
val apiBaseUrl: String = localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8081")
val googleWebClientId: String = localProps.getProperty("SUBIA_GOOGLE_WEB_CLIENT_ID", "")
val keystorePath: String = localProps.getProperty("KEYSTORE_PATH", "")
val keystorePass: String = localProps.getProperty("KEYSTORE_PASSWORD", "")
val releaseKeyAlias: String = localProps.getProperty("KEY_ALIAS", "")
val keyPass: String = localProps.getProperty("KEY_PASSWORD", "")

android {
    namespace = "com.subia.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.subia.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 13
        versionName = "2.12.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "SUBIA_GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
            implementation("com.google.android.gms:play-services-ads:23.6.0")
            implementation("androidx.credentials:credentials:1.3.0")
            implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
            implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
        }
    }
}
