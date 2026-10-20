package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecorderEngine(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var tickerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    // Normalized amplitude 0.0 to 1.0 for visualizer
    private val _currentAmplitude = MutableStateFlow(0.0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    fun startRecording(): File? {
        stopRecording() // Clean up any lingering session

        val recordingsDir = File(context.filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }

        val outputFile = File(recordingsDir, "rec_${System.currentTimeMillis()}.m4a")
        currentFile = outputFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _isPaused.value = false
            _durationSeconds.value = 0L

            startTicker()
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorderEngine", "Failed to start recording", e)
            cleanup()
            return null
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _isRecording.value && !_isPaused.value) {
            try {
                mediaRecorder?.pause()
                _isPaused.value = true
            } catch (e: Exception) {
                Log.e("AudioRecorderEngine", "Failed to pause recording", e)
            }
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && _isRecording.value && _isPaused.value) {
            try {
                mediaRecorder?.resume()
                _isPaused.value = false
            } catch (e: Exception) {
                Log.e("AudioRecorderEngine", "Failed to resume recording", e)
            }
        }
    }

    fun stopRecording(): File? {
        val file = currentFile
        cleanup()
        return if (file != null && file.exists() && file.length() > 0) file else null
    }

    fun cancelRecording() {
        val file = currentFile
        cleanup()
        file?.delete()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var counter = 0
            while (isActive && _isRecording.value) {
                delay(100)
                if (!_isPaused.value) {
                    counter++
                    if (counter % 10 == 0) {
                        _durationSeconds.value += 1
                    }

                    // Poll max amplitude
                    try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        // Normalize 0..32767 to 0f..1f
                        val norm = (maxAmp / 32767f).coerceIn(0f, 1f)
                        _currentAmplitude.value = norm
                    } catch (e: Exception) {
                        _currentAmplitude.value = 0f
                    }
                }
            }
        }
    }

    private fun cleanup() {
        tickerJob?.cancel()
        tickerJob = null
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    // Ignore stop error if immediately cancelled
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderEngine", "Error cleaning up recorder", e)
        }
        mediaRecorder = null
        _isRecording.value = false
        _isPaused.value = false
        _currentAmplitude.value = 0f
    }
}

class AudioPlayerEngine {
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _totalDuration = MutableStateFlow(0)
    val totalDuration: StateFlow<Int> = _totalDuration.asStateFlow()

    fun playAudio(filePath: String, onCompletion: () -> Unit = {}) {
        stopAudio()
        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = 0
                    progressJob?.cancel()
                    onCompletion()
                }
            }
            mediaPlayer = player
            _totalDuration.value = player.duration
            _isPlaying.value = true

            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayerEngine", "Error playing audio file: $filePath", e)
            stopAudio()
        }
    }

    fun pauseAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerEngine", "Error pausing audio", e)
        }
    }

    fun resumeAudio() {
        try {
            mediaPlayer?.let {
                it.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerEngine", "Error resuming audio", e)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _currentPosition.value = positionMs
        } catch (e: Exception) {
            Log.e("AudioPlayerEngine", "Error seeking", e)
        }
    }

    fun stopAudio() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerEngine", "Error releasing player", e)
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    mediaPlayer?.let {
                        _currentPosition.value = it.currentPosition
                    }
                } catch (e: Exception) {
                    break
                }
                delay(200)
            }
        }
    }
}
