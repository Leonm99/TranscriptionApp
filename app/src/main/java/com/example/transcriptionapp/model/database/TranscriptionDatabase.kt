package com.example.transcriptionapp.model.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transcriptions")
data class Transcription(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  @ColumnInfo(name = "transcription_text") val transcriptionText: String,
  @ColumnInfo(name = "summary_text") val summaryText: String?,
  @ColumnInfo(name = "translation_text") val translationText: String?,
  @ColumnInfo(name = "timestamp") val timestamp: String,
)

@Dao
interface TranscriptionDao {
  @Upsert suspend fun upsert(transcription: Transcription)

  @Delete suspend fun delete(transcription: Transcription)

  @Query("SELECT * FROM transcriptions") fun getAllTranscriptions(): Flow<List<Transcription>>

  @Query("SELECT * FROM transcriptions WHERE id = :transcriptionId")
  fun getTranscriptionById(transcriptionId: Int): Flow<Transcription>

  @Query("SELECT * FROM transcriptions ORDER BY id DESC")
  fun getAllTranscriptionsSortedByIdDesc(): Flow<List<Transcription>>

  @Query("DELETE FROM transcriptions") suspend fun deleteAll()

  @Query("DELETE FROM transcriptions WHERE id = :transcriptionId")
  suspend fun deleteTranscriptionById(transcriptionId: Int)
}

@Database(entities = [Transcription::class], version = 1)
abstract class TranscriptionDatabase : RoomDatabase() {
  abstract fun transcriptionDao(): TranscriptionDao
}
