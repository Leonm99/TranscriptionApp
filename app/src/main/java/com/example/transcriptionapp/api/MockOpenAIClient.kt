package com.example.transcriptionapp.api

import androidx.compose.ui.res.stringResource
import com.example.transcriptionapp.model.SettingsRepository
import kotlinx.coroutines.delay
import java.io.File

class MockOpenAiHandler(private val settingsRepository: SettingsRepository) {


val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
        "Quisque eu diam felis." +
        " Integer nec mauris a mauris suscipit tempus eget id lectus."



    suspend fun whisper(file: File): String {
        delay(3000)
        return text
    }

    suspend fun summarize(userText: String): String {
        delay(3000)
        return text.repeat(4)
    }

    suspend fun translate(userText: String): String {
        delay(3000)
      return text.repeat(8)
    }

    suspend fun correctSpelling(userText: String): String {

        return text
    }




    suspend fun checkApiKey(apiKey: String): Boolean {

        return true
    }






}

