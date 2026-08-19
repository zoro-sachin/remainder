package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotifActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        // Stop any ringing audio
        AlarmAudioPlayer.stopSound()

        val action = intent.action
        Log.d(TAG, "Received notification action: $action for taskId: $taskId")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = TaskRepository(db.taskDao(), db.projectDao())
                val task = repository.getTaskById(taskId) ?: return@launch

                when (action) {
                    NotificationHelper.ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 10)
                        repository.snoozeTask(taskId, minutes)
                        NotificationHelper.dismissNotification(context, taskId)

                        // Re-schedule alarm for new time
                        val updatedTask = repository.getTaskById(taskId)
                        if (updatedTask != null) {
                            AlarmScheduler.scheduleTaskAlarm(context, updatedTask)
                        }
                    }

                    NotificationHelper.ACTION_COMPLETE -> {
                        repository.completeTask(task, true)
                        NotificationHelper.dismissNotification(context, taskId)
                        AlarmScheduler.cancelTaskAlarm(context, taskId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing notification action: ${e.message}")
            }
        }
    }
}
