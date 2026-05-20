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
import org.liftrr.di.annotations.creds.GoogleSignInClientId
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleSignInToken(
    val idToken: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)

@Singleton
class GoogleAuthService @Inject constructor(
    @param:GoogleSignInClientId private val webClientId: String
) {
    private companion object {
        const val TAG = "GoogleAuthService"
    }

    suspend fun getGoogleSignInToken(context: Context): Result<GoogleSignInToken> {
        return try {
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

    private fun handleCredentialResponse(response: GetCredentialResponse): Result<GoogleSignInToken> {
        return when (val credential = response.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    extractGoogleIdToken(credential)
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

    private fun extractGoogleIdToken(credential: CustomCredential): Result<GoogleSignInToken> {
        return try {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            Log.d(TAG, "Got Google ID token for backend authentication")

            Result.success(
                GoogleSignInToken(
                    idToken = googleIdTokenCredential.idToken,
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName,
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                )
            )
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Failed to parse Google ID token", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google ID token extraction failed", e)
            Result.failure(e)
        }
    }
}
