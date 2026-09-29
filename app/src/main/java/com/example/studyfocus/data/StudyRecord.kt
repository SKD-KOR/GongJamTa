package com.example.studyfocus.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_records")
data class StudyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val totalFocusSeconds: Long,
    val date: String
)
