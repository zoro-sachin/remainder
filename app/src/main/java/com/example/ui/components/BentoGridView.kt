package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.theme.BentoCardCoral
import com.example.ui.theme.BentoCardCoralText
import com.example.ui.theme.BentoCardPurple
import com.example.ui.theme.BentoCardPurpleText
import com.example.ui.theme.BentoCardSlate
import com.example.ui.theme.BentoLilac
import com.example.ui.theme.BentoLilacDark
import com.example.ui.theme.BentoPulseGreen
import com.example.ui.theme.BentoSurfaceVariant
import com.example.ui.theme.BentoTextMuted
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BentoGridView(
    selectedDate: Date,
    tasks: List<TaskEntity>,
    projects: List<ProjectEntity>,
    isServerRunning: Boolean,
    onTaskClick: (TaskEntity) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onOpenTaskEditor: () -> Unit,
    onOpenSyncDialog: () -> Unit,
    onOpenProjectManager: () -> Unit,
    onSnoozeTask: (TaskEntity, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    val urgentTask = activeTasks.firstOrNull { it.isAlarmEnabled || it.priority == TaskPriority.P1_URGENT }
        ?: activeTasks.firstOrNull { it.priority == TaskPriority.P2_HIGH }
        ?: activeTasks.firstOrNull()

    val infiniteTransition = rememberInfiniteTransition(label = "green_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Bento Header Bar
        item {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Column {
                    Text(
                        text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(selectedDate).uppercase(Locale.getDefault()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = BentoTextSecondary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Today",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoTextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // P2P • E2EE Status Pill
                    Surface(
                        color = BentoCardSlate.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardSlate),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onOpenSyncDialog() }
                            .testTag("bento_p2p_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(BentoPulseGreen.copy(alpha = if (isServerRunning) 1f else pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServerRunning) "P2P • LIVE" else "P2P • E2EE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Security / Sync Icon Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BentoCardSlate)
                            .border(1.dp, BentoTextSecondary.copy(alpha = 0.2f), CircleShape)
                            .clickable { onOpenSyncDialog() }
                            .testTag("bento_security_btn")
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "E2EE Security",
                            tint = BentoLilac,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 2. Bento Card 1: Urgent Reminder Tile (Col-Span 2, Coral #F2B8B5)
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = BentoCardCoral),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bento_urgent_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Subtle background alarm watermark
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = BentoCardCoralText.copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(110.dp)
                            .align(Alignment.TopEnd)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Top status badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = BentoCardCoralText.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (urgentTask?.isAlarmEnabled == true) "URGENT ALARM" else "PRIORITY TASK",
                                    color = BentoCardCoralText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = urgentTask?.dueTimeString ?: "08:30 AM",
                                color = BentoCardCoralText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Title & Subtitle
                        Text(
                            text = urgentTask?.title ?: "Team Sync & Sprint Review",
                            color = BentoCardCoralText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (urgentTask != null) {
                                "Project: ${urgentTask.projectName} • ${urgentTask.priority.displayName}"
                            } else {
                                "Recurring: Every Thursday • Alarm Set"
                            },
                            color = BentoCardCoralText.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions: Snooze 10m & Dismiss / Complete
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    if (urgentTask != null) {
                                        onSnoozeTask(urgentTask, 10)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BentoCardCoralText,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("bento_snooze_btn")
                            ) {
                                Text("Snooze 10m", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (urgentTask != null) {
                                        onToggleComplete(urgentTask)
                                    }
                                },
                                border = androidx.compose.foundation.BorderStroke(2.dp, BentoCardCoralText),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("bento_dismiss_btn")
                            ) {
                                Text(
                                    if (urgentTask?.isCompleted == true) "Completed" else "Dismiss",
                                    color = BentoCardCoralText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bento Row: Tasks Tile (Purple #381E72) & Right Column (Projects #49454F & Offline #25232A)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left Column: Tasks Tile (Deep Royal Purple #381E72, Text #EADDFF)
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoCardPurple),
                    modifier = Modifier
                        .weight(1f)
                        .height(260.dp)
                        .testTag("bento_tasks_tile")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        // Title header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📑", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TASKS",
                                    color = BentoCardPurpleText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "${activeTasks.size}",
                                color = BentoLilac,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Task items list
                        if (tasks.isEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "No tasks yet\nTap + to add",
                                    color = BentoCardPurpleText.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tasks.take(4).forEach { task ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onTaskClick(task) }
                                    ) {
                                        // Colored Priority Bar
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(task.priority.composeColor)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = task.title,
                                                color = BentoCardPurpleText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                modifier = Modifier.alpha(if (task.isCompleted) 0.5f else 1f)
                                            )
                                            Text(
                                                text = "${task.priority.shortName} • ${task.projectName}",
                                                color = BentoCardPurpleText.copy(alpha = 0.6f),
                                                fontSize = 9.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column: Projects Card & Offline Card Stacked
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(260.dp)
                ) {
                    // Projects Bento Tile (Medium Slate #49454F)
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoCardSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.5f)
                            .clickable { onOpenProjectManager() }
                            .testTag("bento_projects_tile")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PROJECTS",
                                color = BentoTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            // Color dot swatches
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                projects.take(5).forEach { proj ->
                                    val col = try {
                                        Color(android.graphics.Color.parseColor(proj.colorHex))
                                    } catch (e: Exception) {
                                        BentoLilac
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                    )
                                }
                            }

                            Text(
                                text = "${projects.size} active categories",
                                color = BentoTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Offline / E2EE Cache Tile (Charcoal #25232A)
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardSlate),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { onOpenSyncDialog() }
                            .testTag("bento_offline_tile")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Offline",
                                color = BentoTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Surface(
                                color = BentoLilac,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Cached",
                                    color = BentoLilacDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Bento Card 5: Quick Add Bento Banner (Lilac #D0BCFF, Text #381E72, Rounded 28.dp)
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = BentoLilac),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenTaskEditor() }
                    .testTag("bento_quick_add_tile")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BentoCardPurple)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = "Quick add task or alarm...",
                            color = BentoCardPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                    }

                    Text(
                        text = "✨",
                        fontSize = 18.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp)) // padding for bottom navigation
        }
    }
}
