package com.example.transcriptionapp.model


import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface OpenAIService {
    @POST("v1/audio/transcriptions")
    @Multipart
    suspend fun transcribeAudio(
        @Part audio: MultipartBody.Part,
        @Part("model") model: RequestBody = "whisper-1".toRequestBody("text/plain".toMediaTypeOrNull())
    ): TranscriptionResponse
}

object OpenAIClient {
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .addInterceptor(Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ***REMOVED***") // Replace with your API key
                .build()
            chain.proceed(request)
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: OpenAIService = retrofit.create(OpenAIService::class.java)
}

data class TranscriptionResponse(
    val text: String
)

