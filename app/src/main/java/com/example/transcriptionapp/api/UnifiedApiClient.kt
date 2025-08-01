package com.example.transcriptionapp.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.net.toUri
import com.example.transcriptionapp.model.ProviderType
import com.example.transcriptionapp.model.SettingsRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.media.MediaMetadataRetriever
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject


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

interface ApiService {
    suspend fun transcribe(audioFile: File): Result<String>
    suspend fun summarize(text: String): Result<String>
    suspend fun translate(text: String): Result<String>
}


class UnifiedApiClient @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : ApiService {

    private val firebaseAppCheck: FirebaseAppCheck = FirebaseAppCheck.getInstance()

    private val ktorHttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
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

    private var firebaseFunctionHttpUrl: String = "https://call-openai-python-wb7tbqxrta-ew.a.run.app"
    private var language: String = "English"
    private var transcriptionProvider: ProviderType = ProviderType.OPEN_AI
    private var summarizationProvider: ProviderType = ProviderType.OPEN_AI
    private var openAiModelForChat: String = "gpt-4o-mini"
    private val geminiModelName: String = "gemini-2.5-flash-lite"

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

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

    // Helper function to get App Check token
    private suspend fun getAppCheckToken(): String? {
        return try {
            val appCheckTokenResult = firebaseAppCheck.getAppCheckToken(false).await() // false = don't force refresh
            Log.d("UnifiedApiClient", "App Check token retrieved")
            appCheckTokenResult.token
        } catch (e: Exception) {
            Log.e("UnifiedApiClient", "Error getting App Check token", e)
            null
        }
    }

    // Helper function to get Firebase ID token
    private suspend fun getFirebaseIdToken(): String? {
        return try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val idTokenResult = currentUser.getIdToken(false).await() // false = don't force refresh
                Log.d("UnifiedApiClient", "Firebase ID token retrieved")
                idTokenResult.token
            } else {
                Log.e("UnifiedApiClient", "No Firebase user logged in.")
                null
            }
        } catch (e: Exception) {
            Log.e("UnifiedApiClient", "Error getting Firebase ID token", e)
            null
        }
    }

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
            val systemPrompt = "You are the most helpful assistant that ONLY summarizes text. Summarize in ${language.uppercase()}."
            processTextWithHttpInternal(text, systemPrompt, openAiModelForChat, "Summarization")
        }
    }

    override suspend fun translate(text: String): Result<String> {
        if (!isNetworkAvailable(context)) return Result.failure(Exception("No network connection available for Translation"))

        return if (summarizationProvider == ProviderType.GEMINI) { // Or a new translationProvider
            translateWithGeminiSdkInternal(text)
        } else {
            val systemPrompt = "You are the most helpful assistant that ONLY translates text. Translate to ${language.uppercase()}."
            processTextWithHttpInternal(text, systemPrompt, openAiModelForChat, "Translation")
        }
    }

    private suspend fun transcribeWithHttpInternal(audioFile: File): Result<String> {
        if (firebaseFunctionHttpUrl.isBlank()) return Result.failure(Exception("Firebase Function URL not configured for HTTP transcription."))

        val appCheckToken = getAppCheckToken() // Get the App Check token
        val idToken = getFirebaseIdToken() // Get the Firebase ID token

        if (appCheckToken == null) {
            return Result.failure(Exception("Failed to retrieve App Check token for transcription."))
        }
        if (idToken == null) {
            return Result.failure(Exception("Failed to retrieve Firebase ID token for transcription. User not authenticated?"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val mediaTypeAudio = contentTypeForExtension(audioFile.extension)

                // Get audio duration
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(audioFile.absolutePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val audioDurationSeconds = durationStr?.toLongOrNull()?.div(1000.0) ?: 0.0
                retriever.release()

                val response = ktorHttpClient.submitFormWithBinaryData(
                    url = firebaseFunctionHttpUrl.trim(),
                    formData = formData {
                        append("audio_file", audioFile.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, mediaTypeAudio)
                            append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
                        })
                        append("model", "whisper-1")
                        append("audio_duration_seconds", audioDurationSeconds.toString()) // Add duration
                    }
                ) {
                    header("X-Firebase-AppCheck", appCheckToken)
                    header("Authorization", "Bearer $idToken") // Add Firebase ID token
                }

                val responseBodyString = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    Log.e("UnifiedApiClient", "Whisper failed via HTTP (Ktor): ${response.status.value} - $responseBodyString")
                    
                    // Parse and provide user-friendly error messages
                    val userFriendlyError = when (response.status.value) {
                        401 -> "Authentication failed. Please sign in again."
                        403 -> "Access denied. Please check your account permissions."
                        429 -> {
                            // Try to extract the specific limit message from the response
                            val limitMessage = responseBodyString.takeIf { it.isNotBlank() } ?: "Too many requests. Please try again later."
                            "Usage limit exceeded: $limitMessage"
                        }
                        500 -> "Server error occurred. Please try again."
                        503 -> "Service temporarily unavailable. Please try again later."
                        else -> {
                            // Try to extract error message from response body if it's a readable error
                            if (responseBodyString.isNotBlank() && responseBodyString.length < 200) {
                                "Transcription failed: $responseBodyString"
                            } else {
                                "Transcription failed with error code ${response.status.value}"
                            }
                        }
                    }
                    
                    if (response.status.value == 401 || response.status.value == 403) {
                        Log.e("UnifiedApiClient", "App Check verification likely failed. Ensure App Check is set up correctly in Firebase Console and client.")
                    }
                    
                    return@withContext Result.failure(Exception(userFriendlyError))
                }

                val transcriptionText = jsonParser.parseToJsonElement(responseBodyString).jsonObject["text"]?.jsonPrimitive?.contentOrNull
                if (transcriptionText != null) Result.success(transcriptionText) else {
                    Log.e("UnifiedApiClient", "Could not parse 'text' from Whisper response (Ktor): $responseBodyString")
                    Result.failure(Exception("Transcription failed (Ktor): Could not parse response. Body: $responseBodyString"))
                }
            } catch (e: Exception) {
                Log.e("UnifiedApiClient", "Error during Whisper HTTP call (Ktor): ${e.message}", e)
                Result.failure(Exception("Transcription failed (HTTP Ktor): ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    private suspend fun processTextWithHttpInternal(
        userText: String,
        systemPrompt: String,
        modelToUse: String,
        clientOperation: String
    ): Result<String> {
        if (firebaseFunctionHttpUrl.isBlank() || firebaseFunctionHttpUrl == " https://call-openai-python-wb7tbqxrta-ew.a.run.app".trim() + "YOUR_PYTHON_FUNCTION_HTTP_URL") {
            Log.e("UnifiedApiClient", "Firebase Function HTTP URL is not configured for $clientOperation.")
            return Result.failure(Exception("Firebase Function URL not configured for $clientOperation."))
        }

        val appCheckToken = getAppCheckToken() // Get the App Check token
        val idToken = getFirebaseIdToken() // Get the Firebase ID token

        if (appCheckToken == null) {
            return Result.failure(Exception("Failed to retrieve App Check token for $clientOperation."))
        }
        if (idToken == null) {
            return Result.failure(Exception("Failed to retrieve Firebase ID token for $clientOperation. User not authenticated?"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", userText)
                )
                val chatRequestBody = ChatCompletionRequest(
                    model = modelToUse,
                    messages = messages
                )

                Log.d("UnifiedApiClient", "Sending $clientOperation request to HTTP endpoint (Ktor) with body: $chatRequestBody")

                val response: HttpResponse = ktorHttpClient.post(firebaseFunctionHttpUrl.trim()) {
                    contentType(ContentType.Application.Json)
                    setBody(chatRequestBody)
                    header("X-Firebase-AppCheck", appCheckToken)
                    header("Authorization", "Bearer $idToken") // Add Firebase ID token
                }

                val responseBodyString = response.bodyAsText()
                if (!response.status.isSuccess()) {
                    Log.e("UnifiedApiClient", "$clientOperation failed via HTTP (Ktor): ${response.status.value} - $responseBodyString")
                    
                    // Parse and provide user-friendly error messages for chat operations
                    val userFriendlyError = when (response.status.value) {
                        401 -> "Authentication failed. Please sign in again."
                        403 -> "Access denied. Please check your account permissions."
                        429 -> {
                            // Try to extract the specific limit message from the response
                            val limitMessage = responseBodyString.takeIf { it.isNotBlank() } ?: "Too many requests. Please try again later."
                            "Usage limit exceeded: $limitMessage"
                        }
                        500 -> "Server error occurred. Please try again."
                        503 -> "Service temporarily unavailable. Please try again later."
                        else -> {
                            // Try to extract error message from response body if it's a readable error
                            if (responseBodyString.isNotBlank() && responseBodyString.length < 200) {
                                "$clientOperation failed: $responseBodyString"
                            } else {
                                "$clientOperation failed with error code ${response.status.value}"
                            }
                        }
                    }
                    
                    if (response.status.value == 401 || response.status.value == 403) {
                        Log.e("UnifiedApiClient", "App Check verification likely failed for $clientOperation. Ensure App Check is set up correctly in Firebase Console and client.")
                    }
                    
                    return@withContext Result.failure(Exception(userFriendlyError))
                }

                val openAIResponse = try {
                    jsonParser.decodeFromString<ApiChatCompletionResponse>(responseBodyString)
                } catch (e: Exception) {
                    Log.e("UnifiedApiClient", "Failed to parse $clientOperation response (Ktor): ${e.message}. Body: $responseBodyString", e)
                    return@withContext Result.failure(Exception("$clientOperation failed (Ktor): Could not parse response. Body: $responseBodyString"))
                }

                val processedText = openAIResponse.choices?.firstOrNull()?.message?.content
                if (processedText != null) {
                    Result.success(processedText)
                } else {
                    val errorDetails = openAIResponse.error?.message ?: "Unknown structure or empty choices"
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
                val audioUri = audioFile.toUri()
                val contentResolver = context.contentResolver
                val prompt = "You are a helpful assistant that ONLY transcribes audio. Transcribe in the Spoken Language and format the Text."

                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(geminiModelName,
                        systemInstruction = content { text(prompt) })

                val inputStream = contentResolver.openInputStream(audioUri)
                if (inputStream == null) {
                    return@withContext Result.failure(Exception("Failed to open audio file stream for Gemini SDK transcription."))
                }
                
                inputStream.use { stream ->
                    val audioBytes = stream.readBytes()
                    val mimeType = contentTypeForExtension(audioFile.extension)
                    val response = model.generateContent(content { inlineData(audioBytes, mimeType) })
                    val responseText = response.text
                    if (responseText != null) {
                        if (responseText.isNotBlank()) {
                            return@withContext Result.success(responseText)
                        } else {
                            Log.e("UnifiedApiClient", "Gemini SDK transcription resulted in empty text.")
                            return@withContext Result.failure(Exception("Transcription failed (Gemini SDK): Empty response"))
                        }
                    } else {
                        Log.e("UnifiedApiClient", "Gemini SDK transcription response was null. Full response: ${response.candidates}")
                        return@withContext Result.failure(Exception("Transcription failed (Gemini SDK): Null response text"))
                    }
                }
            } catch (e: Exception) {
                Log.e("UnifiedApiClient", "Error during Gemini SDK transcription: ${e.message}", e)
                Result.failure(Exception("Transcription failed (Gemini SDK): ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    private suspend fun summarizeWithGeminiSdkInternal(text: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = "You are a helpful assistant that ONLY summarizes text. Summarize in ${language.uppercase()}."
                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(geminiModelName,
                        systemInstruction = content { text(systemPrompt) })

                val response = model.generateContent(text)
                if (response.text.isNullOrBlank()) {
                    Log.e("UnifiedApiClient", "Gemini SDK summarization resulted in empty or null text. Full response: ${response.candidates}")
                    Result.failure(Exception("Summary failed (Gemini SDK): Empty response"))
                } else {
                    Result.success(response.text!!)
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
                val systemPrompt = "You are the most helpful assistant that ONLY translates text. Translate to ${language.uppercase()}."
                val model = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(geminiModelName,
                        systemInstruction = content { text(systemPrompt) })

                val response = model.generateContent(text)
                if (response.text.isNullOrBlank()) {
                    Log.e("UnifiedApiClient", "Gemini SDK translation resulted in empty or null text. Full response: ${response.candidates}")
                    Result.failure(Exception("Translation failed (Gemini SDK): Empty response"))
                } else {
                    Result.success(response.text!!)
                }
            } catch (e: Exception) {
                Log.e("UnifiedApiClient", "Error during Gemini SDK translation: ${e.message}", e)
                Result.failure(Exception("Translation failed (Gemini SDK): ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }


    // --- Common Helper Methods ---
    private fun isNetworkAvailable(context: Context): Boolean {
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

    private fun contentTypeForExtension(extension: String): String = when (extension.lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "flac" -> "audio/flac"
        else -> "audio/mpeg"
    }
}
