package com.example.transcriptionapp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.example.transcriptionapp.api.MockOpenAiHandler
import com.example.transcriptionapp.api.OpenAiHandler
import com.example.transcriptionapp.api.OpenAiServiceFactory
import com.example.transcriptionapp.com.example.transcriptionapp.model.TranscriptionRepository
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.TranscriptionDao
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.TranscriptionDatabase
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.model.UserPreferences
import com.example.transcriptionapp.model.UserPreferencesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HiltModule {

  @Provides
  @Singleton
  fun provideTranscriptionDatabase(@ApplicationContext context: Context): TranscriptionDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        TranscriptionDatabase::class.java,
        "transcription_database",
      )
      .build()
  }

  @Provides
  @Singleton
  fun provideTranscriptionDao(transcriptionDatabase: TranscriptionDatabase): TranscriptionDao {
    return transcriptionDatabase.transcriptionDao()
  }

  private const val USER_PREFERENCES_NAME = "user_preferences"
  private const val DATA_STORE_FILE_NAME = "user_prefs.pb"

  @Provides
  @Singleton
  fun provideSettingsRepository(dataStore: DataStore<UserPreferences>): SettingsRepository {
    return SettingsRepository(dataStore)
  }

  @Provides
  @Singleton
  fun provideTranscriptionRepository(transcriptionDao: TranscriptionDao): TranscriptionRepository {
    return TranscriptionRepository(transcriptionDao)
  }

  @Provides
  @Singleton
  fun providesUserPreferencesDataStore(
    @ApplicationContext context: Context,
    @IODispatcher ioDispatcher: CoroutineDispatcher,
    @ApplicationScope scope: CoroutineScope,
    userPreferencesSerializer: UserPreferencesSerializer,
  ): DataStore<UserPreferences> =
    DataStoreFactory.create(
      serializer = userPreferencesSerializer,
      scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
    ) {
      context.dataStoreFile(USER_PREFERENCES_NAME)
    }

  @Provides
  @Singleton
  fun provideUserPreferencesSerializer(): UserPreferencesSerializer {
    return UserPreferencesSerializer
  }

  @Provides
  @Singleton
  fun provideOpenAiHandler(settingsRepository: SettingsRepository): OpenAiHandler {
    return OpenAiHandler(settingsRepository)
  }

  @Provides
  @Singleton
  fun provideMockOpenAiHandler(): MockOpenAiHandler {
    return MockOpenAiHandler()
  }

  @Provides
  @Singleton
  fun provideOpenAiServiceFactory(
    openAiHandler: OpenAiHandler,
    mockOpenAiHandler: MockOpenAiHandler,
  ): OpenAiServiceFactory {
    return OpenAiServiceFactory(
      openAiHandler = openAiHandler,
      mockOpenAiHandler = mockOpenAiHandler,
    )
  }

  @Provides @IODispatcher fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

  @Provides
  @DefaultDispatcher
  fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

  @Provides
  @Singleton
  @ApplicationScope
  fun providesCoroutineScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
    CoroutineScope(SupervisorJob() + dispatcher)

  @Qualifier @Retention(AnnotationRetention.BINARY) annotation class IODispatcher

  @Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher

  @Retention(AnnotationRetention.RUNTIME) @Qualifier annotation class ApplicationScope
}
