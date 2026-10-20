package com.example.util

import com.example.data.CourseSchedule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ScheduleHelper {
    val PERSIAN_DAYS = listOf(
        "شنبه",
        "یکشنبه",
        "دوشنبه",
        "سه‌شنبه",
        "چهارشنبه",
        "پنج‌شنبه",
        "جمعه"
    )

    fun getCurrentDayIndex(calendar: Calendar = Calendar.getInstance()): Int {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    fun getCurrentDayName(calendar: Calendar = Calendar.getInstance()): String {
        return PERSIAN_DAYS[getCurrentDayIndex(calendar)]
    }

    /**
     * Checks if current time is within [startTime, endTime] string format "HH:mm"
     */
    fun isCurrentTimeWithin(startTime: String, endTime: String, calendar: Calendar = Calendar.getInstance()): Boolean {
        return try {
            val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            val startParts = startTime.split(":").map { it.trim().toInt() }
            val startMinutes = startParts[0] * 60 + startParts[1]

            val endParts = endTime.split(":").map { it.trim().toInt() }
            val endMinutes = endParts[0] * 60 + endParts[1]

            // Give a 15-minute buffer before start
            currentMinutes in (startMinutes - 15)..endMinutes
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Finds the most relevant course schedule for current day and time
     */
    fun findActiveSchedule(schedules: List<CourseSchedule>, calendar: Calendar = Calendar.getInstance()): CourseSchedule? {
        val todayIndex = getCurrentDayIndex(calendar)
        val todaySchedules = schedules.filter { it.dayIndex == todayIndex }

        // First check exact time match
        val activeNow = todaySchedules.firstOrNull { isCurrentTimeWithin(it.startTime, it.endTime, calendar) }
        if (activeNow != null) return activeNow

        // Otherwise return the first schedule for today if exists
        return todaySchedules.firstOrNull()
    }

    fun toPersianDigits(input: String): String {
        val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        return buildString {
            for (ch in input) {
                if (ch in '0'..'9') append(persian[ch - '0']) else append(ch)
            }
        }
    }

    /**
     * Generates standard title: e.g. "شنبه - تاریخ - جلسه ۱" or "شنبه - تاریخ - جلسه ۱ - پارت ۲"
     */
    fun generateRecordingTitle(
        dayOfWeek: String,
        courseName: String,
        sessionNumber: Int,
        partNumber: Int? = null
    ): String {
        val cleanCourse = courseName.trim().ifEmpty { "کلاس عمومی" }
        val cleanDay = dayOfWeek.trim().ifEmpty { getCurrentDayName() }
        val sessionStr = toPersianDigits(sessionNumber.toString())

        return if (partNumber != null && partNumber > 1) {
            val partStr = toPersianDigits(partNumber.toString())
            "$cleanDay - $cleanCourse - جلسه $sessionStr - پارت $partStr"
        } else {
            "$cleanDay - $cleanCourse - جلسه $sessionStr"
        }
    }

    fun formatDuration(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
