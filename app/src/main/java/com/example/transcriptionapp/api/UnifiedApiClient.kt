package com.example.transcriptionapp.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.net.toUri // For Gemini SDK transcription
import com.example.transcriptionapp.model.ProviderType
import com.example.transcriptionapp.model.SettingsRepository
import com.google.firebase.Firebase // For Gemini SDK
import com.google.firebase.ai.ai // For Gemini SDK
import com.google.firebase.ai.type.GenerativeBackend // For Gemini SDK
import com.google.firebase.ai.type.content // For Gemini SDK
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Common Data Classes (previously in GeminiApiClient.kt)
@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val maxTokens: Int? = null,
    val temperature: Double? = null
)

@Serializable
data class ChatChoice(val message: ChatMessage)

@Serializable
data class ApiChatCompletionResponse(
    val choices: List<ChatChoice>? = null,
    val error: ApiError? = null
)

@Serializable
data class ApiError(val message: String, val type: String?, val param: String?, val code: String?)

// Common ApiService Interface (previously in GeminiApiClient.kt)
interface ApiService {
    /**
     * Transcribes the given audio file.
     * @param audioFile The audio file to transcribe.
     * @param useGeminiSdk If true, uses the Firebase Gemini SDK for transcription. Otherwise, uses the HTTP endpoint.
     * @return A Result containing the transcription text or an Exception.
     */
    suspend fun transcribe(audioFile: File): Result<String>

    /**
     * Summarizes the given text.
     * @param text The text to summarize.
     * @param useGeminiSdk If true, uses the Firebase Gemini SDK for summarization. Otherwise, uses the HTTP endpoint.
     * @return A Result containing the summarized text or an Exception.
     */
    suspend fun summarize(text: String): Result<String>

    /**
     * Translates the given text.
     * @param text The text to translate.
     * @param useGeminiSdk If true, uses the Firebase Gemini SDK for translation. Otherwise, uses the HTTP endpoint.
     * @return A Result containing the translated text or an Exception.
     */
    suspend fun translate(text: String): Result<String>
}

class UnifiedApiClient @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ApiService {

    // Configure Ktor HttpClient with OkHttp engine (from OpenaiApiClient)
    private val ktorHttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("KtorLogger", message)
                }
            }
            level = LogLevel.INFO
        }
    }

    private var firebaseFunctionHttpUrl: String = "https://call-openai-python-wb7tbqxrta-ew.a.run.app" //
    private var language: String = "English" //
    private var transcriptionProvider: ProviderType = ProviderType.OPEN_AI
    private var summarizationProvider: ProviderType = ProviderType.OPEN_AI
    private var openAiModelForChat: String = "gpt-4o-mini" // Default OpenAI model for chat via HTTP
    private val geminiModelName: String = "gemini-2.0-flash-lite-001" // Default Gemini model

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true } //

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.userPreferencesFlow.collect { userPreferences ->
                language = userPreferences.selectedLanguage
                transcriptionProvider = userPreferences.selectedTranscriptionProvider
                summarizationProvider = userPreferences.selectedSummaryProvider


                if (firebaseFunctionHttpUrl.isBlank()) {
                    Log.e("UnifiedApiClient", "CRITICAL: firebaseFunctionHttpUrl is not set. HTTP API calls will fail.")
                }
                Log.d("UnifiedApiClient", "Settings updated: Lang=$language, OpenAI ChatModel=$openAiModelForChat, GeminiModel=$geminiModelName FuncURL=$firebaseFunctionHttpUrl")
            }
        }
    }

    // --- ApiService Implementations ---

    override suspend fun transcribe(audioFile: File): Result<String> {
        if (!isNetworkAvailable(context)) return Result.failure(Exception("No network connection available"))

        return if (transcriptionProvider == ProviderType.GEMINI) {
            transcribeWithGeminiSdkInternal(audioFile)
        } else {
            transcribeWithHttpInternal(audioFile)
        }
    }

    override suspend fun summarize(text: String): Result<String> {
        if (!isNetworkAvailable(context)) return Result.failure(Exception("No network connection available for Summarization"))

        return if (summarizationProvider == ProviderType.GEMINI) {
            summarizeWithGeminiSdkInternal(text)
        } else {
            val systemPrompt = "You are the most helpful assistant that ONLY summarizes text. Summarize in ${language.uppercase()}." //
            processTextWithHttpInternal(text, systemPrompt, openAiModelForChat, "Summarization")
        }
    }

    override suspend fun translate(text: String): Result<String> {
        if (!isNetworkAvailable(context)) return Result.failure(Exception("No network connection available for Translation"))

        return if (summarizationProvider == ProviderType.GEMINI) {
            translateWithGeminiSdkInternal(text)
        } else {
            val systemPrompt = "You are the most helpful assistant that ONLY translates text. Translate to ${language.uppercase()}." //
            processTextWithHttpInternal(text, systemPrompt, openAiModelForChat, "Translation")
        }
    }

    // --- HTTP/Cloud Function (OpenAI proxy) Methods ---

    private suspend fun transcribeWithHttpInternal(audioFile: File): Result<String> {
        if (firebaseFunctionHttpUrl.isBlank()) return Result.failure(Exception("Firebase Function URL not configured for HTTP transcription.")) //

        return withContext(Dispatchers.IO) {
            try {
                val mediaTypeAudio = contentTypeForExtension(audioFile.extension) // logic, adapted

                val response = ktorHttpClient.submitFormWithBinaryData(
                    url = firebaseFunctionHttpUrl.trim(),
                    formData = formData {
                        append("audio_file", audioFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, mediaTypeAudio)
                            append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
                        })
                        append("model", "whisper-1") // Assuming "whisper-1" for HTTP transcription
                    }
                )

                val responseBodyString = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    Log.e("UnifiedApiClient", "Whisper failed via HTTP (Ktor): ${response.status.value} - $responseBodyString")
                    return@withContext Result.failure(Exception("Transcription failed (HTTP Ktor): ${response.status.value} - ${responseBodyString ?: "Unknown error"}"))
                }

                val transcriptionText = jsonParser.parseToJsonElement(responseBodyString).jsonObject["text"]?.jsonPrimitive?.contentOrNull //
                if (transcriptionText != null) Result.success(transcriptionText) else {
                    Log.e("UnifiedApiClient", "Could not parse 'text' from Whisper response (Ktor): $responseBodyString")
                    Result.failure(Exception("Transcription failed (Ktor): Could not parse response. Body: $responseBodyString"))
                }
            } catch (e: Exception) {
                Log.e("UnifiedApiClient", "Error during Whisper HTTP call (Ktor): ${e.message}", e)
                Result.failure(Exception("Transcription failed (HTTP Ktor): ${e.localizedMessage ?: "Unknown error"}"))
            } finally {
                // audioFile.delete() // Caller should manage file lifecycle or pass ownership explicitly
            }
        }
    }

    private suspend fun processTextWithHttpInternal(
        userText: String,
        systemPrompt: String,
        modelToUse: String,
        clientOperation: String
    ): Result<String> {
        if (firebaseFunctionHttpUrl.isBlank() || firebaseFunctionHttpUrl == " https://call-openai-python-wb7tbqxrta-ew.a.run.app".trim() + "YOUR_PYTHON_FUNCTION_HTTP_URL") { //
            Log.e("UnifiedApiClient", "Firebase Function HTTP URL is not configured for $clientOperation.")
            return Result.failure(Exception("Firebase Function URL not configured for $clientOperation."))
        }

        return withContext(Dispatchers.IO) {
            try {
                val messages = listOf(
                    ChatMessage("system", systemPrompt), //
                    ChatMessage("user", userText) //
                )
                val chatRequestBody = ChatCompletionRequest( //
                    model = modelToUse,
                    messages = messages
                )

                Log.d("UnifiedApiClient", "Sending $clientOperation request to HTTP endpoint (Ktor) with body: $chatRequestBody")

                val response: HttpResponse = ktorHttpClient.post(firebaseFunctionHttpUrl.trim()) {
                    contentType(ContentType.Application.Json)
                    setBody(chatRequestBody)
                }

                val responseBodyString = response.bodyAsText() // Get body text first for logging in all cases
                if (!response.status.isSuccess()) {
                    Log.e("UnifiedApiClient", "$clientOperation failed via HTTP (Ktor): ${response.status.value} - $responseBodyString")
                    return@withContext Result.failure(Exception("$clientOperation failed (HTTP Ktor): ${response.status.value} - $responseBodyString"))
                }

                val openAIResponse = try {
                    jsonParser.decodeFromString<ApiChatCompletionResponse>(responseBodyString) // using the Json parser directly
                } catch (e: Exception) {
                    Log.e("UnifiedApiClient", "Failed to parse $clientOperation response (Ktor): ${e.message}. Body: $responseBodyString", e)
                    return@withContext Result.failure(Exception("$clientOperation failed (Ktor): Could not parse response. Body: $responseBodyString"))
                }

                val processedText = openAIResponse.choices?.firstOrNull()?.message?.content //
                if (processedText != null) {
                    Result.success(processedText)
                } else {
                    val errorDetails = openAIResponse.error?.message ?: "Unknown structure or empty choices" //
                    Log.e("UnifiedApiClient", "Could not parse 'content' from $clientOperation response (Ktor) or choices array empty/null. Error: $errorDetails. Full Response Body: $responseBodyString")
                    Result.failure(Exception("$clientOperation failed (Ktor): Could not extract content from response ($errorDetails)."))
                }

            } catch (e: Exception) {
                Log.e("UnifiedApiClient", "Error during $clientOperation HTTP call (Ktor): ${e.message}", e)
                Result.failure(Exception("$clientOperation failed (HTTP Ktor): ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    // --- Firebase Gemini SDK Methods ---

    private suspend fun transcribeWithGeminiSdkInternal(audioFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val audioUri = audioFile.toUri() //
                val contentResolver = context.contentResolver //

                // Prompt can be refined here if needed, incorporating `language` more explicitly if the model supports it well for transcription.
                val prompt = "You are a helpful assistant that ONLY transcribes audio. Transcribe in the Spoken Language and format the Text." //

                val model = Firebase.ai(backend = GenerativeBackend.googleAI()) //
                    .generativeModel(geminiModelName, // Using the class property for model name
                        systemInstruction = content { text(prompt) }) //

                contentResolver.openInputStream(audioUri)?.use { stream -> //
                    val audioBytes = stream.readBytes()
                    // Determine MIME type from file extension for Gemini, similar to HTTP.
                    val mimeType = contentTypeForExtension(audioFile.extension)
                    val response = model.generateContent(content { inlineData(audioBytes, mimeType) }) // adapted with mimeType
                    response.text?.let {
                        if (it.isNotBlank()) {
                            return@withContext Result.success(it)
                        } else {
                            Log.e("UnifiedApiClient", "Gemini SDK transcription resulted in empty text.")
                            return@withContext Result.failure(Exception("Transcription failed (Gemini SDK): Empty response"))
                        }
                    } ?: run {
                        Log.e("UnifiedApiClient", "Gemini SDK transcription response was null. Full response: ${response.candidates}")
                        return@withContext Result.failure(Exception("Transcription failed (Gemini SDK): Null response text"))
                    }
                } ?: return@withContext Result.failure(Exception("Failed to open audio file stream for Gemini SDK transcription.")) //
            } catch (e: Exception) {
                Log.e("UnifiedApiClient", "Error during Gemini SDK transcription: ${e.message}", e)
                Result.failure(Exception("Transcription failed (Gemini SDK): ${e.localizedMessage ?: "Unknown error"}"))
            } finally {
                // audioFile.delete() // Caller should manage file lifecycle
            }
        }
    }


    private suspend fun summarizeWithGeminiSdkInternal(text: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = "You are a helpful assistant that ONLY summarizes text. Summarize in ${language.uppercase()}." // (language was part of the prompt)
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()) //
                    .generativeModel(geminiModelName,
                        systemInstruction = content { text(systemPrompt) }) //

                val response = model.generateContent(text) //
                if (response.text.isNullOrBlank()) { //
                    Log.e("UnifiedApiClient", "Gemini SDK summarization resulted in empty or null text. Full response: ${response.candidates}")
                    Result.failure(Exception("Summary failed (Gemini SDK): Empty response")) //
                } else {
                    Result.success(response.text!!) //
                }
            } catch (e: Exception) {
                Log.e("UnifiedApiClient", "Error during Gemini SDK summarization: ${e.message}", e)
                Result.failure(Exception("Summarization failed (Gemini SDK): ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    private suspend fun translateWithGeminiSdkInternal(text: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = "You are the most helpful assistant that ONLY translates text. Translate to ${language.uppercase()}." //
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()) //
                    .generativeModel(geminiModelName,
                        systemInstruction = content { text(systemPrompt) }) //

                val response = model.generateContent(text) //
                if (response.text.isNullOrBlank()) { //
                    Log.e("UnifiedApiClient", "Gemini SDK translation resulted in empty or null text. Full response: ${response.candidates}")
                    Result.failure(Exception("Translation failed (Gemini SDK): Empty response")) //
                } else {
                    Result.success(response.text!!) //
                }
            } catch (e: Exception) {
                Log.e("UnifiedApiClient", "Error during Gemini SDK translation: ${e.message}", e)
                Result.failure(Exception("Translation failed (Gemini SDK): ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    // --- Common Helper Methods ---

    private fun isNetworkAvailable(context: Context): Boolean { //
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun contentTypeForExtension(extension: String): String = when (extension.lowercase()) { //
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4" // Gemini examples use audio/mp4 for m4a
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        else -> "audio/mpeg" // Default fallback
    }
}