package com.example.data.repository

import com.example.data.db.ProjectDao
import com.example.data.db.TaskDao
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectSyncDto
import com.example.data.model.RecurrenceType
import com.example.data.model.SyncPayloadData
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskSyncDto
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskRepository(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao
) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasksFlow()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()

    fun getTasksForDate(dateString: String): Flow<List<TaskEntity>> =
        taskDao.getTasksForDateFlow(dateString)

    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?> =
        taskDao.getTaskByIdFlow(id)

    suspend fun getTaskById(id: Long): TaskEntity? =
        taskDao.getTaskById(id)

    suspend fun getUpcomingAlarmTasks(nowMs: Long = System.currentTimeMillis()): List<TaskEntity> =
        taskDao.getUpcomingAlarmTasks(nowMs)

    suspend fun insertTask(task: TaskEntity): Long {
        val now = System.currentTimeMillis()
        return taskDao.insertTask(task.copy(updatedAt = now))
    }

    suspend fun updateTask(task: TaskEntity) {
        val now = System.currentTimeMillis()
        taskDao.updateTask(task.copy(updatedAt = now))
    }

    suspend fun completeTask(task: TaskEntity, isCompleted: Boolean) {
        val now = System.currentTimeMillis()
        if (isCompleted && task.recurrence != RecurrenceType.NONE) {
            // Handle recurrence: advance the task to the next scheduled date
            val nextTimestamp = task.recurrence.calculateNextTimestamp(task.dueTimestamp, task.customIntervalDays)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val nextDate = Date(nextTimestamp)
            
            // Mark current completed and generate next recurrence task or advance it
            val updatedTask = task.copy(
                dueTimestamp = nextTimestamp,
                dueDateString = dateFormat.format(nextDate),
                dueTimeString = timeFormat.format(nextDate),
                isCompleted = false,
                completedAt = null,
                isSnoozed = false,
                snoozeCount = 0,
                updatedAt = now
            )
            taskDao.updateTask(updatedTask)
        } else {
            taskDao.setTaskCompleted(
                id = task.id,
                isCompleted = isCompleted,
                completedAt = if (isCompleted) now else null,
                updatedAt = now
            )
        }
    }

    suspend fun snoozeTask(taskId: Long, snoozeMinutes: Int) {
        val now = System.currentTimeMillis()
        val newDueTimestamp = now + (snoozeMinutes * 60 * 1000L)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val newDate = Date(newDueTimestamp)

        val existingTask = taskDao.getTaskById(taskId)
        if (existingTask != null) {
            val snoozed = existingTask.copy(
                dueTimestamp = newDueTimestamp,
                dueDateString = dateFormat.format(newDate),
                dueTimeString = timeFormat.format(newDate),
                isSnoozed = true,
                snoozeCount = existingTask.snoozeCount + 1,
                lastSnoozedAt = now,
                updatedAt = now
            )
            taskDao.updateTask(snoozed)
        }
    }

    suspend fun deleteTask(id: Long) {
        taskDao.softDeleteTask(id, System.currentTimeMillis())
    }

    suspend fun insertProject(project: ProjectEntity): Long {
        return projectDao.insertProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(id: Long) {
        projectDao.softDeleteProject(id, System.currentTimeMillis())
    }

    suspend fun exportSyncPayload(deviceName: String): SyncPayloadData {
        val tasks = taskDao.getAllTasksForSync().map { task ->
            TaskSyncDto(
                syncUuid = task.syncUuid,
                title = task.title,
                description = task.description,
                dueTimestamp = task.dueTimestamp,
                dueDateString = task.dueDateString,
                dueTimeString = task.dueTimeString,
                priorityLevel = task.priority.level,
                projectName = task.projectName,
                projectColorHex = task.projectColorHex,
                isAlarmEnabled = task.isAlarmEnabled,
                alarmSound = task.alarmSound,
                isCompleted = task.isCompleted,
                recurrenceName = task.recurrence.name,
                customIntervalDays = task.customIntervalDays,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
                isDeleted = task.isDeleted
            )
        }

        val projects = projectDao.getAllProjectsForSync().map { proj ->
            ProjectSyncDto(
                syncUuid = proj.syncUuid,
                name = proj.name,
                colorHex = proj.colorHex,
                iconName = proj.iconName,
                isDefault = proj.isDefault,
                updatedAt = proj.updatedAt,
                isDeleted = proj.isDeleted
            )
        }

        return SyncPayloadData(
            deviceName = deviceName,
            timestamp = System.currentTimeMillis(),
            tasks = tasks,
            projects = projects
        )
    }

    suspend fun mergeSyncPayload(payload: SyncPayloadData): Int {
        var mergedCount = 0

        // Merge projects first
        for (remoteProj in payload.projects) {
            val localProj = projectDao.getProjectBySyncUuid(remoteProj.syncUuid)
            if (localProj == null) {
                projectDao.insertProject(
                    ProjectEntity(
                        syncUuid = remoteProj.syncUuid,
                        name = remoteProj.name,
                        colorHex = remoteProj.colorHex,
                        iconName = remoteProj.iconName,
                        isDefault = remoteProj.isDefault,
                        updatedAt = remoteProj.updatedAt,
                        isDeleted = remoteProj.isDeleted
                    )
                )
                mergedCount++
            } else if (remoteProj.updatedAt > localProj.updatedAt) {
                projectDao.updateProject(
                    localProj.copy(
                        name = remoteProj.name,
                        colorHex = remoteProj.colorHex,
                        iconName = remoteProj.iconName,
                        isDefault = remoteProj.isDefault,
                        updatedAt = remoteProj.updatedAt,
                        isDeleted = remoteProj.isDeleted
                    )
                )
                mergedCount++
            }
        }

        // Merge tasks
        for (remoteTask in payload.tasks) {
            val localTask = taskDao.getTaskBySyncUuid(remoteTask.syncUuid)
            val recurrence = try {
                RecurrenceType.valueOf(remoteTask.recurrenceName)
            } catch (e: Exception) {
                RecurrenceType.NONE
            }
            val priority = TaskPriority.fromLevel(remoteTask.priorityLevel)

            if (localTask == null) {
                taskDao.insertTask(
                    TaskEntity(
                        syncUuid = remoteTask.syncUuid,
                        title = remoteTask.title,
                        description = remoteTask.description,
                        dueTimestamp = remoteTask.dueTimestamp,
                        dueDateString = remoteTask.dueDateString,
                        dueTimeString = remoteTask.dueTimeString,
                        priority = priority,
                        projectName = remoteTask.projectName,
                        projectColorHex = remoteTask.projectColorHex,
                        isAlarmEnabled = remoteTask.isAlarmEnabled,
                        alarmSound = remoteTask.alarmSound,
                        isCompleted = remoteTask.isCompleted,
                        recurrence = recurrence,
                        customIntervalDays = remoteTask.customIntervalDays,
                        createdAt = remoteTask.createdAt,
                        updatedAt = remoteTask.updatedAt,
                        isDeleted = remoteTask.isDeleted
                    )
                )
                mergedCount++
            } else if (remoteTask.updatedAt > localTask.updatedAt) {
                taskDao.updateTask(
                    localTask.copy(
                        title = remoteTask.title,
                        description = remoteTask.description,
                        dueTimestamp = remoteTask.dueTimestamp,
                        dueDateString = remoteTask.dueDateString,
                        dueTimeString = remoteTask.dueTimeString,
                        priority = priority,
                        projectName = remoteTask.projectName,
                        projectColorHex = remoteTask.projectColorHex,
                        isAlarmEnabled = remoteTask.isAlarmEnabled,
                        alarmSound = remoteTask.alarmSound,
                        isCompleted = remoteTask.isCompleted,
                        recurrence = recurrence,
                        customIntervalDays = remoteTask.customIntervalDays,
                        updatedAt = remoteTask.updatedAt,
                        isDeleted = remoteTask.isDeleted
                    )
                )
                mergedCount++
            }
        }

        return mergedCount
    }
}
