package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.alarm.NotificationHelper
import com.example.ui.components.BentoGridView
import com.example.ui.components.CalendarHeader
import com.example.ui.components.DayTimelineView
import com.example.ui.components.MonthGridView
import com.example.ui.components.NightModeSettingsDialog
import com.example.ui.components.P2PSyncDialog
import com.example.ui.components.ProjectManagerDialog
import com.example.ui.components.QuickTaskEntryBar
import com.example.ui.components.ScheduleAgendaView
import com.example.ui.components.TaskDetailEditSheet
import com.example.ui.components.UrgentAlarmModal
import com.example.ui.components.WeekTimelineView
import com.example.ui.theme.ChronoTaskTheme
import com.example.ui.theme.WarmNightTint
import com.example.ui.viewmodel.CalendarViewModel
import com.example.ui.viewmodel.CalendarViewMode

class MainActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle Intent actions (Deep-link edit, Quick Add)
        handleIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val projects by viewModel.allProjects.collectAsState()
            val tasks by viewModel.filteredTasks.collectAsState()
            val isServerRunning by viewModel.p2pServer.isServerRunning.collectAsState()
            val serverIp by viewModel.p2pServer.serverIp.collectAsState()
            val syncLog by viewModel.p2pServer.lastSyncLog.collectAsState()

            // Runtime Notification Permission Request for Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Permission handled
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            ChronoTaskTheme(themeMode = uiState.themeMode) {
                Scaffold(
                    topBar = {
                        CalendarHeader(
                            uiState = uiState,
                            projects = projects,
                            tasks = tasks,
                            isServerRunning = isServerRunning,
                            onDateSelected = { viewModel.setSelectedDate(it) },
                            onViewModeChanged = { viewModel.setViewMode(it) },
                            onProjectSelected = { viewModel.setSelectedProject(it) },
                            onPrioritySelected = { viewModel.setSelectedPriority(it) },
                            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                            onToggleSearch = { viewModel.toggleSearch(it) },
                            onOpenSyncDialog = { viewModel.openSyncDialog(true) },
                            onOpenProjectManager = { viewModel.openProjectManager(true) },
                            onOpenSettings = { viewModel.openSettingsDialog(true) }
                        )
                    },
                    bottomBar = {
                        QuickTaskEntryBar(
                            projects = projects,
                            onQuickAdd = { title, priority, dueHourOffset, isAlarm, projectId ->
                                viewModel.quickAddTask(title, priority, dueHourOffset, isAlarm, projectId)
                            },
                            onOpenFullEditor = { viewModel.openTaskEditor(null) }
                        )
                    },
                    modifier = Modifier.fillMaxSize().testTag("main_scaffold")
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main View Mode Swapping
                        when (uiState.viewMode) {
                            CalendarViewMode.BENTO -> {
                                BentoGridView(
                                    selectedDate = uiState.selectedDate,
                                    tasks = tasks,
                                    projects = projects,
                                    isServerRunning = isServerRunning,
                                    onTaskClick = { viewModel.openTaskEditor(it) },
                                    onToggleComplete = { viewModel.toggleTaskComplete(it) },
                                    onOpenTaskEditor = { viewModel.openTaskEditor(null) },
                                    onOpenSyncDialog = { viewModel.openSyncDialog(true) },
                                    onOpenProjectManager = { viewModel.openProjectManager(true) },
                                    onSnoozeTask = { task, minutes -> viewModel.snoozeTask(task.id, minutes) }
                                )
                            }
                            CalendarViewMode.SCHEDULE -> {
                                ScheduleAgendaView(
                                    tasks = tasks,
                                    onTaskClick = { viewModel.openTaskEditor(it) },
                                    onToggleComplete = { viewModel.toggleTaskComplete(it) },
                                    onDeleteTask = { viewModel.deleteTask(it) }
                                )
                            }
                            CalendarViewMode.WEEK -> {
                                WeekTimelineView(
                                    selectedDate = uiState.selectedDate,
                                    tasks = tasks,
                                    onTaskClick = { viewModel.openTaskEditor(it) }
                                )
                            }
                            CalendarViewMode.MONTH -> {
                                MonthGridView(
                                    selectedDate = uiState.selectedDate,
                                    tasks = tasks,
                                    onDateSelected = { viewModel.setSelectedDate(it) },
                                    onTaskClick = { viewModel.openTaskEditor(it) },
                                    onToggleComplete = { viewModel.toggleTaskComplete(it) },
                                    onDeleteTask = { viewModel.deleteTask(it) }
                                )
                            }
                            CalendarViewMode.DAY -> {
                                DayTimelineView(
                                    selectedDate = uiState.selectedDate,
                                    tasks = tasks,
                                    onTaskClick = { viewModel.openTaskEditor(it) },
                                    onQuickAddHour = { hour ->
                                        viewModel.openTaskEditor(null)
                                    }
                                )
                            }
                        }

                        // Eye-comfort Night Light Warm Amber Filter Overlay
                        if (uiState.isNightLightWarmEnabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(WarmNightTint)
                            )
                        }
                    }
                }

                // 1. Task Detail / Edit Modal Bottom Sheet
                if (uiState.editingTask != null || uiState.isCreatingNewTask) {
                    TaskDetailEditSheet(
                        initialTask = uiState.editingTask,
                        defaultDate = uiState.selectedDate,
                        projects = projects,
                        onSaveTask = { viewModel.saveTask(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onDismiss = { viewModel.closeTaskEditor() },
                        onPlaySoundPreview = { viewModel.playSoundPreview(it) },
                        onStopSoundPreview = { viewModel.stopSoundPreview() }
                    )
                }

                // 2. Urgent Ringing Alarm Modal Alert (Full Screen)
                if (uiState.ringingAlarmTask != null) {
                    UrgentAlarmModal(
                        task = uiState.ringingAlarmTask!!,
                        onSnooze = { minutes -> viewModel.snoozeRingingAlarm(minutes) },
                        onDismissComplete = { markCompleted -> viewModel.dismissRingingAlarm(markCompleted) },
                        onQuickEdit = { task ->
                            viewModel.dismissRingingAlarm(false)
                            viewModel.openTaskEditor(task)
                        }
                    )
                }

                // 3. P2P Encrypted Sync Server & Client Dialog
                if (uiState.isSyncDialogOpen) {
                    P2PSyncDialog(
                        isServerRunning = isServerRunning,
                        serverIp = serverIp,
                        serverPort = viewModel.p2pServer.port,
                        syncLog = syncLog,
                        syncPassphrase = uiState.syncPassphrase,
                        peerIpInput = uiState.peerIpInput,
                        syncStatusMessage = uiState.syncStatusMessage,
                        isSyncing = uiState.isSyncing,
                        exportedCodeText = uiState.exportedCodeText,
                        onToggleServer = { viewModel.toggleP2PServer() },
                        onUpdatePassphrase = { viewModel.updateSyncPassphrase(it) },
                        onUpdatePeerIp = { viewModel.updatePeerIpInput(it) },
                        onSyncWithPeer = { viewModel.syncWithPeerNow() },
                        onExportCode = { viewModel.exportEncryptedCode() },
                        onImportCode = { viewModel.importEncryptedCode(it) },
                        onDismiss = { viewModel.openSyncDialog(false) }
                    )
                }

                // 4. Color-Coded Project Manager Dialog
                if (uiState.isProjectManagerOpen) {
                    ProjectManagerDialog(
                        projects = projects,
                        onAddProject = { name, color, icon -> viewModel.addProject(name, color, icon) },
                        onDeleteProject = { viewModel.deleteProject(it) },
                        onDismiss = { viewModel.openProjectManager(false) }
                    )
                }

                // 5. Display & Night Mode Settings Dialog
                if (uiState.isSettingsDialogOpen) {
                    NightModeSettingsDialog(
                        currentTheme = uiState.themeMode,
                        isNightLightWarm = uiState.isNightLightWarmEnabled,
                        onThemeChange = { viewModel.setThemeMode(it) },
                        onToggleNightLight = { viewModel.toggleNightLight(it) },
                        onPlaySoundPreview = { viewModel.playSoundPreview(it) },
                        onStopSoundPreview = { viewModel.stopSoundPreview() },
                        onDismiss = { viewModel.openSettingsDialog(false) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (intent.action == NotificationHelper.ACTION_EDIT || taskId != -1L) {
            viewModel.openTaskEditorById(taskId)
        } else if (intent.action == "ACTION_QUICK_ADD") {
            viewModel.openTaskEditor(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.p2pServer.stopServer()
    }
}
