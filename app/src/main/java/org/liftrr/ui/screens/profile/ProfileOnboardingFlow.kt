package org.liftrr.ui.screens.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.liftrr.data.local.UserDao
import org.liftrr.data.models.*
import org.liftrr.data.repository.AuthRepository
import org.liftrr.data.repository.ProgressiveProfileManager
import javax.inject.Inject

/**
 * Sequential profile onboarding flow with progress tracking
 * Shows one screen at a time with progress bar at top
 */
enum class FitnessGoal(val displayName: String, val description: String) {
    LOSE_WEIGHT("Lose Weight", "Burn fat and reduce body weight"),
    BUILD_MUSCLE("Build Muscle", "Gain muscle mass and strength"),
    GET_STRONGER("Get Stronger", "Increase overall strength"),
    IMPROVE_ENDURANCE("Improve Endurance", "Boost cardiovascular fitness"),
    STAY_ACTIVE("Stay Active", "Maintain general fitness and health"),
    IMPROVE_FLEXIBILITY("Improve Flexibility", "Increase range of motion"),
    ATHLETIC_PERFORMANCE("Athletic Performance", "Enhance sports performance"),
    REHABILITATION("Rehabilitation", "Recover from injury")
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOnboardingFlow(
    isEditMode: Boolean = false,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: ProfileOnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentProgress(isEditMode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditMode) "Edit Profile" else "Complete Profile")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveProgress()
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (state.currentStep + 1) / state.totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )

            // Progress text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Step ${state.currentStep + 1} of ${state.totalSteps}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${((state.currentStep + 1) * 100 / state.totalSteps)}% Complete",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Divider()

            // Animated content for each step
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    0 -> FitnessLevelStep(
                        selectedLevel = state.fitnessLevel,
                        onLevelSelected = { viewModel.updateFitnessLevel(it) },
                        onNext = { viewModel.nextStep() },
                        onSkip = { viewModel.skipToEnd() }
                    )
                    1 -> GoalsStep(
                        selectedGoals = state.goals,
                        onGoalsChanged = { viewModel.updateGoals(it) },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() }
                    )
                    2 -> BodyStatsStep(
                        height = state.height,
                        weight = state.weight,
                        unitSystem = state.unitSystem,
                        onHeightChanged = { viewModel.updateHeight(it) },
                        onWeightChanged = { viewModel.updateWeight(it) },
                        onUnitSystemChanged = { viewModel.updateUnitSystem(it) },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onSkip = { viewModel.nextStep() }
                    )
                    3 -> PreferredExercisesStep(
                        selectedExercises = state.preferredExercises,
                        onExercisesChanged = { viewModel.updatePreferredExercises(it) },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onSkip = { viewModel.nextStep() }
                    )
                    4 -> GenderStep(
                        selectedGender = state.gender,
                        onGenderSelected = { viewModel.updateGender(it) },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onSkip = { viewModel.nextStep() }
                    )
                    5 -> DateOfBirthStep(
                        dateOfBirth = state.dateOfBirth,
                        onDateSelected = { viewModel.updateDateOfBirth(it) },
                        onNext = { viewModel.nextStep() },
                        onBack = { viewModel.previousStep() },
                        onSkip = { viewModel.nextStep() }
                    )
                    6 -> {
                        if (state.hasExistingPhoto) {
                            // Skip photo step, go directly to finish
                            LaunchedEffect(Unit) {
                                viewModel.completeOnboarding()
                                onComplete()
                            }
                            Box(modifier = Modifier.fillMaxSize())
                        } else {
                            ProfilePhotoStep(
                                photoUrl = state.photoUrl,
                                onPhotoSelected = { viewModel.updatePhotoUrl(it) },
                                onFinish = {
                                    viewModel.completeOnboarding()
                                    onComplete()
                                },
                                onBack = { viewModel.previousStep() },
                                onSkip = {
                                    viewModel.completeOnboarding()
                                    onComplete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Step 1: Fitness Level ────────────────────────────────────────────────

@Composable
private fun FitnessLevelStep(
    selectedLevel: FitnessLevel?,
    onLevelSelected: (FitnessLevel) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "What's your fitness level?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "This helps us personalize your experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        FitnessLevel.entries.forEach { level ->
            FitnessLevelOption(
                level = level,
                isSelected = selectedLevel == level,
                onSelect = onLevelSelected
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedLevel != null
        ) {
            Text("Continue")
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip onboarding")
        }
    }
}

@Composable
private fun FitnessLevelOption(
    level: FitnessLevel,
    isSelected: Boolean,
    onSelect: (FitnessLevel) -> Unit
) {
    val (title, description) = when (level) {
        FitnessLevel.BEGINNER -> "Beginner" to "New to working out or returning after a long break"
        FitnessLevel.INTERMEDIATE -> "Intermediate" to "Regular training for 6+ months with proper form"
        FitnessLevel.ADVANCED -> "Advanced" to "Consistent training for 2+ years with advanced techniques"
        FitnessLevel.ELITE -> "Elite" to "Competitive athlete or professional level training"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelect(level) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = { onSelect(level) }
            )
        }
    }
}

// ─── Step 2: Goals ─────────────────────────────────────────────────────

@Composable
private fun GoalsStep(
    selectedGoals: Set<FitnessGoal>,
    onGoalsChanged: (Set<FitnessGoal>) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "What are your goals?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Select all that apply.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        FitnessGoal.entries.forEach { goal ->
            GoalOption(
                goal = goal,
                isSelected = selectedGoals.contains(goal),
                onToggle = { selected ->
                    onGoalsChanged(
                        if (selected) selectedGoals + goal
                        else selectedGoals - goal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedGoals.isNotEmpty()
        ) {
            Text("Continue")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun GoalOption(
    goal: FitnessGoal,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) null else CardDefaults.outlinedCardBorder(),
        onClick = { onToggle(!isSelected) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggle
            )
        }
    }
}

// ─── Step 3: Body Stats ────────────────────────────────────────────────

@Composable
private fun BodyStatsStep(
    height: Float?,
    weight: Float?,
    unitSystem: UnitSystem,
    onHeightChanged: (Float?) -> Unit,
    onWeightChanged: (Float?) -> Unit,
    onUnitSystemChanged: (UnitSystem) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    var heightText by remember(height, unitSystem) {
        mutableStateOf(
            if (height != null && unitSystem == UnitSystem.IMPERIAL) {
                (height / 2.54f).toString()
            } else {
                height?.toString() ?: ""
            }
        )
    }
    var weightText by remember(weight, unitSystem) {
        mutableStateOf(
            if (weight != null && unitSystem == UnitSystem.IMPERIAL) {
                (weight / 0.453592f).toString()
            } else {
                weight?.toString() ?: ""
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Add your body stats",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Optional - helps with tracking progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Unit toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = unitSystem == UnitSystem.METRIC,
                onClick = { onUnitSystemChanged(UnitSystem.METRIC) },
                label = { Text("Metric") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = unitSystem == UnitSystem.IMPERIAL,
                onClick = { onUnitSystemChanged(UnitSystem.IMPERIAL) },
                label = { Text("Imperial") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = heightText,
            onValueChange = {
                heightText = it
                val h = it.toFloatOrNull()
                onHeightChanged(
                    if (h != null && unitSystem == UnitSystem.IMPERIAL) h * 2.54f else h
                )
            },
            label = { Text(if (unitSystem == UnitSystem.METRIC) "Height (cm)" else "Height (in)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weightText,
            onValueChange = {
                weightText = it
                val w = it.toFloatOrNull()
                onWeightChanged(
                    if (w != null && unitSystem == UnitSystem.IMPERIAL) w * 0.453592f else w
                )
            },
            label = { Text(if (unitSystem == UnitSystem.METRIC) "Weight (kg)" else "Weight (lbs)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
        }
    }
}

// ─── Step 4: Preferred Exercises ───────────────────────────────────────

enum class MainExercise(val displayName: String, val emoji: String) {
    SQUAT("Squat", "🏋️"),
    BENCH_PRESS("Bench Press", "💪"),
    DEADLIFT("Deadlift", "🔥")
}

@Composable
private fun PreferredExercisesStep(
    selectedExercises: Set<String>,
    onExercisesChanged: (Set<String>) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Your favorite exercises?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Select your favorites from the big three powerlifting exercises.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Display exercises as prominent cards
        MainExercise.entries.forEach { exercise ->
            ExerciseCard(
                exercise = exercise,
                isSelected = selectedExercises.contains(exercise.name),
                onToggle = {
                    onExercisesChanged(
                        if (selectedExercises.contains(exercise.name))
                            selectedExercises - exercise.name
                        else
                            selectedExercises + exercise.name
                    )
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: MainExercise,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onToggle
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            CardDefaults.outlinedCardBorder()
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Large emoji icon in a circle background
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = exercise.emoji,
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                // Exercise name
                Text(
                    text = exercise.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// ─── Step 5: Gender ────────────────────────────────────────────────────

@Composable
private fun GenderStep(
    selectedGender: Gender?,
    onGenderSelected: (Gender) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "What's your gender?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Optional - helps personalize your fitness experience.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Gender.entries.forEach { gender ->
            GenderOption(
                gender = gender,
                isSelected = selectedGender == gender,
                onSelect = onGenderSelected
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
        }
    }
}

@Composable
private fun GenderOption(
    gender: Gender,
    isSelected: Boolean,
    onSelect: (Gender) -> Unit
) {
    val displayName = when (gender) {
        Gender.MALE -> "Male"
        Gender.FEMALE -> "Female"
        Gender.OTHER -> "Other"
        Gender.PREFER_NOT_TO_SAY -> "Prefer not to say"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = { onSelect(gender) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) null else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            RadioButton(
                selected = isSelected,
                onClick = { onSelect(gender) }
            )
        }
    }
}

// ─── Step 6: Date of Birth ─────────────────────────────────────────────

@Composable
private fun DateOfBirthStep(
    dateOfBirth: Long?,
    onDateSelected: (Long) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    if (dateOfBirth != null) {
        calendar.timeInMillis = dateOfBirth
    }

    var selectedDate by remember(dateOfBirth) {
        mutableStateOf(dateOfBirth)
    }

    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                selectedDate = cal.timeInMillis
                onDateSelected(cal.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Set max date to today
            datePicker.maxDate = System.currentTimeMillis()
            // Set min date to 120 years ago
            datePicker.minDate = System.currentTimeMillis() - (120L * 365 * 24 * 60 * 60 * 1000)
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "When's your birthday?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Optional - helps calculate age-appropriate fitness metrics.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { datePickerDialog.show() },
            colors = CardDefaults.cardColors(
                containerColor = if (selectedDate != null) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Date of Birth",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedDate?.let { dateFormat.format(Date(it)) } ?: "Select date",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = if (selectedDate != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedDate != null) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                }
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = "Select date",
                    tint = if (selectedDate != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
        }
    }
}

// ─── Step 7: Profile Photo ─────────────────────────────────────────────

@Composable
private fun ProfilePhotoStep(
    photoUrl: String?,
    onPhotoSelected: (String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var selectedPhotoUri by remember(photoUrl) { mutableStateOf<Uri?>(photoUrl?.let { Uri.parse(it) }) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            onPhotoSelected(it.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Add a profile photo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Optional - personalize your profile with a photo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Photo preview
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(
                    if (selectedPhotoUri != null) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .clickable { launcher.launch("image/*") }
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selectedPhotoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(selectedPhotoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Add photo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (selectedPhotoUri != null) "Change Photo" else "Select Photo")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Complete Profile")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.weight(1f)
            ) {
                Text("Skip")
            }
        }
    }
}

// ─── ViewModel ─────────────────────────────────────────────────────────

@HiltViewModel
class ProfileOnboardingViewModel @Inject constructor(
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val progressiveProfileManager: ProgressiveProfileManager
) : ViewModel() {

    data class OnboardingState(
        val currentStep: Int = 0,
        val totalSteps: Int = 7,
        val fitnessLevel: FitnessLevel? = null,
        val goals: Set<FitnessGoal> = emptySet(),
        val height: Float? = null,
        val weight: Float? = null,
        val unitSystem: UnitSystem = UnitSystem.METRIC,
        val preferredExercises: Set<String> = emptySet(),
        val gender: Gender? = null,
        val dateOfBirth: Long? = null,
        val photoUrl: String? = null,
        val hasExistingPhoto: Boolean = false  // Track if user already has photo from auth
    )

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun loadCurrentProgress(isEditMode: Boolean) {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserOnce() ?: return@launch

            if (isEditMode) {
                // Load all existing data for editing
                val goals = user.goalsJson?.let {
                    Gson().fromJson(it, Array<String>::class.java).mapNotNull { name ->
                        FitnessGoal.entries.find { it.name == name }
                    }.toSet()
                } ?: emptySet()

                val exercises = user.preferredExercises?.let {
                    Gson().fromJson(it, Array<String>::class.java).toSet()
                } ?: emptySet()

                _state.value = _state.value.copy(
                    fitnessLevel = user.fitnessLevel,
                    goals = goals,
                    height = user.height,
                    weight = user.weight,
                    unitSystem = user.preferredUnits,
                    preferredExercises = exercises,
                    gender = user.gender,
                    dateOfBirth = user.dateOfBirth,
                    photoUrl = user.photoUrl,
                    hasExistingPhoto = !user.photoUrl.isNullOrBlank() || !user.photoCloudUrl.isNullOrBlank()
                )
            }
        }
    }

    fun nextStep() {
        saveCurrentStepData()
        if (_state.value.currentStep < _state.value.totalSteps - 1) {
            _state.value = _state.value.copy(currentStep = _state.value.currentStep + 1)
        }
    }

    fun previousStep() {
        if (_state.value.currentStep > 0) {
            _state.value = _state.value.copy(currentStep = _state.value.currentStep - 1)
        }
    }

    fun skipToEnd() {
        _state.value = _state.value.copy(currentStep = _state.value.totalSteps - 1)
    }

    fun updateFitnessLevel(level: FitnessLevel) {
        _state.value = _state.value.copy(fitnessLevel = level)
    }

    fun updateGoals(goals: Set<FitnessGoal>) {
        _state.value = _state.value.copy(goals = goals)
    }

    fun updateHeight(height: Float?) {
        _state.value = _state.value.copy(height = height)
    }

    fun updateWeight(weight: Float?) {
        _state.value = _state.value.copy(weight = weight)
    }

    fun updateUnitSystem(system: UnitSystem) {
        _state.value = _state.value.copy(unitSystem = system)
    }

    fun updatePreferredExercises(exercises: Set<String>) {
        _state.value = _state.value.copy(preferredExercises = exercises)
    }

    fun updateGender(gender: Gender) {
        _state.value = _state.value.copy(gender = gender)
    }

    fun updateDateOfBirth(dob: Long) {
        _state.value = _state.value.copy(dateOfBirth = dob)
    }

    fun updatePhotoUrl(url: String) {
        _state.value = _state.value.copy(photoUrl = url)
    }

    fun saveProgress() {
        saveCurrentStepData()
    }

    private fun saveCurrentStepData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUserOnce() ?: return@launch
            val currentState = _state.value

            val updatedUser = user.copy(
                fitnessLevel = currentState.fitnessLevel,
                goalsJson = if (currentState.goals.isNotEmpty()) {
                    Gson().toJson(currentState.goals.map { it.name })
                } else user.goalsJson,
                height = currentState.height,
                weight = currentState.weight,
                preferredUnits = currentState.unitSystem,
                preferredExercises = if (currentState.preferredExercises.isNotEmpty()) {
                    Gson().toJson(currentState.preferredExercises.toList())
                } else user.preferredExercises,
                gender = currentState.gender,
                dateOfBirth = currentState.dateOfBirth,
                photoUrl = currentState.photoUrl ?: user.photoUrl,
                updatedAt = System.currentTimeMillis()
            )

            userDao.update(updatedUser)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            saveCurrentStepData()

            val user = authRepository.getCurrentUserOnce() ?: return@launch

            // Mark all prompts as completed
            PromptType.entries.forEach { promptType ->
                progressiveProfileManager.markPromptCompleted(user.userId, promptType)
            }
        }
    }
}
