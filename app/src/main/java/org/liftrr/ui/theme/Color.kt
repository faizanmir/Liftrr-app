package org.liftrr.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * LIFTRR Color Palette - Energetic Fitness Theme
 *
 * Vibrant, high-energy colors designed for fitness and performance tracking
 * Primary: Electric Orange (energy, intensity, movement)
 * Secondary: Deep Ocean Blue (stability, trust)
 * Tertiary: Vibrant Cyan (performance, freshness)
 */

// Seed / Brand color - Electric Orange
val Seed = Color(0xFFFF6B35)

// ============================================
// Light Theme Colors - ENERGETIC FITNESS
// ============================================
val md_theme_light_primary = Color(0xFFFF6B35)           // Electric Orange
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFFFDAD1)  // Light peach
val md_theme_light_onPrimaryContainer = Color(0xFF3A0A00)

val md_theme_light_secondary = Color(0xFF1565C0)         // Deep Blue
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD4E4FF)
val md_theme_light_onSecondaryContainer = Color(0xFF001C38)

val md_theme_light_tertiary = Color(0xFF00BCD4)          // Vibrant Cyan
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFB3F5FF)
val md_theme_light_onTertiaryContainer = Color(0xFF003640)

val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onErrorContainer = Color(0xFF410E0B)

val md_theme_light_background = Color(0xFFFFFBFE)
val md_theme_light_onBackground = Color(0xFF1C1B1F)
val md_theme_light_surface = Color(0xFFFFFBFE)
val md_theme_light_onSurface = Color(0xFF1C1B1F)
val md_theme_light_surfaceVariant = Color(0xFFE7E0EC)
val md_theme_light_onSurfaceVariant = Color(0xFF49454F)

val md_theme_light_outline = Color(0xFF79747E)
val md_theme_light_outlineVariant = Color(0xFFCAC4D0)

val md_theme_light_inverseSurface = Color(0xFF313033)
val md_theme_light_inverseOnSurface = Color(0xFFF4EFF4)
val md_theme_light_inversePrimary = Color(0xFFD0BCFF)

val md_theme_light_surfaceTint = Color(0xFF6750A4)
val md_theme_light_scrim = Color(0xFF000000)

// ============================================
// Dark Theme Colors - ENERGETIC FITNESS
// ============================================
val md_theme_dark_primary = Color(0xFFFFB4A3)            // Soft coral (dark mode primary)
val md_theme_dark_onPrimary = Color(0xFF5F1600)
val md_theme_dark_primaryContainer = Color(0xFFD84315)   // Deep orange
val md_theme_dark_onPrimaryContainer = Color(0xFFFFDAD1)

val md_theme_dark_secondary = Color(0xFF90CAF9)          // Light blue
val md_theme_dark_onSecondary = Color(0xFF003258)
val md_theme_dark_secondaryContainer = Color(0xFF004B7C)
val md_theme_dark_onSecondaryContainer = Color(0xFFD4E4FF)

val md_theme_dark_tertiary = Color(0xFF80DEEA)           // Light cyan
val md_theme_dark_onTertiary = Color(0xFF00363D)
val md_theme_dark_tertiaryContainer = Color(0xFF00525E)
val md_theme_dark_onTertiaryContainer = Color(0xFFB3F5FF)

val md_theme_dark_error = Color(0xFFF2B8B5)
val md_theme_dark_onError = Color(0xFF601410)
val md_theme_dark_errorContainer = Color(0xFF8C1D18)
val md_theme_dark_onErrorContainer = Color(0xFFF9DEDC)

val md_theme_dark_background = Color(0xFF1C1B1F)
val md_theme_dark_onBackground = Color(0xFFE6E1E5)
val md_theme_dark_surface = Color(0xFF1C1B1F)
val md_theme_dark_onSurface = Color(0xFFE6E1E5)
val md_theme_dark_surfaceVariant = Color(0xFF49454F)
val md_theme_dark_onSurfaceVariant = Color(0xFFCAC4D0)

val md_theme_dark_outline = Color(0xFF938F99)
val md_theme_dark_outlineVariant = Color(0xFF49454F)

val md_theme_dark_inverseSurface = Color(0xFFE6E1E5)
val md_theme_dark_inverseOnSurface = Color(0xFF313033)
val md_theme_dark_inversePrimary = Color(0xFF6750A4)

val md_theme_dark_surfaceTint = Color(0xFFD0BCFF)
val md_theme_dark_scrim = Color(0xFF000000)

// ============================================
// Energetic Accent Colors (for special UI elements)
// ============================================
object EnergyColors {
    val electricOrange = Color(0xFFFF6B35)    // Primary brand
    val vibrantCyan = Color(0xFF00BCD4)       // Performance
    val limeGreen = Color(0xFF7CB342)         // Success/Growth
    val hotPink = Color(0xFFE91E63)           // Highlights
    val deepBlue = Color(0xFF1565C0)          // Stability
    val sunsetOrange = Color(0xFFFF9800)      // Warmth
}

// ============================================
// Semantic Colors for Metrics (Updated for Energy)
// ============================================
object MetricColors {
    // Velocity zones
    val velocityHigh = Color(0xFF7CB342)   // Lime green - fast/good
    val velocityMedium = Color(0xFFFF9800) // Vibrant orange - moderate
    val velocityLow = Color(0xFFE91E63)    // Hot pink - slow/fatigue

    // Range of motion
    val romGood = Color(0xFF00BCD4)        // Cyan - excellent
    val romWarning = Color(0xFFFF9800)     // Orange - needs work
    val romPoor = Color(0xFFFF5252)        // Bright red - poor

    // Calibration status (0-3 scale)
    val calibrationGood = Color(0xFF00BCD4)   // Cyan - 3
    val calibrationFair = Color(0xFFFF9800)   // Orange - 2
    val calibrationPoor = Color(0xFFFF5252)   // Red - 0-1

    // BLE connection
    val connectionConnected = Color(0xFF7CB342)   // Lime - connected
    val connectionConnecting = Color(0xFFFF9800)  // Orange - connecting
    val connectionDisconnected = Color(0xFF9E9E9E)

    // Battery levels
    val batteryHigh = Color(0xFF7CB342)    // Lime - >50%
    val batteryMedium = Color(0xFFFF9800)  // Orange - 20-50%
    val batteryLow = Color(0xFFFF5252)     // Red - <20%

    // Device orientation
    val orientationOk = Color(0xFF00BCD4)
    val orientationWarning = Color(0xFFFF9800)
    val orientationError = Color(0xFFFF5252)
}

// ============================================
// Chart Colors
// ============================================
object ChartColors {
    // Sensor data lines
    val distanceLine = Color(0xFF2196F3)     // Blue
    val velocityLine = Color(0xFF4CAF50)     // Green
    val accelerationLine = Color(0xFFFF9800) // Orange

    // Orientation angles
    val rollLine = Color(0xFF9C27B0)   // Purple
    val pitchLine = Color(0xFFE91E63)  // Pink
    val yawLine = Color(0xFF00BCD4)    // Cyan

    // Rep phases
    val repConcentric = Color(0xFF4CAF50)  // Green - lifting
    val repEccentric = Color(0xFF2196F3)   // Blue - lowering
    val repMarker = Color(0xFFFF5722)      // Orange - rep boundary

    // Bar path
    val barPathLine = Color(0xFF6750A4)    // Primary purple
    val barPathIdeal = Color(0xFF4CAF50)   // Green reference
}
