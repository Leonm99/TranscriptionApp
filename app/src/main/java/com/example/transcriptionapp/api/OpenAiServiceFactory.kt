package com.example.transcriptionapp.api

import com.example.transcriptionapp.model.UserPreferences
import javax.inject.Inject

class OpenAiServiceFactory
@Inject
constructor(
  private val openAiHandler: OpenAiHandler,
  private val mockOpenAiHandler: MockOpenAiHandler,
) {

  fun create(userPreferences: UserPreferences): OpenAiService {
    return if (userPreferences.mockApi) {
      mockOpenAiHandler
    } else {
      openAiHandler
    }
  }
}
