package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CourseSchedule::class, RecordingItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseScheduleDao(): CourseScheduleDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "class_voice_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialSchedules(database.courseScheduleDao())
                    }
                }
            }
        }

        private suspend fun populateInitialSchedules(dao: CourseScheduleDao) {
            val initialList = listOf(
                CourseSchedule(
                    dayOfWeek = "شنبه",
                    dayIndex = 0,
                    courseName = "تاریخ تمدن اسلامی",
                    startTime = "08:00",
                    endTime = "10:00",
                    currentSession = 1,
                    professor = "دکتر جلالی",
                    classroom = "کلاس ۳۰۱",
                    colorHex = "#4F46E5"
                ),
                CourseSchedule(
                    dayOfWeek = "شنبه",
                    dayIndex = 0,
                    courseName = "ریاضی عمومی ۱",
                    startTime = "10:30",
                    endTime = "12:30",
                    currentSession = 2,
                    professor = "دکتر حسینی",
                    classroom = "تالار خوارزمی",
                    colorHex = "#0EA5E9"
                ),
                CourseSchedule(
                    dayOfWeek = "یکشنبه",
                    dayIndex = 1,
                    courseName = "فیزیک پایه",
                    startTime = "13:30",
                    endTime = "15:30",
                    currentSession = 1,
                    professor = "دکتر رستمی",
                    classroom = "کلاس ۲۰۴",
                    colorHex = "#10B981"
                ),
                CourseSchedule(
                    dayOfWeek = "دوشنبه",
                    dayIndex = 2,
                    courseName = "مبانی کامپیوتر و برنامه‌نویسی",
                    startTime = "09:00",
                    endTime = "11:00",
                    currentSession = 3,
                    professor = "مهندس محمدی",
                    classroom = "سایت کامپیوتر ۱",
                    colorHex = "#8B5CF6"
                ),
                CourseSchedule(
                    dayOfWeek = "سه‌شنبه",
                    dayIndex = 3,
                    courseName = "زبان عمومی",
                    startTime = "14:00",
                    endTime = "16:00",
                    currentSession = 1,
                    professor = "استاد کمالی",
                    classroom = "کلاس ۱۰۵",
                    colorHex = "#F59E0B"
                ),
                CourseSchedule(
                    dayOfWeek = "چهارشنبه",
                    dayIndex = 4,
                    courseName = "آمار و احتمالات مهندسی",
                    startTime = "10:00",
                    endTime = "12:00",
                    currentSession = 2,
                    professor = "دکتر اکبری",
                    classroom = "کلاس ۳۰۵",
                    colorHex = "#EC4899"
                )
            )
            for (schedule in initialList) {
                dao.insertSchedule(schedule)
            }
        }
    }
}
