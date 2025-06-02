package com.example.transcriptionapp.model

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import com.example.transcriptionapp.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class GoogleAuthClient
@Inject
constructor(
    @ApplicationContext private val context: Context, // This is ApplicationContext from Hilt
    private val firebaseAuth: FirebaseAuth
) {

  private val credentialManager = CredentialManager.create(context)

  fun isSignedIn(): Boolean {
    if (firebaseAuth.currentUser != null) {
      Log.i("GoogleAuthClient", "User already signed in")
      return true
    }
    return false
  }

  /**
   * Attempts to sign in the user.
   *
   * @param activity The current Activity, required to show system dialogs like account picker or to
   *   launch the "Add Account" settings.
   * @return Boolean true if sign-in was successful, false otherwise.
   * @throws NoGoogleAccountFoundException if NoCredentialException is caught, allowing UI to react.
   */
  suspend fun signIn(activity: Activity): Boolean {
    if (isSignedIn()) {
      return true
    }

    try {
      val result = buildCredentialRequest(activity)
      return handleSignIn(result)
    } catch (e: NoCredentialException) {
      Log.w("GoogleAuthClient", "No credentials available. Possibly no Google accounts on device.")
      // Specific exception to indicate to the caller that they should prompt to add an account
      throw NoGoogleAccountFoundException("No Google accounts found on the device.", e)
    } catch (e: Exception) {

      Log.e("GoogleAuthClient", "Sign-in error")
      if (e is CancellationException) throw e // Re-throw cancellation
      return false
    }
  }

  private suspend fun handleSignIn(result: GetCredentialResponse): Boolean {
    val credential = result.credential

    if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      try {
        val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        Log.i("GoogleAuthClient", "Google ID Token Credential obtained for: ${tokenCredential.id}")
        // Optionally log displayName, profilePictureUri if needed for debugging
        tokenCredential.profilePictureUri

        val authCredential = GoogleAuthProvider.getCredential(tokenCredential.idToken, null)
        val authResult = firebaseAuth.signInWithCredential(authCredential).await()
        Log.i("GoogleAuthClient", "Firebase sign-in successful: ${authResult.user != null}")
        return authResult.user != null
      } catch (e: GoogleIdTokenParsingException) {
        Log.e("GoogleAuthClient", "Google ID Token parsing failed: ${e.message}")
        return false
      }
    } else {
      Log.w(
          "GoogleAuthClient",
          "Credential is not of type GoogleIdTokenCredential. Type: ${credential.type}")
      return false
    }
  }

  private suspend fun buildCredentialRequest(activity: Activity): GetCredentialResponse {
    val serverClientId = context.getString(R.string.web_client_id)
    if (serverClientId.isEmpty() || serverClientId == "YOUR_WEB_CLIENT_ID") {
      Log.e("GoogleAuthClient", "Web client ID is not configured correctly in strings.xml.")
      // Potentially throw a more specific configuration error here
      // For now, it will likely fail in getCredential if the ID is invalid.
    }
    Log.i("GoogleAuthClient", "Using Server Client ID: $serverClientId")

    val googleIdOption =
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(
                false) // Set to true if you want to attempt auto sign-in for a single account
            .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

    Log.d("GoogleAuthClient", "Requesting credential from manager.")
    // This is where the system UI (account picker) might be shown.
    return credentialManager.getCredential(activity, request)
  }

  suspend fun signOut() {
    try {
      firebaseAuth.signOut()
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
      Log.i("GoogleAuthClient", "User signed out successfully.")
    } catch (e: Exception) {
      Log.i("GoogleAuthClient", "Error during sign out: ${e.message}")
      // Optionally, rethrow or handle as needed
    }
  }

  companion object {
    private const val TAG = "GoogleAuthClient"
  }
}

/**
 * Custom exception to indicate that sign-in failed because no Google accounts were found, and the
 * user should be prompted to add one.
 */
class NoGoogleAccountFoundException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
