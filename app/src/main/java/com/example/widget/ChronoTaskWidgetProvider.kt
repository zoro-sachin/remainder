package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChronoTaskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_chronotask)
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            views.setTextViewText(R.id.widget_date, dateFormat.format(Date()))

            // Intent to open Main Activity
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Intent for Quick Add
            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                action = "ACTION_QUICK_ADD"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val quickAddPendingIntent = PendingIntent.getActivity(
                context,
                1,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quick_add_btn, quickAddPendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val tasks = db.taskDao().getAllTasksForSync()
                    val todayActiveTasks = tasks.filter { !it.isDeleted && !it.isCompleted && it.dueDateString == todayDateStr }
                    val upcomingAlarms = tasks.filter { !it.isDeleted && !it.isCompleted && it.isAlarmEnabled && it.dueTimestamp > System.currentTimeMillis() }
                        .sortedBy { it.dueTimestamp }

                    val summaryText = if (todayActiveTasks.isEmpty()) {
                        "✨ All caught up for today!"
                    } else {
                        "📌 ${todayActiveTasks.size} tasks today • Top: ${todayActiveTasks.first().title}"
                    }

                    val nextAlarmText = if (upcomingAlarms.isNotEmpty()) {
                        val next = upcomingAlarms.first()
                        "⏰ Alarm: ${next.dueTimeString} (${next.title})"
                    } else {
                        "⏰ No active alarms"
                    }

                    views.setTextViewText(R.id.widget_task_summary, summaryText)
                    views.setTextViewText(R.id.widget_next_alarm, nextAlarmText)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_task_summary, "ChronoTask Ready")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ChronoTaskWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }
}
