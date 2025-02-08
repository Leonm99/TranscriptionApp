package com.example.transcriptionapp.api

import kotlinx.coroutines.delay
import java.io.File

class MockOpenAiHandler() : OpenAiService {

  val text =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
      "Quisque eu diam felis." +
      " Integer nec mauris a mauris suscipit tempus eget id lectus."

  override suspend fun whisper(file: File): String {
    delay(3000)
    return text
  }

  override suspend fun summarize(userText: String): String {
    delay(3000)
    return text.repeat(4)
  }

  override suspend fun translate(userText: String): String {
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
