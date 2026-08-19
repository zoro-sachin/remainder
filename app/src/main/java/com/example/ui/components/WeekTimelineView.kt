package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WeekTimelineView(
    selectedDate: Date,
    tasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())

    // 5-day horizontal window starting from 1 day before selected date
    val weekDays = mutableListOf<Date>()
    val cal = Calendar.getInstance().apply {
        time = selectedDate
        add(Calendar.DAY_OF_YEAR, -1)
    }
    for (i in 0 until 5) {
        weekDays.add(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Sticky Header for Week Days
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 54.dp, top = 6.dp, bottom = 6.dp)
                .horizontalScroll(horizontalScroll)
        ) {
            weekDays.forEach { date ->
                val dateStr = dateFormat.format(date)
                val isSelected = dateStr == dateFormat.format(selectedDate)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = dayNameFormat.format(date).uppercase(Locale.getDefault()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dayNumFormat.format(date),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Hourly Grid Scroll View
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(verticalScroll)
                .padding(bottom = 80.dp)
        ) {
            Column {
                // Hours from 6 AM to 11 PM
                for (hour in 6..23) {
                    val hourLabel = when {
                        hour == 12 -> "12 PM"
                        hour > 12 -> "${hour - 12} PM"
                        else -> "$hour AM"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        // Hour Label
                        Text(
                            text = hourLabel,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .width(50.dp)
                                .padding(end = 4.dp, top = 2.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )

                        // Grid lines for days
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                .horizontalScroll(horizontalScroll)
                        ) {
                            weekDays.forEach { date ->
                                val dateStr = dateFormat.format(date)
                                val hourTasks = tasks.filter {
                                    it.dueDateString == dateStr && getHourFromTimestamp(it.dueTimestamp) == hour
                                }

                                Box(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(64.dp)
                                        .border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                        .padding(2.dp)
                                ) {
                                    hourTasks.forEach { task ->
                                        WeekTaskPill(task = task, onClick = { onTaskClick(task) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekTaskPill(task: TaskEntity, onClick: () -> Unit) {
    Surface(
        color = task.priority.bgAlphaColor,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, task.priority.composeColor.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .testTag("week_task_${task.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = task.title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = task.priority.composeColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = task.dueTimeString,
                fontSize = 8.sp,
                color = task.priority.composeColor.copy(alpha = 0.8f)
            )
        }
    }
}

private fun getHourFromTimestamp(timestamp: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal.get(Calendar.HOUR_OF_DAY)
}
