package com.example.transcriptionapp.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.transcriptionapp.model.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

interface OpenAiService {

  suspend fun whisper(audioFile: File): Result<String>

  suspend fun summarize(text: String): Result<String>

  suspend fun translate(text: String): Result<String>

  suspend fun checkApiKey(): Boolean
}

class OpenAiHandler
@Inject
constructor(private val settingsRepository: SettingsRepository, private val context: Context) :
  OpenAiService {

  private val json = Json { ignoreUnknownKeys = true }

  private var apiKey: String = ""
  private var language: String = "English"
  private var model: String = "gpt-4o-mini"
  private var isFormattingEnabled: Boolean = false
  private var mockApi: Boolean = false

  private val MAX_RETRIES = 3
  private val RETRY_DELAY_MS = 1000L

  private val client =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS) // Example increase
      .readTimeout(20, TimeUnit.SECONDS) // Example increase
      .writeTimeout(20, TimeUnit.SECONDS) // Example increase
      .connectionPool(ConnectionPool(5, 1, TimeUnit.MINUTES))
      .build()

  init {
    CoroutineScope(Dispatchers.IO).launch {
      settingsRepository.userPreferencesFlow.collect { userPreferences ->
        apiKey = userPreferences.userApiKey
        Log.d("OpenAiHandler", "API Key: " + userPreferences.userApiKey)
        language = userPreferences.selectedLanguage
        model = userPreferences.selectedModel.takeIf { it.isNotBlank() } ?: "gpt-4o-mini"
        isFormattingEnabled = userPreferences.formatSwitchState
        mockApi = userPreferences.mockApi
      }
      // if (checkApiKey()) {}

      Log.d("OpenAiHandler", "API Key Valid: " + checkApiKey())
    }
  }

  override suspend fun whisper(audioFile: File): Result<String> {
    Log.e("OpenAiHandler", "Creating Transcription")

    if (!isNetworkAvailable(context, client)) {
      return Result.failure(Exception("No network connection available"))
    }

    val mediaTypeAudio = "audio/mpeg".toMediaType()
    val requestBody =
      MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("model", "whisper-1")
        .addFormDataPart("file", audioFile.name, audioFile.asRequestBody(mediaTypeAudio))
        .build()

    val request =
      Request.Builder()
        .url("https://api.openai.com/v1/audio/transcriptions")
        .header("Authorization", "Bearer $apiKey")
        .post(requestBody)
        .build()

    return try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          val message = parseResponseMessage(response.body?.string() ?: "")
          Log.e("OpenAiHandler", "Transcription failed: Error Code:${response.code}\n$message")
          return Result.failure(
            Exception("Transcription failed: Error Code:${response.code}\n$message")
          )
        }
        val responseBody = response.body?.string()
        val jsonResponse = json.parseToJsonElement(responseBody ?: "")
        val result = jsonResponse.jsonObject["text"]?.jsonPrimitive?.content ?: ""

        if (isFormattingEnabled) {
          Result.success(correctSpelling(result))
        } else {
          Result.success(result)
        }
      }
    } catch (e: Exception) {
      Log.e("OpenAiHandler", "Error during transcription: ${e.message}", e)
      Result.failure(Exception("Error during transcription: ${e.message}"))
    }
  }

  override suspend fun summarize(userText: String): Result<String> {

    Log.d("OpenAiHandler", "Creating Summary with text: $userText")
    Log.d("OpenAiHandler", "${client.connectionPool.idleConnectionCount()} idle connections")

    if (!isNetworkAvailable(context, client)) {
      return Result.failure(Exception("No network connection available"))
    }

    val languagePrompt = "in ${language.uppercase()}"
    val mediaTypeJson = "application/json".toMediaType()
    val requestBody =
      """
            {
              "model": "$model",
              "messages": [
                {"role": "system", "content": "You are the most helpful assistant that ONLY summarizes text."},
                {"role": "user", "content": "You will be provided with a transcription, and your task is to summarize it in $languagePrompt: $userText ,make it the best it can be my job depends on it!"}
              ]
            }
        """
        .trimIndent()
        .toRequestBody(mediaTypeJson)

    val request =
      Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .header("Authorization", "Bearer $apiKey")
        .post(requestBody)
        .build()

    return try {
      Log.d("OpenAiHandler", "Making summary request with text: $userText") // Log request start

      val response = client.newCall(request).execute()

      Log.d("OpenAiHandler", "Summary response received: ${response.code}") // Log response code
      Log.d(
        "OpenAiHandler",
        "Summary response message: ${response.message}",
      ) // Log response message

      response.use {
        if (!response.isSuccessful) {
          val errorMessage = "Summary failed: ${response.code} ${response.message}"
          Log.e("OpenAiHandler", errorMessage)
          return Result.failure(Exception(errorMessage))
        }

        val responseBody = response.body?.string()

        Log.d(
          "OpenAiHandler",
          "Summary response body: $responseBody",
        ) // Log the response body (be mindful of large responses)

        val jsonResponse = json.parseToJsonElement(responseBody ?: "")
        val result =
          jsonResponse.jsonObject["choices"]
            ?.jsonArray
            ?.get(0)
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            .orEmpty()

        if (isFormattingEnabled) {
          Result.success(correctSpelling(result))
        } else {
          Result.success(result)
        }
      }
    } catch (e: Exception) {
      Log.e("OpenAiHandler", "Error during summary: ${e.message}", e)
      Result.failure(Exception("Error during summary: ${e.message}"))
    }
  }

  override suspend fun translate(userText: String): Result<String> {
    Log.d("OpenAiHandler", "Creating Translation")

    if (!isNetworkAvailable(context, client)) {
      return Result.failure(Exception("No network connection available"))
    }

    val languagePrompt = "to ${language.uppercase()}"
    val mediaTypeJson = "application/json".toMediaType()
    val requestBody =
      """
            {
              "model": "$model",
              "messages": [
                {"role": "system", "content": "You are the most helpful assistant that ONLY translates text."},
                {"role": "user", "content": "You will be provided with a text, and your task is to translate it into $languagePrompt: $userText ,make it the best it can be my job depends on it!"}
              ]
            }
        """
        .trimIndent()
        .toRequestBody(mediaTypeJson)

    val request =
      Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .header("Authorization", "Bearer $apiKey")
        .post(requestBody)
        .build()

    return try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          Log.e("OpenAiHandler", "Translation failed: ${response.code} ${response.message}")
          return Result.failure(
            Exception("Translation failed: ${response.code} ${response.message}")
          )
        }
        val responseBody = response.body?.string()
        val jsonResponse = json.parseToJsonElement(responseBody ?: "")
        val result =
          jsonResponse.jsonObject["choices"]
            ?.jsonArray
            ?.get(0)
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            .orEmpty()

        if (isFormattingEnabled) {
          Result.success(correctSpelling(result))
        } else {
          Result.success(result)
        }
      }
    } catch (e: Exception) {
      Log.e("OpenAiHandler", "Error during translation: ${e.message}", e)
      Result.failure(Exception("Error during translation: ${e.message}"))
    }
  }

  fun correctSpelling(userText: String): String {

    if (!isNetworkAvailable(context, client)) {
      Log.e("OpenAiHandler", "No network connection available")
      return "No network connection available" // Or throw an exception
    }

    val mediaTypeJson = "application/json".toMediaType()
    val requestBody =
      """
            {
              "model": "$model",
              "messages": [
                {"role": "system", "content": "Your task is to correct any spelling discrepancies in the transcribed text. Make the text look more presentable. Add necessary punctuation such as periods, commas, capitalization, paragraphs, line breaks and use only the context provided. Use the same language the text is written in."},
                {"role": "user", "content": "$userText"}
              ]
            }
        """
        .trimIndent()
        .toRequestBody(mediaTypeJson)

    val request =
      Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .header("Authorization", "Bearer $apiKey")
        .post(requestBody)
        .build()

    return try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          println("Correction failed: ${response.code} ${response.message}")
          return "Correction failed: ${response.code} ${response.message}"
        }
        val responseBody = response.body?.string()
        val jsonResponse = json.parseToJsonElement(responseBody ?: "")
        jsonResponse.jsonObject["choices"]
          ?.jsonArray
          ?.get(0)
          ?.jsonObject
          ?.get("message")
          ?.jsonObject
          ?.get("content")
          ?.jsonPrimitive
          ?.content
          .orEmpty()
      }
    } catch (e: Exception) {
      Log.e("OpenAiHandler", "Error during correction: ${e.message}", e)
      return "Error during correction: ${e.message}"
    }
  }

  override suspend fun checkApiKey(): Boolean {

    if (!isNetworkAvailable(context, client)) {
      Log.e("OpenAiHandler", "No network connection available")
      return false
    }

    if (apiKey.isBlank()) return false
    return withContext(Dispatchers.IO) {
      val request =
        Request.Builder()
          .url("https://api.openai.com/v1/engines")
          .header("Authorization", "Bearer $apiKey")
          .build()

      repeat(MAX_RETRIES) { attempt ->
        try {
          client.newCall(request).execute().use { response ->
            val isApiKeyValid = response.isSuccessful
            if (isApiKeyValid) {
              return@withContext true
            }

            if (response.code in 500..599) delay(RETRY_DELAY_MS)
            else {
              return@withContext false
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
          delay(RETRY_DELAY_MS)
        }
      }
      return@withContext false
    }
  }

  fun isNetworkAvailable(context: Context, client: OkHttpClient): Boolean {
    val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return when {
      activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
      activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
      activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
      else -> {
        Log.e("OpenAiHandler", "No network connection available")
        client.connectionPool.evictAll()
        Log.d(
          "OpenAiHandler",
          "${client.connectionPool.idleConnectionCount()} idle connections AFTER EVICTION",
        )
        false
      }
    }
  }

  fun parseResponseMessage(responseBodyString: String): String {
    val errorJson =
      try {
        json.parseToJsonElement(responseBodyString).jsonObject
      } catch (e: Exception) {
        null // Handle cases where the body is not valid JSON
      }
    val message =
      errorJson?.get("error")?.jsonObject?.get("message")?.jsonPrimitive?.content
        ?: "Bad request. Please check your input."

    return message ?: "Bad request. Please check your input."
  }
}
