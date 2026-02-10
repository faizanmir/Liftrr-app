package org.liftrr.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
fun OnBoardingScreenHeadline(text : String, fontWeight: FontWeight = FontWeight.W500) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = fontWeight
    )
}


@Composable
fun OnBoardingScreenHeadlineDescription(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}