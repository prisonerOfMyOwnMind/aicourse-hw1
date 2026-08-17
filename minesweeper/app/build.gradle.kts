// Плагина org.jetbrains.kotlin.android здесь нет намеренно: начиная с AGP 9.0
// поддержка Kotlin встроена в сам Android-плагин, и подключение отдельного
// плагина считается ошибкой сборки.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.fedorfalchuk.minesweeper"

    // compileSdk 37, а не 36: Compose BOM 2026.08.00 и lifecycle 2.11.0 требуют
    // компиляции против API 37. Поднято решением владельца после остановки
    // на запрете в AGENTS.md, а не молча. targetSdk и minSdk не менялись.
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.fedorfalchuk.minesweeper"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
