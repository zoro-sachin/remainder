package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AlarmSoundType
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.theme.BentoCardCoral
import com.example.ui.theme.BentoCardSlate
import com.example.ui.theme.BentoSurfaceVariant
import com.example.ui.theme.DarkBorder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleAgendaView(
    tasks: List<TaskEntity>,
    onTaskClick: (TaskEntity) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        EmptyScheduleState(modifier = modifier)
        return
    }

    // Group tasks by dueDateString
    val groupedTasks = tasks.groupBy { it.dueDateString }.toSortedMap()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        groupedTasks.forEach { (dateStr, dateTasks) ->
            item(key = "header_$dateStr") {
                DateGroupHeader(dateStr = dateStr)
            }

            items(dateTasks, key = { it.id }) { task ->
                TaskAgendaCard(
                    task = task,
                    onTaskClick = { onTaskClick(task) },
                    onToggleComplete = { onToggleComplete(task) },
                    onDeleteTask = { onDeleteTask(task.id) }
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(88.dp)) // Padding for bottom quick add bar
        }
    }
}

@Composable
private fun DateGroupHeader(dateStr: String) {
    val relativeLabel = getRelativeDateLabel(dateStr)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = relativeLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun TaskAgendaCard(
    task: TaskEntity,
    onTaskClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDeleteTask: () -> Unit
) {
    val projColor = try {
        Color(android.graphics.Color.parseColor(task.projectColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val soundType = AlarmSoundType.fromId(task.alarmSound)

    val cardBg = when {
        task.isCompleted -> BentoSurfaceVariant.copy(alpha = 0.5f)
        task.isAlarmEnabled || task.priority == TaskPriority.P1_URGENT -> BentoCardCoral.copy(alpha = 0.15f)
        else -> BentoSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (task.isAlarmEnabled || task.priority == TaskPriority.P1_URGENT) BentoCardCoral.copy(alpha = 0.4f) else BentoCardSlate.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onTaskClick() }
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Priority Color Ribbon (Left indicator)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(task.priority.composeColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 2. Complete Checkbox
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isCompleted) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.primary else task.priority.composeColor,
                        shape = CircleShape
                    )
                    .clickable { onToggleComplete() }
                    .testTag("checkbox_task_${task.id}")
            ) {
                if (task.isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 3. Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority Tag Pill
                    Surface(
                        color = task.priority.bgAlphaColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.priority.displayName,
                            color = task.priority.composeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Project Tag Pill
                    Surface(
                        color = projColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.projectName,
                            color = projColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Urgent Alarm Tag
                    if (task.isAlarmEnabled) {
                        Surface(
                            color = Color(0x33FF3B30),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Alarm,
                                    contentDescription = "Alarm",
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = soundType.displayName,
                                    color = Color(0xFFFF3B30),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Recurrence Tag
                    if (task.recurrence != RecurrenceType.NONE) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "Recurring",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Snoozed Tag
                    if (task.isSnoozed) {
                        Icon(
                            Icons.Default.Snooze,
                            contentDescription = "Snoozed",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Time Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏰ ${task.dueTimeString}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isAlarmEnabled) Color(0xFFFF3B30) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 4. Quick Action buttons
            IconButton(
                onClick = onTaskClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_edit_task_${task.id}")
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyScheduleState(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.hero_schedule_art),
            contentDescription = "No tasks",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your schedule is clear",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Use the quick entry bar below to add urgent alarms, recurring tasks, and color-coded projects!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun getRelativeDateLabel(dateStr: String): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    return try {
        val date = dateFormat.parse(dateStr) ?: return dateStr
        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -2)
        val yesterdayStr = dateFormat.format(cal.time)

        when (dateStr) {
            todayStr -> "Today • ${displayFormat.format(date)}"
            tomorrowStr -> "Tomorrow • ${displayFormat.format(date)}"
            yesterdayStr -> "Yesterday • ${displayFormat.format(date)}"
            else -> displayFormat.format(date)
        }
    } catch (e: Exception) {
        dateStr
    }
}
