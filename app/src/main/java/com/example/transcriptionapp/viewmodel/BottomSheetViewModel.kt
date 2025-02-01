package com.example.transcriptionapp.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.api.MockOpenAiHandler
import com.example.transcriptionapp.com.example.transcriptionapp.model.TranscriptionRepository
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.TranscriptionDao
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimestamp(timestamp: Long): String {
  val date = Date(timestamp)
  val format = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
  return format.format(date)
}

private const val TAG = "TranscriptionViewModel"

class TranscriptionViewModel(
  settingsRepository: SettingsRepository,
  transcriptionDao: TranscriptionDao,
) : ViewModel() {

  private val transcriptionRepository = TranscriptionRepository(transcriptionDao)

  val openAiHandler = MockOpenAiHandler(settingsRepository)

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _transcription =
    MutableStateFlow(
      Transcription(
        id = 0,
        transcriptionText = "",
        summaryText = null,
        translationText = null,
        timestamp = null,
      )
    )
  val transcription: StateFlow<Transcription> = _transcription.asStateFlow()

  private val _isBottomSheetVisible = MutableStateFlow(false)
  val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

  private val _transcriptionList = MutableStateFlow<List<Transcription>>(emptyList())
  val transcriptionList: StateFlow<List<Transcription>> = _transcriptionList.asStateFlow()

  init {
    viewModelScope.launch {
      try {
        _isLoading.value = true
        transcriptionRepository.allTranscriptions.collectLatest { transcriptions ->
          _transcriptionList.value = transcriptions
        }
        _isLoading.value = false
      } catch (e: Exception) {
        Log.e(TAG, "Error fetching transcriptions", e)
        _isLoading.value = false
      }
    }
  }

  fun onAudioSelected(audioUri: Uri, context: Context) {
    viewModelScope.launch {
      _isBottomSheetVisible.value = true
      _isLoading.value = true

      try {
        val audioFile = FileUtils.getFileFromUri(audioUri, context)
        val transcriptionText = openAiHandler.whisper(audioFile!!)
        _transcription.value =
          _transcription.value.copy(
            transcriptionText = transcriptionText,
            timestamp = formatTimestamp(System.currentTimeMillis()),
          )
      } catch (e: Exception) {
        Log.e(TAG, "Error transcribing audio", e)
      } finally {
        _isLoading.value = false
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
        val summaryText = openAiHandler.summarize(_transcription.value.transcriptionText)
        _transcription.value = _transcription.value.copy(summaryText = summaryText)
      } catch (e: Exception) {
        Log.e(TAG, "Error summarizing text", e)
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun onTranslateClick() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val translationText = openAiHandler.translate(_transcription.value.transcriptionText)
        _transcription.value = _transcription.value.copy(translationText = translationText)
      } catch (e: Exception) {
        Log.e(TAG, "Error translating text", e)
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun onSaveClick() {
    viewModelScope.launch {
      transcriptionRepository.upsertTranscription(_transcription.value)
      _transcription.value =
        _transcription.value.copy(
          transcriptionText = "",
          summaryText = null,
          translationText = null,
          timestamp = null,
        )
      hideBottomSheet()
    }
  }

  fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Transcription", text)
    clipboardManager.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
  }

  internal fun hideBottomSheet() {
    _isBottomSheetVisible.value = false
  }
}
