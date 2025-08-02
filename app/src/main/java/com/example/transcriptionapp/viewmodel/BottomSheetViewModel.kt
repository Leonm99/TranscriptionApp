package com.example.transcriptionapp.viewmodel // Or your correct package

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
import com.example.transcriptionapp.model.GoogleAuthClient
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.model.TranscriptionRepository
import com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.util.FileUtils // Ensure this is your FileUtils with hashing
import com.example.transcriptionapp.util.FileUtils.clearTempDir
import com.example.transcriptionapp.util.FileUtils.convertToMP3WithSilenceTrimming
import com.example.transcriptionapp.util.formatTimestamp
import com.example.transcriptionapp.util.showToast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

// Data class to hold URI and its ORIGINAL content hash for convenience
data class AudioFileWithHash(
  val uri: Uri,
  val hash: String?, // This will store the hash of the ORIGINAL, UNCONVERTED file content
  var originalFileName: String? = null
)

enum class LastAction {
  TRANSCRIPTION,
  SUMMARIZATION,
  TRANSLATION,
  NONE,
}

@HiltViewModel
class BottomSheetViewModel
@Inject
constructor(
  private val settingsRepository: SettingsRepository,
  private val transcriptionRepository: TranscriptionRepository,
  private val apiServiceFactory: ApiServiceFactory,
  @ApplicationContext private val context: Context,
  private val googleAuthClient: GoogleAuthClient, // O
) : ViewModel() {
  val visiblePermissionDialogQueue = mutableStateListOf<String>()
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _transcription = MutableStateFlow(
    Transcription(id = 0, transcriptionText = "", summaryText = null, translationText = null, timestamp = "", fileHash = null)
  )
  val transcription: StateFlow<Transcription> = _transcription.asStateFlow()

  private val _transcriptionError = MutableStateFlow<String?>(null)
  val transcriptionError: StateFlow<String?> = _transcriptionError.asStateFlow()

  private val _isBottomSheetVisible = MutableStateFlow(false)
  val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

  private val _lastAction = MutableStateFlow(LastAction.NONE)

  private val _closeApp = MutableStateFlow(false)
  val closeApp: StateFlow<Boolean> = _closeApp.asStateFlow()

  private val _transcriptionList = MutableStateFlow<List<Transcription>>(emptyList())
  val transcriptionList: StateFlow<List<Transcription>> = _transcriptionList.asStateFlow()

  private val _totalAudioCount = MutableStateFlow(0)
  val totalAudioCount: StateFlow<Int> = _totalAudioCount.asStateFlow()

  private val _currentAudioIndex = MutableStateFlow(0)
  val currentAudioIndex: StateFlow<Int> = _currentAudioIndex.asStateFlow()

  enum class ProcessingStep {
    HASHING_AND_CHECKING, // For original file hashing and DB check
    CONVERTING,           // For MP3 conversion and silence trimming
    TRANSCRIPTION,        // For API call
  }

  private val _processingStep = MutableStateFlow(ProcessingStep.HASHING_AND_CHECKING)
  val processingStep: StateFlow<ProcessingStep> = _processingStep.asStateFlow()

  var endAfterSave: Boolean = false
  var saveAfterEnd: Boolean = true

  // Holds AudioFileWithHash objects (original URI + original content hash) for the current operation
  private var filesForCurrentOperation = mutableStateListOf<AudioFileWithHash>()

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

  private val _showDuplicateFileWarning = MutableStateFlow<List<AudioFileWithHash>>(emptyList())
  val showDuplicateFileWarning: StateFlow<List<AudioFileWithHash>> = _showDuplicateFileWarning.asStateFlow()

  // Holds all AudioFileWithHash objects selected by the user in the current picker session,
  // before the duplicate dialog is resolved.
  private var pendingAudioSelectionsWithOriginalHashes = mutableStateListOf<AudioFileWithHash>()

  private lateinit var apiService: ApiService
  private val TAG = "BottomSheetViewModel"

  init {
    viewModelScope.launch {
      settingsRepository.userPreferencesFlow.collect { userPreferences ->
        apiService = apiServiceFactory.create(userPreferences)
        saveAfterEnd = userPreferences.autoSave
      }
    }
    viewModelScope.launch {
      transcriptionRepository.allTranscriptions.collect { transcriptions ->
        _transcriptionList.value = transcriptions
      }
    }
  }

  fun toggleBottomSheet(toggle: Boolean) {
    _isBottomSheetVisible.value = toggle
    if (!toggle && saveAfterEnd && _transcription.value.transcriptionText.isNotBlank()) {
      onSaveClick()
    } else if (!toggle) {
      clearTranscriptionAndFlags(clearFilesForOperation = true)
    }
  }

  fun markAsFirstOpen() {
    _isFirstOpenSinceTranscriptionUpdate.value = true
  }

  fun markAsNotFirstOpen() {
    _isFirstOpenSinceTranscriptionUpdate.value = false
  }

  fun clearJustSummarizedFlag() {
    _justSummarized.value = false
  }

  fun clearJustTranslatedFlag() {
    _justTranslated.value = false
  }

  // Handles initial audio selection from user
  fun onAudioSelected(selectedUrisFromPicker: List<Uri>) {
    if (selectedUrisFromPicker.isEmpty()) return

    if (!googleAuthClient.isSignedIn()) { // Or !isUserAuthenticated() if you create a helper
      _transcriptionError.value = "Authentication required. Please sign in to transcribe audio."
      _isLoading.value = false // Ensure loading state is reset
      // Optionally, you might want to trigger a navigation to the sign-in screen
      // _navigateToSignIn.value = true // (You'd need to define this StateFlow)
      showToast(context, "Please sign in to use transcription.", true)
      return // Stop further processing
    }

    viewModelScope.launch {
      _isLoading.value = true
      toggleBottomSheet(true)
      _processingStep.value = ProcessingStep.HASHING_AND_CHECKING
      _currentAudioIndex.value = 0
      _totalAudioCount.value = selectedUrisFromPicker.size

      pendingAudioSelectionsWithOriginalHashes.clear()

      // Calculate hashes of original files in parallel
      val filesWithOriginalHashes = selectedUrisFromPicker.mapIndexed { index, uri ->
        async(Dispatchers.IO) {
          Log.d(TAG, "Hashing original file: ${uri.lastPathSegment ?: uri.toString()}")
          val originalHash = FileUtils.getFileHash(context, uri) // HASH OF ORIGINAL, UNCONVERTED FILE
          val name = uri.lastPathSegment ?: uri.toString()
          Log.d(TAG, "Original file ${name}, Hash: $originalHash")
          withContext(Dispatchers.Main) { _currentAudioIndex.value = index + 1 }
          AudioFileWithHash(uri, originalHash, name)
        }
      }.awaitAll()

      val newFilesToProcess = mutableListOf<AudioFileWithHash>()
      val potentiallyDuplicateFiles = mutableListOf<AudioFileWithHash>()

      _currentAudioIndex.value = 0 // Reset for DB check progress (optional)
      for ((index, fileWithOriginalHash) in filesWithOriginalHashes.withIndex()) {
        if (fileWithOriginalHash.hash == null) {
          Log.w(TAG, "Could not generate original hash for ${fileWithOriginalHash.uri}, treating as new.")
          newFilesToProcess.add(fileWithOriginalHash)
          withContext(Dispatchers.Main) { _currentAudioIndex.value = index + 1 }
          continue
        }

        try {
          Log.d(TAG, "DB Check for original hash: ${fileWithOriginalHash.hash} (File: ${fileWithOriginalHash.originalFileName})")
          val existingTranscription = transcriptionRepository.getTranscriptionByFileHash(fileWithOriginalHash.hash)
          if (existingTranscription != null) {
            Log.i(TAG, "DUPLICATE (Original Hash): File ${fileWithOriginalHash.originalFileName} (hash ${fileWithOriginalHash.hash}) already transcribed (ID: ${existingTranscription.id}).")
            _transcription.value = existingTranscription
            potentiallyDuplicateFiles.add(fileWithOriginalHash)
          } else {
            Log.i(TAG, "NEW FILE (Original Hash): File ${fileWithOriginalHash.originalFileName} (hash ${fileWithOriginalHash.hash}) is new.")
            newFilesToProcess.add(fileWithOriginalHash)
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error checking original hash ${fileWithOriginalHash.hash} in DB. Treating as new.", e)
          newFilesToProcess.add(fileWithOriginalHash) // Fallback
        }
        withContext(Dispatchers.Main) { _currentAudioIndex.value = index + 1 }
      }

      pendingAudioSelectionsWithOriginalHashes.addAll(filesWithOriginalHashes) // Store all selections for the dialog logic

      if (potentiallyDuplicateFiles.isNotEmpty()) {
        _isLoading.value = false
        _showDuplicateFileWarning.value = potentiallyDuplicateFiles // This list contains AudioFileWithHash (with original hashes)
        Log.i(TAG, "Showing duplicate warning for ${potentiallyDuplicateFiles.size} files.")
      } else {
        filesForCurrentOperation.clear()
        filesForCurrentOperation.addAll(newFilesToProcess)
        _totalAudioCount.value = filesForCurrentOperation.size
        if (filesForCurrentOperation.isNotEmpty()) {
          transcribeAudiosInternal()
        } else {
          _isLoading.value = false
          toggleBottomSheet(false)
          showToast(context, "All selected files have been processed before or failed hashing.")
        }
      }
    }
  }

  fun proceedWithTranscription(transcribeDuplicates: Boolean) {
    val warningList = _showDuplicateFileWarning.value // Capture current warning list (contains original hashes)
    _showDuplicateFileWarning.value = emptyList()

    val filesToActuallyProcess = if (transcribeDuplicates) {
      pendingAudioSelectionsWithOriginalHashes.toList() // Process all originally selected files
    } else {
      // Filter out the confirmed duplicates (those that were in the warning list and have a valid hash)
      pendingAudioSelectionsWithOriginalHashes.filter { pendingFile ->
        pendingFile.hash == null || warningList.none { duplicateFileInWarning -> duplicateFileInWarning.hash == pendingFile.hash }
      }
    }

    filesForCurrentOperation.clear()
    filesForCurrentOperation.addAll(filesToActuallyProcess)
    _totalAudioCount.value = filesForCurrentOperation.size

    if (filesForCurrentOperation.isNotEmpty()) {
      _isLoading.value = true
      transcribeAudiosInternal()
    } else {
      _isLoading.value = false
      showToast(context, if (transcribeDuplicates) "No files selected for transcription." else "No new files to transcribe.")
      toggleBottomSheet(false)
    }
    pendingAudioSelectionsWithOriginalHashes.clear()
  }

  fun dismissDuplicateWarning() {
    val warningList = _showDuplicateFileWarning.value
    _showDuplicateFileWarning.value = emptyList()
    
    // Default action: skip duplicates
    val filesToActuallyProcess = pendingAudioSelectionsWithOriginalHashes.filter { pendingFile ->
      pendingFile.hash == null || warningList.none { duplicateFile -> duplicateFile.hash == pendingFile.hash }
    }

    filesForCurrentOperation.clear()
    filesForCurrentOperation.addAll(filesToActuallyProcess)
    _totalAudioCount.value = filesForCurrentOperation.size

    if (filesForCurrentOperation.isEmpty()) {
      showToast(context, "No new files to transcribe.")
      toggleBottomSheet(false)
      _isLoading.value = false
    } else {
      // Auto-start transcription for non-duplicate files
      _isLoading.value = true
      transcribeAudiosInternal()
    }
    pendingAudioSelectionsWithOriginalHashes.clear()
  }

  private fun transcribeAudiosInternal() {
    if (filesForCurrentOperation.isEmpty()) {
      _isLoading.value = false
      Log.d(TAG, "transcribeAudiosInternal called with no files.")
      return
    }

    val currentBatchFilesToProcess = filesForCurrentOperation.toList()

    viewModelScope.launch {
      _lastAction.value = LastAction.TRANSCRIPTION
      _transcription.value = Transcription(id = 0, transcriptionText = "", summaryText = null, translationText = null, timestamp = "null", fileHash = null)
      _transcriptionError.value = null // Clear previous errors
      _isLoading.value = true
      markAsFirstOpen()
      clearJustSummarizedFlag()
      clearJustTranslatedFlag()

      if (!_isBottomSheetVisible.value) toggleBottomSheet(true)

      _processingStep.value = ProcessingStep.CONVERTING
      _currentAudioIndex.value = 0

      val convertedFilesForApi = mutableListOf<File>()
      val mapConvertedFileToOriginalHash = mutableMapOf<File, String?>()
      val transcriptionErrors = mutableListOf<String>() // To collect specific errors

      try {
        val userSettings = settingsRepository.userPreferencesFlow.first()

        currentBatchFilesToProcess.forEachIndexed { index, audioFileWithOriginalHash ->
          _currentAudioIndex.value = index + 1
          Log.d(TAG, "Converting: ${audioFileWithOriginalHash.originalFileName}")
          val convertedFile = withContext(Dispatchers.IO) {
            convertToMP3WithSilenceTrimming(
              inputUri = audioFileWithOriginalHash.uri,
              context = context,
              enableSilenceTrimming = userSettings.enableSilenceTrimming,
              silenceThresholdDb = userSettings.silenceThresholdDb,
              silenceDurationSeconds = userSettings.silenceDurationSeconds
            )
          }
          if (convertedFile != null) {
            convertedFilesForApi.add(convertedFile)
            val convertedFileHash = withContext(Dispatchers.IO) { FileUtils.getFileHash(convertedFile) }
            Log.d(TAG, "Converted ${audioFileWithOriginalHash.originalFileName} to ${convertedFile.name}, OriginalHash: ${audioFileWithOriginalHash.hash}, ConvertedHash: $convertedFileHash")
            mapConvertedFileToOriginalHash[convertedFile] = audioFileWithOriginalHash.hash
          } else {
            Log.e(TAG, "Failed to convert ${audioFileWithOriginalHash.originalFileName}")
            transcriptionErrors.add("Failed to convert: ${audioFileWithOriginalHash.originalFileName}")
          }
        }

        if (convertedFilesForApi.isEmpty() && currentBatchFilesToProcess.isNotEmpty() && transcriptionErrors.isEmpty()) {
          // If no files were converted but no specific conversion error was logged, add a generic one.
          transcriptionErrors.add("Failed to convert any audio files for transcription.")
        }

        if (transcriptionErrors.isNotEmpty() && convertedFilesForApi.isEmpty()) {
          // If there are conversion errors and no files to transcribe, show these errors.
          _transcriptionError.value = transcriptionErrors.joinToString("\n")
          _isLoading.value = false
          clearTempDir(context) // Ensure cleanup
          return@launch // Stop further processing
        }


        _processingStep.value = ProcessingStep.TRANSCRIPTION
        _currentAudioIndex.value = 0

        val transcriptionResultsTextList = mutableListOf<String>()
        // Iterate through files that were successfully converted
        convertedFilesForApi.forEachIndexed { index, apiReadyFile ->
          _currentAudioIndex.value = index + 1
          Log.d(TAG, "Transcribing (API call): ${apiReadyFile.name}")
          val result = withContext(Dispatchers.IO) { apiService.transcribe(apiReadyFile) }

          result.onSuccess { text ->
            transcriptionResultsTextList.add(text)
          }.onFailure { exception ->
            // Capture the specific error message from the API client
            val errorMessage = exception.message ?: "Transcription failed for ${apiReadyFile.name}"
            Log.e(TAG, "API Transcription Error for ${apiReadyFile.name}: $errorMessage", exception)
            transcriptionErrors.add(errorMessage)
          }
        }

        // Consolidate transcription results
        val finalTranscriptionText = transcriptionResultsTextList.joinToString("\n\n")

        if (finalTranscriptionText.isNotBlank()) {
          val primaryOriginalHashForRecord = currentBatchFilesToProcess.firstOrNull { audioFile ->
            // Find the original hash corresponding to the first *successfully* transcribed file
            // This logic might need adjustment if you only want a hash if ALL files succeed.
            // For now, let's assume the first file in the original batch is the primary reference.
            // A more robust way would be to link hashes to successful transcriptions.
            convertedFilesForApi.any { converted -> mapConvertedFileToOriginalHash[converted] == audioFile.hash }
          }?.hash ?: currentBatchFilesToProcess.firstOrNull()?.hash // Fallback

          _transcription.value = _transcription.value.copy(
            transcriptionText = finalTranscriptionText,
            timestamp = formatTimestamp(System.currentTimeMillis()),
            fileHash = primaryOriginalHashForRecord
          )
          Log.d(TAG, "Transcription successful. Original content hash for DB record: $primaryOriginalHashForRecord")
        }

        // Handle errors after all attempts
        if (transcriptionErrors.isNotEmpty()) {
          // If there was some success, but also errors, decide how to present.
          // For now, let's show all errors.
          _transcriptionError.value = transcriptionErrors.joinToString("\n")
          if (finalTranscriptionText.isBlank()) { // No successful transcription at all
            Log.e(TAG, "All transcription attempts failed or produced no text. Errors: ${transcriptionErrors.joinToString()}")
          } else {
            Log.w(TAG, "Partial transcription success with errors: ${transcriptionErrors.joinToString()}")
            // Optionally, you could still show the partial transcription and the errors.
            // For now, _transcriptionError will display the issues.
          }
        } else if (finalTranscriptionText.isBlank() && convertedFilesForApi.isNotEmpty()) {
          // This means API calls were made, succeeded in terms of Result.isSuccess, but returned no text.
          _transcriptionError.value = "Transcription produced no text from the audio."
          Log.e(TAG, "Transcription API calls succeeded but produced no text for the batch.")
        }

        _isLoading.value = false
        clearTempDir(context)

      } catch (e: Exception) { // Catch-all for unexpected errors during the whole process
        Log.e(TAG, "Error in transcribeAudiosInternal's main try-catch", e)
        // Prioritize already collected specific errors if any
        if (transcriptionErrors.isNotEmpty()) {
          _transcriptionError.value = transcriptionErrors.joinToString("\n") + "\nAdditional error: " + (e.message ?: "Unexpected error during transcription process")
        } else {
          _transcriptionError.value = e.message ?: "Unexpected error during transcription process"
        }
        _isLoading.value = false
        clearTempDir(context)
      }
    }
  }


  fun buttonOnClick(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
      type = "audio/*"
      addCategory(Intent.CATEGORY_OPENABLE)
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
    launcher.launch(intent)
  }

  fun onSummarizeClick() {
    if (_transcription.value.transcriptionText.isBlank() || _isLoadingSummary.value) return
    _lastAction.value = LastAction.SUMMARIZATION
    viewModelScope.launch {
      _isLoadingSummary.value = true
      _transcriptionError.value = null
      clearJustSummarizedFlag()
      try {
        val summaryResult = withContext(Dispatchers.IO) {
          apiService.summarize(transcription.value.transcriptionText)
        }
        summaryResult.onSuccess { text ->
          _transcription.value = _transcription.value.copy(summaryText = text)
          _justSummarized.value = true
        }.onFailure { e ->
          _transcriptionError.value = e.message ?: "Unknown summary error"
        }
      } catch (e: Exception) {
        _transcriptionError.value = e.message ?: "Error during summary"
      } finally {
        _isLoadingSummary.value = false
      }
    }
  }

  fun onTranslateClick() {
    if (_transcription.value.transcriptionText.isBlank() || _isLoadingTranslation.value) return
    _lastAction.value = LastAction.TRANSLATION
    viewModelScope.launch {
      _isLoadingTranslation.value = true
      _transcriptionError.value = null
      clearJustTranslatedFlag()
      try {
        val translateResult = withContext(Dispatchers.IO) {
          apiService.translate(transcription.value.transcriptionText)
        }
        translateResult.onSuccess { text ->
          _transcription.value = _transcription.value.copy(translationText = text)
          _justTranslated.value = true
        }.onFailure { e ->
          _transcriptionError.value = e.message ?: "Unknown translation error"
        }
      } catch (e: Exception) {
        _transcriptionError.value = e.message ?: "Error during translation"
      } finally {
        _isLoadingTranslation.value = false
      }
    }
  }

  fun onSaveClick() {
    viewModelScope.launch {
      val currentTranscription = _transcription.value
      if (currentTranscription.transcriptionText.isNotBlank() && _transcriptionError.value == null) {
        try {
          // Ensure the fileHash being saved is indeed the original content hash
          if(currentTranscription.fileHash == null && filesForCurrentOperation.isNotEmpty()) {
            // Attempt to re-assign if it was somehow missed, though it should be set in transcribeAudiosInternal
            val hashToSave = filesForCurrentOperation.firstOrNull()?.hash
            Log.w(TAG, "FileHash was null during save, attempting to use hash from filesForCurrentOperation: $hashToSave")
            _transcription.value = currentTranscription.copy(fileHash = hashToSave)
          }
          transcriptionRepository.upsertTranscription(_transcription.value) // Save the potentially updated one
          showToast(context, "Saved")
          clearTranscriptionAndFlags(clearFilesForOperation = true)
          if (endAfterSave) _closeApp.value = true
        } catch (e: Exception) {
          Log.e(TAG, "Error saving transcription", e)
          showToast(context, "Error saving: ${e.message}", true)
        }
      } else if (_transcriptionError.value != null) {
        showToast(context, "Cannot save, error: ${_transcriptionError.value}", true)
      } else if (currentTranscription.transcriptionText.isBlank()) {
        showToast(context, "Nothing to save.")
      }

      if (endAfterSave && !_closeApp.value) _closeApp.value = true
      if (_isBottomSheetVisible.value && !_closeApp.value) {
        toggleBottomSheet(false) // Ensure sheet is closed if not closing app
      }
    }
  }

  fun onRetryClick() {
    val lastActionType = _lastAction.value
    _transcriptionError.value = null

    viewModelScope.launch {
      when (lastActionType) {
        LastAction.TRANSCRIPTION -> {
          if (filesForCurrentOperation.isEmpty()) { // Check if there are files staged from the last attempt
            showToast(context, "No audio data from last attempt to retry. Please select audio again.", true)
            _isLoading.value = false
            toggleBottomSheet(false)
            clearTranscriptionAndFlags(clearFilesForOperation = true)
            return@launch
          }
          // filesForCurrentOperation already contains the AudioFileWithHash objects (with original hashes)
          Log.d(TAG, "Retrying transcription for ${filesForCurrentOperation.size} files using their original URIs.")
          _isLoading.value = true
          _totalAudioCount.value = filesForCurrentOperation.size
          transcribeAudiosInternal() // This will use the files in filesForCurrentOperation
        }
        LastAction.SUMMARIZATION -> onSummarizeClick()
        LastAction.TRANSLATION -> onTranslateClick()
        LastAction.NONE -> {
          showToast(context, "No action to retry.", true)
          toggleBottomSheet(false)
          clearTranscriptionAndFlags(clearFilesForOperation = true)
        }
      }
    }
  }

  fun onDeleteSelectedClick(selectedItemIds: List<Int>) {
    viewModelScope.launch {
      try {
        selectedItemIds.forEach { id -> transcriptionRepository.deleteTranscriptionById(id) }
        showToast(context, "Deleted selected transcriptions.")
      } catch (e: Exception) {
        showToast(context, "Error deleting: ${e.message}", true)
      }
    }
  }

  fun clearTranscriptionAndFlags(clearFilesForOperation: Boolean = false) {
    _transcription.value = Transcription(id = 0, transcriptionText = "", summaryText = null, translationText = null, timestamp = "", fileHash = null)
    _transcriptionError.value = null
    _isLoading.value = false
    _isLoadingSummary.value = false
    _isLoadingTranslation.value = false
    _currentAudioIndex.value = 0
    _totalAudioCount.value = 0
    _processingStep.value = ProcessingStep.HASHING_AND_CHECKING
    _showDuplicateFileWarning.value = emptyList()
    pendingAudioSelectionsWithOriginalHashes.clear()

    if (clearFilesForOperation) {
      filesForCurrentOperation.clear()
    }
    // _lastAction.value = LastAction.NONE // Consider if this should always be reset
  }

  fun onSampleClick() {
    viewModelScope.launch {
      val sample = Transcription(
        0, "Sample transcription.", "Sample summary.", "Sample translation.",
        formatTimestamp(System.currentTimeMillis()), "sample_hash_${System.currentTimeMillis()}"
      )
      transcriptionRepository.upsertTranscription(sample)
      showToast(context, "Sample added.")
    }
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
