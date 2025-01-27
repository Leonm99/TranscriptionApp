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
import com.example.transcriptionapp.com.example.transcriptionapp.api.MockOpenAiHandler
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TranscriptionState(
    val isLoading: Boolean = false,
    val transcription: String = "Not Transcribed yet!",
    val summary: String? = null,
    val translation: String? = null,
    val timestamp: String = formatTimestamp(System.currentTimeMillis()),
    val error: String? = null

)

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
    return format.format(date)
}

private const val TAG = "TranscriptionViewModel"
 class TranscriptionViewModel(settingsRepository: SettingsRepository) : ViewModel() {

  val openAiHandler = MockOpenAiHandler(settingsRepository)
  private val _transcriptionState = MutableStateFlow(TranscriptionState())
  val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()

  private val _isBottomSheetVisible = MutableStateFlow(false)
  val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()



    fun hideBottomSheet() {
    _isBottomSheetVisible.value = false
  }


    fun onAudioSelected(audioUri: Uri, context: Context) {
    viewModelScope.launch {
      withContext(Dispatchers.Main) {
        _isBottomSheetVisible.value = true
        _transcriptionState.value = _transcriptionState.value.copy(isLoading = true)
      }

      try {
        val audioFile = withContext(Dispatchers.IO) { FileUtils.getFileFromUri(audioUri, context) }
         withContext(Dispatchers.IO) {
          Log.d(TAG, "Transcribing audio...")

          _transcriptionState.value = _transcriptionState.value.copy(
            isLoading = false,
            transcription =  openAiHandler.whisper(audioFile!!)
          )
        }


      } catch (e: Exception) {
        // Handle error, e.g., update UI with error message
        Log.e(TAG, "Error transcribing audio", e)
        _transcriptionState.value = _transcriptionState.value.copy(isLoading = false)
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

  fun summarize() {
    viewModelScope.launch {
      _transcriptionState.value = _transcriptionState.value.copy(isLoading = true)
      try {
        val summaryResult = withContext(Dispatchers.IO) {
          openAiHandler.summarize(transcriptionState.value.transcription.orEmpty())
        }

        _transcriptionState.value = _transcriptionState.value.copy(
          isLoading = false,
          summary = summaryResult

        )

          Log.d(TAG, "Summary: " + transcriptionState.value.summary)

      } catch (e: Exception) {
        Log.e(TAG, "Error summarizing text", e)
      }
    }
  }

  fun translate() {
    viewModelScope.launch {
      _transcriptionState.value = _transcriptionState.value.copy(isLoading = true)
      try {
        val translateResult = withContext(Dispatchers.IO) {
          openAiHandler.translate(transcriptionState.value.transcription.orEmpty())
        }

        _transcriptionState.value = _transcriptionState.value.copy(
          isLoading = false,
          translation = translateResult

        )
            Log.d(TAG, "Summary: " + transcriptionState.value.translation)
      } catch (e: Exception) {
        Log.e(TAG, "Error Translating text", e)
      }
    }
  }

  fun copyToClipboard(context: Context, text: String) {
    val clipboardManager =
      context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Transcription", text)
    clipboardManager.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
  }


}
