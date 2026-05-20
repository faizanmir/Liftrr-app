# Liftrr

AI-powered workout form analyzer for Android. Uses on-device pose detection to track reps, grade form, and help you lift better — no cloud required.

## Features

- **Real-time pose analysis** — MediaPipe Pose Landmarker tracks 33 body landmarks during exercises (squat, deadlift, bench press)
- **Rep counting & form grading** — Automatic rep detection with per-rep quality scoring (A–F grades) based on ROM, tempo, and symmetry
- **Workout history** — Browse past sessions with filtering by exercise type, date grouping, and swipe-to-delete
- **Video recording & playback** — Record workouts with CameraX and replay them with pose overlay
- **On-device LLM** — Gemma model via MediaPipe for personalized workout tips (no server required)
- **Google Sign-In** — Firebase authentication with local Room database fallback
- **Material 3 theming** — Dynamic color with dark mode support

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Jetpack Navigation 3 |
| DI | Hilt |
| Database | Room 2.8.x |
| Preferences | DataStore + Security Crypto |
| Camera | CameraX 1.6.x |
| Video | Media3 ExoPlayer |
| Pose Detection | MediaPipe Tasks Vision |
| On-device AI | MediaPipe Tasks GenAI (Gemma) |
| Auth | Firebase Auth, Google Credential Manager |
| Networking | Retrofit 3, OkHttp 4 |
| Image loading | Coil 3 |
| Charts | ComposeCharts |
| Build | AGP 9, Kotlin 2.3.x, KSP, Java 11 toolchain (JDK 17 to run Gradle) |
| Static analysis | detekt |

## Requirements

- Android Studio Hedgehog or newer
- JDK 17+
- Min SDK 27 (Android 8.1)
- Target SDK 36

## Setup

See [SETUP.md](SETUP.md) for full setup instructions including CI/CD configuration.

**Quick start:**

1. Clone the repo.
2. Add `google-services.json` to `app/`.
3. Copy the secrets template into the `core:data` module and fill in your Google Web Client ID:
   ```bash
   mkdir -p core/data/src/main/res/values
   cp app/secrets.xml.template core/data/src/main/res/values/secrets.xml
   ```
4. Add `GOOGLE_WEB_CLIENT_ID=...` to `local.properties` (not committed).
5. Build:
   ```bash
   ./gradlew assembleDebug
   ```

## Module Structure

Single-app, multi-module clean architecture. Feature modules depend only on `core:*`, never on each other.

```text
:app                  — MainActivity, LiftrrApplication, root navigation, Hilt entry point
:core:domain          — Pure Kotlin; domain models, repository interfaces, use cases
:core:data            — Room, Retrofit, Firebase, DataStore, Hilt modules, mappers
:core:ui              — Shared Compose components, Material 3 theme, navigation animations
:core:ml              — MediaPipe pose detection, Gemma LLM inference, skeletal overlay
:feature:auth         — Onboarding, sign-in / signup
:feature:workout      — Camera session, rep counting, video recording
:feature:summary      — Post-workout report and export
:feature:history      — Workout history list, filtering, playback
:feature:analytics    — Charts and performance trends
:feature:profile      — User settings and weight tracking
:benchmark            — Baseline profile macrobenchmarks
```

## Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (needs keystore env vars)
./gradlew build                  # Full build (auto-downloads pose model)
./gradlew detekt                 # Static analysis across all modules
./gradlew lint                   # Android Lint
./gradlew test                   # All unit tests
./gradlew connectedAndroidTest   # Instrumented tests (device/emulator required)
./gradlew :benchmark:connectedAndroidTest  # Macro benchmarks
```

## Models

### Pose Detection

The MediaPipe Pose Landmarker model (`pose_landmarker_heavy.task`) downloads automatically on first build via the `downloadPoseModel` Gradle task. No manual steps needed.

```bash
./gradlew downloadPoseModel  # manual trigger
```

### On-Device LLM (Gemma)

The LLM model is not bundled in the APK due to size. It must be downloaded from Kaggle and pushed to the device separately.

**Available variants:**

| Variant | Size | Notes |
|---------|------|-------|
| `gemma-3-1b` (default) | ~500 MB | Fast, good for quick tips |
| `gemma-1b` | ~500 MB | Older, lighter model |
| `gemma-2b` | ~1 GB | Better quality |
| `gemma-2b-fp16` | ~4 GB | Best quality, large |

**Download via Kaggle CLI:**

```bash
pip install kaggle
# Set up credentials: https://www.kaggle.com/settings → API token → save to ~/.kaggle/kaggle.json

kaggle models instances versions download google/gemma/tfLite/gemma-3-1b-it-int4
mv *.bin app/src/main/assets/llm/gemma-3-1b.task
```

Run `./gradlew downloadLLMModel` for variant-specific instructions.

**Push to device:**

```bash
./gradlew pushLLMModel   # pushes to /data/local/tmp/llm/ via adb
./gradlew cleanLLMModel  # remove downloaded model files
```

## CI

GitHub Actions workflow at `.github/workflows/android.yml`:

1. Decodes `google-services.json`, `secrets.xml`, and the release keystore from repo secrets.
2. Runs `./gradlew detekt` — fails on style violations.
3. Runs `./gradlew build` — compile, lint, unit tests, assemble.
4. Uploads the detekt HTML report as a build artifact.

Required GitHub secrets: `GOOGLE_SERVICES_JSON`, `GOOGLE_WEB_CLIENT_ID`, `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `SERVER_URL_RELEASE`.
