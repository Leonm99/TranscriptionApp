package com.example.transcriptionapp.model

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64

@Serializable
data class UserPreferences(
  val selectedLanguage: String = "English",
  val selectedModel: String = "gpt-4o-mini",
  val autoSave: Boolean = true,
  val mockApi: Boolean = false,
  val dynamicColor: Boolean = true
)

object UserPreferencesSerializer : Serializer<UserPreferences> {

  private val json = Json { ignoreUnknownKeys = true }

  override val defaultValue: UserPreferences
    get() = UserPreferences()

  override suspend fun readFrom(input: InputStream): UserPreferences {
    return try {
      val encryptedBytes = withContext(Dispatchers.IO) { input.use { it.readBytes() } }
      if (encryptedBytes.isEmpty()) {
        return defaultValue
      }
      val encryptedBytesDecoded = Base64.getDecoder().decode(encryptedBytes)
      val decryptedBytes = Crypto.decrypt(encryptedBytesDecoded)
      val decodedJsonString = decryptedBytes.decodeToString()
      json.decodeFromString(decodedJsonString)
    } catch (e: SerializationException) {
      android.util.Log.e("UserPreferences", "Error reading user preferences, using default. Error: ${e.message}")
      defaultValue
    } catch (e: Exception) {
      android.util.Log.e("UserPreferences", "Failed to read or decrypt preferences: ${e.message}", e)
      defaultValue
    }
  }

  override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
    try {
      val jsonString = json.encodeToString(t)
      val bytes = jsonString.toByteArray()
      val encryptedBytes = Crypto.encrypt(bytes)
      val encryptedBytesBase64 = Base64.getEncoder().encode(encryptedBytes)
      withContext(Dispatchers.IO) { output.use { it.write(encryptedBytesBase64) } }
    } catch (e: Exception) {
      android.util.Log.e("UserPreferences", "Failed to write or encrypt preferences: ${e.message}", e)

      throw e
    }
  }
}