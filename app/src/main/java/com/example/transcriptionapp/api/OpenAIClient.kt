package com.example.transcriptionapp.api

import android.util.Log
import com.example.transcriptionapp.api.SettingsHolder.apiKey
import com.example.transcriptionapp.model.SummarizationRequest
import com.example.transcriptionapp.model.SummarizationResponse
import com.example.transcriptionapp.model.TranscriptionResponse
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

object SettingsHolder {
    var apiKey: String = "not_set"
    var language: String = "English"
    var model: String = "gpt-4o-mini"
    var format: Boolean = false
}

interface OpenAIService {
    @POST("v1/audio/transcriptions")
    @Multipart
    suspend fun transcribeAudio(
        @Part audio: MultipartBody.Part?,
        @Part("model")
        model: RequestBody = "whisper-1".toRequestBody("text/plain".toMediaTypeOrNull()),
    ): TranscriptionResponse

    @POST("v1/chat/completions")
    suspend fun summarizeText(
        @Body request: SummarizationRequest,
    ): SummarizationResponse
}

object OpenAIClient {

    fun createService(): OpenAIService {





        Log.d("OpenAIClient", "API Key: $apiKey")

        val okHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                .addInterceptor(
                    Interceptor { chain ->
                        val request =
                            chain
                                .request()
                                .newBuilder()
                                .addHeader(
                                    "Authorization",
                                    "Bearer $apiKey")
                                .build()
                        chain.proceed(request)
                    })
                .build()

        val retrofit =
            Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        return retrofit.create(OpenAIService::class.java)

    }
}