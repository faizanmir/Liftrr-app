package org.liftrr.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import android.util.Log
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation3.ui.NavDisplay
import org.liftrr.ui.navigation.NavigationAnimations
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import kotlinx.serialization.Serializable
import org.liftrr.data.preferences.ThemePreferences
import org.liftrr.domain.workout.WorkoutReportHolder
import org.liftrr.ml.ExerciseType
import org.liftrr.ui.screens.connection.DeviceConnectionScreen
import org.liftrr.ui.screens.analytics.AnalyticsScreen
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
    @Serializable data object Analytics : Screen()
    @Serializable data object Settings : Screen()

    // Workout flow
    @Serializable data object SessionSetup : Screen()
    @Serializable data class ExerciseSelection(val mode: WorkoutMode) : Screen()
    @Serializable data class WorkoutPreparation(val mode: WorkoutMode, val exerciseType: String) : Screen()
    @Serializable data class Workout(val mode: WorkoutMode, val exerciseType: String, val weight: Float? = null) : Screen()
    @Serializable data object WorkoutSummary : Screen()
    @Serializable data class WorkoutPlayback(val sessionId: String) : Screen()
}

// ─── Navigation Helpers ──────────────────────────────────────────────────────

/**
 * Top-level destinations - back gesture exits app instead of navigating back
 * These are the main "home" screens where user can access primary app features
 */
private fun isTopLevelDestination(screen: NavKey) = screen is Screen.Home ||
        screen is Screen.History ||
        screen is Screen.Analytics ||
        screen is Screen.Settings

/**
 * Determines if a screen should use modal animations (slide up from bottom)
 */
private fun isModalScreen(screen: NavKey) = screen is Screen.Settings ||
        screen is Screen.WorkoutSummary

/**
 * Determines if a screen should use fade animations (onboarding)
 */
private fun isFadeScreen(screen: NavKey) = screen is Screen.Welcome ||
        screen is Screen.PermissionScreen ||
        screen is Screen.DeviceConnectionScreen ||
        screen is Screen.CreateProfile

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

    // Track previous backstack size to detect forward vs back navigation
    val previousBackStackSize = remember { mutableIntStateOf(backStack.size) }
    val isNavigatingBack = backStack.size < previousBackStackSize.intValue
    LaunchedEffect(backStack.size) {
        previousBackStackSize.intValue = backStack.size
    }

    // Navigation functions
    fun navigate(screen: Screen) { backStack.add(screen) }

    // For screens that need direct back callback (WorkoutScreen, etc.)
    val goBack: () -> Unit = { backStack.removeLastOrNull() }

    // Current screen to determine back behavior
    val currentScreen = backStack.lastOrNull() ?: startDestination

    // Only Home screen allows system back to exit app
    // All other screens intercept back to navigate
    val isHome = currentScreen is Screen.Home
    val shouldInterceptBack = !isHome

    LiftrrTheme(darkTheme = darkTheme, dynamicColor = useDynamicColor) {
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcherOwner) {
            // Handle system back gesture/button
            // Only Home screen allows back to exit app
            // All other screens navigate back or to Home
            BackHandler(enabled = shouldInterceptBack) {
                if (backStack.size > 1) {
                    // Has previous screen in backstack: navigate back
                    Log.d("LiftrrNav", "Back: Navigating from $currentScreen to previous screen")
                    backStack.removeLastOrNull()
                } else {
                    // No previous screen: navigate to Home
                    Log.d("LiftrrNav", "Back: Navigating from $currentScreen to Home (no backstack)")
                    backStack.clear()
                    backStack.add(Screen.Home)
                }
            }

            // Log navigation state for debugging
            LaunchedEffect(currentScreen, shouldInterceptBack, backStack.size) {
                Log.d("LiftrrNav", "Current screen: $currentScreen")
                Log.d("LiftrrNav", "Is Home: $isHome")
                Log.d("LiftrrNav", "BackHandler enabled: $shouldInterceptBack")
                Log.d("LiftrrNav", "BackStack size: ${backStack.size}")
            }

            // Material Motion animations based on screen type and navigation direction
            // Use remember to avoid recomposing animation on every state change
            val currentDestination = remember(backStack.size) {
                backStack.lastOrNull() ?: startDestination
            }

            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    val targetScreen = targetState
                    val initialScreen = initialState

                    when {
                        // Back navigation: scale out effect for all screens
                        isNavigatingBack -> {
                            NavigationAnimations.popEnterNone() togetherWith
                                    NavigationAnimations.popExitScaleOut()
                        }

                        // Modal screens: slide up from bottom
                        isModalScreen(targetScreen) -> {
                            NavigationAnimations.slideUpFromBottom() togetherWith
                                    NavigationAnimations.dimBackground()
                        }

                        // Onboarding screens: simple fade
                        isFadeScreen(targetScreen) || isFadeScreen(initialScreen) -> {
                            NavigationAnimations.fadeIn() togetherWith
                                    NavigationAnimations.fadeOut()
                        }

                        // Forward navigation: horizontal slide
                        else -> {
                            NavigationAnimations.slideInFromRight() togetherWith
                                    NavigationAnimations.slideOutToLeft()
                        }
                    }
                },
                label = "nav_animation"
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = {}, // No-op: we handle back with BackHandler above
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
            onNavigateToAnalytics = { navigate(Screen.Analytics) },
            onNavigateToProfile = { navigate(Screen.Settings) },
            onStartWorkout = { navigate(Screen.SessionSetup) },
            onWorkoutClick = { id -> navigate(Screen.WorkoutPlayback(id)) }
        )
    }

    entry<Screen.History> {
        HistoryScreen(
            onNavigateBack = goBack,  // Toolbar back button works
            onWorkoutClick = { id -> navigate(Screen.WorkoutPlayback(id)) }
        )
    }

    entry<Screen.Analytics> {
        AnalyticsScreen(onNavigateBack = goBack)  // Toolbar back button works
    }

    entry<Screen.Settings> {
        ProfileScreen(
            onNavigateBack = goBack,  // Toolbar back button works
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

