// Archivo de build raíz: solo declara los plugins (sin aplicarlos) para que todos los módulos usen la misma versión.
// Con AGP 9 Kotlin viene integrado, por eso NO se declara org.jetbrains.kotlin.android.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
