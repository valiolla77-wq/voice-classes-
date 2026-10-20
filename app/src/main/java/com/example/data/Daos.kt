package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseScheduleDao {
    @Query("SELECT * FROM course_schedules ORDER BY dayIndex ASC, startTime ASC")
    fun getAllSchedules(): Flow<List<CourseSchedule>>

    @Query("SELECT * FROM course_schedules WHERE dayIndex = :dayIndex ORDER BY startTime ASC")
    fun getSchedulesForDay(dayIndex: Int): Flow<List<CourseSchedule>>

    @Query("SELECT * FROM course_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): CourseSchedule?

    @Query("SELECT DISTINCT courseName FROM course_schedules ORDER BY courseName ASC")
    fun getAllCourseNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: CourseSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: CourseSchedule)

    @Query("UPDATE course_schedules SET currentSession = currentSession + 1 WHERE id = :id")
    suspend fun incrementSession(id: Long)

    @Delete
    suspend fun deleteSchedule(schedule: CourseSchedule)
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingItem>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): RecordingItem?

    @Query("SELECT * FROM recordings WHERE courseName = :courseName AND sessionNumber = :sessionNumber ORDER BY partNumber DESC")
    suspend fun getRecordingsForSession(courseName: String, sessionNumber: Int): List<RecordingItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingItem): Long

    @Update
    suspend fun updateRecording(recording: RecordingItem)

    @Query("UPDATE recordings SET transcription = :transcription, status = :status WHERE id = :id")
    suspend fun updateTranscription(id: Long, transcription: String, status: String)

    @Query("UPDATE recordings SET pdfFilePath = :pdfPath WHERE id = :id")
    suspend fun updatePdfPath(id: Long, pdfPath: String)

    @Delete
    suspend fun deleteRecording(recording: RecordingItem)
}
