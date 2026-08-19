package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Boot completed, rescheduling all active task alarms")
            NotificationHelper.createNotificationChannels(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val upcomingTasks = db.taskDao().getUpcomingAlarmTasks(System.currentTimeMillis())
                    for (task in upcomingTasks) {
                        AlarmScheduler.scheduleTaskAlarm(context, task)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms on boot: ${e.message}")
                }
            }
        }
    }
}
