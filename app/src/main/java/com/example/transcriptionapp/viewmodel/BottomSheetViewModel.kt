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
import com.example.transcriptionapp.api.OpenAiService
import com.example.transcriptionapp.api.OpenAiServiceFactory
import com.example.transcriptionapp.com.example.transcriptionapp.model.TranscriptionRepository
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.util.FileUtils
import com.example.transcriptionapp.util.FileUtils.clearTempDir
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

fun formatTimestamp(timestamp: Long): String {
  val date = Date(timestamp)
  val format = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
  return format.format(date)
}

private const val TAG = "TranscriptionViewModel"

@HiltViewModel
class BottomSheetViewModel
@Inject
constructor(
  private val settingsRepository: SettingsRepository,
  private val transcriptionRepository: TranscriptionRepository,
  private val openAiServiceFactory: OpenAiServiceFactory,
) : ViewModel() {

  private lateinit var openAiService: OpenAiService

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

  private val _isBottomSheetVisible = MutableStateFlow(false)
  val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

  private val _transcriptionList = MutableStateFlow<List<Transcription>>(emptyList())
  val transcriptionList: StateFlow<List<Transcription>> = _transcriptionList.asStateFlow()

  private val _isLoading = MutableStateFlow(true)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _shouldFinishActivity = MutableStateFlow(false)
  val shouldFinishActivity: StateFlow<Boolean> = _shouldFinishActivity.asStateFlow()

  fun hideBottomSheet() {
    _isBottomSheetVisible.value = false
  }

  fun showBottomSheet() {
    _isBottomSheetVisible.value = true
  }

  fun finishActivity() {
    viewModelScope.launch {
      withContext(Dispatchers.Main) {
        delay(2)
        _shouldFinishActivity.value = true
      }
    }
  }

  fun dontFinishActivity() {
    _shouldFinishActivity.value = false
  }

  init {
    viewModelScope.launch {
      settingsRepository.userPreferencesFlow.collect { userPreferences ->
        openAiService = openAiServiceFactory.create(userPreferences)
      }
    }
    viewModelScope.launch {
      _isLoading.value = true
      transcriptionRepository.allTranscriptions.collect { transcriptions ->
        _transcriptionList.value = transcriptions
        // _isLoading.value = false
      }
    }
  }

  fun onAudioSelected(audioUri: Uri, context: Context) {

    viewModelScope.launch {
      withContext(Dispatchers.Main) {
        showBottomSheet()
        _isLoading.value = true
      }

      try {

        withContext(Dispatchers.IO) {
          val audioFile = FileUtils.getFileFromUri(audioUri, context)

          val transcriptionResult = openAiService.whisper(audioFile!!)
          clearTempDir(context)
          Log.d(TAG, "Transcribing audio...")

          _transcription.value =
            _transcription.value.copy(
              transcriptionText = transcriptionResult,
              timestamp = formatTimestamp(System.currentTimeMillis()),
            )
          withContext(Dispatchers.Main) { _isLoading.value = false }
        }
      } catch (e: Exception) {
        // Handle error, e.g., update UI with error message
        Log.e(TAG, "Error transcribing audio", e)
        _isLoading.value = false

        // ...
      }
    }
  }

  fun buttonOnClick(launcher: ActivityResultLauncher<Intent>) {
    val intent =
      Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        type = "audio/*"
        addCategory(Intent.CATEGORY_OPENABLE)
      }
    launcher.launch(intent)
  }

  fun onSummarizeClick() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val summaryResult =
          withContext(Dispatchers.IO) {
            openAiService.summarize(transcription.value.transcriptionText)
          }

        _transcription.value = _transcription.value.copy(summaryText = summaryResult)
        _isLoading.value = false

        Log.d(TAG, "Summary: " + transcription.value.summaryText)
      } catch (e: Exception) {
        Log.e(TAG, "Error summarizing text", e)
      }
    }
  }

  fun onTranslateClick() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val translateResult =
          withContext(Dispatchers.IO) {
            openAiService.translate(transcription.value.transcriptionText)
          }

        _transcription.value = _transcription.value.copy(translationText = translateResult)
        _isLoading.value = false

        Log.d(TAG, "Summary: " + transcription.value.translationText)
      } catch (e: Exception) {
        Log.e(TAG, "Error Translating text", e)
      }
    }
  }

  fun onSaveClick() {
    viewModelScope.launch {
      transcriptionRepository.upsertTranscription(_transcription.value)
      clearTranscription()
      hideBottomSheet()
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
      transcriptionRepository.upsertTranscription(
        Transcription(
          0,
          "Sample".repeat(50),
          "Sample".repeat(50),
          "Sample".repeat(50),
          timestamp = formatTimestamp(System.currentTimeMillis()),
        )
      )
    }
  }

  fun clearTranscription() {
    _transcription.value =
      _transcription.value.copy(
        transcriptionText = "",
        summaryText = null,
        translationText = null,
        timestamp = "null",
      )
  }

  fun showToast(context: Context, text: String, long: Boolean = false) {
    Toast.makeText(context, text, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
  }

  fun dismissDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
      visiblePermissionDialogQueue.removeFirst()
    } else {
      visiblePermissionDialogQueue.removeAt(0)
    }
  }

  fun onPermissionResult(permission: String, isGranted: Boolean) {
    if (!isGranted && !visiblePermissionDialogQueue.contains(permission)) {
      visiblePermissionDialogQueue.add(permission)
    }
  }
}
