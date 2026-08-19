package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AlarmSoundType
import com.example.data.model.ProjectEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(entities = [TaskEntity::class, ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chronotask_database"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.projectDao(), database.taskDao())
                }
            }
        }

        private suspend fun populateInitialData(projectDao: ProjectDao, taskDao: TaskDao) {
            val projects = listOf(
                ProjectEntity(id = 1, name = "General & Inbox", colorHex = "#6366F1", iconName = "Inbox", isDefault = true),
                ProjectEntity(id = 2, name = "Work & Team", colorHex = "#3B82F6", iconName = "Work", isDefault = false),
                ProjectEntity(id = 3, name = "Urgent & Priority", colorHex = "#EF4444", iconName = "Warning", isDefault = false),
                ProjectEntity(id = 4, name = "Personal & Family", colorHex = "#10B981", iconName = "Home", isDefault = false),
                ProjectEntity(id = 5, name = "Fitness & Health", colorHex = "#F59E0B", iconName = "FitnessCenter", isDefault = false),
                ProjectEntity(id = 6, name = "Side Projects", colorHex = "#EC4899", iconName = "RocketLaunch", isDefault = false)
            )
            projectDao.insertAll(projects)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

            val cal = Calendar.getInstance()
            
            // Task 1: Today 09:00 AM - Urgent standup
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val todayDateStr = dateFormat.format(cal.time)
            
            val task1 = TaskEntity(
                title = "Sprint Planning & Roadmap Review",
                description = "Review Q3 milestone deliverables and update blocker status with engineering team.",
                dueTimestamp = cal.timeInMillis,
                dueDateString = todayDateStr,
                dueTimeString = "09:00 AM",
                priority = TaskPriority.P1_URGENT,
                projectId = 2,
                projectName = "Work & Team",
                projectColorHex = "#3B82F6",
                isAlarmEnabled = true,
                alarmSound = AlarmSoundType.URGENT_RADAR.id,
                recurrence = RecurrenceType.WEEKDAYS
            )

            // Task 2: Today 02:30 PM - High Priority
            cal.set(Calendar.HOUR_OF_DAY, 14)
            cal.set(Calendar.MINUTE, 30)
            val task2 = TaskEntity(
                title = "Client Demo: P2P Encrypted Sync Feature",
                description = "Demonstrate zero-cloud offline peer-to-peer data sync and AES-256-GCM verification.",
                dueTimestamp = cal.timeInMillis,
                dueDateString = todayDateStr,
                dueTimeString = "02:30 PM",
                priority = TaskPriority.P2_HIGH,
                projectId = 6,
                projectName = "Side Projects",
                projectColorHex = "#EC4899",
                isAlarmEnabled = true,
                alarmSound = AlarmSoundType.CYBER_BEACON.id,
                recurrence = RecurrenceType.NONE
            )

            // Task 3: Today 06:00 PM - Health
            cal.set(Calendar.HOUR_OF_DAY, 18)
            cal.set(Calendar.MINUTE, 0)
            val task3 = TaskEntity(
                title = "Evening Cardio & Hydration Check",
                description = "45-minute zone 2 running session + core workout.",
                dueTimestamp = cal.timeInMillis,
                dueDateString = todayDateStr,
                dueTimeString = "06:00 PM",
                priority = TaskPriority.P3_MEDIUM,
                projectId = 5,
                projectName = "Fitness & Health",
                projectColorHex = "#F59E0B",
                isAlarmEnabled = false,
                alarmSound = AlarmSoundType.ZEN_CHIME.id,
                recurrence = RecurrenceType.DAILY
            )

            // Task 4: Tomorrow 10:00 AM
            val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0) }
            val tomorrowDateStr = dateFormat.format(tomorrowCal.time)
            val task4 = TaskEntity(
                title = "Review Security Audit & Passphrase Rotation",
                description = "Ensure end-to-end encryption keys and PBKDF2 iterations meet modern security standards.",
                dueTimestamp = tomorrowCal.timeInMillis,
                dueDateString = tomorrowDateStr,
                dueTimeString = "10:00 AM",
                priority = TaskPriority.P1_URGENT,
                projectId = 3,
                projectName = "Urgent & Priority",
                projectColorHex = "#EF4444",
                isAlarmEnabled = true,
                alarmSound = AlarmSoundType.CLASSIC_BELL.id,
                recurrence = RecurrenceType.NONE
            )

            // Task 5: 3 Days Later - Personal
            val laterCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3); set(Calendar.HOUR_OF_DAY, 11); set(Calendar.MINUTE, 30) }
            val laterDateStr = dateFormat.format(laterCal.time)
            val task5 = TaskEntity(
                title = "Weekly Grocery & Meal Prep",
                description = "Pick up fresh organic produce, hydration electrolytes, and meal containers.",
                dueTimestamp = laterCal.timeInMillis,
                dueDateString = laterDateStr,
                dueTimeString = "11:30 AM",
                priority = TaskPriority.P4_NORMAL,
                projectId = 4,
                projectName = "Personal & Family",
                projectColorHex = "#10B981",
                isAlarmEnabled = false,
                alarmSound = AlarmSoundType.DAWN_HARMONY.id,
                recurrence = RecurrenceType.WEEKLY
            )

            taskDao.insertAll(listOf(task1, task2, task3, task4, task5))
        }
    }
}
