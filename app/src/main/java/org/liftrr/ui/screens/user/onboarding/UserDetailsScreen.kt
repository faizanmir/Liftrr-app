package org.liftrr.ui.screens.user.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.liftrr.domain.user.FitnessLevel
import org.liftrr.domain.user.Gender
import org.liftrr.domain.user.UnitSystem
import org.liftrr.ui.theme.LiftrrTheme

@Composable
fun UserDetailsScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    viewModel: UserDetailsViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is UserDetailsSaveState.Success) {
            viewModel.resetSaveState()
            onComplete()
        }
    }

    UserDetailsContent(
        formState = formState,
        onFirstNameChange = viewModel::setFirstName,
        onLastNameChange = viewModel::setLastName,
        onGenderSelect = viewModel::setGender,
        onHeightChange = viewModel::setHeight,
        onHeightUnitToggle = viewModel::toggleHeightUnit,
        onFitnessLevelSelect = viewModel::setFitnessLevel,
        onGoalToggle = viewModel::toggleGoal,
        onWeightChange = viewModel::setWeight,
        onPreferredExerciseSelect = viewModel::setPreferredExercise,
        onPreferredUnitsToggle = viewModel::togglePreferredUnits,
        onDobChange = viewModel::setDob,
        onContinue = viewModel::saveProfile,
        onSkip = onSkip
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsContent(
    formState: UserDetailsFormState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onGenderSelect: (Gender) -> Unit,
    onHeightChange: (Float) -> Unit,
    onHeightUnitToggle: () -> Unit,
    onFitnessLevelSelect: (FitnessLevel) -> Unit,
    onGoalToggle: (String) -> Unit,
    onWeightChange: (Float) -> Unit,
    onPreferredExerciseSelect: (org.liftrr.domain.workout.ExerciseType) -> Unit,
    onPreferredUnitsToggle: () -> Unit,
    onDobChange: (Long) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var showDobPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = formState.dob)
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    if (showDobPicker) {
        DatePickerDialog(
            onDismissRequest = { showDobPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDobChange(it) }
                    showDobPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDobPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                LinearProgressIndicator(
                    progress = { 0.5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Tell us about yourself",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This helps us personalize your workout experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Scrollable sections ───────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {

                // ── Name ──────────────────────────────────────────────────
                SectionHeader(title = "Your Name")
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = formState.firstName,
                    onValueChange = onFirstNameChange,
                    label = { Text("First Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = formState.lastName,
                    onValueChange = onLastNameChange,
                    label = { Text("Last Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Gender ─────────────────────────────────────────────────
                SectionHeader(title = "Gender")
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Gender.MALE to "Male",
                        Gender.FEMALE to "Female"
                    ).forEach { (gender, label) ->
                        SelectableChip(
                            label = label,
                            selected = formState.gender == gender,
                            onClick = { onGenderSelect(gender) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Gender.OTHER to "Other",
                        Gender.PREFER_NOT_TO_SAY to "Prefer not to say"
                    ).forEach { (gender, label) ->
                        SelectableChip(
                            label = label,
                            selected = formState.gender == gender,
                            onClick = { onGenderSelect(gender) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Height ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Height")
                    TextButton(onClick = onHeightUnitToggle) {
                        Text(
                            text = if (formState.heightUnit == UnitSystem.METRIC) "Switch to ft/in" else "Switch to cm",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = UserDetailsViewModel.cmToDisplay(formState.heightCm, formState.heightUnit),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = formState.heightCm,
                    onValueChange = onHeightChange,
                    valueRange = 100f..220f,
                    steps = 0,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (formState.heightUnit == UnitSystem.METRIC) "100 cm" else "3'3\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (formState.heightUnit == UnitSystem.METRIC) "220 cm" else "7'3\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Fitness Level ─────────────────────────────────────────
                SectionHeader(title = "Fitness Level")
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        FitnessLevel.BEGINNER to Triple("Beginner", "Just starting out", "0–6 months"),
                        FitnessLevel.INTERMEDIATE to Triple("Intermediate", "Some experience", "6m–2 years"),
                        FitnessLevel.ADVANCED to Triple("Advanced", "Consistent training", "2–5 years"),
                        FitnessLevel.ELITE to Triple("Elite", "Competitive athlete", "5+ years")
                    ).forEach { (level, info) ->
                        val (title, subtitle, duration) = info
                        FitnessLevelCard(
                            title = title,
                            subtitle = subtitle,
                            duration = duration,
                            selected = formState.fitnessLevel == level,
                            onClick = { onFitnessLevelSelect(level) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Goals ─────────────────────────────────────────────────
                SectionHeader(title = "Your Goals")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select all that apply",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FITNESS_GOALS.forEach { goal ->
                        SelectableChip(
                            label = goal,
                            selected = goal in formState.selectedGoals,
                            onClick = { onGoalToggle(goal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Date of Birth ─────────────────────────────────────────────
                SectionHeader(title = "Date of Birth")
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dateFormatter.format(Date(formState.dob)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date of Birth") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showDobPicker = true }) {
                            Text("Change", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Body Weight ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "Body Weight")
                    TextButton(onClick = onPreferredUnitsToggle) {
                        Text(
                            text = if (formState.preferredUnits == UnitSystem.METRIC) "Switch to lbs" else "Switch to kg",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val weightDisplay = if (formState.preferredUnits == UnitSystem.METRIC) {
                        "${formState.weight?.toInt() ?: 0} kg"
                    } else {
                        "${((formState.weight ?: 0f) * 2.205f).toInt()} lbs"
                    }
                    Text(
                        text = weightDisplay,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = formState.weight ?: 70f,
                    onValueChange = onWeightChange,
                    valueRange = 30f..200f,
                    steps = 0,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (formState.preferredUnits == UnitSystem.METRIC) "30 kg" else "66 lbs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (formState.preferredUnits == UnitSystem.METRIC) "200 kg" else "441 lbs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Preferred Exercise ────────────────────────────────────────
                SectionHeader(title = "Preferred Exercise")
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    org.liftrr.domain.workout.ExerciseType.entries.forEach { type ->
                        SelectableChip(
                            label = type.displayName(),
                            selected = formState.preferredExerciseType == type,
                            onClick = { onPreferredExerciseSelect(type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Bottom action buttons ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Skip")
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !formState.isSaving
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Continue",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable sub-components ───────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(150),
        label = "chip_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(150),
        label = "chip_fg"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun FitnessLevelCard(
    title: String,
    subtitle: String,
    duration: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(150),
        label = "fitness_card_bg"
    )

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = duration,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(name = "User Details - Light", showBackground = true, showSystemUi = true)
@Composable
fun UserDetailsScreenLightPreview() {
    LiftrrTheme(darkTheme = false) {
        UserDetailsContent(
            formState = UserDetailsFormState(
                gender = Gender.MALE,
                fitnessLevel = FitnessLevel.INTERMEDIATE,
                selectedGoals = setOf("Build Muscle", "Increase Strength")
            ),
            onFirstNameChange = {},
            onLastNameChange = {},
            onGenderSelect = {},
            onHeightChange = {},
            onHeightUnitToggle = {},
            onFitnessLevelSelect = {},
            onGoalToggle = {},
            onWeightChange = {},
            onPreferredExerciseSelect = {},
            onPreferredUnitsToggle = {},
            onDobChange = {},
            onContinue = {},
            onSkip = {}
        )
    }
}

@Preview(name = "User Details - Dark", showBackground = true, showSystemUi = true)
@Composable
fun UserDetailsScreenDarkPreview() {
    LiftrrTheme(darkTheme = true) {
        UserDetailsContent(
            formState = UserDetailsFormState(
                gender = Gender.FEMALE,
                heightCm = 165f,
                fitnessLevel = FitnessLevel.BEGINNER,
                selectedGoals = setOf("Lose Weight", "General Fitness")
            ),
            onFirstNameChange = {},
            onLastNameChange = {},
            onGenderSelect = {},
            onHeightChange = {},
            onHeightUnitToggle = {},
            onFitnessLevelSelect = {},
            onGoalToggle = {},
            onWeightChange = {},
            onPreferredExerciseSelect = {},
            onPreferredUnitsToggle = {},
            onDobChange = {},
            onContinue = {},
            onSkip = {}
        )
    }
}
