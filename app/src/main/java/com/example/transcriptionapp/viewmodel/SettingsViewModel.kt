package com.example.transcriptionapp.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.model.GoogleAuthClient
import com.example.transcriptionapp.model.NoGoogleAccountFoundException
import com.example.transcriptionapp.model.ProviderType
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.model.TranscriptionRepository
import com.example.transcriptionapp.model.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

// Helper function to create a Flow from FirebaseAuth's AuthStateListener
fun FirebaseAuth.currentUserFlow(): Flow<FirebaseUser?> = callbackFlow {
  val authStateListener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser).isSuccess
  }
  addAuthStateListener(authStateListener)
  awaitClose {
    removeAuthStateListener(authStateListener)
  }
}

enum class DialogType {
  LANGUAGE,
  TRANSCRIPTION_PROVIDER,
  SUMMARIZATION_PROVIDER,
  DELETE,
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val settingsRepository: SettingsRepository,
  private val transcriptionRepository: TranscriptionRepository,
  private val googleAuthClient: GoogleAuthClient,
  private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

  val settings: StateFlow<UserPreferences> =
    settingsRepository.userPreferencesFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      UserPreferences(),
    )



  val currentUser: StateFlow<FirebaseUser?> = firebaseAuth.currentUserFlow()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = firebaseAuth.currentUser
    )

  val userDisplayName: StateFlow<String?> = currentUser.map { firebaseUser ->
    firebaseUser?.displayName
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = firebaseAuth.currentUser?.displayName
  )

  val userProfilePictureUrl: StateFlow<String?> = currentUser.map { firebaseUser ->
    firebaseUser?.photoUrl?.toString()
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = firebaseAuth.currentUser?.photoUrl?.toString()
  )

  val userEmail: StateFlow<String?> = currentUser.map { firebaseUser ->
    firebaseUser?.email
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = firebaseAuth.currentUser?.email
  )

  private val _showDialog = MutableStateFlow(false)
  var showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

  private val _dialogType = MutableStateFlow(DialogType.LANGUAGE)
  var dialogType: StateFlow<DialogType> = _dialogType.asStateFlow()


  private val _navigateToSystemAddAccount = MutableSharedFlow<Unit>()
  val navigateToSystemAddAccount = _navigateToSystemAddAccount.asSharedFlow()


  fun signIn(activity: Activity) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val signedIn = googleAuthClient.signIn(activity)
        if (signedIn) {
          Log.i(TAG, "Sign-in successful from ViewModel perspective.")
        }
      } catch (e: NoGoogleAccountFoundException) {
        Log.w(TAG, "No Google account found, requesting to add one via system settings.")
        _navigateToSystemAddAccount.emit(Unit)
      } catch (e: Exception) {
        Log.e(TAG, "Sign-in error in ViewModel", e)
      }
    }
  }

  fun signOut() {
    viewModelScope.launch(Dispatchers.IO) {
      googleAuthClient.signOut()
    }
  }

  fun showDialog(type: DialogType = DialogType.LANGUAGE) {
    _dialogType.value = type
    _showDialog.value = true
  }

  fun hideDialog() {
    _showDialog.value = false
  }

  fun deleteDatabase() {
    viewModelScope.launch(Dispatchers.IO) { transcriptionRepository.deleteAllTranscriptions() }
  }

  fun setSelectedLanguage(language: String) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setLanguage(language) }
  }

  fun setSelectedTranscriptionProvider(provider: ProviderType) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setTranscriptionProvider(provider) }
  }

  fun setSelectedSummaryProvider(provider: ProviderType) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setSummaryProvider(provider) }
  }

  fun setAutoSave(autoSave: Boolean) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setAutoSave(autoSave) }
  }
  fun setMockApi(mockApi: Boolean) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setMockApi(mockApi) }
  }

  fun setDynamicColor(dynamicColor: Boolean) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setDynamicColor(dynamicColor) }
  }
}