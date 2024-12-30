package com.example.transcriptionapp.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.model.OpenAIClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class TranscriptionViewModel : ViewModel() {
  private val _transcription = MutableStateFlow<String?>(null)
  var transcription: StateFlow<String?> = _transcription.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  var isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _showBottomSheet = MutableStateFlow(false)
  var showBottomSheet: StateFlow<Boolean> = _showBottomSheet.asStateFlow()

  fun hideBottomSheet() {
    _showBottomSheet.value = false
  }

  private suspend fun transcribeAudioFile(audioUri: Uri, context: Context): String {
    val tempFile =
        getFileFromUri(audioUri, context) ?: throw IllegalArgumentException("Invalid URI")
    val requestBody = tempFile.asRequestBody("audio/*".toMediaTypeOrNull())
    val audioPart = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

    val response = OpenAIClient.service.transcribeAudio(audioPart)
    return response.text
  }


  private fun getFileFromUri(uri: Uri, context: Context): File? {
    return try {
      // Open an input stream from the URI
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      // Create a temporary file in the app's cache directory
      val tempFile = File.createTempFile("temp_audio", ".mp3", context.cacheDir)
      Log.d("TAG", "Created temp file: ${tempFile.absolutePath}")
      // Write the content of the input stream to the temporary file
      tempFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
      tempFile
    } catch (e: Exception) {
      Log.e("TAG", "Error copying file from URI: ${e.localizedMessage}")
      null
    }
  }

  fun onAudioSelected(audioUri: Uri, context: Context) {
    viewModelScope.launch {
      _showBottomSheet.value = true
      _isLoading.value = true
      val transcriptionResult =
          withContext(Dispatchers.IO) { transcribeAudioFile(audioUri, context) }

      _transcription.value = transcriptionResult

      _isLoading.value = false
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

  fun summaryOnClick(){

  }
}
