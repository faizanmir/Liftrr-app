package org.liftrr.ui.screens.user.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.liftrr.domain.auth.AuthRepository
import org.liftrr.domain.user.FitnessLevel
import org.liftrr.domain.user.Gender
import org.liftrr.domain.user.UnitSystem
import org.liftrr.domain.user.UserProfile
import org.liftrr.domain.user.UserProfileRepository
import org.liftrr.domain.workout.ExerciseType
import javax.inject.Inject

val FITNESS_GOALS = listOf(
    "Lose Weight",
    "Build Muscle",
    "Improve Endurance",
    "Increase Strength",
    "Improve Flexibility",
    "Athletic Performance",
    "General Fitness",
    "Stress Relief"
)

data class UserDetailsFormState(
    val firstName: String = "",
    val lastName: String = "",
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    val heightCm: Float = 170f,
    val heightUnit: UnitSystem = UnitSystem.METRIC,
    val fitnessLevel: FitnessLevel = FitnessLevel.BEGINNER,
    val selectedGoals: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val weight: Float? = 0f,
    val preferredExerciseType: ExerciseType = ExerciseType.DEADLIFT,
    val preferredUnits : UnitSystem = UnitSystem.METRIC,
    val dob : Long = System.currentTimeMillis()
) {

}

sealed class UserDetailsSaveState {
    data object Idle : UserDetailsSaveState()
    data object Success : UserDetailsSaveState()
    data class Error(val message: String) : UserDetailsSaveState()
}

@HiltViewModel
class UserDetailsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(UserDetailsFormState())
    val formState: StateFlow<UserDetailsFormState> = _formState.asStateFlow()

    private val _saveState = MutableStateFlow<UserDetailsSaveState>(UserDetailsSaveState.Idle)
    val saveState: StateFlow<UserDetailsSaveState> = _saveState.asStateFlow()

    init {
        loadExistingProfile()
    }

    private fun loadExistingProfile() {
        viewModelScope.launch {
            val user = userProfileRepository.getUserProfile() ?: return@launch
            _formState.update { state ->
                state.copy(
                    firstName = user.firstName.ifBlank { state.firstName },
                    lastName = user.lastName.ifBlank { state.lastName },
                    gender = user.gender,
                    heightCm = user.height,
                    fitnessLevel = user.fitnessLevel,
                    selectedGoals = user.goalsJson?.let { parseGoalsJson(it) } ?: state.selectedGoals
                )
            }
        }
    }

    fun setFirstName(value: String) = _formState.update { it.copy(firstName = value) }

    fun setLastName(value: String) = _formState.update { it.copy(lastName = value) }

    fun setGender(gender: Gender) = _formState.update { it.copy(gender = gender) }

    fun setHeight(heightCm: Float) = _formState.update { it.copy(heightCm = heightCm) }

    fun toggleHeightUnit() = _formState.update {
        it.copy(heightUnit = if (it.heightUnit == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC)
    }

    fun setFitnessLevel(level: FitnessLevel) = _formState.update { it.copy(fitnessLevel = level) }

    fun setWeight(weight: Float) = _formState.update { it.copy(weight = weight) }

    fun setPreferredExercise(type: ExerciseType) = _formState.update { it.copy(preferredExerciseType = type) }

    fun togglePreferredUnits() = _formState.update {
        it.copy(preferredUnits = if (it.preferredUnits == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC)
    }

    fun setDob(dob: Long) = _formState.update { it.copy(dob = dob) }

    fun toggleGoal(goal: String) = _formState.update { state ->
        val goals = state.selectedGoals.toMutableSet()
        if (goal in goals) goals.remove(goal) else goals.add(goal)
        state.copy(selectedGoals = goals)
    }

    fun saveProfile() {
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            try {
                val state = _formState.value
                val goalsJson = if (state.selectedGoals.isEmpty()) null
                else "[${state.selectedGoals.joinToString(",") { "\"$it\"" }}]"

                userProfileRepository.insertUserProfile(
                    UserProfile(
                        firstName = state.firstName.ifBlank { "" },
                        lastName = state.lastName.ifBlank { "" },
                        gender = state.gender,
                        height = state.heightCm,
                        fitnessLevel = state.fitnessLevel,
                        goalsJson = goalsJson,
                        userId = authRepository.getCurrentUserOnce()?.userId ?: throw Exception("User not signed in"),
                        dateOfBirth = state.dob,
                        weight = state.weight,
                        preferredExercises = state.preferredExerciseType,
                        preferredUnits = state.preferredUnits
                    )
                )
                _saveState.value = UserDetailsSaveState.Success
            } catch (e: Exception) {
                _saveState.value = UserDetailsSaveState.Error(e.message ?: "Failed to save profile")
            } finally {
                _formState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = UserDetailsSaveState.Idle
    }

    private fun parseGoalsJson(json: String): Set<String> {
        return try {
            json.trim('[', ']').split(",")
                .map { it.trim().trim('"') }
                .filter { it.isNotBlank() }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // Helpers for display conversion
    companion object {
        fun cmToDisplay(cm: Float, unit: UnitSystem): String {
            return if (unit == UnitSystem.METRIC) {
                "${cm.toInt()} cm"
            } else {
                val totalInches = (cm / 2.54).toInt()
                val feet = totalInches / 12
                val inches = totalInches % 12
                "${feet}' ${inches}\""
            }
        }
    }
}
