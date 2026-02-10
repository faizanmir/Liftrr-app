# Workout Analytics System

Camera-based workout analytics and reporting system for Liftrr. Analyzes pose detection data to generate comprehensive workout reports with insights and recommendations.

## Overview

This system collects pose detection data during workouts and generates detailed analysis reports including:

- **Range of Motion** - Depth and consistency of movements
- **Tempo & Timing** - Rep duration and pacing
- **Symmetry** - Left vs right balance
- **Form Consistency** - Quality patterns over time
- **Rep-by-Rep Breakdown** - Detailed analysis of each rep
- **Personalized Recommendations** - Actionable feedback for improvement

## Architecture

### Data Collection

**WorkoutSession** - Container for all workout data
```kotlin
data class WorkoutSession(
    val id: String,
    val exerciseType: ExerciseType,
    val workoutMode: WorkoutMode,
    val startTime: Long,
    val endTime: Long?,
    val reps: List<RepData>,
    val poseFrames: List<PoseFrame>,  // All pose detection frames
    val videoPath: String?,            // Optional video recording
    val completed: Boolean
)
```

**PoseFrame** - Single frame of pose data
```kotlin
data class PoseFrame(
    val timestamp: Long,
    val frameNumber: Int,
    val poseResult: PoseDetectionResult,
    val repNumber: Int?  // Which rep this frame belongs to
)
```

**WorkoutSessionBuilder** - Collects data during workout
```kotlin
class WorkoutSessionBuilder(
    exerciseType: ExerciseType,
    workoutMode: WorkoutMode
) {
    fun addRep(rep: RepData)
    fun addPoseFrame(poseResult: PoseDetectionResult, currentRepNumber: Int?)
    fun setVideoPath(path: String)
    fun build(completed: Boolean): WorkoutSession
}
```

### Analysis Engine

**WorkoutAnalyzer** - Analyzes session data
```kotlin
object WorkoutAnalyzer {
    fun analyzeSession(session: WorkoutSession): WorkoutReport
}
```

**WorkoutReport** - Comprehensive analysis results
```kotlin
data class WorkoutReport(
    val sessionId: String,
    val exerciseType: ExerciseType,
    val totalReps: Int,
    val goodReps: Int,
    val badReps: Int,
    val averageQuality: Float,
    val durationMs: Long,
    val rangeOfMotion: RangeOfMotionAnalysis,
    val tempo: TempoAnalysis,
    val symmetry: SymmetryAnalysis,
    val formConsistency: FormConsistencyAnalysis,
    val repAnalyses: List<RepAnalysis>,
    val recommendations: List<String>
) {
    val overallScore: Float  // 0-100 overall workout score
    val grade: String        // A, B, C, D, F
}
```

### Analysis Components

**Range of Motion Analysis**
```kotlin
data class RangeOfMotionAnalysis(
    val averageDepth: Float,     // Average depth across all reps
    val minDepth: Float,          // Shallowest rep
    val maxDepth: Float,          // Deepest rep
    val consistency: Float        // How consistent (0-100)
)
```

**Tempo Analysis**
```kotlin
data class TempoAnalysis(
    val averageRepDurationMs: Long,
    val averageRestBetweenRepsMs: Long,
    val tempoConsistency: Float
)
```

**Symmetry Analysis**
```kotlin
data class SymmetryAnalysis(
    val overallSymmetry: Float,        // 0-100, higher = more symmetrical
    val leftRightAngleDifference: Float,
    val issues: List<String>
)
```

**Form Consistency Analysis**
```kotlin
data class FormConsistencyAnalysis(
    val consistencyScore: Float,  // 0-100
    val qualityTrend: String      // "Improving", "Declining", "Stable"
)
```

**Rep Analysis**
```kotlin
data class RepAnalysis(
    val repNumber: Int,
    val isGoodForm: Boolean,
    val quality: Float,
    val depth: Float?,             // Depth for this specific rep
    val tempo: RepTempo?,          // Timing breakdown
    val peakFrameIndex: Int?,      // Frame at deepest point
    val formIssues: List<String>
)

data class RepTempo(
    val totalMs: Long,
    val eccentricMs: Long,  // Lowering phase
    val concentricMs: Long  // Lifting phase
)
```

## Usage

### 1. Start a Workout Session

```kotlin
// In WorkoutEngine or ViewModel
workoutEngine.startSession(workoutMode)
```

### 2. Collect Data During Workout

The WorkoutEngine automatically collects data when `processPoseResult()` is called:

```kotlin
// This is called automatically for each frame
fun processPoseResult(result: PoseDetectionResult): WorkoutState {
    // ... process pose

    // Automatically records frame for analytics
    if (isSessionActive) {
        sessionBuilder?.addPoseFrame(result, currentRepNumber)
    }

    // Automatically records reps
    if (repCompleted && isSessionActive) {
        sessionBuilder?.addRep(repData)
    }

    // ...
}
```

### 3. End Workout and Generate Report

```kotlin
// In ViewModel
fun finishWorkout(): WorkoutReport? {
    val session = workoutEngine.endSession() ?: return null
    return WorkoutAnalyzer.analyzeSession(session)
}
```

### 4. Display Report

```kotlin
// Navigate to WorkoutSummaryScreen
val report = viewModel.finishWorkout()
if (report != null) {
    WorkoutSummaryScreen(
        report = report,
        onNavigateBack = { /* navigate back */ }
    )
}
```

## Exercise-Specific Analysis

### Squats
- **Depth**: Hip angle (smaller = deeper)
- **Form Issues**: Knees caving, not going deep enough
- **Symmetry**: Left vs right hip/knee angles

### Deadlifts
- **Depth**: Range of motion (max hip angle - min hip angle)
- **Form Issues**: Squatting vs hinging, rounded back
- **Symmetry**: Left vs right hip angles

### Bench Press
- **Depth**: Elbow angle (smaller = deeper/touch chest)
- **Form Issues**: Partial reps, uneven bar path
- **Symmetry**: Left vs right elbow angles

## Metrics Calculation

### Range of Motion (Depth)

For each exercise, depth is calculated differently:

**Squats:**
```kotlin
val hipAngle = calculateAngle(shoulder, hip, knee)
val depth = hipAngles.min()  // Minimum angle = deepest squat
```

**Deadlifts:**
```kotlin
val hipAngles = frames.map { calculateAngle(shoulder, hip, knee) }
val depth = hipAngles.max() - hipAngles.min()  // Full range
```

### Tempo

```kotlin
val startTime = frames.first().timestamp
val endTime = frames.last().timestamp
val totalDuration = endTime - startTime

// Find peak (deepest point)
val peakTime = frames[peakIndex].timestamp

val eccentricDuration = peakTime - startTime  // Going down
val concentricDuration = endTime - peakTime   // Coming up
```

### Symmetry

```kotlin
val leftAngle = calculateAngle(leftShoulder, leftHip, leftKnee)
val rightAngle = calculateAngle(rightShoulder, rightHip, rightKnee)
val difference = abs(leftAngle - rightAngle)
val symmetry = 100 - (difference / leftAngle * 100)
```

### Form Consistency

```kotlin
val qualityScores = reps.map { it.poseQuality }
val average = qualityScores.average()
val variance = qualityScores.map { (it - average)^2 }.average()
val consistency = 100 - (variance / average * 100)
```

## Recommendations Engine

The system generates personalized recommendations based on:

1. **Form Quality**: Good/bad rep ratio
2. **Consistency**: Variation in depth and tempo
3. **Symmetry**: Left/right balance
4. **Trends**: Improving vs declining over time

Example recommendations:
- "Focus on form quality - you had more bad reps than good ones"
- "Work on consistency - your depth varies significantly between reps"
- "Check for muscle imbalances - you're favoring one side"
- "Try to maintain a consistent tempo between reps"
- "Great workout! Your form and consistency are excellent"

## Future Enhancements

### Video Integration
```kotlin
data class VideoHighlight(
    val type: HighlightType,  // BEST_REP, WORST_REP, FORM_ISSUE, etc.
    val timestampMs: Long,
    val frameNumber: Int,
    val description: String
)

enum class HighlightType {
    BEST_REP,
    WORST_REP,
    FORM_ISSUE,
    PERSONAL_BEST,
    KEY_MOMENT
}
```

### Data Persistence
- Save WorkoutSession to database
- Track progress over time
- Compare workouts
- Personal records

### Advanced Analytics
- Bar path tracking (for barbell exercises)
- Velocity/power metrics
- Fatigue detection
- Volume tracking (reps × weight)

### Sensor Integration
- Combine camera data with accelerometer/gyroscope
- Heart rate tracking
- Force plate data
- Wearable sensors

## Example Report Output

```
=== WORKOUT SUMMARY ===
Exercise: Squat
Overall Score: 87% (Grade: B)
Duration: 5m 30s
Total Reps: 10 (8 good, 2 bad)

Range of Motion:
- Average Depth: 95°
- Consistency: 85%
- Range: 89° - 102°

Tempo:
- Average Rep Time: 3.2s
- Consistency: 78%

Symmetry:
- Symmetry Score: 92%
- No major imbalances detected

Form Consistency:
- Consistency Score: 83%
- Trend: Stable

Recommendations:
✓ Great depth and consistency
⚠ Try to maintain a more consistent tempo
✓ Good symmetry - no imbalances detected
```

## Integration with WorkoutEngine

The WorkoutEngine has been enhanced with session tracking:

```kotlin
class WorkoutEngine {
    private var sessionBuilder: WorkoutSessionBuilder? = null
    private var isSessionActive = false

    fun startSession(workoutMode: WorkoutMode) {
        sessionBuilder = WorkoutSessionBuilder(currentExerciseType, workoutMode)
        isSessionActive = true
    }

    fun endSession(): WorkoutSession? {
        isSessionActive = false
        return sessionBuilder?.build(completed = true)
    }

    fun isSessionActive(): Boolean = isSessionActive
}
```

## UI Integration

The WorkoutSummaryScreen displays the report with:

- Overall score and grade (A-F)
- Quick stats (good/bad reps, avg quality)
- Detailed sections for ROM, tempo, symmetry, consistency
- Personalized recommendations
- Rep-by-rep breakdown with visual indicators

All metrics are presented with clear visualizations and color coding for easy understanding.
