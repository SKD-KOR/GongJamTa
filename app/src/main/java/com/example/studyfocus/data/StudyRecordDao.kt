package com.example.studyfocus.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: StudyRecord)

    @Query("SELECT * FROM study_records ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<StudyRecord>>

    @Query("SELECT SUM(totalFocusSeconds) FROM study_records")
    fun getTotalFocusSeconds(): Flow<Long?>

    @Delete
    suspend fun deleteRecord(record: StudyRecord)

    @Query("DELETE FROM study_records")
    suspend fun deleteAll()
}
