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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "transcriptions")
data class Transcription(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  @ColumnInfo(name = "transcription_text") val transcriptionText: String,
  @ColumnInfo(name = "summary_text") val summaryText: String?,
  @ColumnInfo(name = "translation_text") val translationText: String?,
  @ColumnInfo(name = "timestamp") val timestamp: String,
  @ColumnInfo(name = "fileHash") val fileHash: String?,
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

  @Query("SELECT * FROM transcriptions WHERE fileHash = :fileHash LIMIT 1")
  suspend fun getTranscriptionByFileHash(fileHash: String): Transcription?
}

@Database(entities = [Transcription::class], version = 2)
abstract class TranscriptionDatabase : RoomDatabase() {
  abstract fun transcriptionDao(): TranscriptionDao

}

val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    // Adding the fileHash column to the transcriptions table
    // Since fileHash is nullable (String?), we don't need NOT NULL or a DEFAULT here.
    // Existing rows will get NULL for fileHash.
    db.execSQL("ALTER TABLE transcriptions ADD COLUMN fileHash TEXT")
  }
}