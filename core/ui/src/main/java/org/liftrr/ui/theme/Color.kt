package org.liftrr.ui.theme

import androidx.compose.ui.graphics.Color

val Seed = Color(0xFFFF6B35)

val md_theme_light_primary = Color(0xFFFF6B35)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFFFDAD1)  // Light peach
val md_theme_light_onPrimaryContainer = Color(0xFF3A0A00)

val md_theme_light_secondary = Color(0xFF1565C0)         // Deep Blue
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD4E4FF)
val md_theme_light_onSecondaryContainer = Color(0xFF001C38)

val md_theme_light_tertiary = Color(0xFF00BCD4)
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

val md_theme_dark_primary = Color(0xFFFFB4A3)
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

object EnergyColors {
    val electricOrange = Color(0xFFFF6B35)
    val vibrantCyan = Color(0xFF00BCD4)
    val limeGreen = Color(0xFF7CB342)
    val hotPink = Color(0xFFE91E63)
    val deepBlue = Color(0xFF1565C0)
    val sunsetOrange = Color(0xFFFF9800)
}

object MetricColors {
    val velocityHigh = Color(0xFF7CB342)
    val velocityMedium = Color(0xFFFF9800)
    val velocityLow = Color(0xFFE91E63)

    val romGood = Color(0xFF00BCD4)
    val romWarning = Color(0xFFFF9800)
    val romPoor = Color(0xFFFF5252)

    val calibrationGood = Color(0xFF00BCD4)
    val calibrationFair = Color(0xFFFF9800)
    val calibrationPoor = Color(0xFFFF5252)

    val connectionConnected = Color(0xFF7CB342)
    val connectionConnecting = Color(0xFFFF9800)
    val connectionDisconnected = Color(0xFF9E9E9E)

    val batteryHigh = Color(0xFF7CB342)
    val batteryMedium = Color(0xFFFF9800)
    val batteryLow = Color(0xFFFF5252)

    val orientationOk = Color(0xFF00BCD4)
    val orientationWarning = Color(0xFFFF9800)
    val orientationError = Color(0xFFFF5252)
}

object ChartColors {
    val distanceLine = Color(0xFF2196F3)
    val velocityLine = Color(0xFF4CAF50)
    val accelerationLine = Color(0xFFFF9800)

    val rollLine = Color(0xFF9C27B0)
    val pitchLine = Color(0xFFE91E63)
    val yawLine = Color(0xFF00BCD4)

    val repConcentric = Color(0xFF4CAF50)
    val repEccentric = Color(0xFF2196F3)
    val repMarker = Color(0xFFFF5722)

    val barPathLine = Color(0xFF6750A4)
    val barPathIdeal = Color(0xFF4CAF50)
}
