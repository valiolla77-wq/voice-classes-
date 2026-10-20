package com.example

import com.example.util.ScheduleHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTitleGeneration_basic() {
        val title = ScheduleHelper.generateRecordingTitle(
            dayOfWeek = "شنبه",
            courseName = "تاریخ",
            sessionNumber = 1,
            partNumber = 1
        )
        assertEquals("شنبه - تاریخ - جلسه ۱", title)
    }

    @Test
    fun testTitleGeneration_withPart() {
        val title = ScheduleHelper.generateRecordingTitle(
            dayOfWeek = "شنبه",
            courseName = "تاریخ",
            sessionNumber = 1,
            partNumber = 2
        )
        assertEquals("شنبه - تاریخ - جلسه ۱ - پارت ۲", title)
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("00:00", ScheduleHelper.formatDuration(0))
        assertEquals("01:05", ScheduleHelper.formatDuration(65))
        assertEquals("10:00", ScheduleHelper.formatDuration(600))
    }

    @Test
    fun testPersianDigitsConversion() {
        assertEquals("۱۲۳", ScheduleHelper.toPersianDigits("123"))
        assertEquals("جلسه ۴", "جلسه " + ScheduleHelper.toPersianDigits("4"))
    }
}
