package org.liftrr.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Single bottom sheet that triggers the onboarding flow
 * Simplified - no more individual prompt types
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressiveProfilePromptSheet(
    isComplete: Boolean, percentageString: String, onStartFlow: () -> Unit, onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Filled.CheckCircle else Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (isComplete) "Profile Complete!" else "Complete Your Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isComplete) "You're all set! You can edit your profile anytime."
                else "You're $percentageString there! Complete your profile to unlock personalized insights.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!isComplete) {
                Button(
                    onClick = {
                        onStartFlow()
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Let's Complete It!")
                }

                OutlinedButton(
                    onClick = onDismiss, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Later")
                }
            } else {
                Button(
                    onClick = onDismiss, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

/**
 * Container that shows prompt after workout
 */
@Composable
fun ProgressiveProfilePromptContainer(
    viewModel: ProgressiveProfileViewModel = hiltViewModel(), onStartOnboardingFlow: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    if (state.showPrompt) {
        ProgressiveProfilePromptSheet(
            isComplete = state.profileCompleteness.isComplete,
            percentageString = state.profileCompleteness.percentageString,
            onStartFlow = onStartOnboardingFlow,
            onDismiss = { viewModel.dismissPrompt() })
    }
}

/**
 * Check for prompts after workout
 */
@Composable
fun CheckForPromptAfterWorkout(
    viewModel: ProgressiveProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.checkForPromptAfterWorkout()
    }
}
