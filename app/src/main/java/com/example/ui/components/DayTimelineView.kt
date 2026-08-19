package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DayTimelineView(
    selectedDate: Date,
    tasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onQuickAddHour: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selectedDateStr = dateFormat.format(selectedDate)
    val dayTasks = tasks.filter { it.dueDateString == selectedDateStr }

    val verticalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(verticalScroll)
            .padding(bottom = 88.dp)
    ) {
        for (hour in 0..23) {
            val hourLabel = when {
                hour == 0 -> "12 AM"
                hour == 12 -> "12 PM"
                hour > 12 -> "${hour - 12} PM"
                else -> "$hour AM"
            }

            val hourTasks = dayTasks.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.dueTimestamp }
                cal.get(Calendar.HOUR_OF_DAY) == hour
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                // Time Label
                Text(
                    text = hourLabel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .width(58.dp)
                        .padding(end = 8.dp, top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )

                // Hour Slot Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    if (hourTasks.isEmpty()) {
                        // Empty slot click to quick add
                        Box(
                            contentAlignment = Alignment.CenterEnd,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onQuickAddHour(hour) }
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add task at $hourLabel",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            hourTasks.forEach { task ->
                                Surface(
                                    color = task.priority.bgAlphaColor,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, task.priority.composeColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onTaskClick(task) }
                                        .testTag("day_timeline_task_${task.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        Text(
                                            text = task.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = task.priority.composeColor,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${task.dueTimeString} • ${task.projectName}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
