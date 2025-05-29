package com.example.transcriptionapp.model

import com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.model.database.TranscriptionDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TranscriptionRepository @Inject constructor(private val transcriptionDao: TranscriptionDao) {
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

  suspend fun deleteTranscriptionById(transcriptionId: Int) {
    transcriptionDao.deleteTranscriptionById(transcriptionId)
  }
}
