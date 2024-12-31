package com.example.transcriptionapp.model

import android.util.Log
import com.example.transcriptionapp.api.OpenAIService
import com.example.transcriptionapp.api.SettingsHolder
import com.example.transcriptionapp.api.SettingsHolder.language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class TranscriptionRepository(private val openAIService: OpenAIService) {

    suspend fun transcribeAudio(audioFile: File?): TranscriptionResponse = withContext(Dispatchers.IO){
        val requestBody = audioFile?.asRequestBody("audio/*".toMediaTypeOrNull())
        val audioPart =
            requestBody?.let { MultipartBody.Part.createFormData("file", audioFile.name, it) }

        openAIService.transcribeAudio(audioPart)
    }

    suspend fun summarizeText(text: String): String {
        Log.d("TranscriptionRepository", "Summarizing text: $text")
        val request = SummarizationRequest(
            model = SettingsHolder.model,
            messages = listOf(
                SummarizationRequest.Message(role = "system", content = "You will be provided with a transcription, and your task is to summarize it in $language"),
                SummarizationRequest.Message(role = "user", content = text)
            )
        )
        val response = openAIService.summarizeText(request)
        return response.choices.firstOrNull()?.message?.content ?: ""
    }

}