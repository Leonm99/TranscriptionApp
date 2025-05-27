package com.example.transcriptionapp.api

import com.example.transcriptionapp.model.UserPreferences
import javax.inject.Inject

class ApiServiceFactory
@Inject
constructor(
  private val UnifiedApiClient: UnifiedApiClient,
  private val MockApiHandler: MockApiHandler,
) {

  fun create(userPreferences: UserPreferences): ApiService {
    return if (userPreferences.mockApi) {
      MockApiHandler
    } else {
      UnifiedApiClient
    }
  }
}
