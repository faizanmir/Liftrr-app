plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.liftrr.ml"
    compileSdk = 36

    defaultConfig {
        minSdk = 27
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":core:domain"))

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // App Startup (for MediaPipeInitializer)
    implementation(libs.androidx.startup.runtime)

    // MediaPipe — api so consumers (feature:workout) can access NormalizedLandmark etc.
    api(libs.mediapipe.tasks.vision)
    implementation(libs.tasks.genai)

    // CameraX (for ImageProxy in pose detection)
    api(libs.androidx.camera.core)

    // Compose (geometry + graphics used by pose rendering)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)

    // Gson (WorkoutReportExporter serializes WorkoutReport to JSON/PDF)
    implementation(libs.converter.gson)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
