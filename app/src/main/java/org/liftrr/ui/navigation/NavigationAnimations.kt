package org.liftrr.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.graphics.TransformOrigin

/**
 * Navigation animation configurations following Material Design 3 motion principles.
 *
 * Animation types:
 * - SLIDE: Left-right slide for main navigation (Home, History, Analytics)
 * - MODAL: Bottom-up slide for modal screens (Settings, Profile, Summary)
 * - FADE: Cross-fade for onboarding/simple transitions
 *
 * Timing: 350ms (Material Design recommended duration for screen transitions)
 */
object NavigationAnimations {

    // Animation duration in milliseconds - optimized for smoothness
    private const val ANIMATION_DURATION = 300  // Reduced from 350ms for snappier feel

    // Slide distance for enter/exit animations (as fraction of screen width/height)
    private const val SLIDE_DISTANCE = 0.10f  // Reduced from 0.15f for less work

    /**
     * Material Design easing function for natural motion
     * Fast start, slow end - feels responsive and smooth
     */
    private val materialEasing = FastOutSlowInEasing

    // ─── Forward Navigation (entering new screen) ────────────────────────────

    /**
     * Slide in from right (for forward navigation)
     * Used when navigating deeper into the app hierarchy
     */
    fun slideInFromRight(): EnterTransition = slideInHorizontally(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        initialOffsetX = { fullWidth -> (fullWidth * SLIDE_DISTANCE).toInt() }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        initialAlpha = 0.8f
    )

    /**
     * Slide out to left (when new screen enters from right)
     * Previous screen moves slightly and fades
     */
    fun slideOutToLeft(): ExitTransition = slideOutHorizontally(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        targetOffsetX = { fullWidth -> -(fullWidth * SLIDE_DISTANCE).toInt() }
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        targetAlpha = 0.8f
    )

    // ─── Back Navigation (returning to previous screen) ──────────────────────

    /**
     * No animation for entering previous screen (when going back)
     * Previous screen just appears underneath the scaling current screen
     */
    fun popEnterNone(): EnterTransition = EnterTransition.None

    /**
     * Scale out effect when going back (leaving current screen)
     * Screen shrinks to 90% from center, creating a "zoom back" effect
     * This is the modern Material Design pattern for back navigation
     */
    fun popExitScaleOut(): ExitTransition = scaleOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        targetScale = 0.9f,
        transformOrigin = TransformOrigin(
            pivotFractionX = 0.5f,  // Center X
            pivotFractionY = 0.5f   // Center Y
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        )
    )

    // ─── Modal Transitions (bottom sheet style) ──────────────────────────────

    /**
     * Slide up from bottom (for modal screens)
     * Used for Settings, Profile, WorkoutSummary - feels like opening a sheet
     */
    fun slideUpFromBottom(): EnterTransition = slideInVertically(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        initialOffsetY = { fullHeight -> fullHeight }
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION / 2, // Fade in faster
            easing = materialEasing
        )
    )

    /**
     * Slide down to bottom (when closing modal)
     * Modal slides down and fades out
     */
    fun slideDownToBottom(): ExitTransition = slideOutVertically(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        targetOffsetY = { fullHeight -> fullHeight }
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION / 2,
            easing = materialEasing
        )
    )

    /**
     * Dim background when modal enters
     * Background screen fades slightly when modal appears
     */
    fun dimBackground(): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        targetAlpha = 0.95f
    )

    /**
     * Restore background when modal exits
     * Background screen fades back to full opacity
     */
    fun restoreBackground(): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        ),
        initialAlpha = 0.95f
    )

    // ─── Simple Fade Transitions ─────────────────────────────────────────────

    /**
     * Simple fade in (for onboarding or simple screens)
     */
    fun fadeIn(): EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        )
    )

    /**
     * Simple fade out
     */
    fun fadeOut(): ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = ANIMATION_DURATION,
            easing = materialEasing
        )
    )
}
