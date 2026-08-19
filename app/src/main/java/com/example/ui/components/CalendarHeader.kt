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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.BentoCardSlate
import com.example.ui.theme.BentoLilac
import com.example.ui.theme.BentoLilacDark
import com.example.ui.theme.BentoPulseGreen
import com.example.ui.theme.BentoTextMuted
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.DarkBorder
import com.example.ui.viewmodel.CalendarUiState
import com.example.ui.viewmodel.CalendarViewMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarHeader(
    uiState: CalendarUiState,
    projects: List<ProjectEntity>,
    tasks: List<TaskEntity>,
    isServerRunning: Boolean,
    onDateSelected: (Date) -> Unit,
    onViewModeChanged: (CalendarViewMode) -> Unit,
    onProjectSelected: (Long?) -> Unit,
    onPrioritySelected: (TaskPriority?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onOpenSyncDialog: () -> Unit,
    onOpenProjectManager: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateStr = dateFormat.format(Date())

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_header")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha_h"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
        ) {
            // 1. Top Action Bar in Bento aesthetic
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                if (uiState.isSearching) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = { Text("Search tasks, notes, projects...", fontSize = 14.sp) },
                        trailingIcon = {
                            IconButton(onClick = { onToggleSearch(false) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search")
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoLilac,
                            unfocusedBorderColor = DarkBorder
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("search_input")
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoLilac)
                        ) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = "App Icon",
                                tint = BentoLilacDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ChronoTask",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = monthYearFormat.format(uiState.selectedDate),
                                fontSize = 11.sp,
                                color = BentoTextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // P2P E2EE Status badge button
                        Surface(
                            color = BentoCardSlate.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BentoCardSlate),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onOpenSyncDialog() }
                                .testTag("btn_header_sync_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(BentoPulseGreen.copy(alpha = if (isServerRunning) 1f else pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "E2EE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary
                                )
                            }
                        }

                        // Search Button
                        IconButton(
                            onClick = { onToggleSearch(true) },
                            modifier = Modifier.testTag("btn_open_search")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = BentoTextSecondary)
                        }

                        // Project Manager Button
                        IconButton(
                            onClick = onOpenProjectManager,
                            modifier = Modifier.testTag("btn_open_projects")
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Projects", tint = BentoTextSecondary)
                        }

                        // Settings Button
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("btn_open_settings")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = BentoTextSecondary)
                        }
                    }
                }
            }

            // 2. View Mode Tabs (Bento, Schedule, Week, Month, Day)
            TabRow(
                selectedTabIndex = uiState.viewMode.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BentoLilac,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.viewMode.ordinal]),
                        color = BentoLilac,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                CalendarViewMode.entries.forEach { mode ->
                    Tab(
                        selected = uiState.viewMode == mode,
                        onClick = { onViewModeChanged(mode) },
                        text = {
                            Text(
                                text = mode.title,
                                fontWeight = if (uiState.viewMode == mode) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_view_${mode.name.lowercase()}")
                    )
                }
            }

            // 3. Horizontal Date Selector Strip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            time = uiState.selectedDate
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        onDateSelected(cal.time)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", modifier = Modifier.size(20.dp), tint = BentoTextSecondary)
                }

                val dateList = rememberDaysAround(uiState.selectedDate)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp)
                ) {
                    dateList.forEach { date ->
                        val dateStr = dateFormat.format(date)
                        val isSelected = dateStr == uiState.selectedDateString
                        val isToday = dateStr == todayDateStr
                        val dayTasks = tasks.filter { it.dueDateString == dateStr && !it.isCompleted }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        isSelected -> BentoLilac
                                        isToday -> BentoCardSlate
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("date_chip_$dateStr")
                        ) {
                            Text(
                                text = dayNameFormat.format(date).uppercase(Locale.getDefault()),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BentoLilacDark else BentoTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayNumFormat.format(date),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) BentoLilacDark else BentoTextPrimary
                            )
                            // Task dots indicator
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .height(6.dp)
                                    .padding(top = 2.dp)
                            ) {
                                dayTasks.take(3).forEach { t ->
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) BentoLilacDark else t.priority.composeColor)
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            time = uiState.selectedDate
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        onDateSelected(cal.time)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", modifier = Modifier.size(20.dp), tint = BentoTextSecondary)
                }
            }

            // 4. Project and Priority Filter Chips Bar
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                // All Projects Chip
                FilterChip(
                    selected = uiState.selectedProjectId == null,
                    onClick = { onProjectSelected(null) },
                    label = { Text("All Projects", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BentoLilac.copy(alpha = 0.25f),
                        selectedLabelColor = BentoLilac
                    ),
                    modifier = Modifier.testTag("filter_proj_all")
                )

                // Project Chips
                projects.forEach { proj ->
                    val projColor = try {
                        Color(android.graphics.Color.parseColor(proj.colorHex))
                    } catch (e: Exception) {
                        BentoLilac
                    }
                    FilterChip(
                        selected = uiState.selectedProjectId == proj.id,
                        onClick = {
                            onProjectSelected(if (uiState.selectedProjectId == proj.id) null else proj.id)
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(projColor)
                            )
                        },
                        label = { Text(proj.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = projColor.copy(alpha = 0.3f),
                            selectedLabelColor = projColor
                        ),
                        modifier = Modifier.testTag("filter_proj_${proj.id}")
                    )
                }

                // Priority Filter Chips
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = uiState.selectedPriority == priority,
                        onClick = {
                            onPrioritySelected(if (uiState.selectedPriority == priority) null else priority)
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(priority.composeColor)
                            )
                        },
                        label = { Text(priority.shortName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = priority.bgAlphaColor,
                            selectedLabelColor = priority.composeColor
                        ),
                        modifier = Modifier.testTag("filter_prio_${priority.name}")
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberDaysAround(selectedDate: Date): List<Date> {
    val list = mutableListOf<Date>()
    val cal = Calendar.getInstance()
    cal.time = selectedDate
    cal.add(Calendar.DAY_OF_YEAR, -5)
    for (i in 0 until 14) {
        list.add(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return list
}
