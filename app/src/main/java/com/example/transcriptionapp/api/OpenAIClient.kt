package com.example.transcriptionapp.api

import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.transcriptionapp.model.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import kotlin.time.Duration.Companion.seconds

class OpenAiHandler(private val settingsRepository: SettingsRepository) {

    private var openai: OpenAI? = null
    private val client = OkHttpClient()

    private var apiKey: String = ""
    private var language: String = "English"
    private var model: String = "gpt-o4-mini"
    private var isFormattingEnabled: Boolean = false

    private val MAX_RETRIES = 3
    private val RETRY_DELAY_MS = 1000L

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settingsRepository.userPreferencesFlow.collect { userPreferences ->

                apiKey = userPreferences.userApiKey
                language = userPreferences.selectedLanguage
                model = userPreferences.selectedModel
                isFormattingEnabled = userPreferences.formatSwitchState
                initOpenAI(apiKey)
            }
        }

    }

    private fun initOpenAI(apiKey: String) {
        openai = OpenAI(
            token = apiKey,
            timeout = Timeout(socket = 120.seconds)
        )
    }


    suspend fun whisper(file: File): String {
        println("Create transcription...")
        val path = Path(file.absolutePath)
        val transcriptionRequest = TranscriptionRequest(
            audio = FileSource(
                name = file.name,
                source = SystemFileSystem.source(path)
            ),
            model = ModelId("whisper-1")
        )
        val useCorrection = isFormattingEnabled
        var result = openai?.transcription(transcriptionRequest)?.text.orEmpty()

        if (useCorrection) {
            result = correctSpelling(result)
        }
        return result
    }

    suspend fun summarize(userText: String): String {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You will be provided with a transcription, and your task is to summarize it in the SAME language it's written in."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = userText
                )
            )
        )
        return openai?.chatCompletion(chatCompletionRequest)?.choices?.get(0)?.message?.content.orEmpty()
    }

    suspend fun translate(userText: String): String {

        val language = language.uppercase()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You will be provided with a text, and your task is to translate it into $language."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = userText
                )
            )
        )
        return openai?.chatCompletion(chatCompletionRequest)?.choices?.get(0)?.message?.content.orEmpty()
    }

    suspend fun correctSpelling(userText: String): String {
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "Your task is to correct any spelling discrepancies in the transcribed text." +
                            " Make the text look more presentable." +
                            " Add necessary punctuation such as periods, commas, capitalization, paragraphs, line breaks and use only the context provided." +
                            " Use the same language the text is written in."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = userText
                )
            )
        )
        return openai?.chatCompletion(chatCompletionRequest)?.choices?.get(0)?.message?.content.orEmpty()
    }




    suspend fun checkApiKey(apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.openai.com/v1/engines")
                .header("Authorization", "Bearer $apiKey")
                .build()

            repeat(MAX_RETRIES) { attempt ->
                try {
                    val response: Response = client.newCall(request).execute()
                    val isApiKeyValid = response.isSuccessful
                    if (isApiKeyValid){
                        response.close()
                        return@withContext true
                    }

                    if (response.code in 500..599) delay(RETRY_DELAY_MS)
                    else{
                        response.close()
                        return@withContext false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(RETRY_DELAY_MS)
                }
            }

            false
        }
    }






}

