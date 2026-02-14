package org.liftrr.data.services

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import org.liftrr.di.annotations.creds.GoogleSignInClientId
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthService @Inject constructor(
    @param:GoogleSignInClientId private val webClientId: String
) {
    private companion object {
        const val TAG = "GoogleAuthService"
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Starting Google Sign-In")
            Log.d(TAG, "WEB_CLIENT_ID: $webClientId")

            val credentialManager = CredentialManager.create(context)
            val nonce = generateNonce()
            val googleIdOption = buildGoogleIdOption(nonce)
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "Requesting credentials...")

            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            Log.d(TAG, "Credentials received, handling response...")

            handleCredentialResponse(response)

        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled sign-in")
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credentials available - Check Google Cloud Console setup", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.message}", e)
            Log.e(TAG, "Error type: ${e.type}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun buildGoogleIdOption(nonce: String): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .setNonce(nonce)
            .build()
    }

    private suspend fun handleCredentialResponse(response: GetCredentialResponse): Result<FirebaseUser> {
        return when (val credential = response.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    authenticateWithFirebase(credential)
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    Result.failure(Exception("Unexpected credential type"))
                }
            }
            else -> {
                Log.e(TAG, "Unexpected credential: ${credential.javaClass.name}")
                Result.failure(Exception("Unexpected credential"))
            }
        }
    }

    private suspend fun authenticateWithFirebase(credential: CustomCredential): Result<FirebaseUser> {
        return try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            Log.d(TAG, "Got Google ID token, authenticating with Firebase...")

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = FirebaseAuth.getInstance()
                .signInWithCredential(firebaseCredential)
                .await()

            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Firebase auth successful: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("Firebase returned null user"))
            }

        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Failed to parse Google ID token", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Firebase authentication failed", e)
            Result.failure(e)
        }
    }
}
