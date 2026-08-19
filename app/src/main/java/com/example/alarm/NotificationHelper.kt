package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority

object NotificationHelper {
    const val CHANNEL_URGENT_ALARM = "channel_urgent_alarm_id"
    const val CHANNEL_REMINDERS = "channel_reminders_id"

    const val ACTION_SNOOZE = "com.example.action.SNOOZE_TASK"
    const val ACTION_COMPLETE = "com.example.action.COMPLETE_TASK"
    const val ACTION_EDIT = "com.example.action.EDIT_TASK"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT_ALARM,
                "Urgent Task Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alarm clock alerts with sound and heads-up banner"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Standard Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily task reminders and schedule notifications"
                enableLights(true)
                lightColor = Color.BLUE
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(urgentChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showTaskAlarmNotification(context: Context, task: TaskEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open Main Activity directly in Edit Mode
        val editIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_EDIT
            putExtra(EXTRA_TASK_ID, task.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val editPendingIntent = PendingIntent.getActivity(
            context,
            (task.id * 10 + 1).toInt(),
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze 10m
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_SNOOZE_MINUTES, 10)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark Complete
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 3).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (task.isAlarmEnabled || task.priority == TaskPriority.P1_URGENT) {
            CHANNEL_URGENT_ALARM
        } else {
            CHANNEL_REMINDERS
        }

        val priorityBadge = when (task.priority) {
            TaskPriority.P1_URGENT -> "🚨 [P1 URGENT ALARM]"
            TaskPriority.P2_HIGH -> "⚡ [P2 HIGH PRIORITY]"
            TaskPriority.P3_MEDIUM -> "📌 [P3 MEDIUM]"
            TaskPriority.P4_NORMAL -> "✓ [NORMAL]"
        }

        val bigText = buildString {
            append("⏰ Due: ${task.dueTimeString} • 📁 ${task.projectName}\n")
            if (task.description.isNotBlank()) {
                append("\n${task.description}\n")
            }
            if (task.recurrence.name != "NONE") {
                append("\n🔁 Repeats: ${task.recurrence.displayName}")
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$priorityBadge ${task.title}")
            .setContentText("📁 ${task.projectName} • ${task.dueTimeString}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(editPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 10m", snoozePendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Done", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_edit, "Edit Task", editPendingIntent)

        notificationManager.notify(task.id.toInt(), builder.build())
    }

    fun dismissNotification(context: Context, taskId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())
    }
}
