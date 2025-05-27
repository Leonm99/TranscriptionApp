//package com.example.transcriptionapp.api
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.NetworkCapabilities
//import android.util.Log
//import com.example.transcriptionapp.model.SettingsRepository
//import io.ktor.client.*
//import io.ktor.client.call.*
//import io.ktor.client.engine.okhttp.*
//import io.ktor.client.plugins.contentnegotiation.*
//import io.ktor.client.plugins.logging.*
//import io.ktor.client.request.*
//import io.ktor.client.request.forms.*
//import io.ktor.client.statement.*
//import io.ktor.http.*
//import io.ktor.serialization.kotlinx.json.*
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.jsonObject
//import kotlinx.serialization.json.jsonPrimitive
//import kotlinx.serialization.json.contentOrNull
//import java.io.File
//import java.util.concurrent.TimeUnit
//import javax.inject.Inject
//
//class OpenaiApiClient @Inject constructor(
//    private val settingsRepository: SettingsRepository,
//    private val context: Context
//) : ApiService {
//
//    // Configure Ktor HttpClient with OkHttp engine
//    private val ktorHttpClient = HttpClient(OkHttp) {
//        engine {
//            // Configure OkHttp specific settings if needed
//            config {
//                connectTimeout(60, TimeUnit.SECONDS)
//                readTimeout(60, TimeUnit.SECONDS)
//                writeTimeout(60, TimeUnit.SECONDS)
//            }
//        }
//        // Install JSON content negotiation
//        install(ContentNegotiation) {
//            json(Json {
//                prettyPrint = true
//                isLenient = true
//                ignoreUnknownKeys = true
//            })
//        }
//        // Optional: Install logging for Ktor requests
//        install(Logging) {
//            logger = object : Logger {
//                override fun log(message: String) {
//                    Log.v("KtorLogger", message)
//                }
//            }
//            level = LogLevel.INFO // Or LogLevel.BODY for request/response bodies
//        }
//    }
//
//    private var firebaseFunctionHttpUrl: String = "https://call-openai-python-wb7tbqxrta-ew.a.run.app"
//    private var language: String = "English"
//    private var modelForChat: String = "gpt-4.1-nano"
//
//    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
//
//
//    init {
//        CoroutineScope(Dispatchers.IO).launch {
//            settingsRepository.userPreferencesFlow.collect { userPreferences ->
//                language = userPreferences.selectedLanguage
//
//                if (firebaseFunctionHttpUrl.isBlank()) {
//                    Log.e("OpenaiApiClient", "CRITICAL: firebaseFunctionHttpUrl is not set. All API calls will fail.")
//                }
//                Log.d("OpenaiApiClient", "Settings updated: Lang=$language, ChatModel=$modelForChat, FuncURL=$firebaseFunctionHttpUrl")
//            }
//        }
//    }
//
//    override suspend fun transcribe(audioFile: File): Result<String> {
//        if (!isNetworkAvailable(context)) return Result.failure(Exception("No network connection available"))
//        if (firebaseFunctionHttpUrl.isBlank()) return Result.failure(Exception("Firebase Function URL not configured."))
//
//        return withContext(Dispatchers.IO) {
//            try {
//                val mediaTypeAudio = audioFile.extension.lowercase().let { ext ->
//                    when (ext) {
//                        "mp3" -> "audio/mpeg"
//                        "m4a" -> "audio/mp4"
//                        "wav" -> "audio/wav"
//                        else -> "audio/mpeg" // Default or throw error
//                    }
//                }
//
//                val response = ktorHttpClient.submitFormWithBinaryData(
//                    url = firebaseFunctionHttpUrl.trim(),
//                    formData = formData {
//                        append("audio_file", audioFile.readBytes(), Headers.build {
//                            append(HttpHeaders.ContentType, mediaTypeAudio)
//                            append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
//                        })
//                        append("model", "whisper-1")
//                    }
//                )
//
//                val responseBodyString = response.bodyAsText()
//                if (!response.status.isSuccess()) {
//                    Log.e("OpenaiApiClient", "Whisper failed via HTTP (Ktor): ${response.status.value} - $responseBodyString")
//                    return@withContext Result.failure(Exception("Transcription failed (HTTP Ktor): ${response.status.value} - ${responseBodyString ?: "Unknown error"}"))
//                }
//
//                val transcriptionText = jsonParser.parseToJsonElement(responseBodyString).jsonObject["text"]?.jsonPrimitive?.contentOrNull
//                if (transcriptionText != null) Result.success(transcriptionText) else {
//                    Log.e("OpenaiApiClient", "Could not parse 'text' from Whisper response (Ktor): $responseBodyString")
//                    Result.failure(Exception("Transcription failed (Ktor): Could not parse response. Body: $responseBodyString"))
//                }
//            } catch (e: Exception) {
//                Log.e("OpenaiApiClient", "Error during Whisper HTTP call (Ktor): ${e.message}", e)
//                Result.failure(Exception("Transcription failed (HTTP Ktor): ${e.localizedMessage ?: "Unknown error"}"))
//            } finally {
//                audioFile.delete()
//            }
//        }
//    }
//
//    private suspend fun processTextWithHttp(
//        userText: String,
//        systemPrompt: String,
//        modelToUse: String,
//        clientOperation: String
//    ): Result<String> {
//        if (!isNetworkAvailable(context)) {
//            return Result.failure(Exception("No network connection available for $clientOperation"))
//        }
//        if (firebaseFunctionHttpUrl.isBlank() || firebaseFunctionHttpUrl == " https://call-openai-python-wb7tbqxrta-ew.a.run.app".trim() + "YOUR_PYTHON_FUNCTION_HTTP_URL") {
//            Log.e("OpenaiApiClient", "Firebase Function HTTP URL is not configured for $clientOperation.")
//            return Result.failure(Exception("Firebase Function URL not configured for $clientOperation."))
//        }
//
//        return withContext(Dispatchers.IO) {
//            try {
//                val messages = listOf(
//                    ChatMessage("system", systemPrompt),
//                    ChatMessage("user", userText)
//                )
//                val chatRequestBody = ChatCompletionRequest(
//                    model = modelToUse,
//                    messages = messages
//                )
//
//                Log.d("OpenaiApiClient", "Sending $clientOperation request to HTTP endpoint (Ktor) with body: $chatRequestBody")
//
//                val response: HttpResponse = ktorHttpClient.post(firebaseFunctionHttpUrl.trim()) {
//                    contentType(ContentType.Application.Json)
//                    setBody(chatRequestBody) // Ktor handles serialization
//                }
//
//                if (!response.status.isSuccess()) {
//                    val responseBodyString = response.bodyAsText()
//                    Log.e("OpenaiApiClient", "$clientOperation failed via HTTP (Ktor): ${response.status.value} - $responseBodyString")
//                    return@withContext Result.failure(Exception("$clientOperation failed (HTTP Ktor): ${response.status.value} - ${responseBodyString ?: "Unknown error"}"))
//                }
//
//                // Ktor can deserialize directly if ApiChatCompletionResponse is correctly annotated
//                val openAIResponse = try {
//                    response.body<ApiChatCompletionResponse>()
//                } catch (e: Exception) {
//                    val responseBodyString = response.bodyAsText() // Get body for logging if deserialization fails
//                    Log.e("OpenaiApiClient", "Failed to parse $clientOperation response (Ktor): ${e.message}. Body: $responseBodyString", e)
//                    return@withContext Result.failure(Exception("$clientOperation failed (Ktor): Could not parse response. Body: $responseBodyString"))
//                }
//
//
//                val processedText = openAIResponse.choices?.firstOrNull()?.message?.content
//                if (processedText != null) {
//                    Result.success(processedText)
//                } else {
//                    val errorDetails = openAIResponse.error?.message ?: "Unknown structure"
//                    Log.e("OpenaiApiClient", "Could not parse 'content' from $clientOperation response (Ktor) or choices array empty/null. Error: $errorDetails. Full Response: $openAIResponse")
//                    Result.failure(Exception("$clientOperation failed (Ktor): Could not extract content from response ($errorDetails)."))
//                }
//
//            } catch (e: Exception) {
//                Log.e("OpenaiApiClient", "Error during $clientOperation HTTP call (Ktor): ${e.message}", e)
//                Result.failure(Exception("$clientOperation failed (HTTP Ktor): ${e.localizedMessage ?: "Unknown error"}"))
//            }
//        }
//    }
//
//    override suspend fun summarize(text: String): Result<String> {
//        val systemPrompt = "You are the most helpful assistant that ONLY summarizes text. Summarize in ${language.uppercase()}."
//        return processTextWithHttp(text, systemPrompt, modelForChat, "Summarization")
//    }
//
//    override suspend fun translate(text: String): Result<String> {
//        val systemPrompt = "You are the most helpful assistant that ONLY translates text. Translate to ${language.uppercase()}."
//        return processTextWithHttp(text, systemPrompt, modelForChat, "Translation")
//    }
//
//    private fun isNetworkAvailable(context: Context): Boolean {
//        val connectivityManager =
//            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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