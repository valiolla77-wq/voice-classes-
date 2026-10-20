package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CourseSchedule
import com.example.util.ScheduleHelper
import com.example.viewmodel.MainViewModel

@Composable
fun ScheduleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    var selectedDayIndex by remember { mutableIntStateOf(ScheduleHelper.getCurrentDayIndex()) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<CourseSchedule?>(null) }
    var scheduleToDelete by remember { mutableStateOf<CourseSchedule?>(null) }

    val days = ScheduleHelper.PERSIAN_DAYS
    val filteredSchedules = remember(schedules, selectedDayIndex) {
        schedules.filter { it.dayIndex == selectedDayIndex }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSchedule = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(bottom = 70.dp)
                    .testTag("add_schedule_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "افزودن درس جدید"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "برنامه کلاسی هفتگی",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "کلاس‌های شما در این بخش برای نام‌گذاری خودکار ضبط‌ها استفاده می‌شوند.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Day Selector Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(days.indices.toList()) { index ->
                    val isSelected = selectedDayIndex == index
                    val isToday = index == ScheduleHelper.getCurrentDayIndex()
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDayIndex = index },
                        label = {
                            Text(
                                text = if (isToday) "${days[index]} (امروز)" else days[index],
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredSchedules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "برای روز «${days[selectedDayIndex]}» کلاسی ثبت نشده است.",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "با دکمه + می‌توانید درس‌های این روز را اضافه کنید.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSchedules, key = { it.id }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            onIncrementSession = {
                                viewModel.saveSchedule(schedule.copy(currentSession = schedule.currentSession + 1))
                            },
                            onEdit = {
                                editingSchedule = schedule
                                showAddEditDialog = true
                            },
                            onDelete = { scheduleToDelete = schedule }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Schedule Dialog
    if (showAddEditDialog) {
        AddEditScheduleDialog(
            initialSchedule = editingSchedule,
            initialDayIndex = selectedDayIndex,
            onDismiss = { showAddEditDialog = false },
            onSave = { newSchedule ->
                viewModel.saveSchedule(newSchedule)
                showAddEditDialog = false
            }
        )
    }

    // Delete Schedule Confirmation
    scheduleToDelete?.let { schedule ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("حذف درس از برنامه", fontWeight = FontWeight.Bold) },
            text = { Text("آیا مطمئن هستید که می‌خواهید درس «${schedule.courseName}» را از برنامه روز ${schedule.dayOfWeek} حذف کنید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSchedule(schedule)
                        scheduleToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun ScheduleCard(
    schedule: CourseSchedule,
    onIncrementSession: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = schedule.courseName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "جلسه فعلی: ${schedule.currentSession}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${schedule.startTime} تا ${schedule.endTime}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (schedule.professor.isNotBlank()) {
                    Text(
                        text = "استاد: ${schedule.professor}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (schedule.classroom.isNotBlank()) {
                    Text(
                        text = schedule.classroom,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onIncrementSession,
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlusOne, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("جلسه بعد (+۱)", style = MaterialTheme.typography.labelSmall)
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditScheduleDialog(
    initialSchedule: CourseSchedule?,
    initialDayIndex: Int,
    onDismiss: () -> Unit,
    onSave: (CourseSchedule) -> Unit
) {
    val days = ScheduleHelper.PERSIAN_DAYS
    var selectedDayIndex by remember { mutableIntStateOf(initialSchedule?.dayIndex ?: initialDayIndex) }
    var courseName by remember { mutableStateOf(initialSchedule?.courseName ?: "") }
    var startTime by remember { mutableStateOf(initialSchedule?.startTime ?: "08:00") }
    var endTime by remember { mutableStateOf(initialSchedule?.endTime ?: "10:00") }
    var sessionNumber by remember { mutableStateOf((initialSchedule?.currentSession ?: 1).toString()) }
    var professor by remember { mutableStateOf(initialSchedule?.professor ?: "") }
    var classroom by remember { mutableStateOf(initialSchedule?.classroom ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSchedule == null) "افزودن کلاس جدید" else "ویرایش کلاس",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Day selector
                Text("روز هفته:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(days.indices.toList()) { idx ->
                        FilterChip(
                            selected = selectedDayIndex == idx,
                            onClick = { selectedDayIndex = idx },
                            label = { Text(days[idx], style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("نام درس (الزامی)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("ساعت شروع") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("ساعت پایان") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sessionNumber,
                        onValueChange = { sessionNumber = it.filter { c -> c.isDigit() } },
                        label = { Text("شماره جلسه") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("کلاس / اتاق") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = professor,
                    onValueChange = { professor = it },
                    label = { Text("نام استاد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseName.isNotBlank()) {
                        val schedule = CourseSchedule(
                            id = initialSchedule?.id ?: 0L,
                            dayOfWeek = days[selectedDayIndex],
                            dayIndex = selectedDayIndex,
                            courseName = courseName.trim(),
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            currentSession = sessionNumber.toIntOrNull() ?: 1,
                            professor = professor.trim(),
                            classroom = classroom.trim()
                        )
                        onSave(schedule)
                    }
                },
                enabled = courseName.isNotBlank()
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
