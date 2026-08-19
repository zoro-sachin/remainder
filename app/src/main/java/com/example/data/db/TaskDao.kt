package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY dueTimestamp ASC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND dueDateString = :dateString ORDER BY dueTimestamp ASC")
    fun getTasksForDateFlow(dateString: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isCompleted = 0 ORDER BY dueTimestamp ASC")
    fun getIncompleteTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE syncUuid = :syncUuid LIMIT 1")
    suspend fun getTaskBySyncUuid(syncUuid: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isCompleted = 0 AND isAlarmEnabled = 1 AND dueTimestamp > :nowMs ORDER BY dueTimestamp ASC")
    suspend fun getUpcomingAlarmTasks(nowMs: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun getAllTasksForSync(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity): Int

    @Delete
    suspend fun deleteTask(task: TaskEntity): Int

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteTask(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET dueTimestamp = :newDueTimestamp, isSnoozed = 1, snoozeCount = snoozeCount + 1, lastSnoozedAt = :nowMs, updatedAt = :nowMs WHERE id = :id")
    suspend fun snoozeTask(id: Long, newDueTimestamp: Long, nowMs: Long = System.currentTimeMillis())
}
