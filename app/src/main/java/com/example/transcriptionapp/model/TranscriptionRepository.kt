package com.example.transcriptionapp.model

import com.example.transcriptionapp.api.OpenAIService
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
        val request = SummarizationRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                SummarizationRequest.Message(role = "user", content = "Summarize this: $text")
            )
        )
        val response = openAIService.summarizeText(request)
        return response.choices.firstOrNull()?.message?.content ?: ""
    }

}