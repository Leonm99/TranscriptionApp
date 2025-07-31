package com.example.transcriptionapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
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

  fun convertToMP3(inputUri: Uri, context: Context): File? {
    return convertToMP3WithSilenceTrimming(
      inputUri = inputUri,
      context = context,
      enableSilenceTrimming = false
    )
  }

  fun convertToMP3WithSilenceTrimming(
    inputUri: Uri,
    context: Context,
    enableSilenceTrimming: Boolean = true,
    silenceThresholdDb: Int = -40,
    silenceDurationSeconds: Float = 2.0f
  ): File? {
    Log.d("FileUtil", "Input URI: ${inputUri.path}")
    Log.d("FileUtil", "Silence trimming enabled: $enableSilenceTrimming")
    
    // Check if the input is already an MP3 file
    val mimeType = context.contentResolver.getType(inputUri)
    if (mimeType == "audio/mp3" && !enableSilenceTrimming) {
      return getFileFromUri(inputUri, context)
    }

    return try {
      // Define the output path using the random name
      val outputPath =
        File("${context.cacheDir}/temp", inputUri.lastPathSegment + ".mp3").absolutePath

      // Build FFmpeg command with optional silence removal
      val command = if (enableSilenceTrimming) {
        // FFmpeg command with silence removal filter
        val silenceFilter = "silenceremove=stop_periods=-1:stop_duration=$silenceDurationSeconds:stop_threshold=${silenceThresholdDb}dB"
        "-y -i ${inputUri.path} -af $silenceFilter -ac 1 -ar 16000 -c:a libmp3lame -b:a 64k $outputPath"
      } else {
        // Original FFmpeg command without silence removal
        "-y -i ${inputUri.path} -ac 1 -ar 16000 -c:a libmp3lame -b:a 64k $outputPath"
      }

      Log.d("FileUtil", "FFmpeg command: $command")

      // Execute the FFmpeg command using FFmpegKit
      val session = FFmpegKit.execute(command)

      // Check the return code of the execution
      val returnCode = session.returnCode

      // Check if the conversion was successful
      if (ReturnCode.isSuccess(returnCode)) {
        val outputFile = File(outputPath)
        if (enableSilenceTrimming) {
          Log.d("FileUtil", "Successfully converted and trimmed silence from audio file")
        } else {
          Log.d("FileUtil", "Successfully converted audio file")
        }
        outputFile
      } else {
        Log.e("FileUtil", "Failed to convert file to MP3: $returnCode")
        Log.e("FileUtil", "FFmpeg logs: ${session.allLogsAsString}")
        null
      }
    } catch (e: Exception) {
      Log.e("FileUtil", "Error converting file to MP3", e)
      null
    }
  }

  fun getFileHash(context: Context, uri: Uri): String? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { inputStream ->
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          digest.update(buffer, 0, bytesRead)
        }
        // Convert byte array to Hex String
        val md5sum = digest.digest()
        val hexString = StringBuilder()
        for (byte in md5sum) {
          hexString.append(String.format("%02x", byte))
        }
        hexString.toString()
      }
    } catch (e: Exception) {
       Log.e("FileUtils", "Error calculating file hash for URI: $uri", e)
      null // Return null if hashing fails
    }
  }

  fun getFileHash(file: File): String? {
    return try {
      FileInputStream(file).use { fis ->
        val digest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fis.read(buffer).also { bytesRead = it } != -1) {
          digest.update(buffer, 0, bytesRead)
        }
        val md5sum = digest.digest()
        val hexString = StringBuilder()
        for (byte in md5sum) {
          hexString.append(String.format("%02x", byte))
        }
        hexString.toString()
      }
    } catch (e: Exception) {
       Log.e("FileUtils", "Error calculating file hash for File: ${file.path}", e)
      null
    }
  }
}
