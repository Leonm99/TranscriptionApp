package com.example.transcriptionapp.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.api.OpenAiHandler
import com.example.transcriptionapp.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TranscriptionState(
  val isLoading: Boolean = false,
  val transcription: String? = null
)
private const val TAG = "TranscriptionViewModel"
class TranscriptionViewModel(application: Application) : AndroidViewModel(application) {

  private val _transcriptionState = MutableStateFlow(TranscriptionState())
  val transcriptionState: StateFlow<TranscriptionState> = _transcriptionState.asStateFlow()

  private val _isBottomSheetVisible = MutableStateFlow(false)
  val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

  private lateinit var openAiHandler: OpenAiHandler

  fun hideBottomSheet() {
    _isBottomSheetVisible.value = false
  }
init {
  openAiHandler = OpenAiHandler(getApplication<Application>().applicationContext)
}


    fun onAudioSelected(audioUri: Uri, context: Context) {
    viewModelScope.launch {
      withContext(Dispatchers.Main) { _isBottomSheetVisible.value = true }
      _transcriptionState.value = _transcriptionState.value.copy(isLoading = true)

      try {
        val audioFile = withContext(Dispatchers.IO) { FileUtils.getFileFromUri(audioUri, context) }
        val transcriptionResult = withContext(Dispatchers.IO) {
          openAiHandler.whisper(audioFile!!.absolutePath)
        }

        _transcriptionState.value = _transcriptionState.value.copy(
          isLoading = false,
          transcription = transcriptionResult
        )
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
          transcription = summaryResult

        )

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
          transcription = translateResult

        )

      } catch (e: Exception) {
        Log.e(TAG, "Error Translating text", e)
      }
    }
  }




}
