# Liftrr

AI-powered workout form analyzer for Android. Uses on-device pose detection to track your reps, grade your form, and help you lift better.

## Features

- **Real-time pose analysis** — MediaPipe Pose Landmarker tracks 33 body landmarks during exercises (squat, deadlift, bench press)
- **Rep counting & form grading** — Automatic rep detection with per-rep quality scoring (A-F grades) based on ROM, tempo, and symmetry
- **Workout history** — Browse past sessions with filtering by exercise type, date grouping, and swipe-to-delete
- **Video recording & playback** — Record workouts with CameraX and replay them with pose overlay
- **On-device LLM** — Gemma model integration via MediaPipe for personalized workout tips (no server required)
- **BLE device support** — Connect to Bluetooth fitness devices for additional sensor data
- **Google Sign-In** — Firebase authentication with local Room database fallback
- **Material 3 theming** — Dynamic color support with dark mode

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3, Navigation 3 |
| DI | Hilt |
| Database | Room |
| Camera | CameraX (camera2, video) |
| Pose Detection | MediaPipe Tasks Vision |
| On-device AI | MediaPipe Tasks GenAI (Gemma) |
| Video Playback | Media3 ExoPlayer |
| Auth | Firebase Auth, Google Credential Manager |
| Networking | Retrofit, Coil |
| Charts | ComposeCharts |

## Requirements

- Android Studio Ladybug or newer
- Min SDK 27 (Android 8.1)
- Target SDK 36

## Setup

1. Clone the repo
2. Add your `google-services.json` to `app/`
3. Add your Google Web Client ID to `local.properties`:
   ```
   GOOGLE_WEB_CLIENT_ID=your_client_id_here
   ```

## Building

```bash
./gradlew assembleDebug
```

## Model Setup

### Pose Detection (Vision Model)

The MediaPipe Pose Landmarker model (`pose_landmarker_heavy.task`) downloads automatically during the first build via the `downloadPoseModel` Gradle task. No manual steps needed.

To manually trigger the download:
```bash
./gradlew downloadPoseModel
```

The model is saved to `app/src/main/assets/` and bundled into the APK.

### On-Device LLM (Gemma)

The LLM model is **not** bundled in the APK due to its size. It must be downloaded separately and pushed to the device.

**Available models:**

| Variant | Size | Notes |
|---------|------|-------|
| `gemma-3-1b` (default) | ~500MB | Fast, good for quick tips |
| `gemma-1b` | ~500MB | Older, lighter model |
| `gemma-2b` | ~1GB | Balanced quality/size |
| `gemma-2b-fp16` | ~4GB | Best quality, large |

**Step 1 — Download the model:**

```bash
./gradlew downloadLLMModel
```

Gemma models require manual download from Kaggle. The task will print detailed instructions. In short:

1. Install the Kaggle CLI: `pip install kaggle`
2. Set up credentials at https://www.kaggle.com/settings (create API token, save to `~/.kaggle/kaggle.json`)
3. Download:
   ```bash
   kaggle models instances versions download google/gemma/tfLite/gemma-3-1b-it-int4
   ```
4. Move the downloaded file:
   ```bash
   mv *.bin app/src/main/assets/llm/gemma-3-1b.task
   ```

To use a different model variant:
```bash
./gradlew downloadLLMModel -Pmodel=gemma-2b
```

**Step 2 — Push to device (for development):**

```bash
./gradlew pushLLMModel
```

This pushes the model to `/data/local/tmp/llm/` on the connected device via `adb`.

**Other LLM tasks:**

```bash
./gradlew cleanLLMModel    # Remove downloaded model files
```

## Architecture

```
org.liftrr/
├── bluetooth/       # BLE device scanning and connection
├── data/
│   ├── local/       # Room database (WorkoutDao, UserDao)
│   ├── models/      # Entity and DTO classes
│   ├── preferences/ # DataStore preferences
│   ├── repository/  # Auth and workout repositories
│   └── services/    # Google auth service
├── di/              # Hilt modules
├── domain/
│   ├── analytics/   # Workout scoring and report generation
│   ├── video/       # Video recording manager
│   └── workout/     # Exercise logic (squat, deadlift, bench press)
├── ml/              # MediaPipe pose detection and LLM inference
├── ui/
│   ├── components/  # Shared composables (camera, onboarding)
│   ├── screens/     # Feature screens (home, history, workout, profile)
│   └── theme/       # Material 3 theme, colors, typography
└── utils/           # Bitmap pooling, coroutine dispatchers
```
