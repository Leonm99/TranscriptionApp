package com.example.transcriptionapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileUtils {

  fun saveFileToCache(context: Context, uri: Uri): File? {
    val cacheDir = context.cacheDir
    val tempDir = File(cacheDir, "temp")
    if (!tempDir.exists()) {
      tempDir.mkdirs()
    }
    return try {
      val randomName = UUID.randomUUID().toString()
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val file = File(tempDir, "$randomName.${getFileExtensionFromUri(context, uri)}")
        FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }
        Log.d("ReceiveIntentActivity", "File saved to cache: ${file.absolutePath}")
        file
      }
    } catch (e: Exception) {
      Log.e("ReceiveIntentActivity", "Failed to save file to cache", e)
      null
    }
  }

  private fun getFileExtensionFromUri(context: Context, uri: Uri): String {
    val mimeType = context.contentResolver.getType(uri)
    return android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
  }

  fun getFileFromUri(uri: Uri, context: Context): File? {
    val cacheDir = context.cacheDir
    val tempDir = File(cacheDir, "temp")
    if (!tempDir.exists()) {
      tempDir.mkdirs()
    }

    return try {
      // Open an input stream from the URI
      val inputStream = context.contentResolver.openInputStream(uri) ?: return null
      // Create a temporary file in the app's cache directory

      val tempFile = File.createTempFile("temp_audio", ".mp3", tempDir)
      Log.d("TAG", "Created temp file: ${tempFile.absolutePath}")
      // Write the content of the input stream to the temporary file
      tempFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
      tempFile
    } catch (e: Exception) {
      Log.e("TAG", "Error copying file from URI: ${e.localizedMessage}")
      null
    }
  }

  fun clearTempDir(context: Context) {
    val cacheDir = context.cacheDir
    val tempDir = File(cacheDir, "temp")
    if (tempDir.exists()) {
      tempDir.listFiles()?.forEach { file ->
        if (file.isFile) {
          file.delete()
        }
      }
    }
    Log.d("TAG", "Cleared temp directory: ${tempDir.absolutePath}")
  }

  suspend fun convertToMP3(inputUri: Uri, context: Context): File? {
    Log.d("FileUtil", "Input URI: ${inputUri.path}")
    // Check if the input is already an MP3 file
    val mimeType = context.contentResolver.getType(inputUri)
    if (mimeType == "audio/mp3") {
      return getFileFromUri(inputUri, context)
    }

    return try {
      // Generate a random output name

      // Define the output path using the random name
      val outputPath =
        File("${context.cacheDir}/temp", inputUri.lastPathSegment + ".mp3").absolutePath

      // FFmpeg command to convert the audio file to MP3, optimized for Whisper transcription
      val command = "-y -i ${inputUri.path} -ac 1 -ar 16000 -c:a libmp3lame -b:a 64k $outputPath"

      // Execute the FFmpeg command using FFmpegKit
      val session = FFmpegKit.execute(command)

      // Check the return code of the execution
      val returnCode = session.returnCode

      // Check if the conversion was successful
      if (ReturnCode.isSuccess(returnCode)) {
        File(outputPath) // Return the file that gets created
      } else {
        Log.d("FileUtil", "Failed to convert file to MP3: $returnCode")
        null // Return null if conversion failed
      }
    } catch (e: Exception) {
      Log.d("FileUtil", "Error converting file to MP3", e)
      null // Return null in case of an error
    }
  }
}
