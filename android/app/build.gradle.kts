import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// URL base de la API: api.baseUrl en local.properties (no se versiona) o la del emulador
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val apiBaseUrl: String = (localProps.getProperty("api.baseUrl")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "http://10.0.2.2/CuidaDosis/api/public/")
    .let { if (it.endsWith("/")) it else "$it/" }

// Release (beta): servidor fijo en el hosting, solo HTTPS y sin opción de cambiarlo en la app
val releaseApiBaseUrl = "https://areproyectos.com.mx/cuidadosis/api/"

// Firma del release: .secrets/release-signing.properties (no se versiona). Sin ese archivo
// el release se compila sin firmar.
val signingProps = Properties().apply {
    val file = rootProject.file("../.secrets/release-signing.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseSigning = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !signingProps.getProperty(it).isNullOrBlank() }

android {
    namespace = "mx.cuidadosis.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "mx.cuidadosis.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file("../.secrets/" + signingProps.getProperty("storeFile"))
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Pruebas locales: XAMPP (emulador o PC en la red) y enlace "Servidor" para cambiarla
            buildConfigField("String", "DEFAULT_API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("boolean", "ALLOW_SERVER_CHANGE", "true")
        }
        release {
            buildConfigField("String", "DEFAULT_API_BASE_URL", "\"$releaseApiBaseUrl\"")
            buildConfigField("boolean", "ALLOW_SERVER_CHANGE", "false")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8 desactivado (D49): con packageScope parcial, la 1.1.2 se cerraba al abrir
            // (IllegalAccessError en kotlin.sequences). El release usa el mismo código que debug.
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
