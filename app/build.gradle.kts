import java.io.FileOutputStream
import java.net.URI
import java.util.Properties
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "org.liftrr"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.liftrr"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        buildConfigField(
            "String",
            "WEB_CLIENT_ID",
            "\"${localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")}\""
        )

    }

    signingConfigs {
        create("release") {
            storeFile = file("$rootDir/keystore/liftrr.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = true
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
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
        buildConfig = true
        compose = true
    }
}

// Define asset directory for MediaPipe models
val assetDir = file("$projectDir/src/main/assets")

// Task to download MediaPipe Pose Landmarker model
tasks.register("downloadPoseModel") {
    group = "mediapipe"
    description = "Downloads the MediaPipe Pose Landmarker model file"

    val modelUrl = "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_heavy/float16/1/pose_landmarker_heavy.task"
    val modelFile = file("$assetDir/pose_landmarker_heavy.task")

    inputs.property("modelUrl", modelUrl)
    outputs.file(modelFile)

    doLast {
        if (!modelFile.exists()) {
            println("Downloading MediaPipe Pose Landmarker model...")
            assetDir.mkdirs()

            val url = URI(modelUrl).toURL()
            url.openStream().use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
            println("Model downloaded successfully to ${modelFile.absolutePath}")
        } else {
            println("Model already exists at ${modelFile.absolutePath}")
        }
    }
}

// Ensure model is downloaded before building
tasks.named("preBuild") {
    dependsOn("downloadPoseModel")
}


dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)

    // Navigation
    implementation(libs.bundles.navigation)
    implementation(libs.kotlinx.serialization.core)

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.ui)
    ksp(libs.hilt.compiler)

    // Database (Room)
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Google Services & Auth
    implementation(libs.bundles.credentials)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.play.services)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // Media3 ExoPlayer for video playback
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")

    // ComposeCharts for data visualization
    implementation("io.github.ehsannarmani:compose-charts:0.2.0")

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // UI & Material
    implementation(libs.material)
    implementation(libs.accompanist.permissions)

    // Image Loading
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    // MediaPipe for Pose Detection
    implementation(libs.mediapipe.tasks.vision)

    // MediaPipe for LLM Inference (On-device AI)
    implementation(libs.tasks.genai)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// ====================================================================================
// LLM Model Download Configuration
// ====================================================================================

/**
 * Configuration for LLM model downloads
 *
 * Available models:
 * - gemma-1b: Fast, smaller model (~500MB) - good for quick tips
 * - gemma-2b: Balanced model (~1GB) - recommended for production
 * - gemma-2b-fp16: High quality (~4GB) - best quality but large
 *
 * To use a different model, change the modelVariant property below.
 */
object LLMModelConfig {
    // Model configuration - change this to use different models
    val modelVariant = "gemma-3-1b"  // Options: "gemma-3-1b", "gemma-1b", "gemma-2b"

    // Model URLs and checksums
    // NOTE: Gemma models require manual download from Kaggle or Hugging Face
    // See instructions in the download task below or LLM_MODEL_SETUP.md
    val models = mapOf(
        "gemma-3-1b" to ModelInfo(
            url = "MANUAL_DOWNLOAD_REQUIRED",  // Download from kaggle.com/models/google/gemma
            kaggleModel = "google/gemma/tfLite/gemma-3-1b-it-int4",
            huggingFaceModel = "google/gemma-3-1b-it",
            sha256 = "",
            size = "500MB"
        ),
        "gemma-1b" to ModelInfo(
            url = "MANUAL_DOWNLOAD_REQUIRED",  // Download from kaggle.com/models/google/gemma
            kaggleModel = "google/gemma/tfLite/gemma-1.1-2b-it-cpu-int4",
            huggingFaceModel = "google/gemma-1.1-2b-it",
            sha256 = "",
            size = "500MB"
        ),
        "gemma-2b" to ModelInfo(
            url = "MANUAL_DOWNLOAD_REQUIRED",  // Download from kaggle.com/models/google/gemma
            kaggleModel = "google/gemma/tfLite/gemma-2b-it-cpu-int4",
            huggingFaceModel = "google/gemma-2b-it",
            sha256 = "",
            size = "1GB"
        ),
        "gemma-2b-fp16" to ModelInfo(
            url = "MANUAL_DOWNLOAD_REQUIRED",
            kaggleModel = "google/gemma/tfLite/gemma-2b-it-gpu-fp16",
            huggingFaceModel = "google/gemma-2b-it",
            sha256 = "",
            size = "4GB"
        )
    )

    data class ModelInfo(
        val url: String,
        val kaggleModel: String,
        val huggingFaceModel: String,
        val sha256: String,
        val size: String
    )
}

/**
 * Download LLM model for on-device AI inference
 *
 * Usage:
 *   ./gradlew downloadLLMModel           # Download configured model
 *   ./gradlew downloadLLMModel -Pmodel=gemma-1b  # Download specific model
 *   ./gradlew pushLLMModel               # Push model to connected device
 *   ./gradlew cleanLLMModel              # Remove downloaded model
 *
 * The model is downloaded to: app/src/main/assets/llm/
 * For development/testing, push to device with: adb push model.bin /data/local/tmp/llm/
 */
tasks.register("downloadLLMModel") {
    group = "liftrr"
    description = "Download MediaPipe LLM model for on-device AI inference"

    doLast {
        // Get model variant from project property or use default
        val modelName = (project.findProperty("model") as? String) ?: LLMModelConfig.modelVariant
        val modelInfo = LLMModelConfig.models[modelName]
            ?: throw GradleException("Unknown model: $modelName. Available: ${LLMModelConfig.models.keys}")

        // Setup paths
        val assetsDir = file("src/main/assets/llm")
        // MediaPipe uses .task files, but also support .bin for compatibility
        val modelFile = file("${assetsDir}/${modelName}.task")

        // Create assets directory if it doesn't exist
        if (!assetsDir.exists()) {
            assetsDir.mkdirs()
            println("📁 Created directory: ${assetsDir.absolutePath}")
        }

        // Check if model already exists
        if (modelFile.exists()) {
            println("✅ Model already exists: ${modelFile.absolutePath}")
            println("   Size: ${modelFile.length() / 1024 / 1024}MB")
            println("   To re-download, run: ./gradlew cleanLLMModel downloadLLMModel")
            return@doLast
        }

        // Check if manual download is required
        if (modelInfo.url == "MANUAL_DOWNLOAD_REQUIRED") {
            println("⚠️  Gemma models require manual download from Kaggle or Hugging Face")
            println()
            println("📥 OPTION 1: Download from Kaggle (Recommended)")
            println("   1. Install Kaggle CLI:")
            println("      pip install kaggle")
            println()
            println("   2. Setup Kaggle credentials:")
            println("      - Go to https://www.kaggle.com/settings")
            println("      - Create API token")
            println("      - Save to ~/.kaggle/kaggle.json")
            println()
            println("   3. Download model:")
            println("      kaggle models instances versions download ${modelInfo.kaggleModel}")
            println()
            println("   4. Move model to assets:")
            println("      mv *.bin ${modelFile.absolutePath}")
            println()
            println("📥 OPTION 2: Download from Hugging Face")
            println("   1. Visit: https://huggingface.co/${modelInfo.huggingFaceModel}")
            println("   2. Download the quantized .bin file")
            println("   3. Move to: ${modelFile.absolutePath}")
            println()
            println("📥 OPTION 3: Use test model (for development only)")
            println("   Create a dummy file for testing:")
            println("      echo 'test' > ${modelFile.absolutePath}")
            println()
            throw GradleException(
                "Model requires manual download. Follow instructions above.\n" +
                "After downloading, run: ./gradlew pushLLMModel"
            )
        }

        println("📥 Downloading LLM model: $modelName")
        println("   URL: ${modelInfo.url}")
        println("   Expected size: ${modelInfo.size}")
        println("   Destination: ${modelFile.absolutePath}")
        println()

        try {
            // Download with progress tracking
            val url = URI(modelInfo.url).toURL()
            val connection = url.openConnection()
            val totalSize = connection.contentLengthLong

            connection.getInputStream().use { input ->
                FileOutputStream(modelFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastProgress = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Print progress every 10%
                        if (totalSize > 0) {
                            val progress = ((totalBytesRead * 100) / totalSize).toInt()
                            if (progress >= lastProgress + 10) {
                                println("   Progress: $progress% (${totalBytesRead / 1024 / 1024}MB / ${totalSize / 1024 / 1024}MB)")
                                lastProgress = progress
                            }
                        }
                    }

                    println()
                    println("✅ Download complete!")
                    println("   Final size: ${totalBytesRead / 1024 / 1024}MB")
                }
            }

            // Verify checksum if provided
            if (modelInfo.sha256.isNotEmpty()) {
                println("🔍 Verifying checksum...")
                val actualChecksum = calculateSHA256(modelFile)
                if (actualChecksum.equals(modelInfo.sha256, ignoreCase = true)) {
                    println("✅ Checksum verified!")
                } else {
                    throw GradleException("Checksum mismatch!\nExpected: ${modelInfo.sha256}\nActual: $actualChecksum")
                }
            }

            println()
            println("🎉 Model ready for use!")
            println()
            println("Next steps:")
            println("  1. For emulator/development: ./gradlew pushLLMModel")
            println("  2. For production: Model will be bundled in APK assets")
            println()

        } catch (e: Exception) {
            // Clean up partial download
            if (modelFile.exists()) {
                modelFile.delete()
            }
            throw GradleException("Failed to download model: ${e.message}", e)
        }
    }
}

/**
 * Push downloaded model to connected Android device for development/testing
 */
tasks.register<Exec>("pushLLMModel") {
    group = "liftrr"
    description = "Push LLM model to connected Android device (requires adb)"

    doFirst {
        val modelName = (project.findProperty("model") as? String) ?: LLMModelConfig.modelVariant
        // Try .task file first (MediaPipe format), then .bin for compatibility
        val modelFile = file("src/main/assets/llm/${modelName}.task").let { taskFile ->
            if (taskFile.exists()) taskFile else file("src/main/assets/llm/${modelName}.bin")
        }

        if (!modelFile.exists()) {
            throw GradleException(
                "Model not found: ${modelFile.absolutePath}\n" +
                "Run './gradlew downloadLLMModel' first"
            )
        }

        println("📱 Pushing model to device...")
        println("   Model: $modelName (${modelFile.length() / 1024 / 1024}MB)")
        println("   Destination: /data/local/tmp/llm/model.bin")
    }

    val modelName = (project.findProperty("model") as? String) ?: LLMModelConfig.modelVariant
    // Try .task file first (MediaPipe format), then .bin for compatibility
    val modelFile = file("src/main/assets/llm/${modelName}.task").let { taskFile ->
        if (taskFile.exists()) taskFile else file("src/main/assets/llm/${modelName}.bin")
    }

    // Push with appropriate extension
    val targetPath = if (modelFile.name.endsWith(".task")) {
        "/data/local/tmp/llm/model.task"
    } else {
        "/data/local/tmp/llm/model.bin"
    }

    commandLine = listOf(
        "adb", "push",
        modelFile.absolutePath,
        targetPath
    )

    doLast {
        println("✅ Model pushed successfully!")
        println()
        println("To verify, run: adb shell ls -lh /data/local/tmp/llm/")
    }
}

/**
 * Remove downloaded model files
 */
tasks.register<Delete>("cleanLLMModel") {
    group = "liftrr"
    description = "Remove downloaded LLM model files"

    delete(fileTree("src/main/assets/llm") {
        include("*.bin")
        include("*.task")
        include("*.tflite")
    })

    doLast {
        println("🗑️  Removed LLM model files")
    }
}

/**
 * Helper function to calculate SHA256 checksum
 */
fun calculateSHA256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
