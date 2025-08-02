package com.example.transcriptionapp.model

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.transcriptionapp.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {

  private val credentialManager = CredentialManager.create(context)

  fun isSignedIn(): Boolean {
    return firebaseAuth.currentUser != null
  }

  /**
   * Attempts to sign in the user using Credential Manager API
   */
  suspend fun signIn(activity: Activity): Boolean {
    if (isSignedIn()) {
      Log.i(TAG, "User already signed in")
      return true
    }

    val webClientId = context.getString(R.string.web_client_id)
    Log.i(TAG, "Using web client ID: $webClientId")
    
    // Validate web client ID
    if (webClientId.isEmpty() || webClientId == "YOUR_WEB_CLIENT_ID") {
      Log.e(TAG, "Invalid web client ID configuration")
      throw IllegalStateException("Web client ID is not properly configured")
    }

    return try {
      // First try with authorized accounts only (seamless sign-in)
      val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(webClientId)
        .build()
      
      val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
      
      try {
        Log.d(TAG, "Attempting sign-in with authorized accounts")
        val result = credentialManager.getCredential(
          context = activity,
          request = request
        )
        handleSignIn(result)
      } catch (e: NoCredentialException) {
        Log.i(TAG, "No authorized accounts found, trying Sign In with Google")
        handleNoCredentialException(activity)
      }
    } catch (e: GetCredentialException) {
      Log.e(TAG, "GetCredentialException: ${e.message}", e)
      handleCredentialException(e)
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error during sign-in: ${e.message}", e)
      if (e is CancellationException) throw e
      false
    }
  }

  private suspend fun handleNoCredentialException(activity: Activity): Boolean {
    return try {
      val webClientId = context.getString(R.string.web_client_id)
      Log.i(TAG, "Building Sign In with Google option with client ID: $webClientId")
      
      val signInWithGoogleOption = GetSignInWithGoogleOption
        .Builder(serverClientId = webClientId)
        .build()
      
      val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()
      
      Log.d(TAG, "Attempting Sign In with Google")
      val result = credentialManager.getCredential(
        context = activity,
        request = request
      )
      handleSignIn(result)
    } catch (e: Exception) {
      Log.e(TAG, "Sign In with Google failed: ${e.message}", e)
      
      // Check if this is a configuration issue
      if (e.message?.contains("Account reauth failed") == true) {
        Log.e(TAG, "Account reauth failed - this might indicate:")
        Log.e(TAG, "1. OAuth client ID configuration issue")
        Log.e(TAG, "2. Google account needs re-authentication")
        Log.e(TAG, "3. App not properly registered with Google")
        Log.e(TAG, "4. SHA-1 fingerprint mismatch in Google Console")
      }
      
      if (e is GetCredentialException) {
        handleCredentialException(e)
      } else {
        if (e is CancellationException) throw e
        false
      }
    }
  }

  private suspend fun handleSignIn(result: GetCredentialResponse): Boolean {
    // Handle the successfully returned credential.
    when (val credential = result.credential) {
      is CustomCredential -> {
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          try {
            // Use googleIdTokenCredential and extract id to validate and authenticate on your server.
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            
            Log.i(TAG, "Received Google ID token for user: ${googleIdTokenCredential.id}")
            
            // Create Firebase credential and sign in
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
            
            val isSuccessful = authResult.user != null
            Log.i(TAG, "Firebase authentication successful: $isSuccessful")
            return isSuccessful
            
          } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "Received an invalid google id token response", e)
            return false
          }
        } else {
          // Catch any unrecognized custom credential type here.
          Log.e(TAG, "Unexpected type of credential: ${credential.type}")
          return false
        }
      }
      else -> {
        // Catch any unrecognized credential type here.
        Log.e(TAG, "Unexpected type of credential: ${credential::class.java.name}")
        return false
      }
    }
  }

  private fun handleCredentialException(e: GetCredentialException): Boolean {
    when (e) {
      is NoCredentialException -> {
        // The user must create a credential or sign in to an existing one.
        Log.w(TAG, "No credential available. User needs to create or sign in to an account.")
        throw NoGoogleAccountFoundException(
          "No Google accounts found. Please add a Google account in Settings > Accounts.",
          e
        )
      }
      is GetCredentialCancellationException -> {
        // User canceled the sign-in flow or account reauth failed
        Log.i(TAG, "Sign-in was canceled by user or account reauth failed")
        return false
      }
      else -> {
        // Handle other credential exceptions
        Log.e(TAG, "Credential exception: ${e.javaClass.simpleName} - ${e.message}", e)
        return false
      }
    }
  }

  suspend fun signOut() {
    try {
      firebaseAuth.signOut()
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
      Log.i(TAG, "User signed out successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Error during sign out: ${e.message}", e)
    }
  }

  /**
   * Refreshes the credential manager state. Call this after the user adds a Google account
   * to ensure the credential manager recognizes the new account.
   */
  suspend fun refreshCredentialState() {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
      Log.i(TAG, "Credential state refreshed successfully")
    } catch (e: Exception) {
      Log.w(TAG, "Error refreshing credential state: ${e.message}", e)
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
