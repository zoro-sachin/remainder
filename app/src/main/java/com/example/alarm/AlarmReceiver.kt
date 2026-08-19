package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.AlarmSoundType
import com.example.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private val _alarmTriggerEvents = MutableSharedFlow<TaskEntity>(extraBufferCapacity = 5)
        val alarmTriggerEvents = _alarmTriggerEvents.asSharedFlow()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        Log.d(TAG, "Alarm triggered for task ID: $taskId")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "ChronoTask::AlarmWakeLock"
        )
        wakeLock.acquire(60 * 1000L) // 1 minute max

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null && !task.isCompleted && !task.isDeleted) {
                    // Play synthesized custom tone
                    val sound = AlarmSoundType.fromId(task.alarmSound)
                    AlarmAudioPlayer.playSound(sound, loop = true)

                    // Show notification
                    NotificationHelper.showTaskAlarmNotification(context, task)

                    // Broadcast in-app modal trigger
                    _alarmTriggerEvents.emit(task)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm: ${e.message}")
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }
}
