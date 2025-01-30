package com.example.transcriptionapp.com.example.transcriptionapp.model

import com.example.transcriptionapp.com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.TranscriptionDao
import kotlinx.coroutines.flow.Flow

class TranscriptionRepository(private val transcriptionDao: TranscriptionDao) {
  val allTranscriptions: Flow<List<Transcription>> =
    transcriptionDao.getAllTranscriptionsSortedByIdDesc()

  suspend fun upsertTranscription(transcription: Transcription) {
    transcriptionDao.upsert(transcription)
  }

  suspend fun deleteTranscription(transcription: Transcription) {
    transcriptionDao.delete(transcription)
  }

  suspend fun deleteAllTranscriptions() {
    transcriptionDao.deleteAll()
  }
}
