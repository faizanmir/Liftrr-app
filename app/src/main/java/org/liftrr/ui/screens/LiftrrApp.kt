package org.liftrr.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import kotlinx.serialization.Serializable
import org.liftrr.data.preferences.ThemePreferences
import org.liftrr.domain.workout.WorkoutReportHolder
import org.liftrr.ml.ExerciseType
import org.liftrr.ui.screens.connection.DeviceConnectionScreen
import org.liftrr.ui.screens.history.HistoryScreen
import org.liftrr.ui.screens.home.HomeScreen
import org.liftrr.ui.screens.permissions.PermissionScreen
import org.liftrr.ui.screens.playback.WorkoutPlaybackScreen
import org.liftrr.ui.screens.session.SessionSetupScreen
import org.liftrr.ui.screens.session.WorkoutMode
import org.liftrr.ui.screens.user.profile.AuthenticationScreen
import org.liftrr.ui.screens.user.profile.ProfileScreen
import org.liftrr.ui.screens.welcome.WelcomeScreen
import org.liftrr.ui.screens.workout.ExerciseSelectionScreen
import org.liftrr.ui.screens.workout.WorkoutPreparationScreen
import org.liftrr.ui.screens.workout.WorkoutScreen
import org.liftrr.ui.screens.workout.WorkoutSummaryScreen
import org.liftrr.ui.theme.LiftrrTheme

// ─── Screen Definitions ─────────────────────────────────────────────────────

sealed class Screen : NavKey {
    // Onboarding
    @Serializable data object Welcome : Screen()
    @Serializable data object PermissionScreen : Screen()
    @Serializable data object DeviceConnectionScreen : Screen()
    @Serializable data class CreateProfile(val returnToProfile: Boolean = false) : Screen()

    // Main
    @Serializable data object Home : Screen()
    @Serializable data object History : Screen()
    @Serializable data object Settings : Screen()

    // Workout flow
    @Serializable data object SessionSetup : Screen()
    @Serializable data class ExerciseSelection(val mode: WorkoutMode) : Screen()
    @Serializable data class WorkoutPreparation(val mode: WorkoutMode, val exerciseType: String) : Screen()
    @Serializable data class Workout(val mode: WorkoutMode, val exerciseType: String, val weight: Float? = null) : Screen()
    @Serializable data object WorkoutSummary : Screen()
    @Serializable data class WorkoutPlayback(val sessionId: String) : Screen()
}

// ─── App Root ────────────────────────────────────────────────────────────────

@Composable
fun LiftrrApp(appViewModel: AppViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }

    val useDynamicColor by themePreferences.useDynamicColor.collectAsState(initial = false)
    val useDarkMode by themePreferences.useDarkMode.collectAsState(initial = null)
    val isUserLoggedIn by appViewModel.isUserLoggedIn.collectAsState()

    val darkTheme = useDarkMode ?: isSystemInDarkTheme()
    val startDestination = if (isUserLoggedIn) Screen.Home else Screen.Welcome

    val backStack = rememberNavBackStack(startDestination)
    val dispatcherOwner = rememberNavigationEventDispatcherOwner(parent = null)

    val goBack: () -> Unit = { backStack.removeLastOrNull() }
    fun navigate(screen: Screen) { backStack.add(screen) }

    LiftrrTheme(darkTheme = darkTheme, dynamicColor = useDynamicColor) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcherOwner) {
            NavDisplay(
                backStack = backStack,
                onBack = goBack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = entryProvider {
                    onboardingEntries(goBack = goBack, navigate = ::navigate, backStack = backStack)
                    mainEntries(goBack = goBack, navigate = ::navigate)
                    workoutEntries(
                        goBack = goBack,
                        navigate = ::navigate,
                        backStack = backStack,
                        reportHolder = appViewModel.workoutReportHolder
                    )
                }
            )
        }
    }
}

// ─── Onboarding Entries ──────────────────────────────────────────────────────

private fun EntryProviderScope<NavKey>.onboardingEntries(
    goBack: () -> Unit,
    navigate: (Screen) -> Unit,
    backStack: MutableList<NavKey>
) {
    entry<Screen.Welcome> {
        WelcomeScreen { navigate(Screen.PermissionScreen) }
    }

    entry<Screen.PermissionScreen> {
        PermissionScreen { navigate(Screen.DeviceConnectionScreen) }
    }

    entry<Screen.DeviceConnectionScreen> {
        val isFromSessionSetup = backStack.contains(Screen.SessionSetup)
        DeviceConnectionScreen(
            onSkip = if (isFromSessionSetup) goBack else { { navigate(Screen.CreateProfile()) } },
            onConnectionSuccess = if (isFromSessionSetup) goBack else null
        )
    }

    entry<Screen.CreateProfile> { (returnToProfile) ->
        val onSuccess: () -> Unit =
            if (returnToProfile) goBack else { { navigate(Screen.Home) } }
        AuthenticationScreen(
            onSignInSuccess = onSuccess,
            onSkip = if (returnToProfile) goBack else { { navigate(Screen.Home) } },
            onSignUpSuccess = { onSuccess(); true }
        )
    }
}

// ─── Main Entries ────────────────────────────────────────────────────────────

private fun EntryProviderScope<NavKey>.mainEntries(
    goBack: () -> Unit,
    navigate: (Screen) -> Unit
) {
    entry<Screen.Home> {
        HomeScreen(
            onNavigateToHistory = { navigate(Screen.History) },
            onNavigateToAnalytics = { },
            onNavigateToProfile = { navigate(Screen.Settings) },
            onStartWorkout = { navigate(Screen.SessionSetup) },
            onWorkoutClick = { id -> navigate(Screen.WorkoutPlayback(id)) }
        )
    }

    entry<Screen.History> {
        HistoryScreen(
            onNavigateBack = goBack,
            onWorkoutClick = { id -> navigate(Screen.WorkoutPlayback(id)) }
        )
    }

    entry<Screen.Settings> {
        ProfileScreen(
            onNavigateBack = goBack,
            onLoginClick = { navigate(Screen.CreateProfile(returnToProfile = true)) }
        )
    }
}

// ─── Workout Flow Entries ────────────────────────────────────────────────────

private fun EntryProviderScope<NavKey>.workoutEntries(
    goBack: () -> Unit,
    navigate: (Screen) -> Unit,
    backStack: MutableList<NavKey>,
    reportHolder: WorkoutReportHolder
) {
    entry<Screen.SessionSetup> {
        SessionSetupScreen(
            onNavigateBack = goBack,
            onNavigateToDeviceConnection = { navigate(Screen.DeviceConnectionScreen) },
            onStartWorkout = { mode -> navigate(Screen.ExerciseSelection(mode)) }
        )
    }

    entry<Screen.ExerciseSelection> { (mode) ->
        ExerciseSelectionScreen(
            onNavigateBack = goBack,
            onExerciseSelected = { type -> navigate(Screen.WorkoutPreparation(mode, type.name)) }
        )
    }

    entry<Screen.WorkoutPreparation> { (mode, exerciseType) ->
        WorkoutPreparationScreen(
            workoutMode = mode,
            exerciseType = ExerciseType.valueOf(exerciseType),
            onNavigateBack = goBack,
            onStartRecording = { weight -> navigate(Screen.Workout(mode, exerciseType, weight)) }
        )
    }

    entry<Screen.Workout> { (mode, exerciseType, weight) ->
        WorkoutScreen(
            workoutMode = mode,
            exerciseType = ExerciseType.valueOf(exerciseType),
            weight = weight,
            onNavigateBack = goBack,
            onWorkoutComplete = { navigate(Screen.WorkoutSummary) }
        )
    }

    entry<Screen.WorkoutSummary> {
        val report = reportHolder.getReport()
        if (report != null) {
            WorkoutSummaryScreen(
                report = report,
                onNavigateBack = {
                    reportHolder.clearReport()
                    backStack.clear()
                    backStack.add(Screen.Home)
                }
            )
        } else {
            LaunchedEffect(Unit) {
                backStack.clear()
                backStack.add(Screen.Home)
            }
            Box(modifier = Modifier.fillMaxSize())
        }
    }

    entry<Screen.WorkoutPlayback> { (sessionId) ->
        WorkoutPlaybackScreen(sessionId = sessionId, onNavigateBack = goBack)
    }
}

