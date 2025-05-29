package com.example.transcriptionapp.api

import com.example.transcriptionapp.model.UserPreferences
import javax.inject.Inject

class ApiServiceFactory
@Inject
constructor(
  private val unifiedApiClient: UnifiedApiClient,
  private val mockApiHandler: MockApiHandler,
) {

  fun create(userPreferences: UserPreferences): ApiService {
    return if (userPreferences.mockApi) {
      mockApiHandler
    } else {
      unifiedApiClient
    }
  }
}
