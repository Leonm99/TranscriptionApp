package com.example.transcriptionapp.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.api.ApiService
import com.example.transcriptionapp.api.ApiServiceFactory
import com.example.transcriptionapp.com.example.transcriptionapp.model.TranscriptionRepository
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.util.FileUtils.clearTempDir
import com.example.transcriptionapp.util.FileUtils.convertToMP3
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

fun formatTimestamp(timestamp: Long): String {
  val date = Date(timestamp)
  val format = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
  return format.format(date)
}

enum class LastAction {
  TRANSCRIPTION,
  SUMMARIZATION,
  TRANSLATION,
  NONE,
}

private const val TAG = "BottomSheetViewModel"

@HiltViewModel
class BottomSheetViewModel
@Inject
constructor(
  private val settingsRepository: SettingsRepository,
  private val transcriptionRepository: TranscriptionRepository,
  private val ApiServiceFactory: ApiServiceFactory,
  @ApplicationContext private val context: Context,
) : ViewModel() {

  private lateinit var ApiService: ApiService

  private val _transcription =
    MutableStateFlow(
      Transcription(
        id = 0,
        transcriptionText = "",
        summaryText = null,
        translationText = null,
        timestamp = "null",
      )
    )
  val transcription: StateFlow<Transcription> = _transcription.asStateFlow()

  val visiblePermissionDialogQueue = mutableStateListOf<String>()

  private val _transcriptionError = MutableStateFlow<String?>(null)
  val transcriptionError: StateFlow<String?> = _transcriptionError.asStateFlow()

  private val _isBottomSheetVisible = MutableStateFlow(false)
  val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

  private val _transcriptionList = MutableStateFlow<List<Transcription>>(emptyList())
  val transcriptionList: StateFlow<List<Transcription>> = _transcriptionList.asStateFlow()

  // General loading state, primarily for the initial transcription process
  private val _isLoading = MutableStateFlow(false) // Changed default to false, set true during operations
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _lastAction = MutableStateFlow(LastAction.NONE)
  val lastAction: StateFlow<LastAction> = _lastAction.asStateFlow()

  private val _closeApp = MutableStateFlow(false)
  val closeApp: StateFlow<Boolean>
    get() = _closeApp

  private val _totalAudioCount = MutableStateFlow(0)
  val totalAudioCount: StateFlow<Int> = _totalAudioCount.asStateFlow()

  private val _currentAudioIndex = MutableStateFlow(0)
  val currentAudioIndex: StateFlow<Int> = _currentAudioIndex.asStateFlow()

  enum class ProcessingStep {
    PROCESSING,
    TRANSCRIPTION,
  }

  private val _processingStep = MutableStateFlow(ProcessingStep.PROCESSING)
  val processingStep: StateFlow<ProcessingStep> = _processingStep.asStateFlow()

  var endAfterSave: Boolean = false
  var saveAfterEnd: Boolean = true

  private var cachedAudioUris = mutableStateListOf<Uri>()
  private var audioUris = mutableStateListOf<Uri>()

  // --- New StateFlows for ModalBottomSheet Pager Control & Auto-Scroll ---
  private val _isFirstOpenSinceTranscriptionUpdate = MutableStateFlow(true)
  val isFirstOpenSinceTranscriptionUpdate: StateFlow<Boolean> = _isFirstOpenSinceTranscriptionUpdate.asStateFlow()

  private val _justSummarized = MutableStateFlow(false)
  val justSummarized: StateFlow<Boolean> = _justSummarized.asStateFlow()

  private val _justTranslated = MutableStateFlow(false)
  val justTranslated: StateFlow<Boolean> = _justTranslated.asStateFlow()

  private val _isLoadingSummary = MutableStateFlow(false)
  val isLoadingSummary: StateFlow<Boolean> = _isLoadingSummary.asStateFlow()

  private val _isLoadingTranslation = MutableStateFlow(false)
  val isLoadingTranslation: StateFlow<Boolean> = _isLoadingTranslation.asStateFlow()

  // --- End of New StateFlows ---

  fun toggleBottomSheet(toggle: Boolean) {
    _isBottomSheetVisible.value = toggle
    if (toggle) {
      // When sheet becomes visible, reset flags that might have been true from a previous interaction
      // if the content has been cleared or changed.
      // The `isFirstOpenSinceTranscriptionUpdate` is handled more specifically below.
    } else if(saveAfterEnd) {
      onSaveClick() // Save when sheet is dismissed by user action (swipe, back press handled by ModalSheet)
    }
  }

  init {
    viewModelScope.launch {
      settingsRepository.userPreferencesFlow.collect { userPreferences ->
        ApiService = ApiServiceFactory.create(userPreferences)
        saveAfterEnd = userPreferences.autoSave
      }
    }
    viewModelScope.launch {
      // _isLoading.value = true // Initial loading for list can be handled by UI if needed
      transcriptionRepository.allTranscriptions.collect { transcriptions ->
        _transcriptionList.value = transcriptions
        // _isLoading.value = false
      }
    }
  }

  // Call this when a new transcription is successfully generated or loaded
  fun markAsFirstOpen() {
    _isFirstOpenSinceTranscriptionUpdate.value = true
  }

  // Call this from the Composable after the "first open" logic (e.g., scroll to page 0) is handled
  fun markAsNotFirstOpen() {
    _isFirstOpenSinceTranscriptionUpdate.value = false
  }

  fun clearJustSummarizedFlag() {
    _justSummarized.value = false
  }

  fun clearJustTranslatedFlag() {
    _justTranslated.value = false
  }


  fun onAudioSelected(audioUri: Uri) {
    audioUris += audioUri
    _totalAudioCount.value = audioUris.size
  }

  fun transcribeAudios() {
    if (audioUris.isEmpty()) {
      return
    }

    viewModelScope.launch {
      _lastAction.value = LastAction.TRANSCRIPTION
      // Reset states for a new transcription
      _transcription.value = Transcription(id = 0, transcriptionText = "", summaryText = null, translationText = null, timestamp = "null")
      _transcriptionError.value = null
      _isLoading.value = true // Main loading for transcription process
      markAsFirstOpen() // New transcription, so treat as first open for pager reset
      clearJustSummarizedFlag()
      clearJustTranslatedFlag()

      withContext(Dispatchers.Main) {
        toggleBottomSheet(true) // Ensure bottom sheet is visible
        _currentAudioIndex.value = 0
        _processingStep.value = ProcessingStep.PROCESSING
      }

      try {
        val audioFiles: List<File> =
          audioUris.mapIndexed { index, uri ->
            if (!cachedAudioUris.contains(uri)) { // Add to cache only if not already there from a retry
              cachedAudioUris.add(uri)
            }
            val convertedFile = withContext(Dispatchers.IO) { convertToMP3(uri, context) }
            withContext(Dispatchers.Main) { _currentAudioIndex.value = index + 1 }
            convertedFile!!
          }
        withContext(Dispatchers.Main) {
          _processingStep.value = ProcessingStep.TRANSCRIPTION
          _currentAudioIndex.value = 0 // Reset index for transcription step
        }

        val transcriptionResults =
          audioFiles.mapIndexed { index, audioFile ->
            withContext(Dispatchers.Main) { _currentAudioIndex.value = index + 1 }
            withContext(Dispatchers.IO) { ApiService.transcribe(audioFile) }
          }

        val successfulResults =
          transcriptionResults.mapIndexedNotNull { index, result ->
            if (transcriptionResults.size < 2) {
              result.getOrNull()
            } else {
              "Transcription ${index + 1}: " + result.getOrNull()
            }
          }

        if (successfulResults.isNotEmpty()) {
          _transcription.value =
            _transcription.value.copy(
              transcriptionText = successfulResults.joinToString("\n\n"),
              timestamp = formatTimestamp(System.currentTimeMillis()),
            )
          clearTempDir(context)
          audioUris.clear() // Clear after successful transcription
        } else {
          _transcriptionError.value =
            transcriptionResults.firstOrNull { it.isFailure }?.exceptionOrNull()?.message
              ?: "Unknown transcription error"
          Log.e(TAG, "Transcription failed: ${_transcriptionError.value}")
        }

        _isLoading.value = false // Transcription process finished
        // toggleBottomSheet(true) // Already visible, no need to toggle again unless logic dictates
      } catch (e: Exception) {
        Log.e(TAG, "Error transcribing audios", e)
        _transcriptionError.value = e.message ?: "Error during transcription process"
        _isLoading.value = false
        // audioUris.clear() // Decide if you want to clear URIs on general exception
        // cachedAudioUris.clear() // Or keep them for retry
      }
    }
  }

  fun buttonOnClick(launcher: ActivityResultLauncher<Intent>) {
    val intent =
      Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        type = "audio/*"
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      }
    launcher.launch(intent)
  }

  fun onSummarizeClick() {
    if (_transcription.value.transcriptionText.isBlank() || _isLoadingSummary.value) {
      return
    }
    _lastAction.value = LastAction.SUMMARIZATION
    viewModelScope.launch {
      _isLoading.value = true
      _isLoadingSummary.value = true
      _transcriptionError.value = null // Clear previous general errors, or use a specific summaryError
      clearJustSummarizedFlag()

      try {
        val summaryResult =
          withContext(Dispatchers.IO) {
            ApiService.summarize(transcription.value.transcriptionText)
          }

        summaryResult
          .onSuccess { text ->
            _transcription.value = _transcription.value.copy(summaryText = text)
            _justSummarized.value = true // Signal that summary is newly available
            Log.d(TAG, "Summary: " + transcription.value.summaryText)
          }
          .onFailure { e ->
            _transcriptionError.value = e.message ?: "Unknown summary error" // Or a specific summary error StateFlow

            Log.e(TAG, "Summary failed: ${e.message}", e)
          }
      } catch (e: Exception) {
        _transcriptionError.value = e.message ?: "Error during summary process"
        Log.e(TAG, "Error summarizing text", e)
      } finally {
        _isLoading.value = false
        _isLoadingSummary.value = false
      }
    }
  }

  fun onTranslateClick() {
    if (_transcription.value.transcriptionText.isBlank() || _isLoadingTranslation.value) {
      return
    }
    _lastAction.value = LastAction.TRANSLATION
    viewModelScope.launch {
      _isLoading.value = true
      _isLoadingTranslation.value = true
      _transcriptionError.value = null
      clearJustTranslatedFlag()

      try {
        val translateResult =
          withContext(Dispatchers.IO) {
            ApiService.translate(transcription.value.transcriptionText)
          }

        translateResult
          .onSuccess { text ->
            _transcription.value = _transcription.value.copy(translationText = text)
            _justTranslated.value = true // Signal that translation is newly available
            Log.d(TAG, "Translation: " + transcription.value.translationText)
          }
          .onFailure { e ->
            _transcriptionError.value = e.message ?: "Unknown translation error" // Or a specific translation error StateFlow
            Log.e(TAG, "Translation failed: ${e.message}", e)
          }
      } catch (e: Exception) {
        _transcriptionError.value = e.message ?: "Error during translation process"
        Log.e(TAG, "Error Translating text", e)
      } finally {
        _isLoading.value = false
        _isLoadingTranslation.value = false
      }
    }
  }

  fun onSaveClick() {
    viewModelScope.launch {
      if (_transcription.value.transcriptionText.isNotBlank() && _transcriptionError.value == null) {
        transcriptionRepository.upsertTranscription(_transcription.value)
        showToast("Saved") // Consider making this a one-shot event for the UI to observe
      }

      clearTranscriptionAndFlags()
      if (endAfterSave) {
        _closeApp.value = true
      }
      if (_isBottomSheetVisible.value == true) {
        toggleBottomSheet(false)
      }
    }
  }

  fun onRetryClick() {
    _transcriptionError.value = null // Clear error before retrying
    viewModelScope.launch {
      when (_lastAction.value) {
        LastAction.TRANSCRIPTION -> {
          if (cachedAudioUris.isEmpty()) {
            showToast("No audio cached, please select audio again.", true)
            _isLoading.value = false // Ensure loading is false if nothing to retry
            toggleBottomSheet(false) // Close sheet if retry isn't possible
            clearTranscriptionAndFlags()
            return@launch
          }
          // Re-populate audioUris from cache for transcription
          audioUris.clear()
          cachedAudioUris.forEach { uri ->
            audioUris.add(uri) // Re-add to audioUris to be processed
          }
          _totalAudioCount.value = audioUris.size
          // cachedAudioUris.clear() // Do not clear cache here, clear it on successful transcription or explicit clear
          transcribeAudios()
        }
        LastAction.SUMMARIZATION -> onSummarizeClick()
        LastAction.TRANSLATION -> onTranslateClick()
        LastAction.NONE -> {
          showToast("No action to retry", true)
          toggleBottomSheet(false) // Close sheet if no action
          clearTranscriptionAndFlags()
        }
      }
    }
  }

  fun onDeleteSelectedClick(selectedItems: List<Int>) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        selectedItems.forEach { id -> transcriptionRepository.deleteTranscriptionById(id) }
      }
    }
  }

  fun onSampleClick() {
    viewModelScope.launch {
      val sample = Transcription(
        0,
        "This is a sample transcription text. It can be quite long to demonstrate the scrolling behavior within the card. ".repeat(10),
        "This is a concise summary of the sample transcription. ".repeat(3),
        "Ceci est une traduction exemplaire du texte de transcription. ".repeat(3),
        timestamp = formatTimestamp(System.currentTimeMillis()),
      )
      transcriptionRepository.upsertTranscription(sample)
      // To also display it in the bottom sheet immediately:
      // _transcription.value = sample
      // markAsFirstOpen()
      // toggleBottomSheet(true)
    }
  }

  // Renamed for clarity and to include resetting related flags
  fun clearTranscriptionAndFlags() {
    _transcription.value =
      _transcription.value.copy(
        id = 0, // Reset ID for a new transcription object
        transcriptionText = "",
        summaryText = null,
        translationText = null,
        timestamp = "null",
      )
    _transcriptionError.value = null
    _lastAction.value = LastAction.NONE
    audioUris.clear()
    _totalAudioCount.value = 0
    _currentAudioIndex.value = 0
    markAsFirstOpen()
    clearJustSummarizedFlag()
    clearJustTranslatedFlag()
    _isLoading.value = false
    _isLoadingSummary.value = false
    _isLoadingTranslation.value = false
  }


  fun showToast(text: String, long: Boolean = false) {
    Toast.makeText(context, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
  }

  fun dismissDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
      if (visiblePermissionDialogQueue.isNotEmpty()) visiblePermissionDialogQueue.removeFirst()
    } else {
      if (visiblePermissionDialogQueue.isNotEmpty()) visiblePermissionDialogQueue.removeAt(0)
    }
  }

  fun onPermissionResult(permission: String, isGranted: Boolean) {
    if (!isGranted && !visiblePermissionDialogQueue.contains(permission)) {
      // Consider if you want to show dialog only for specific permissions
      visiblePermissionDialogQueue.add(permission)
    }
  }
}