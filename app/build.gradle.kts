import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.campuspocket"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.campuspocket"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Los esquemas exportados de Room viajan con las pruebas instrumentadas (migraciones).
    sourceSets {
        val schemasDir = "$projectDir/schemas"
        named("androidTest") {
            assets.srcDir(schemasDir)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // lo usa LogUtil (BuildConfig.DEBUG)
    }

    signingConfigs {
        create("release") {
            val props = Properties().apply {
                File(rootProject.projectDir, "keystore.properties").inputStream().use { load(it) }
            }
            storeFile = File(rootProject.projectDir, "keystore.jks")
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Firma: lee del keystore.properties local (no se sube a ningún repo)
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // coroutines-android y coroutines-test (en debug) traen el MISMO service file;
            // sin fusionar ambas entradas el rule de Compose en dispositivo falla con
            // "Exception handler was not found via a ServiceLoader".
            merges += "META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler"
        }
    }

    testOptions {
        packaging {
            resources {
                // core y coroutines-test traen el MISMO service file y el merge por defecto
                // conserva solo uno; sin ambos, el rule de Compose en dispositivo falla con
                // "Exception handler was not found via a ServiceLoader".
                merges += "META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler"
            }
        }
    }
}

// Esquemas de Room exportados (sirven para escribir migraciones explícitas).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX base
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    // Navegación
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Corrutinas
    implementation(libs.coroutines.android)
    // coroutines-test también en debug: su ServiceLoader (ExceptionCollector) se consulta en el
    // proceso de la app instrumentada y el recurso solo se lee del APK principal.
    debugImplementation(libs.coroutines.test)

    // JSON del respaldo (Fase 4.5)
    implementation(libs.kotlinx.serialization.json)

    // Revisión diaria de pagos a las 8:00 (Fase 5B)
    implementation(libs.work)

    // Biometría para el bloqueo opcional (Fase 7)
    implementation("androidx.biometric:biometric:1.1.0")

    // PDF
    implementation(libs.pdfbox.android)

    // Pruebas unitarias (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    // Arnés del extractor con PDFs reales (no va en la app)
    testImplementation(libs.pdfbox.jvm)

    // Pruebas instrumentadas
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    // el rule de Compose usa runTest de coroutines-test en el dispositivo
    androidTestImplementation(libs.coroutines.test)
    debugImplementation(libs.compose.ui.test.manifest)
}

// room-migration (dentro de room-testing) trae serialization-json 1.8.1, mientras que
// savedstate fija core a {strictly 1.7.3}; ese desfase revienta en el dispositivo con
// AbstractMethodError (FieldBundle$$serializer). Bajar json no basta: se fuerzan core y
// json a 1.8.1 en los classpaths de debug y androidTest (las de KSP no se tocan).
configurations.matching {
    it.name in setOf(
        "debugCompileClasspath", "debugRuntimeClasspath",
        "debugAndroidTestCompileClasspath", "debugAndroidTestRuntimeClasspath",
        "releaseCompileClasspath", "releaseRuntimeClasspath"
    )
}.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1"
    )
}
