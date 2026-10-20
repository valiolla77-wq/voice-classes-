package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerEngine
import com.example.audio.AudioRecorderEngine
import com.example.data.AppDatabase
import com.example.data.ClassVoiceRepository
import com.example.data.CourseSchedule
import com.example.data.RecordingItem
import com.example.network.CloudflareRelayClient
import com.example.pdf.PdfExportEngine
import com.example.util.ScheduleHelper
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ClassVoiceRepository(database.courseScheduleDao(), database.recordingDao())

    val recorderEngine = AudioRecorderEngine(application)
    val playerEngine = AudioPlayerEngine()
    val relayClient = CloudflareRelayClient(application)

    // Schedules and Recordings reactive streams
    val schedules: StateFlow<List<CourseSchedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recordings: StateFlow<List<RecordingItem>> = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active auto-naming selections
    private val _selectedCourseSchedule = MutableStateFlow<CourseSchedule?>(null)
    val selectedCourseSchedule: StateFlow<CourseSchedule?> = _selectedCourseSchedule.asStateFlow()

    private val _customCourseName = MutableStateFlow("")
    val customCourseName: StateFlow<String> = _customCourseName.asStateFlow()

    private val _sessionNumber = MutableStateFlow(1)
    val sessionNumber: StateFlow<Int> = _sessionNumber.asStateFlow()

    private val _partNumber = MutableStateFlow(1)
    val partNumber: StateFlow<Int> = _partNumber.asStateFlow()

    private val _customTitleOverride = MutableStateFlow("")
    val customTitleOverride: StateFlow<String> = _customTitleOverride.asStateFlow()

    // Status / Messages
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Currently selected recording for detail modal / bottom sheet
    private val _selectedRecording = MutableStateFlow<RecordingItem?>(null)
    val selectedRecording: StateFlow<RecordingItem?> = _selectedRecording.asStateFlow()

    init {
        // When schedules load, pick the active one if none is selected yet
        viewModelScope.launch {
            schedules.collect { list ->
                if (_selectedCourseSchedule.value == null && list.isNotEmpty()) {
                    val active = ScheduleHelper.findActiveSchedule(list) ?: list.firstOrNull()
                    if (active != null) {
                        selectCourseSchedule(active)
                    }
                }
            }
        }
    }

    fun selectCourseSchedule(schedule: CourseSchedule) {
        _selectedCourseSchedule.value = schedule
        _customCourseName.value = schedule.courseName
        _sessionNumber.value = schedule.currentSession
        _customTitleOverride.value = ""
        checkExistingParts(schedule.courseName, schedule.currentSession)
    }

    fun setCustomCourseName(name: String) {
        _customCourseName.value = name
        _selectedCourseSchedule.value = null
        checkExistingParts(name, _sessionNumber.value)
    }

    fun setSessionNumber(number: Int) {
        _sessionNumber.value = number.coerceAtLeast(1)
        val course = _selectedCourseSchedule.value?.courseName ?: _customCourseName.value
        checkExistingParts(course, _sessionNumber.value)
    }

    fun setPartNumber(part: Int) {
        _partNumber.value = part.coerceAtLeast(1)
    }

    fun setCustomTitleOverride(title: String) {
        _customTitleOverride.value = title
    }

    private fun checkExistingParts(courseName: String, session: Int) {
        if (courseName.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getRecordingsForSession(courseName.trim(), session)
            _partNumber.value = existing.size + 1
        }
    }

    fun getCurrentGeneratedTitle(): String {
        if (_customTitleOverride.value.isNotBlank()) {
            return _customTitleOverride.value
        }
        val day = ScheduleHelper.getCurrentDayName()
        val course = _selectedCourseSchedule.value?.courseName ?: _customCourseName.value.ifBlank { "کلاس آزاد" }
        return ScheduleHelper.generateRecordingTitle(day, course, _sessionNumber.value, _partNumber.value)
    }

    fun startRecording(): Boolean {
        playerEngine.stopAudio()
        val file = recorderEngine.startRecording()
        return file != null
    }

    fun pauseRecording() = recorderEngine.pauseRecording()
    fun resumeRecording() = recorderEngine.resumeRecording()
    fun cancelRecording() = recorderEngine.cancelRecording()

    fun stopAndProcessRecording(onFinished: (RecordingItem) -> Unit = {}) {
        val duration = recorderEngine.durationSeconds.value
        val audioFile = recorderEngine.stopRecording()

        if (audioFile == null) {
            Toast.makeText(getApplication(), "فایل صوتی ذخیره نشد یا زمان ضبط بسیار کوتاه بود.", Toast.LENGTH_SHORT).show()
            return
        }

        val title = getCurrentGeneratedTitle()
        val course = _selectedCourseSchedule.value?.courseName ?: _customCourseName.value.ifBlank { "کلاس آزاد" }
        val day = ScheduleHelper.getCurrentDayName()
        val session = _sessionNumber.value
        val part = _partNumber.value

        viewModelScope.launch {
            val recordingItem = RecordingItem(
                title = title,
                courseName = course,
                sessionNumber = session,
                partNumber = part,
                dayOfWeek = day,
                audioFilePath = audioFile.absolutePath,
                durationSeconds = duration,
                status = RecordingItem.STATUS_TRANSCRIBING
            )

            val newId = repository.insertRecording(recordingItem)
            val insertedItem = recordingItem.copy(id = newId)
            _selectedRecording.value = insertedItem
            onFinished(insertedItem)

            // Start Transcription in background
            transcribeRecordingItem(insertedItem)

            // Increment schedule currentSession if this is a completed full session (or update schedule)
            _selectedCourseSchedule.value?.let { schedule ->
                repository.incrementScheduleSession(schedule.id)
            }
            _partNumber.value += 1
        }
    }

    fun importAudioFile(uri: Uri, onFinished: (RecordingItem) -> Unit = {}) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var fileName = "import_${System.currentTimeMillis()}.mp3"
                try {
                    app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                val name = cursor.getString(nameIndex)
                                if (!name.isNullOrBlank()) {
                                    fileName = name
                                }
                            }
                        }
                    }
                } catch (t: Throwable) {
                    // Ignore cursor query error and keep default fileName
                }

                val recordingsDir = File(app.filesDir, "recordings")
                if (!recordingsDir.exists()) recordingsDir.mkdirs()

                val cleanFileName = "imp_${System.currentTimeMillis()}_${fileName.replace(" ", "_")}"
                val targetFile = File(recordingsDir, cleanFileName)

                val inputStream = app.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(app, "امکان خواندن فایل صوتی وجود ندارد.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                var durationSeconds = 0L
                try {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(targetFile.absolutePath)
                        val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durMs = durStr?.toLongOrNull() ?: 0L
                        durationSeconds = durMs / 1000
                    } finally {
                        try {
                            retriever.release()
                        } catch (t: Throwable) {
                            // ignore release error
                        }
                    }
                } catch (t: Throwable) {
                    durationSeconds = 0L
                }

                val title = getCurrentGeneratedTitle()
                val course = _selectedCourseSchedule.value?.courseName ?: _customCourseName.value.ifBlank { "فایل وارد شده" }
                val day = ScheduleHelper.getCurrentDayName()
                val session = _sessionNumber.value
                val part = _partNumber.value

                val recordingItem = RecordingItem(
                    title = title,
                    courseName = course,
                    sessionNumber = session,
                    partNumber = part,
                    dayOfWeek = day,
                    audioFilePath = targetFile.absolutePath,
                    durationSeconds = durationSeconds,
                    status = RecordingItem.STATUS_TRANSCRIBING
                )

                val newId = repository.insertRecording(recordingItem)
                val insertedItem = recordingItem.copy(id = newId)
                _selectedRecording.value = insertedItem

                withContext(Dispatchers.Main) {
                    onFinished(insertedItem)
                    Toast.makeText(app, "فایل صوتی اضافه شد و در حال تبدیل مستقیم است.", Toast.LENGTH_SHORT).show()
                }

                // Start Transcription
                transcribeRecordingItem(insertedItem)

                _partNumber.value += 1
            } catch (t: Throwable) {
                t.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(app, "خطا در وارد کردن فایل: ${t.localizedMessage ?: "عدم دسترسی به فایل"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun transcribeRecordingItem(item: RecordingItem) {
        viewModelScope.launch {
            _isTranscribing.value = true
            repository.updateTranscription(item.id, "", RecordingItem.STATUS_TRANSCRIBING)

            val audioFile = File(item.audioFilePath)
            val result = relayClient.transcribeAudio(
                audioFile = audioFile,
                courseName = item.courseName,
                sessionNumber = item.sessionNumber
            )

            result.onSuccess { transcript ->
                val finalTranscript = transcript.ifBlank { "متنی در صوت تشخیص داده نشد." }
                repository.updateTranscription(item.id, finalTranscript, RecordingItem.STATUS_COMPLETED)

                // Update selected recording if it's the one being viewed
                if (_selectedRecording.value?.id == item.id) {
                    _selectedRecording.value = item.copy(
                        transcription = finalTranscript,
                        status = RecordingItem.STATUS_COMPLETED
                    )
                }
                _isTranscribing.value = false
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "تبدیل صوت به متن با موفقیت انجام شد.", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                val errorMsg = "خطا در پیاده‌سازی صوت: ${error.localizedMessage ?: "ارتباط برقرار نشد"}"
                repository.updateTranscription(item.id, errorMsg, RecordingItem.STATUS_FAILED)

                if (_selectedRecording.value?.id == item.id) {
                    _selectedRecording.value = item.copy(
                        transcription = errorMsg,
                        status = RecordingItem.STATUS_FAILED
                    )
                }
                _isTranscribing.value = false
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "خطا در تبدیل صوت. لطفاً مجدداً تلاش کنید.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun setSelectedRecording(recording: RecordingItem?) {
        _selectedRecording.value = recording
    }

    fun updateRecordingText(item: RecordingItem, newText: String) {
        viewModelScope.launch {
            val updated = item.copy(transcription = newText)
            repository.updateRecording(updated)
            _selectedRecording.value = updated
        }
    }

    fun deleteRecording(item: RecordingItem) {
        viewModelScope.launch {
            playerEngine.stopAudio()
            try {
                File(item.audioFilePath).delete()
                item.pdfFilePath?.let { File(it).delete() }
            } catch (e: Exception) {
                // Ignore file delete errors
            }
            repository.deleteRecording(item)
            if (_selectedRecording.value?.id == item.id) {
                _selectedRecording.value = null
            }
        }
    }

    fun exportAndSharePdf(context: Context, item: RecordingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val pdfFile = PdfExportEngine.generatePdf(context, item)
            if (pdfFile != null && pdfFile.exists()) {
                repository.updatePdfPath(item.id, pdfFile.absolutePath)
                withContext(Dispatchers.Main) {
                    val shareIntent = PdfExportEngine.createShareIntent(context, pdfFile)
                    context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری فایل PDF"))
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "خطا در تولید فایل PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun copyTranscriptionToClipboard(context: Context, text: String) {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ClassVoice Transcription", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "متن در حافظه کپی شد.", Toast.LENGTH_SHORT).show()
    }

    // Schedule Management
    fun saveSchedule(schedule: CourseSchedule) {
        viewModelScope.launch {
            if (schedule.id == 0L) {
                repository.insertSchedule(schedule)
            } else {
                repository.updateSchedule(schedule)
            }
        }
    }

    fun deleteSchedule(schedule: CourseSchedule) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
            if (_selectedCourseSchedule.value?.id == schedule.id) {
                _selectedCourseSchedule.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderEngine.stopRecording()
        playerEngine.stopAudio()
    }
}
