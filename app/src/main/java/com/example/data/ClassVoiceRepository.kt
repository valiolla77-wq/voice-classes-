package com.example.data

import kotlinx.coroutines.flow.Flow

class ClassVoiceRepository(
    private val courseScheduleDao: CourseScheduleDao,
    private val recordingDao: RecordingDao
) {
    val allSchedules: Flow<List<CourseSchedule>> = courseScheduleDao.getAllSchedules()
    val allCourseNames: Flow<List<String>> = courseScheduleDao.getAllCourseNames()
    val allRecordings: Flow<List<RecordingItem>> = recordingDao.getAllRecordings()

    fun getSchedulesForDay(dayIndex: Int): Flow<List<CourseSchedule>> {
        return courseScheduleDao.getSchedulesForDay(dayIndex)
    }

    suspend fun getScheduleById(id: Long): CourseSchedule? {
        return courseScheduleDao.getScheduleById(id)
    }

    suspend fun insertSchedule(schedule: CourseSchedule): Long {
        return courseScheduleDao.insertSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: CourseSchedule) {
        courseScheduleDao.updateSchedule(schedule)
    }

    suspend fun incrementScheduleSession(id: Long) {
        courseScheduleDao.incrementSession(id)
    }

    suspend fun deleteSchedule(schedule: CourseSchedule) {
        courseScheduleDao.deleteSchedule(schedule)
    }

    suspend fun insertRecording(recording: RecordingItem): Long {
        return recordingDao.insertRecording(recording)
    }

    suspend fun updateRecording(recording: RecordingItem) {
        recordingDao.updateRecording(recording)
    }

    suspend fun updateTranscription(id: Long, transcription: String, status: String) {
        recordingDao.updateTranscription(id, transcription, status)
    }

    suspend fun updatePdfPath(id: Long, pdfPath: String) {
        recordingDao.updatePdfPath(id, pdfPath)
    }

    suspend fun getRecordingsForSession(courseName: String, sessionNumber: Int): List<RecordingItem> {
        return recordingDao.getRecordingsForSession(courseName, sessionNumber)
    }

    suspend fun deleteRecording(recording: RecordingItem) {
        recordingDao.deleteRecording(recording)
    }
}
