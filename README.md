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
4. The MediaPipe pose model downloads automatically on first build
5. (Optional) For on-device LLM, run `./gradlew downloadLLMModel` and follow the instructions

## Building

```bash
./gradlew assembleDebug
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
