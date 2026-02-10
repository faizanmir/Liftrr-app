package org.liftrr.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shape System
 *
 * Material You uses more rounded corners for a softer, modern aesthetic.
 * These values follow the Material 3 design guidelines.
 */
val Shapes = Shapes(
    // Extra Small: Buttons, chips, small components
    extraSmall = RoundedCornerShape(4.dp),

    // Small: Filled buttons, text fields
    small = RoundedCornerShape(8.dp),

    // Medium: Cards, dialogs (default for most surfaces)
    medium = RoundedCornerShape(12.dp),

    // Large: FABs, large cards, bottom sheets
    large = RoundedCornerShape(16.dp),

    // Extra Large: Modals, navigation drawers
    extraLarge = RoundedCornerShape(28.dp)
)