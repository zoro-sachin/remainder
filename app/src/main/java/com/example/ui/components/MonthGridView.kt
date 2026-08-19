package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.AccentIndigo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MonthGridView(
    selectedDate: Date,
    tasks: List<TaskEntity>,
    onDateSelected: (Date) -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
    onToggleComplete: (TaskEntity) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selectedDateStr = dateFormat.format(selectedDate)
    val todayDateStr = dateFormat.format(Date())

    val monthDays = rememberMonthDays(selectedDate)
    val selectedDayTasks = tasks.filter { it.dueDateString == selectedDateStr }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Weekday column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calendar Month 7-column Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(monthDays) { dayInfo ->
                if (dayInfo == null) {
                    Box(modifier = Modifier.aspectRatio(1f))
                } else {
                    val dateStr = dateFormat.format(dayInfo.date)
                    val isSelected = dateStr == selectedDateStr
                    val isToday = dateStr == todayDateStr
                    val dayTasks = tasks.filter { it.dueDateString == dateStr && !it.isCompleted }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isSelected -> AccentIndigo
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                            .clickable { onDateSelected(dayInfo.date) }
                            .padding(2.dp)
                            .testTag("month_day_$dateStr")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayInfo.dayNumber.toString(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            // Priority dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.height(4.dp)
                            ) {
                                dayTasks.take(3).forEach { t ->
                                    Box(
                                        modifier = Modifier
                                            .size(3.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else t.priority.composeColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Day's Task Section
        Text(
            text = "Tasks for ${SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(selectedDate)} (${selectedDayTasks.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedDayTasks.isEmpty()) {
                item {
                    Text(
                        text = "No tasks scheduled for this day.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(selectedDayTasks, key = { it.id }) { task ->
                    TaskAgendaCard(
                        task = task,
                        onTaskClick = { onTaskClick(task) },
                        onToggleComplete = { onToggleComplete(task) },
                        onDeleteTask = { onDeleteTask(task.id) }
                    )
                }
            }
        }
    }
}

private data class DayInfo(val date: Date, val dayNumber: Int)

private fun rememberMonthDays(selectedDate: Date): List<DayInfo?> {
    val list = mutableListOf<DayInfo?>()
    val cal = Calendar.getInstance()
    cal.time = selectedDate
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    for (i in 0 until firstDayOfWeek) {
        list.add(null)
    }

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (day in 1..maxDays) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        list.add(DayInfo(cal.time, day))
    }
    return list
}
