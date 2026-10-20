package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CourseSchedule
import com.example.data.RecordingItem
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RecordRed
import com.example.util.ScheduleHelper
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    viewModel: MainViewModel,
    onOpenTranscript: (RecordingItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val isRecording by viewModel.recorderEngine.isRecording.collectAsStateWithLifecycle()
    val isPaused by viewModel.recorderEngine.isPaused.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.recorderEngine.durationSeconds.collectAsStateWithLifecycle()
    val amplitude by viewModel.recorderEngine.currentAmplitude.collectAsStateWithLifecycle()
    val isTranscribing by viewModel.isTranscribing.collectAsStateWithLifecycle()
    val selectedSchedule by viewModel.selectedCourseSchedule.collectAsStateWithLifecycle()
    val customCourseName by viewModel.customCourseName.collectAsStateWithLifecycle()
    val sessionNumber by viewModel.sessionNumber.collectAsStateWithLifecycle()
    val partNumber by viewModel.partNumber.collectAsStateWithLifecycle()
    val lastRecording by viewModel.selectedRecording.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            viewModel.startRecording()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importAudioFile(it) { item ->
                onOpenTranscript(item)
            }
        }
    }

    val currentDayName = remember { ScheduleHelper.getCurrentDayName() }
    val todayIndex = remember { ScheduleHelper.getCurrentDayIndex() }
    val todaySchedules = remember(schedules) { schedules.filter { it.dayIndex == todayIndex } }

    val generatedTitle = viewModel.getCurrentGeneratedTitle()

    // Pulse animation for recording ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "کلاس‌یار هوشمند",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "برنامه امروز: $currentDayName",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "امروز",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentDayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule Course Quick Selection Chips
        if (!isRecording) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "انتخاب درس از برنامه هفتگی:",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Show today's courses first
                    items(todaySchedules) { schedule ->
                        val isSelected = selectedSchedule?.id == schedule.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCourseSchedule(schedule) },
                            label = {
                                Text(
                                    text = "${schedule.courseName} (${schedule.startTime})",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }

                    // Other courses if none or for flexibility
                    items(schedules.filter { it.dayIndex != todayIndex }) { schedule ->
                        val isSelected = selectedSchedule?.id == schedule.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCourseSchedule(schedule) },
                            label = {
                                Text("${schedule.dayOfWeek}: ${schedule.courseName}")
                            },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Generated Title Preview Card
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "نام‌گذاری هوشمند بر اساس برنامه کلاسی",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    if (!isRecording) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ویرایش مشخصات جلسه",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = generatedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("recording_title_preview")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BadgeChip(text = "درس: ${selectedSchedule?.courseName ?: customCourseName.ifBlank { "آزاد" }}")
                    BadgeChip(text = "جلسه $sessionNumber")
                    if (partNumber > 1) {
                        BadgeChip(text = "پارت $partNumber", isAccent = true)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer & Visualizer Section
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when {
                        isRecording && !isPaused -> RecordRed.copy(alpha = 0.15f)
                        isRecording && isPaused -> AmberAccent.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isRecording && !isPaused -> RecordRed
                                        isRecording && isPaused -> AmberAccent
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isRecording && !isPaused -> "در حال ضبط صدای استاد..."
                                isRecording && isPaused -> "ضبط متوقف موقت شد"
                                else -> "آماده برای ضبط کلاس"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isRecording && !isPaused -> RecordRed
                                    isRecording && isPaused -> AmberAccent
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Duration Display
                Text(
                    text = ScheduleHelper.formatDuration(durationSeconds),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (isRecording) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("record_timer_text")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Waveform Visualizer
                WaveformVisualizer(
                    amplitude = amplitude,
                    isRecording = isRecording,
                    isPaused = isPaused,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Main Record Action Button with pulse
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(110.dp)
        ) {
            if (isRecording && !isPaused) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(RecordRed.copy(alpha = 0.22f))
                )
            }

            Surface(
                onClick = {
                    if (isRecording) {
                        viewModel.stopAndProcessRecording { finishedItem ->
                            onOpenTranscript(finishedItem)
                        }
                    } else {
                        if (hasAudioPermission) {
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                shape = CircleShape,
                color = if (isRecording) RecordRed else MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(80.dp)
                    .testTag("record_main_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "پایان و تبدیل به متن" else "شروع ضبط",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Text(
            text = if (isRecording) "برای پایان و تبدیل به متن لمس کنید" else "برای شروع ضبط کلاس لمس کنید",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 10.dp)
        )

        if (!isRecording) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier.testTag("import_audio_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("یا وارد کردن فایل صوتی ضبط‌شده (MP3، M4A، WAV...)")
            }
        }

        // Secondary controls (Pause/Resume, Cancel) when recording
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                    },
                    modifier = Modifier.testTag("pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPaused) "ادامه ضبط" else "توقف موقت")
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedButton(
                    onClick = { viewModel.cancelRecording() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("انصراف")
                }
            }
        }

        // Active Transcription Progress Card
        if (isTranscribing) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "هوش مصنوعی در حال تبدیل صوت به متن...",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "پس از اتمام، متن پیاده‌شده جهت خروجی PDF آماده خواهد شد.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Last recording quick access button
        if (lastRecording != null && !isRecording && !isTranscribing) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = { onOpenTranscript(lastRecording!!) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("مشاهده متن آخرین ضبط و دریافت PDF")
            }
        }
    }

    // Dialog for Editing Title, Session & Part numbers
    if (showEditDialog) {
        var tempCourseName by remember { mutableStateOf(selectedSchedule?.courseName ?: customCourseName) }
        var tempSession by remember { mutableStateOf(sessionNumber.toString()) }
        var tempPart by remember { mutableStateOf(partNumber.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("تنظیم اطلاعات کلاس و جلسه", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempCourseName,
                        onValueChange = { tempCourseName = it },
                        label = { Text("نام درس") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tempSession,
                            onValueChange = { tempSession = it.filter { ch -> ch.isDigit() } },
                            label = { Text("شماره جلسه") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = tempPart,
                            onValueChange = { tempPart = it.filter { ch -> ch.isDigit() } },
                            label = { Text("شماره پارت") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Text(
                        text = "پیش‌نمایش: ${ScheduleHelper.generateRecordingTitle(currentDayName, tempCourseName, tempSession.toIntOrNull() ?: 1, tempPart.toIntOrNull() ?: 1)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomCourseName(tempCourseName)
                        viewModel.setSessionNumber(tempSession.toIntOrNull() ?: 1)
                        viewModel.setPartNumber(tempPart.toIntOrNull() ?: 1)
                        showEditDialog = false
                    }
                ) {
                    Text("اعمال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun BadgeChip(text: String, isAccent: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isAccent) AmberAccent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (isAccent) AmberAccent else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
