package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.ProjectEntity
import com.example.data.model.TaskPriority
import com.example.ui.theme.BentoCardCoral
import com.example.ui.theme.BentoCardCoralText
import com.example.ui.theme.BentoCardPurple
import com.example.ui.theme.BentoCardSlate
import com.example.ui.theme.BentoLilac
import com.example.ui.theme.BentoLilacDark
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoSurfaceVariant
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.DarkBorder

@Composable
fun QuickTaskEntryBar(
    projects: List<ProjectEntity>,
    onQuickAdd: (title: String, priority: TaskPriority, dueHourOffset: Int, isAlarm: Boolean, projectId: Long) -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.P3_MEDIUM) }
    var dueHourOffset by remember { mutableIntStateOf(1) }
    var isAlarmEnabled by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableLongStateOf(projects.firstOrNull()?.id ?: 1L) }

    Surface(
        color = BentoSurface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardSlate.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Main Bento Quick Input Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "Quick add task or alarm...",
                            fontSize = 14.sp,
                            color = BentoTextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoLilac,
                        unfocusedBorderColor = BentoCardSlate.copy(alpha = 0.6f),
                        focusedContainerColor = BentoSurfaceVariant,
                        unfocusedContainerColor = BentoSurfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("quick_task_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Urgent Alarm Toggle Button
                IconButton(
                    onClick = { isAlarmEnabled = !isAlarmEnabled },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isAlarmEnabled) BentoCardCoral else BentoCardSlate.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("quick_alarm_toggle")
                ) {
                    Icon(
                        if (isAlarmEnabled) Icons.Default.AlarmOn else Icons.Default.Alarm,
                        contentDescription = "Urgent Alarm",
                        tint = if (isAlarmEnabled) BentoCardCoralText else BentoTextSecondary
                    )
                }

                // Full Screen Editor Expand Button
                IconButton(
                    onClick = onOpenFullEditor,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = BentoCardSlate.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_expand_full_editor")
                ) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = "Full Editor",
                        tint = BentoLilac,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Add Submit Button
                IconButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            onQuickAdd(
                                title,
                                selectedPriority,
                                dueHourOffset,
                                isAlarmEnabled,
                                selectedProjectId
                            )
                            title = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (title.isNotBlank()) BentoLilac else BentoCardSlate.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_submit_quick_task")
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Add Task",
                        tint = if (title.isNotBlank()) BentoLilacDark else BentoTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Filters & Options Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Priority chips
                TaskPriority.entries.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    Surface(
                        color = if (isSelected) priority.bgAlphaColor else BentoSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) priority.composeColor else BentoCardSlate.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedPriority = priority
                                if (priority == TaskPriority.P1_URGENT) {
                                    isAlarmEnabled = true
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("chip_prio_${priority.shortName}")
                    ) {
                        Text(
                            text = priority.shortName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) priority.composeColor else BentoTextSecondary
                        )
                    }
                }

                // Time Presets (+1h, +3h, +6h)
                listOf(1 to "+1h", 3 to "+3h", 6 to "+6h", 12 to "+12h").forEach { (offset, label) ->
                    val isSelected = dueHourOffset == offset
                    Surface(
                        color = if (isSelected) BentoLilac.copy(alpha = 0.2f) else BentoSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) BentoLilac else BentoCardSlate.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { dueHourOffset = offset }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) BentoLilac else BentoTextSecondary
                        )
                    }
                }

                // Project Selector
                projects.take(4).forEach { proj ->
                    val projColor = try {
                        Color(android.graphics.Color.parseColor(proj.colorHex))
                    } catch (e: Exception) {
                        BentoLilac
                    }
                    val isSelected = selectedProjectId == proj.id
                    Surface(
                        color = if (isSelected) projColor.copy(alpha = 0.25f) else BentoSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) projColor else BentoCardSlate.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedProjectId = proj.id }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = proj.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) projColor else BentoTextSecondary
                        )
                    }
                }
            }
        }
    }
}
