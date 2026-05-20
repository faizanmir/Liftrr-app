# AGENTS.md

This file gives Codex project-specific instructions for this Android repository.

## Required memory files

Before making architectural, navigation, module, ML, storage, auth, build-system, or package-structure decisions, read:

- `~/ai-vault/02-AI-RULES/global-ai-rules.md`
- `~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/project-brief.md`
- `~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/architecture-decisions.md`
- `~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/recurring-bugs.md`
- `~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/next-actions.md`
- `~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/sessions/session-index.md`

If these files are unavailable, continue with local repository context, but mention that memory files could not be read.

## Working rules

- Keep changes small and reviewable.
- Inspect existing patterns before adding new abstractions.
- Do not perform destructive Git commands.
- Do not rewrite module structure unless explicitly asked.
- Do not replace Jetpack Navigation 3 with Compose Navigation.
- Do not make feature modules depend on each other.
- Prefer fixing the current design over introducing a new framework or large restructure.
- Explain why each meaningful change is needed.
- Do not change secrets, local config, CI config, model download tasks, signing setup, or generated files unless explicitly asked.
- Do not commit secrets, tokens, credentials, keystores, `local.properties`, or local machine paths.
- After meaningful work, summarize:
    - files changed
    - why they changed
    - risks
    - checks/tests run
    - suggested memory updates

## Architecture

### Module Structure

Single-app multi-module with clean architecture layering:

```text
:app                  — MainActivity, LiftrrApplication, root navigation, Hilt entry point
:core:domain          — Pure Kotlin; domain models, repository interfaces, use cases
:core:data            — Room, Retrofit, Firebase Auth, DataStore, Hilt modules, mappers
:core:ui              — Shared Compose components, Material 3 theme, navigation animations
:core:ml              — MediaPipe pose detection, Gemma LLM inference, skeletal overlay
:feature:auth         — Onboarding, sign-in/signup screens
:feature:workout      — Camera session, rep counting, video recording
:feature:summary      — Post-workout report display and export
:feature:history      — Workout history list, filtering, playback
:feature:analytics    — Charts and performance trends
:feature:profile      — User settings and weight tracking
:benchmark            — Baseline profile macrobenchmarks: startup, navigation, camera
```

Feature modules depend only on `core:*` modules, never on each other.

### Architecture rules

- `:core:domain` must remain pure Kotlin.
- `:feature:*` modules should depend only on `:core:*`.
- Shared UI belongs in `:core:ui`.
- Data implementations belong in `:core:data`.
- ML implementation belongs in `:core:ml`.
- Screens should not call DAOs or Retrofit directly.
- ViewModels should depend on domain-level abstractions where practical.
- Data mappers should stay in the data layer.
- Domain models should not depend on Android framework classes.
- Avoid leaking Firebase, Room, or Retrofit types into feature modules.

### Navigation

Uses **Jetpack Navigation 3**, not Compose Navigation.

Key patterns:
- Screens are defined as `@Serializable` data objects/classes in a sealed `Screen : NavKey` hierarchy in `LiftrrApp.kt`.
- `rememberNavBackStack()` manages the back stack manually.
- `NavDisplay` uses entry decorators:
    - `rememberSaveableStateHolderNavEntryDecorator`
    - `rememberViewModelStoreNavEntryDecorator`
- Material Motion animations use slide/fade/scale transitions.
- Direction-aware transitions are handled by `NavigationAnimations` in `:core:ui`.
- Top-level destinations are Home, History, Analytics, and Settings.
- Top-level destinations allow system back to exit; other screens pop or navigate to Home.

Navigation rules:
- Do not migrate to Compose Navigation.
- Do not introduce a second navigation framework.
- Keep screen keys serializable.
- Keep back stack behavior explicit and predictable.

### Dependency Injection

The app uses Hilt.

Key setup:
- `@HiltAndroidApp` on `LiftrrApplication`
- `hiltViewModel()` in Compose screens

Important modules:
- `DatabaseModule` — Room database and DAO bindings
- `RepositoryModule` — repository interface to implementation bindings
- `NetworkModule` — Retrofit and OkHttp setup
- `AppModule` — dispatchers and app-level dependencies
- `MediaPipeModule` — MediaPipe dependencies
- `LLMModule` — on-device LLM dependencies

Rules:
- Prefer constructor injection.
- Avoid service locator patterns.
- Do not create dependencies manually in screens.
- Keep DI bindings close to the module that owns the implementation.

### Data Flow

```text
Compose Screen
    ↓
hiltViewModel()
    ↓
ViewModel using StateFlow / mutableState
    ↓
Repository interface from :core:domain
    ↓
Local source: Room DAO
Remote source: Retrofit / Firebase
```

Encrypted `DataStore` via Security Crypto is used for sensitive preferences such as tokens. Plain `DataStore` is used for non-sensitive preferences such as theme.

### ML Layer

- **Pose detection:** `MediaPipePoseDetector` wraps MediaPipe Tasks Vision; 33-landmark model auto-downloaded to `assets/pose_landmarker_heavy.task`.
- **On-device LLM:** `WorkoutLLM` wraps Gemma via MediaPipe Tasks GenAI; model file must be manually downloaded from Kaggle and placed in `assets/llm/` or pushed via `pushLLMModel`.
- App Startup library warms up MediaPipe at launch via initializer in `:app`.

ML rules:
- Do not block the main thread with model loading or inference.
- Keep heavy ML work off the UI thread.
- Be careful with memory usage during video processing.
- Do not commit large model files unless the repo already intentionally tracks them.
- Do not change model download tasks without checking build impact.

### Key Files

| File | Purpose |
|------|---------|
| `app/.../ui/screens/LiftrrApp.kt` | Navigation root, `Screen` sealed class, backstack logic |
| `core/data/.../di/DatabaseModule.kt` | Room DB setup, all DAO bindings |
| `app/build.gradle.kts` | Custom Gradle tasks, build variants, model download config |
| `gradle/libs.versions.toml` | Centralized version catalog for all dependencies |
| `.github/workflows/android.yml` | CI/CD pipeline |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose, Material 3, dynamic colors |
| Navigation | Jetpack Navigation 3 |
| DI | Hilt |
| Database | Room 2.8.4 |
| Preferences | DataStore + Security Crypto |
| Camera | CameraX 1.6.0-beta01 |
| Video | Media3 ExoPlayer |
| Networking | Retrofit 3 + OkHttp 4 |
| Image loading | Coil 3 |
| Auth | Firebase Auth + Google Credential Manager |
| Pose detection | MediaPipe Tasks Vision 0.10.14 |
| On-device LLM | MediaPipe Tasks GenAI / Gemma |
| Paging | Paging 3 for workout history |
| Build | AGP 9.0.0, Kotlin 2.3.0, KSP 2.3.3, Java 11 |

## Implementation guardrails

### Compose

- Keep composables stateless where practical.
- Hoist state to ViewModels or parent composables.
- Avoid business logic inside composables.
- Keep previews lightweight.
- Avoid direct repository access from composables.

### ViewModels

- ViewModels should expose immutable state.
- Avoid multiple unrelated mutable state sources where one UI state would be clearer.
- Keep one-off events separate from persistent UI state.
- Avoid long-running work directly in init blocks unless intentional.

### Room / DataStore

- Room entities should stay in the data layer.
- Domain models should remain independent from Room annotations.
- Sensitive preferences should use encrypted DataStore.
- Plain DataStore is acceptable for non-sensitive preferences like theme.

### Networking / Auth

- Do not leak Retrofit DTOs into domain or UI.
- Keep auth token handling centralized.
- Do not store sensitive tokens in plain preferences.
- Do not bypass Firebase/Auth abstractions from feature modules.

### Camera / Video

- Be careful with lifecycle handling.
- Release camera/video resources properly.
- Avoid blocking UI during recording, encoding, upload, or analysis.
- Prefer background processing for heavy operations.

## Knowledge graph (graphify)

A graphify knowledge graph lives in `graphify-out/`. Query it before reading files.

```bash
graphify query "how does X work"           # BFS - broad context
graphify query "how does X reach Y" --dfs  # DFS - trace a path
graphify path "NodeA" "NodeB"              # shortest path
graphify explain "SymbolName"              # all edges for one node
```

Rebuild when needed:
```bash
/graphify          # full rebuild (Claude Code skill)
/graphify --update # incremental
```

## Previous AI sessions

Relevant prior Claude Code and Codex conversations are stored in:

- `~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/sessions/`

The session index is:

- `~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/sessions/session-index.md`

Before starting a task:
- Read the four core memory files first.
- Then read `sessions/session-index.md`.
- Do not read every old session by default.
- Open old session notes only if they are relevant to the current task.
- Use session notes to recover context, past decisions, failed approaches, known risks, and follow-up items.
- Ask before opening more than 3 old session notes.


## Knowledge graph (code-review-graph)

**ALWAYS use code-review-graph MCP tools BEFORE Grep/Glob/Read.** (Available in Codex via `~/.codex/config.toml` and in Claude Code via `.mcp.json` — no setup needed.) The graph gives structural context (callers, dependents, impact radius) that file scanning cannot, with fewer tokens.

| Tool | Use when |
|------|----------|
| `semantic_search_nodes` | Find a function/class by name or keyword |
| `query_graph` | Trace callers_of / callees_of / imports_of / tests_for |
| `detect_changes` | Risk-score a diff before reviewing |
| `get_review_context` | Token-efficient source snippets for review |
| `get_impact_radius` | Blast radius of a change |
| `get_affected_flows` | Which execution paths are impacted |
| `get_architecture_overview` | High-level codebase structure |
| `refactor_tool` | Plan renames, find dead code |

Graph auto-updates on file save via hooks. Rebuild manually: `code-review-graph update`.



## Build commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK, requires keystore env vars
./gradlew build                  # Full build, auto-downloads pose model
./gradlew lint                   # Lint checks, abortOnError = false
./gradlew test                   # All unit tests
./gradlew testDebug              # Unit tests, debug variant
./gradlew :app:testDebugUnitTest # Single module unit tests
./gradlew test --tests "ClassName" # Specific test class
./gradlew connectedAndroidTest   # Instrumented tests, requires device/emulator
./gradlew :benchmark:connectedAndroidTest  # Macro benchmarks
```

### ML Model Tasks

```bash
./gradlew downloadPoseModel      # Auto-runs on preBuild; downloads pose_landmarker_heavy.task
./gradlew downloadLLMModel       # Gemma model, requires manual Kaggle download first
./gradlew pushLLMModel           # Push LLM model to device via ADB
./gradlew cleanLLMModel          # Remove downloaded LLM files
```

### Local Setup

`local.properties` is not in VCS and must contain:

```properties
GOOGLE_WEB_CLIENT_ID=...
```

CI injects `google-services.json` and `keystore/liftrr.jks` from GitHub Actions secrets.

Rules:
- Do not commit `local.properties`.
- Do not commit real keystores.
- Do not commit real Firebase or Google secrets.
- Do not hardcode local absolute paths.

## Testing and checks

For normal code changes, run:

```bash
./gradlew test
```

For build, Gradle, dependency, or module changes, run:

```bash
./gradlew build
```

For UI or camera behavior changes, use:

```bash
./gradlew connectedAndroidTest
```

For benchmark-sensitive startup, navigation, or camera changes, use:

```bash
./gradlew :benchmark:connectedAndroidTest
```

If a command is expensive or requires a device/emulator, mention it before running or report that it was not run.

## Code review

```bash
/review          # Claude Code skill — reviews current branch against main
/ultrareview     # multi-agent cloud review; user-triggered and billed
```

Run `/review` before opening a PR. It checks architecture rule violations, including feature-to-feature dependencies, DAO calls from screens, and domain layer purity, in addition to standard correctness checks.

## End-of-session logging

**REQUIRED: At the end of every session — no exceptions — write an Obsidian session note. Do not skip this step even for short or exploratory sessions.**

Store it under:

- `~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/sessions/`

Use filename format:

```text
YYYY-MM-DD-codex-short-topic.md
```

The session note must include:
- task
- context given
- files discussed or changed
- summary of work
- durable decisions
- bugs/watchouts discovered
- commands run
- final outcome
- follow-up items
- links to related notes using Obsidian links

After writing the session note, also update:
- `sessions/session-index.md` — always
- `architecture-decisions.md` — only if a real technical decision was made
- `recurring-bugs.md` — only if a repeatable issue was found
- `next-actions.md` — if next steps changed

## Memory update rules

Only update the Obsidian memory vault for durable information.

Update files under:

```text
~/ai-vault/01-PERSONAL-PROJECTS/liftrr-android/
```

Update:
- `architecture-decisions.md` when a real technical decision is made
- `recurring-bugs.md` when a repeatable bug pattern is found
- `next-actions.md` when next steps change
- `project-brief.md` only when the project direction changes

Do not store raw logs, full transcripts, generated code dumps, duplicate summaries, or large model output.

When suggesting a memory update, keep it concise and ask before writing unless explicitly told to update memory.