package org.liftrr.ui.screens.user.profile

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.liftrr.R
import org.liftrr.ui.theme.LiftrrTheme

private const val TAG = "AuthScreen"

/**
 * UI State for the authentication form
 */
data class AuthFormState(
    val isSignUp: Boolean = true,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * Stateful authentication screen with ViewModel integration
 * This is the entry point used in navigation
 */
@Composable
fun AuthenticationScreen(
    onSignInSuccess: () -> Unit,
    onSkip: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    onSignUpSuccess: () -> Boolean
) {
    val context = LocalContext.current

    var formState by remember { mutableStateOf(AuthFormState()) }
    val uiState by viewModel.uiState.collectAsState()

    // Handle authentication state changes
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Success -> {
                Log.d(TAG, "Authentication successful: ${state.user}")
                Toast.makeText(
                    context,
                    "Welcome ${state.user.firstName ?: state.user.email}!",
                    Toast.LENGTH_SHORT
                ).show()
                onSignInSuccess()
                viewModel.resetState()
            }

            is AuthUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }

            else -> {}
        }
    }

    // Update loading state from ViewModel
    LaunchedEffect(uiState) {
        formState = formState.copy(isLoading = uiState is AuthUiState.Loading)
    }

    AuthenticationScreenContent(
        formState = formState,
        onFormStateChange = { formState = it },
        onSignIn = { email, password ->
            viewModel.signInWithEmailPassword(email, password)
        },
        onSignUp = { email, password, firstName, lastName ->
            viewModel.signUpWithEmailPassword(email, password, firstName, lastName)
        },
        onGoogleSignIn = {
            viewModel.signInWithGoogle(context)
        },
        onSkip = onSkip
    )
}

/**
 * Stateless authentication screen content
 * This composable is pure and easy to preview
 */
@Composable
fun AuthenticationScreenContent(
    formState: AuthFormState,
    onFormStateChange: (AuthFormState) -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String, firstName: String, lastName: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    fun validateSignUp() {
        when {
            formState.firstName.isBlank() -> Toast.makeText(
                context, "First name is required", Toast.LENGTH_SHORT
            ).show()

            formState.lastName.isBlank() -> Toast.makeText(
                context, "Last name is required", Toast.LENGTH_SHORT
            ).show()

            formState.email.isBlank() -> Toast.makeText(
                context, "Email is required", Toast.LENGTH_SHORT
            ).show()

            !Patterns.EMAIL_ADDRESS.matcher(formState.email).matches() -> Toast.makeText(
                context, "Invalid email address", Toast.LENGTH_SHORT
            ).show()

            formState.password.isBlank() -> Toast.makeText(
                context, "Password is required", Toast.LENGTH_SHORT
            ).show()

            formState.password.length < 6 -> Toast.makeText(
                context,
                "Password must be at least 6 characters",
                Toast.LENGTH_SHORT
            ).show()

            formState.password != formState.confirmPassword -> Toast.makeText(
                context, "Passwords don't match", Toast.LENGTH_SHORT
            ).show()

            else -> onSignUp(
                formState.email,
                formState.password,
                formState.firstName,
                formState.lastName
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (formState.isSignUp) "Create Your Account" else "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email/Password Form
            if (formState.isSignUp) {
                OutlinedTextField(
                    value = formState.firstName,
                    onValueChange = { onFormStateChange(formState.copy(firstName = it)) },
                    label = { Text("First Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(
                            FocusDirection.Down
                        )
                    }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = formState.lastName,
                    onValueChange = { onFormStateChange(formState.copy(lastName = it)) },
                    label = { Text("Last Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        focusManager.moveFocus(
                            FocusDirection.Down
                        )
                    }),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = formState.email,
                onValueChange = { onFormStateChange(formState.copy(email = it)) },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.moveFocus(
                        FocusDirection.Down
                    )
                }),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = formState.password,
                onValueChange = { onFormStateChange(formState.copy(password = it)) },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { onFormStateChange(formState.copy(passwordVisible = !formState.passwordVisible)) }) {
                        Icon(
                            imageVector = if (formState.passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (formState.passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (formState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (formState.isSignUp) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = {
                        focusManager.clearFocus()
                        if (!formState.isSignUp) {
                            onSignIn(formState.email, formState.password)
                        }
                    }),
                singleLine = true
            )

            if (formState.isSignUp) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = formState.confirmPassword,
                    onValueChange = { onFormStateChange(formState.copy(confirmPassword = it)) },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            onFormStateChange(
                                formState.copy(
                                    confirmPasswordVisible = !formState.confirmPasswordVisible
                                )
                            )
                        }) {
                            Icon(
                                imageVector = if (formState.confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (formState.confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (formState.confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password, imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            validateSignUp()
                        }),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign In/Sign Up Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (formState.isSignUp) {
                        validateSignUp()
                    } else {
                        when {
                            formState.email.isBlank() -> Toast.makeText(
                                context, "Email is required", Toast.LENGTH_SHORT
                            ).show()

                            formState.password.isBlank() -> Toast.makeText(
                                context, "Password is required", Toast.LENGTH_SHORT
                            ).show()

                            else -> onSignIn(formState.email, formState.password)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !formState.isLoading
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (formState.isSignUp) "Sign Up" else "Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle Sign In / Sign Up
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (formState.isSignUp) "Already have an account?" else "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { onFormStateChange(formState.copy(isSignUp = !formState.isSignUp)) }) {
                    Text(
                        text = if (formState.isSignUp) "Sign In" else "Sign Up",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // OR Divider
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign-In Button
            GoogleSignInButton(
                isLoading = formState.isLoading,
                onClick = onGoogleSignIn
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSkip) {
                Text("Skip for now")
            }
        }
    }
}


/**
 * Google Sign-In Button Component
 */
@Composable
fun GoogleSignInButton(
    isLoading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    // Determine if the theme is dark based on background color luminance
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier.clickable(
            onClick = onClick, enabled = !isLoading
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp), strokeWidth = 2.dp
            )
        } else {
            Image(
                painter = painterResource(
                    id = if (isDarkTheme) R.drawable.android_dark_rd_ctn
                    else R.drawable.android_neutral_rd_ctn
                ),
                contentDescription = "Continue with Google",
            )
        }
    }
}


// ============================================>
// PREVIEWS
// ============================================>

@Preview(name = "Sign In Screen - Light", showBackground = true)
@Composable
fun AuthenticationScreenSignInPreview() {
    LiftrrTheme(darkTheme = false) {
        AuthenticationScreenContent(
            formState = AuthFormState(isSignUp = false),
            onFormStateChange = {},
            onSignIn = { _, _ -> },
            onSignUp = { _, _, _, _ -> },
            onGoogleSignIn = {},
            onSkip = {}
        )
    }
}

@Preview(name = "Sign In Screen - Dark", showBackground = true)
@Composable
fun AuthenticationScreenSignInPreviewDark() {
    LiftrrTheme(darkTheme = true) {
        AuthenticationScreenContent(
            formState = AuthFormState(isSignUp = false),
            onFormStateChange = {},
            onSignIn = { _, _ -> },
            onSignUp = { _, _, _, _ -> },
            onGoogleSignIn = {},
            onSkip = {}
        )
    }
}

@Preview(name = "Sign Up Screen - Light", showBackground = true)
@Composable
fun AuthenticationScreenSignUpPreview() {
    LiftrrTheme(darkTheme = false) {
        AuthenticationScreenContent(
            formState = AuthFormState(isSignUp = true),
            onFormStateChange = {},
            onSignIn = { _, _ -> },
            onSignUp = { _, _, _, _ -> },
            onGoogleSignIn = {},
            onSkip = {}
        )
    }
}

@Preview(name = "Sign Up Screen - Dark", showBackground = true)
@Composable
fun AuthenticationScreenSignUpPreviewDark() {
    LiftrrTheme(darkTheme = true) {
        AuthenticationScreenContent(
            formState = AuthFormState(isSignUp = true),
            onFormStateChange = {},
            onSignIn = { _, _ -> },
            onSignUp = { _, _, _, _ -> },
            onGoogleSignIn = {},
            onSkip = {}
        )
    }
}

@Preview(name = "Google Sign-In Button - Light", showBackground = true)
@Composable
fun GoogleSignInButtonPreview() {
    LiftrrTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoogleSignInButton(
                isLoading = false, onClick = {})
            GoogleSignInButton(
                isLoading = true, onClick = {})
        }
    }
}

@Preview(name = "Google Sign-In Button - Dark", showBackground = true)
@Composable
fun GoogleSignInButtonPreviewDark() {
    LiftrrTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GoogleSignInButton(
                isLoading = false, onClick = {})
            GoogleSignInButton(
                isLoading = true, onClick = {})
        }
    }
}
