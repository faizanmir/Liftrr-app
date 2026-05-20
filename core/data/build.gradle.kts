import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "org.liftrr.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 27
        consumerProguardFiles("consumer-rules.pro")

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val serverUrl = localProperties.getProperty("SERVER_URL_DEBUG")
            ?: System.getenv("SERVER_URL_DEBUG")
            ?: "http://10.0.2.2:8080"
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
        buildConfigField("String", "DEBUG_SERVER_URL", "\"$serverUrl\"")
    }

    buildTypes {
        getByName("release") {
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localProperties.load(localPropertiesFile.inputStream())
            }
            val serverUrl = localProperties.getProperty("SERVER_URL_RELEASE")
                ?: System.getenv("SERVER_URL_RELEASE")
                ?: ""
            buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
            buildConfigField("String", "DEBUG_SERVER_URL", "\"\"")
        }
    }

    buildFeatures {
        buildConfig = true
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
}

dependencies {
    api(project(":core:domain"))

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Database
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Network
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Auth
    implementation(libs.bundles.credentials)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)

    // App Startup
    implementation(libs.androidx.startup.runtime)

    // Serialization
    implementation(libs.kotlinx.serialization.core)

    // Core
    implementation(libs.androidx.core.ktx)
}
