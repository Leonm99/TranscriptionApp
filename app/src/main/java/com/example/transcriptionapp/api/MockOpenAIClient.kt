package com.example.transcriptionapp.api

import java.io.File
import kotlinx.coroutines.delay

class MockOpenAiHandler() : OpenAiService {

  val text =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
      "Quisque eu diam felis." +
      " Integer nec mauris a mauris suscipit tempus eget id lectus."

  override suspend fun whisper(file: File): Result<String> {
    delay(3000)
    return Result.success(text)
  }

  override suspend fun summarize(userText: String): Result<String> {
    delay(3000)
    return Result.success(text.repeat(4))
  }

  override suspend fun translate(userText: String): Result<String> {
    delay(3000)
    return Result.success(text.repeat(8))
  }

  suspend fun checkApiKey(apiKey: String): Boolean {

    return true
  }

  override suspend fun checkApiKey(): Boolean {
    delay(3000)
    return true
  }
}
