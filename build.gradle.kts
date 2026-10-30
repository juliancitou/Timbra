plugins {
    alias(libs.plugins.android.application) apply false
    // AGP trae Kotlin 2.2.10 integrado; declararlo aquí lo sube a la versión del catálogo.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
