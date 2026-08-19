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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmSoundType
import com.example.data.model.ProjectEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.DarkBorder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailEditSheet(
    initialTask: TaskEntity?,
    defaultDate: Date,
    projects: List<ProjectEntity>,
    onSaveTask: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onDismiss: () -> Unit,
    onPlaySoundPreview: (AlarmSoundType) -> Unit,
    onStopSoundPreview: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isNew = initialTask == null
    val cal = remember {
        Calendar.getInstance().apply {
            if (initialTask != null) {
                timeInMillis = initialTask.dueTimestamp
            } else {
                time = defaultDate
                set(Calendar.HOUR_OF_DAY, (get(Calendar.HOUR_OF_DAY) + 1).coerceAtMost(23))
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
    }

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.P3_MEDIUM) }
    var selectedProjectId by remember { mutableLongStateOf(initialTask?.projectId ?: projects.firstOrNull()?.id ?: 1L) }
    var isAlarmEnabled by remember { mutableStateOf(initialTask?.isAlarmEnabled ?: false) }
    var alarmSound by remember { mutableStateOf(AlarmSoundType.fromId(initialTask?.alarmSound)) }
    var recurrence by remember { mutableStateOf(initialTask?.recurrence ?: RecurrenceType.NONE) }
    var customIntervalDays by remember { mutableIntStateOf(initialTask?.customIntervalDays ?: 1) }

    var selectedHour by remember { mutableIntStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(cal.get(Calendar.MINUTE)) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = {
            onStopSoundPreview()
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("task_detail_edit_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isNew) "New Task" else "Edit Task",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = {
                    onStopSoundPreview()
                    onDismiss()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task Title *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = priority.composeColor,
                    unfocusedBorderColor = DarkBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_task_title")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description Notes
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Detailed Notes & Details (Optional)") },
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentIndigo,
                    unfocusedBorderColor = DarkBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_task_description")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date & Time Selectors Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quick Date Switcher
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            // Cycle +1 day for easy manipulation
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Date", tint = AccentIndigo, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Due Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateFormat.format(cal.time), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Time picker cycle
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedHour = (selectedHour + 1) % 24
                            cal.set(Calendar.HOUR_OF_DAY, selectedHour)
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = "Time", tint = AccentIndigo, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Due Time (Tap +1h)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(timeFormat.format(cal.time), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selector
            Text("Priority & Visual Color Tag", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TaskPriority.entries.forEach { prio ->
                    val isSelected = priority == prio
                    Surface(
                        color = if (isSelected) prio.bgAlphaColor else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) prio.composeColor else DarkBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                priority = prio
                                if (prio == TaskPriority.P1_URGENT) {
                                    isAlarmEnabled = true
                                }
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(prio.composeColor)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = prio.shortName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) prio.composeColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Project Selector
            Text("Category Project", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                projects.forEach { proj ->
                    val projColor = try {
                        Color(android.graphics.Color.parseColor(proj.colorHex))
                    } catch (e: Exception) {
                        AccentIndigo
                    }
                    val isSelected = selectedProjectId == proj.id
                    Surface(
                        color = if (isSelected) projColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) projColor else DarkBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedProjectId = proj.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(projColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(proj.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Urgent Alarm Clock Toggle & Custom Sound Preset Selector
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAlarmEnabled) Color(0x22FF3B30) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAlarmEnabled) Color(0xFFFF3B30) else DarkBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = "Alarm Clock",
                                tint = if (isAlarmEnabled) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Urgent Alarm Clock Reminder",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlarmEnabled) Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Full-screen alert tone & exact wake-lock",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isAlarmEnabled,
                            onCheckedChange = { isAlarmEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFF3B30),
                                checkedTrackColor = Color(0x44FF3B30)
                            )
                        )
                    }

                    if (isAlarmEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Select Alarm Tone:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AlarmSoundType.entries.forEach { sound ->
                                val isSoundSelected = alarmSound == sound
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSoundSelected) AccentIndigo.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { alarmSound = sound }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sound.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSoundSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSoundSelected) AccentIndigo else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = sound.subtitle,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Preview play button
                                    IconButton(
                                        onClick = {
                                            if (isPlayingPreview) {
                                                onStopSoundPreview()
                                                isPlayingPreview = false
                                            } else {
                                                onPlaySoundPreview(sound)
                                                isPlayingPreview = true
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (isPlayingPreview && isSoundSelected) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Test Audio",
                                            tint = AccentIndigo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recurrence Selector
            Text("Recurring Reminder Rule", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                RecurrenceType.entries.forEach { rec ->
                    val isSelected = recurrence == rec
                    Surface(
                        color = if (isSelected) AccentIndigo.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) AccentIndigo else DarkBorder
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { recurrence = rec }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = rec.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentIndigo else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (Save & Delete)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isNew && initialTask != null) {
                    OutlinedButton(
                        onClick = {
                            onStopSoundPreview()
                            onDeleteTask(initialTask.id)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3B30)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_delete_task")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onStopSoundPreview()
                            val proj = projects.find { it.id == selectedProjectId } ?: projects.firstOrNull()
                            val taskToSave = (initialTask ?: TaskEntity(
                                title = title.trim(),
                                dueTimestamp = cal.timeInMillis,
                                dueDateString = dateFormat.format(cal.time),
                                dueTimeString = timeFormat.format(cal.time)
                            )).copy(
                                title = title.trim(),
                                description = description.trim(),
                                dueTimestamp = cal.timeInMillis,
                                dueDateString = dateFormat.format(cal.time),
                                dueTimeString = timeFormat.format(cal.time),
                                priority = priority,
                                projectId = proj?.id ?: 1L,
                                projectName = proj?.name ?: "General",
                                projectColorHex = proj?.colorHex ?: "#6366F1",
                                isAlarmEnabled = isAlarmEnabled,
                                alarmSound = alarmSound.id,
                                recurrence = recurrence,
                                customIntervalDays = customIntervalDays
                            )
                            onSaveTask(taskToSave)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("btn_save_task")
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isNew) "Create Task" else "Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
