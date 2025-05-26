package com.example.transcriptionapp.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.transcriptionapp.model.SettingsRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File
import javax.inject.Inject

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int? = null,
    val temperature: Double? = null
)

@Serializable
data class OpenAIChatChoice(val message: ChatMessage)

@Serializable
data class OpenAIChatCompletionResponse(
    val choices: List<OpenAIChatChoice>? = null,
    val error: OpenAIError? = null
)

@Serializable
data class OpenAIError(val message: String, val type: String?, val param: String?, val code: String?)

interface OpenAiService {
    suspend fun whisper(audioFile: File): Result<String>
    suspend fun summarize(text: String): Result<String>
    suspend fun translate(text: String): Result<String>

}

class FirebaseApiClient @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : OpenAiService {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private var firebaseFunctionHttpUrl: String = "https://call-openai-python-wb7tbqxrta-ew.a.run.app"
    private var language: String = "English"
    private var modelForChat: String = "gpt-4.1-nano"

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.userPreferencesFlow.collect { userPreferences ->
                language = userPreferences.selectedLanguage
                modelForChat = userPreferences.selectedModel.takeIf { it.isNotBlank() } ?: "gpt-4.1-nano"
                Log.d("FirebaseApiClient", "Settings updated: Lang=$language, ChatModel=$modelForChat, FuncURL=$firebaseFunctionHttpUrl")
            }
        }
    }

    override suspend fun whisper(audioFile: File): Result<String> {
        if (!isNetworkAvailable(context)) {
            return Result.failure(Exception("No network connection available"))
        }
        if (firebaseFunctionHttpUrl.isBlank()) {
            return Result.failure(Exception("Firebase Function URL not configured."))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = client.submitFormWithBinaryData(
                    url = firebaseFunctionHttpUrl,
                    formData = formData {
                        append("audio_file", audioFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, contentTypeForExtension(audioFile.extension))
                            append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
                        })
                        append("model", "whisper-1")
                    }
                )

                val responseBody = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    Log.e("FirebaseApiClient", "Whisper failed: ${response.status.value} - $responseBody")
                    return@withContext Result.failure(Exception("Transcription failed: ${response.status.value} - $responseBody"))
                }

                val jsonResponse = jsonParser.parseToJsonElement(responseBody)
                val transcriptionText = jsonResponse.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                transcriptionText?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Transcription failed: No 'text' field in response"))
            } catch (e: Exception) {
                Log.e("FirebaseApiClient", "Whisper error: ${e.message}", e)
                Result.failure(Exception("Transcription failed: ${e.localizedMessage}"))
            } finally {
                audioFile.delete()
            }
        }
    }

    private fun contentTypeForExtension(extension: String): String = when (extension.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "wav" -> "audio/wav"
        else -> "audio/mpeg"
    }

    override suspend fun summarize(text: String): Result<String> {
        if (!isNetworkAvailable(context)) {
            return Result.failure(Exception("No network connection available"))
        }
        val prompt = "You are the most helpful assistant that ONLY summarizes text. Summarize in ${language.uppercase()}."
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash-lite-001",
                systemInstruction = content { text(prompt) })

        val response = model.generateContent(text)
        return if (response.text.isNullOrBlank()) {
            Result.failure(Exception("Summary failed: Empty response"))
        } else {
            Result.success(response.text!!)
        }
    }

    override suspend fun translate(text: String): Result<String> {
        if (!isNetworkAvailable(context)) {
            return Result.failure(Exception("No network connection available"))
        }
        val prompt = "You are the most helpful assistant that ONLY translates text. Translate to ${language.uppercase()}."
        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-2.0-flash-lite-001",
                systemInstruction = content { text(prompt) })

        val response = model.generateContent(text)
        return if (response.text.isNullOrBlank()) {
            Result.failure(Exception("Translation failed: Empty response"))
        } else {
            Result.success(response.text!!)
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
