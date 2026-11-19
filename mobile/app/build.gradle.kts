import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ID de cliente OAuth (tipo «Aplicación web») para «Continuar con Google». Va en mobile/local.properties como
// google.client.id=...apps.googleusercontent.com (ese archivo no se sube al repositorio). Vacío = el botón no se muestra.
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val googleClientId: String = propiedadesLocales.getProperty("google.client.id")?.trim().orEmpty()

android {
    namespace = "com.mastercode.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mastercode.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
    }

    buildTypes {
        debug {
            // 10.0.2.2 es la PC anfitriona vista desde el emulador de Android.
            buildConfigField("String", "ADMIN_API_URL", "\"http://10.0.2.2:8000/\"")
            buildConfigField("String", "USER_API_URL", "\"http://10.0.2.2:8081/\"")
        }
        release {
            // Pendiente: todavía no hay URL de producción de admin-api ni de user-api.
            buildConfigField("String", "ADMIN_API_URL", "\"\"")
            buildConfigField("String", "USER_API_URL", "\"\"")
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
}
