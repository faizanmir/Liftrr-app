package org.liftrr.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.liftrr.data.models.PromptType

/**
 * Data class for progressive profile prompt configuration
 */
data class PromptConfig(
    val icon: ImageVector,
    val title: String,
    val message: String,
    val primaryButtonText: String = "Let's do it!",
    val secondaryButtonText: String = "Later",
    val showDismissForever: Boolean = true
)

/**
 * Get prompt configuration for each prompt type
 */
fun getPromptConfig(promptType: PromptType): PromptConfig {
    return when (promptType) {
        PromptType.FITNESS_LEVEL -> PromptConfig(
            icon = Icons.Default.FitnessCenter,
            title = "What's your fitness level?",
            message = "Help us personalize your experience by telling us your fitness level. This helps us provide better recommendations!",
            primaryButtonText = "Set fitness level"
        )
        PromptType.GOALS -> PromptConfig(
            icon = Icons.Default.Flag,
            title = "What are your goals?",
            message = "Setting goals helps you stay motivated and track your progress. What would you like to achieve?",
            primaryButtonText = "Set my goals"
        )
        PromptType.BODY_STATS -> PromptConfig(
            icon = Icons.Default.Scale,
            title = "Add your body stats",
            message = "Your height and weight help us provide more accurate tracking and recommendations.",
            primaryButtonText = "Add stats"
        )
        PromptType.PREFERRED_EXERCISES -> PromptConfig(
            icon = Icons.Default.Star,
            title = "What are your favorite exercises?",
            message = "Tell us which exercises you enjoy most, and we'll help you incorporate them into your routine.",
            primaryButtonText = "Choose exercises"
        )
        PromptType.NOTIFICATIONS -> PromptConfig(
            icon = Icons.Default.Notifications,
            title = "Stay on track with reminders",
            message = "Would you like to receive notifications to help you stay consistent with your workouts?",
            primaryButtonText = "Enable notifications",
            secondaryButtonText = "No thanks",
            showDismissForever = true
        )
        PromptType.REMINDER_TIME -> PromptConfig(
            icon = Icons.Default.Schedule,
            title = "When do you usually work out?",
            message = "Set a reminder time to help build a consistent workout routine.",
            primaryButtonText = "Set reminder"
        )
        PromptType.PROFILE_PHOTO -> PromptConfig(
            icon = Icons.Default.Person,
            title = "Add a profile photo",
            message = "Personalize your profile with a photo. It's completely optional!",
            primaryButtonText = "Add photo",
            showDismissForever = true
        )
        PromptType.COMPLETE_PROFILE -> PromptConfig(
            icon = Icons.Default.CheckCircle,
            title = "Complete your profile",
            message = "You're ${70}% there! Completing your profile unlocks personalized insights and recommendations.",
            primaryButtonText = "Complete profile"
        )
    }
}

/**
 * Bottom sheet modal for progressive profile prompts
 * Shows a friendly, non-intrusive prompt to collect user information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressiveProfilePromptBottomSheet(
    promptType: PromptType,
    profileCompleteness: Float = 0f, // 0.0 to 1.0
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    onDismissForever: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val config = getPromptConfig(promptType).let {
        if (promptType == PromptType.COMPLETE_PROFILE) {
            it.copy(
                message = "You're ${(profileCompleteness * 100).toInt()}% there! Completing your profile unlocks personalized insights and recommendations."
            )
        } else it
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = config.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            Text(
                text = config.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary button
            Button(
                onClick = {
                    onComplete()
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(config.primaryButtonText)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary button (Later)
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(config.secondaryButtonText)
            }

            // "Don't ask again" option
            if (config.showDismissForever) {
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        onDismissForever()
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Don't ask again",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Compact card prompt (alternative to bottom sheet)
 * Can be shown inline in the UI (e.g., on home screen)
 */
@Composable
fun ProgressiveProfilePromptCard(
    promptType: PromptType,
    profileCompleteness: Float = 0f,
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = getPromptConfig(promptType)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Tap to complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
