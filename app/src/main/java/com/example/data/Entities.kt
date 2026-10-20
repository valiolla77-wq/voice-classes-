package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a university/school course schedule item.
 * dayOfWeek: شنبه، یکشنبه، دوشنبه، سه‌شنبه، چهارشنبه، پنج‌شنبه، جمعه
 * dayIndex: 0 = شنبه, 1 = یکشنبه, 2 = دوشنبه, 3 = سه‌شنبه, 4 = چهارشنبه, 5 = پنج‌شنبه, 6 = جمعه
 */
@Entity(tableName = "course_schedules")
data class CourseSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: String,
    val dayIndex: Int,
    val courseName: String,
    val startTime: String = "08:00",
    val endTime: String = "10:00",
    val currentSession: Int = 1,
    val professor: String = "",
    val classroom: String = "",
    val colorHex: String = "#4F46E5"
)

@Entity(tableName = "recordings")
data class RecordingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String, // e.g. "شنبه - تاریخ - جلسه ۱ - پارت ۱"
    val courseName: String,
    val sessionNumber: Int,
    val partNumber: Int = 1,
    val dayOfWeek: String,
    val audioFilePath: String,
    val transcription: String = "",
    val durationSeconds: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_RECORDED, // RECORDED, TRANSCRIBING, COMPLETED, FAILED
    val pdfFilePath: String? = null
) {
    companion object {
        const val STATUS_RECORDED = "RECORDED"
        const val STATUS_TRANSCRIBING = "TRANSCRIBING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
