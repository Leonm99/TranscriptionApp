package com.example.transcriptionapp.api

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

  suspend fun whisper(audioFile: File): String

  suspend fun summarize(text: String): String

  suspend fun translate(text: String): String

  suspend fun checkApiKey(): Boolean
}

class OpenAiHandler @Inject constructor(private val settingsRepository: SettingsRepository) :
  OpenAiService {

  private val json = Json { ignoreUnknownKeys = true }

  private var apiKey: String = ""
  private var language: String = "English"
  private var model: String = "gpt-4o-mini"
  private var isFormattingEnabled: Boolean = false
  private var mockApi: Boolean = false

  private val MAX_RETRIES = 3
  private val RETRY_DELAY_MS = 1000L

  // Configure OkHttpClient with increased timeouts
  private val client =
    OkHttpClient.Builder()
      .connectTimeout(180, TimeUnit.SECONDS) // Increased connection timeout
      .readTimeout(120, TimeUnit.SECONDS) // Increased read timeout
      .writeTimeout(120, TimeUnit.SECONDS) // Increased write timeout
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
      if (checkApiKey()) {}

      Log.d("OpenAiHandler", "API Key Valid: " + checkApiKey())
    }
  }

  override suspend fun whisper(audioFile: File): String {
    println("Create transcription...")
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

    val useCorrection = isFormattingEnabled
    return try {
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          println("Transcription failed: ${response.code} ${response.message}")
          return "Transcription failed: ${response.code} ${response.message}"
        }
        val responseBody = response.body?.string()
        val jsonResponse = json.parseToJsonElement(responseBody ?: "")
        val result = jsonResponse.jsonObject["text"]?.jsonPrimitive?.content ?: ""

        if (useCorrection) {
          correctSpelling(result)
        } else {
          result
        }
      }
    } catch (e: Exception) {
      Log.e("OpenAiHandler", "Error during transcription: ${e.message}", e)
      return "Error during transcription: ${e.message}"
    }
  }

  override suspend fun summarize(userText: String): String {
    val languagePrompt = "in ${language.uppercase()}"
    val mediaTypeJson = "application/json".toMediaType()
    val requestBody =
      """
            {
              "model": "$model",
              "messages": [
                {"role": "system", "content": "You are a helpful assistant that summarizes text."},
                {"role": "user", "content": "You will be provided with a transcription, and your task is to summarize it in $languagePrompt: $userText"}
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
          println("Summary failed: ${response.code} ${response.message}")
          return "Summary failed: ${response.code} ${response.message}"
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
      Log.e("OpenAiHandler", "Error during summary: ${e.message}", e)
      return "Error during summary: ${e.message}"
    }
  }

  override suspend fun translate(userText: String): String {
    val languagePrompt = "to ${language.uppercase()}"
    val mediaTypeJson = "application/json".toMediaType()
    val requestBody =
      """
            {
              "model": "$model",
              "messages": [
                {"role": "system", "content": "You are a helpful assistant that translates text."},
                {"role": "user", "content": "You will be provided with a text, and your task is to translate it into $languagePrompt: $userText"}
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
          println("Translation failed: ${response.code} ${response.message}")
          return "Translation failed: ${response.code} ${response.message}"
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
      Log.e("OpenAiHandler", "Error during translation: ${e.message}", e)
      return "Error during translation: ${e.message}"
    }
  }

  suspend fun correctSpelling(userText: String): String {
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
}
