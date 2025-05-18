package com.example.transcriptionapp.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.transcriptionapp.model.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable


import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Data classes for chat completion request and response
@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int? = null, // Optional
    val temperature: Double? = null // Optional
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
    suspend fun checkApiKey(): Boolean
}

class FirebaseApiClient @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val context: Context
) : OpenAiService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var firebaseFunctionHttpUrl: String = " https://call-openai-python-wb7tbqxrta-ew.a.run.app"

    private var language: String = "English"
    private var modelForChat: String = "gpt-4o-mini"

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }


    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.userPreferencesFlow.collect { userPreferences ->
                language = userPreferences.selectedLanguage
                modelForChat = userPreferences.selectedModel.takeIf { it.isNotBlank() } ?: "gpt-4o-mini"

                if (firebaseFunctionHttpUrl.isBlank()) {
                    Log.e("FirebaseApiClient", "CRITICAL: firebaseFunctionHttpUrl is not set. All API calls will fail.")
                }
                Log.d("FirebaseApiClient", "Settings updated: Lang=$language, ChatModel=$modelForChat, FuncURL=$firebaseFunctionHttpUrl")
            }
        }
    }

    override suspend fun whisper(audioFile: File): Result<String> {
        if (!isNetworkAvailable(context)) {
            return Result.failure(Exception("No network connection available"))
        }
        if (firebaseFunctionHttpUrl.isBlank()) {
            Log.e("FirebaseApiClient", "Firebase Function HTTP URL is not configured for whisper.")
            return Result.failure(Exception("Firebase Function URL not configured."))
        }

        return withContext(Dispatchers.IO) {
            try {
                val mediaTypeAudio = (audioFile.extension.lowercase().let { ext ->
                    when (ext) {
                        "mp3" -> "audio/mpeg"
                        "m4a" -> "audio/mp4"
                        "wav" -> "audio/wav"
                        else -> "audio/mpeg"
                    }
                }).toMediaType()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("audio_file", audioFile.name, audioFile.asRequestBody(mediaTypeAudio))
                    .addFormDataPart("model", "whisper-1")
                    .build()

                val request = Request.Builder()
                    .url(firebaseFunctionHttpUrl.trim())
                    .post(requestBody)
                    .build()

                Log.d("FirebaseApiClient", "Calling HTTP endpoint for transcription.")
                okHttpClient.newCall(request).execute().use { response ->
                    val responseBodyString = response.body?.string()
                    if (!response.isSuccessful) {
                        Log.e("FirebaseApiClient", "Whisper failed via HTTP: ${response.code} - $responseBodyString")
                        return@withContext Result.failure(Exception("Transcription failed (HTTP): ${response.code} - ${responseBodyString ?: "Unknown error"}"))
                    }
                    if (responseBodyString == null) {
                        Log.e("FirebaseApiClient", "Whisper response body is null.")
                        return@withContext Result.failure(Exception("Transcription failed: Empty response body"))
                    }

                    val jsonResponse = jsonParser.parseToJsonElement(responseBodyString)
                    val transcriptionText = jsonResponse.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                    if (transcriptionText != null) {
                        Result.success(transcriptionText)
                    } else {
                        Log.e("FirebaseApiClient", "Could not parse 'text' from Whisper response: $responseBodyString")
                        Result.failure(Exception("Transcription failed: Could not parse response. Body: $responseBodyString"))
                    }
                }
            } catch (e: Exception) {
                Log.e("FirebaseApiClient", "Error during Whisper HTTP call: ${e.message}", e)
                Result.failure(Exception("Transcription failed (HTTP): ${e.localizedMessage ?: "Unknown error"}"))
            } finally {
                audioFile.delete()
            }
        }
    }

    private suspend fun processTextWithHttp(
        userText: String,
        systemPrompt: String,
        modelToUse: String,
        clientOperation: String
    ): Result<String> {
        if (!isNetworkAvailable(context)) {
            return Result.failure(Exception("No network connection available for $clientOperation"))
        }
        if (firebaseFunctionHttpUrl.isBlank() || firebaseFunctionHttpUrl == " https://call-openai-python-wb7tbqxrta-ew.a.run.app".trim() + "YOUR_PYTHON_FUNCTION_HTTP_URL") {
            Log.e("FirebaseApiClient", "Firebase Function HTTP URL is not configured for $clientOperation.")
            return Result.failure(Exception("Firebase Function URL not configured for $clientOperation."))
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

                val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                val requestBodyString = jsonParser.encodeToString(chatRequestBody)
                val body: RequestBody = requestBodyString.toRequestBody(jsonMediaType)

                Log.d("FirebaseApiClient", "Sending $clientOperation request to HTTP endpoint with body: $requestBodyString")

                val request = Request.Builder()
                    .url(firebaseFunctionHttpUrl.trim())
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    val responseBodyString = response.body?.string()
                    if (!response.isSuccessful) {
                        Log.e("FirebaseApiClient", "$clientOperation failed via HTTP: ${response.code} - $responseBodyString")
                        return@withContext Result.failure(Exception("$clientOperation failed (HTTP): ${response.code} - ${responseBodyString ?: "Unknown error"}"))
                    }
                    if (responseBodyString == null) {
                        Log.e("FirebaseApiClient", "$clientOperation response body is null.")
                        return@withContext Result.failure(Exception("$clientOperation failed: Empty response body"))
                    }

                    Log.d("FirebaseApiClient", "$clientOperation response: $responseBodyString")
                    val openAIResponse = try {
                        jsonParser.decodeFromString<OpenAIChatCompletionResponse>(responseBodyString)
                    } catch (e: Exception) {
                        Log.e("FirebaseApiClient", "Failed to parse $clientOperation response: ${e.message}", e)
                        return@withContext Result.failure(Exception("$clientOperation failed: Could not parse response. Body: $responseBodyString"))
                    }

                    val processedText = openAIResponse.choices?.firstOrNull()?.message?.content
                    if (processedText != null) {
                        Result.success(processedText)
                    } else {
                        val errorDetails = openAIResponse.error?.message ?: "Unknown structure"
                        Log.e("FirebaseApiClient", "Could not parse 'content' from $clientOperation response or choices array empty/null. Error: $errorDetails. Full Body: $responseBodyString")
                        Result.failure(Exception("$clientOperation failed: Could not extract content from response ($errorDetails)."))
                    }
                }

            } catch (e: Exception) {
                Log.e("FirebaseApiClient", "Error during $clientOperation HTTP call: ${e.message}", e)
                Result.failure(Exception("$clientOperation failed (HTTP): ${e.localizedMessage ?: "Unknown error"}"))
            }
        }
    }

    override suspend fun summarize(text: String): Result<String> {
        val systemPrompt = "You are the most helpful assistant that ONLY summarizes text. Summarize in ${language.uppercase()}."
        return processTextWithHttp(text, systemPrompt, modelForChat, "Summarization")
    }

    override suspend fun translate(text: String): Result<String> {
        val systemPrompt = "You are the most helpful assistant that ONLY translates text. Translate to ${language.uppercase()}."
        return processTextWithHttp(text, systemPrompt, modelForChat, "Translation")
    }

    override suspend fun checkApiKey(): Boolean {
        Log.d("FirebaseApiClient", "Checking Firebase Function reachability (network status) for HTTP endpoint")
        if (!isNetworkAvailable(context)) {
            Log.e("FirebaseApiClient", "No network connection available.")
            return false
        }
        return true
    }

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
}