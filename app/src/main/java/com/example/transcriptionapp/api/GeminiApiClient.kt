package com.example.transcriptionapp.api
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.NetworkCapabilities
//import android.util.Log
//import androidx.core.net.toUri
//import com.example.transcriptionapp.model.SettingsRepository
//import com.google.firebase.Firebase
//import com.google.firebase.ai.ai
//import com.google.firebase.ai.type.GenerativeBackend
//import com.google.firebase.ai.type.content
//import io.ktor.client.*
//import io.ktor.client.engine.okhttp.*
//import io.ktor.client.statement.*
//import io.ktor.client.plugins.contentnegotiation.*
//import io.ktor.client.plugins.logging.*
//import io.ktor.client.request.forms.formData
//import io.ktor.client.request.forms.submitFormWithBinaryData
//import io.ktor.http.*
//import io.ktor.serialization.kotlinx.json.*
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.*
//import java.io.File
//import javax.inject.Inject
//
//
//@Serializable
//data class ChatMessage(val role: String, val content: String)
//
//@Serializable
//data class ChatCompletionRequest(
//    val model: String,
//    val messages: List<ChatMessage>,
//    val maxTokens: Int? = null,
//    val temperature: Double? = null
//)
//
//@Serializable
//data class ChatChoice(val message: ChatMessage)
//
//@Serializable
//data class ApiChatCompletionResponse(
//    val choices: List<ChatChoice>? = null,
//    val error: ApiError? = null
//)
//
//@Serializable
//data class ApiError(val message: String, val type: String?, val param: String?, val code: String?)
//
//interface ApiService {
//    suspend fun transcribe(audioFile: File): Result<String>
//    suspend fun summarize(text: String): Result<String>
//    suspend fun translate(text: String): Result<String>
//
//}
//
//class GeminiApiClient @Inject constructor(
//    private val settingsRepository: SettingsRepository,
//    private val context: Context
//) : ApiService {
//
//    private val client = HttpClient(OkHttp) {
//        install(ContentNegotiation) {
//            json(Json { ignoreUnknownKeys = true; isLenient = true })
//        }
//        install(Logging) {
//            level = LogLevel.INFO
//        }
//    }
//
//    private var firebaseFunctionHttpUrl: String = "https://call-openai-python-wb7tbqxrta-ew.a.run.app"
//    private var language: String = "English"
//    private var modelForChat: String = "gpt-4.1-nano"
//
//    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
//
//    init {
//        CoroutineScope(Dispatchers.IO).launch {
//            settingsRepository.userPreferencesFlow.collect { userPreferences ->
//                language = userPreferences.selectedLanguage
//                Log.d("GeminiApiClient", "Settings updated: Lang=$language, ChatModel=$modelForChat, FuncURL=$firebaseFunctionHttpUrl")
//            }
//        }
//    }
//
//    override suspend fun transcribe(audioFile: File): Result<String> {
//        if (!isNetworkAvailable(context)) {
//            return Result.failure(Exception("No network connection available"))
//        }
//        if (firebaseFunctionHttpUrl.isBlank()) {
//            return Result.failure(Exception("Firebase Function URL not configured."))
//        }
//
//        val audioUri = audioFile.toUri()
//        val contentResolver = context.contentResolver
//
//        val prompt = "You are a helpful assistant that ONLY transcribes audio. Transcribe in the Spoken Language and format the Text."
//
//        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
//            .generativeModel("gemini-2.0-flash-lite-001",
//                systemInstruction = content { text(prompt) })
//
//        contentResolver.openInputStream(audioUri)?.use { stream ->
//            val response = model.generateContent(content { inlineData(stream.readBytes(), "audio/mpeg") })
//            return Result.success(response.text!!)
//        }
//
//        return Result.failure(Exception("Failed to transcribe audio"))
//    }
//
//    suspend fun transcribeCloudFunction(audioFile: File): Result<String> {
//        if (!isNetworkAvailable(context)) {
//            return Result.failure(Exception("No network connection available"))
//        }
//        if (firebaseFunctionHttpUrl.isBlank()) {
//            return Result.failure(Exception("Firebase Function URL not configured."))
//        }
//
//        return withContext(Dispatchers.IO) {
//            try {
//                val response: HttpResponse = client.submitFormWithBinaryData(
//                    url = firebaseFunctionHttpUrl,
//                    formData = formData {
//                        append("audio_file", audioFile.readBytes(), Headers.build {
//                            append(HttpHeaders.ContentType, contentTypeForExtension(audioFile.extension))
//                            append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
//                        })
//                        append("model", "whisper-1")
//                    }
//                )
//
//                val responseBody = response.bodyAsText()
//                if (!response.status.isSuccess()) {
//                    Log.e("GeminiApiClient", "Whisper failed: ${response.status.value} - $responseBody")
//                    return@withContext Result.failure(Exception("Transcription failed: ${response.status.value} - $responseBody"))
//                }
//
//                val jsonResponse = jsonParser.parseToJsonElement(responseBody)
//                val transcriptionText = jsonResponse.jsonObject["text"]?.jsonPrimitive?.contentOrNull
//                transcriptionText?.let {
//                    Result.success(it)
//                } ?: Result.failure(Exception("Transcription failed: No 'text' field in response"))
//            } catch (e: Exception) {
//                Log.e("GeminiApiClient", "Whisper error: ${e.message}", e)
//                Result.failure(Exception("Transcription failed: ${e.localizedMessage}"))
//            } finally {
//                audioFile.delete()
//            }
//        }
//    }
//
//
//
//    private fun contentTypeForExtension(extension: String): String = when (extension.lowercase()) {
//        "mp3" -> "audio/mpeg"
//        "m4a" -> "audio/mp4"
//        "wav" -> "audio/wav"
//        else -> "audio/mpeg"
//    }
//
//    override suspend fun summarize(text: String): Result<String> {
//        if (!isNetworkAvailable(context)) {
//            return Result.failure(Exception("No network connection available"))
//        }
//        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
//            .generativeModel("gemini-2.0-flash-lite-001",
//                systemInstruction = content { text("You are a helpful assistant that ONLY summarizes text. Summarize in $language.") })
//
//        val response = model.generateContent(text)
//        return if (response.text.isNullOrBlank()) {
//            Result.failure(Exception("Summary failed: Empty response"))
//        } else {
//            Result.success(response.text!!)
//        }
//    }
//
//    override suspend fun translate(text: String): Result<String> {
//        if (!isNetworkAvailable(context)) {
//            return Result.failure(Exception("No network connection available"))
//        }
//        val prompt = "You are a helpful assistant that ONLY translates text. Translate to ${language.uppercase()}."
//        val model = Firebase.ai(backend = GenerativeBackend.googleAI())
//            .generativeModel("gemini-2.0-flash-lite-001",
//                systemInstruction = content { text(prompt) })
//
//        val response = model.generateContent(text)
//        return if (response.text.isNullOrBlank()) {
//            Result.failure(Exception("Translation failed: Empty response"))
//        } else {
//            Result.success(response.text!!)
//        }
//    }
//
//    private fun isNetworkAvailable(context: Context): Boolean {
//        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val network = connectivityManager.activeNetwork ?: return false
//        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
//        return when {
//            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
//            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
//            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
//            else -> false
//        }
//    }
//}
