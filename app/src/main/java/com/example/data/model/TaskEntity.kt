package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val syncUuid: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dueTimestamp: Long, // timestamp in ms for scheduled time
    val dueDateString: String, // e.g. "2026-08-19"
    val dueTimeString: String, // e.g. "09:30 AM"
    val priority: TaskPriority = TaskPriority.P3_MEDIUM,
    val projectId: Long = 1,
    val projectName: String = "General",
    val projectColorHex: String = "#6366F1",
    val isAlarmEnabled: Boolean = false, // Alarm clock urgency
    val alarmSound: String = AlarmSoundType.ZEN_CHIME.id,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val customIntervalDays: Int = 1,
    val snoozeCount: Int = 0,
    val lastSnoozedAt: Long? = null,
    val isSnoozed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
