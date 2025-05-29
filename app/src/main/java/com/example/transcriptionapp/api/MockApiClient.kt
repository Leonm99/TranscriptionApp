package com.example.transcriptionapp.api

import java.io.File
import kotlinx.coroutines.delay

class MockApiHandler() : ApiService {

  val text =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
      "Quisque eu diam felis." +
      " Integer nec mauris a mauris suscipit tempus eget id lectus."

  override suspend fun transcribe(file: File): Result<String> {
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

}
