package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmAudioPlayer
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmScheduler
import com.example.data.db.AppDatabase
import com.example.data.model.AlarmSoundType
import com.example.data.model.ProjectEntity
import com.example.data.model.RecurrenceType
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.repository.TaskRepository
import com.example.sync.P2PSyncClient
import com.example.sync.P2PSyncServer
import com.example.ui.theme.AppThemeMode
import com.example.widget.ChronoTaskWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CalendarViewMode(val title: String) {
    BENTO("Bento"),
    SCHEDULE("Schedule"),
    WEEK("Week"),
    MONTH("Month"),
    DAY("Day")
}

data class CalendarUiState(
    val selectedDate: Date = Date(),
    val selectedDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val viewMode: CalendarViewMode = CalendarViewMode.BENTO,
    val selectedProjectId: Long? = null, // null means All Projects
    val selectedPriority: TaskPriority? = null, // null means All Priorities
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val editingTask: TaskEntity? = null, // if non-null, opens task editor
    val isCreatingNewTask: Boolean = false,
    val ringingAlarmTask: TaskEntity? = null, // if non-null, full screen alarm modal
    val isSyncDialogOpen: Boolean = false,
    val isProjectManagerOpen: Boolean = false,
    val isSettingsDialogOpen: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.BENTO_GRID,
    val isNightLightWarmEnabled: Boolean = false,
    val syncPassphrase: String = "ChronoTaskSecure2026",
    val peerIpInput: String = "192.168.1.",
    val syncStatusMessage: String? = null,
    val isSyncing: Boolean = false,
    val exportedCodeText: String? = null
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = TaskRepository(db.taskDao(), db.projectDao())
    val p2pServer = P2PSyncServer(application, repository)
    val p2pClient = P2PSyncClient(repository)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tasks for UI
    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        rawTasks,
        _uiState
    ) { tasks, state ->
        tasks.filter { task ->
            if (task.isDeleted) return@filter false
            if (state.selectedProjectId != null && task.projectId != state.selectedProjectId) return@filter false
            if (state.selectedPriority != null && task.priority != state.selectedPriority) return@filter false
            if (state.searchQuery.isNotBlank()) {
                val q = state.searchQuery.trim().lowercase(Locale.getDefault())
                val matchTitle = task.title.lowercase(Locale.getDefault()).contains(q)
                val matchDesc = task.description.lowercase(Locale.getDefault()).contains(q)
                val matchProj = task.projectName.lowercase(Locale.getDefault()).contains(q)
                if (!matchTitle && !matchDesc && !matchProj) return@filter false
            }
            true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Observe broadcast alarm triggers to show in-app modal
        viewModelScope.launch {
            AlarmReceiver.alarmTriggerEvents.collect { triggeredTask ->
                _uiState.value = _uiState.value.copy(ringingAlarmTask = triggeredTask)
            }
        }
    }

    fun setSelectedDate(date: Date) {
        val dateStr = dateFormat.format(date)
        _uiState.value = _uiState.value.copy(selectedDate = date, selectedDateString = dateStr)
    }

    fun setViewMode(mode: CalendarViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setSelectedProject(projectId: Long?) {
        _uiState.value = _uiState.value.copy(selectedProjectId = projectId)
    }

    fun setSelectedPriority(priority: TaskPriority?) {
        _uiState.value = _uiState.value.copy(selectedPriority = priority)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSearch(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isSearching = isOpen, searchQuery = if (!isOpen) "" else _uiState.value.searchQuery)
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun toggleNightLight(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isNightLightWarmEnabled = enabled)
    }

    fun openTaskEditor(task: TaskEntity?) {
        _uiState.value = _uiState.value.copy(editingTask = task, isCreatingNewTask = (task == null))
    }

    fun openTaskEditorById(taskId: Long) {
        viewModelScope.launch {
            val task = repository.getTaskById(taskId)
            if (task != null) {
                _uiState.value = _uiState.value.copy(editingTask = task, isCreatingNewTask = false)
            }
        }
    }

    fun closeTaskEditor() {
        _uiState.value = _uiState.value.copy(editingTask = null, isCreatingNewTask = false)
    }

    fun openSyncDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSyncDialogOpen = open, syncStatusMessage = null)
    }

    fun openProjectManager(open: Boolean) {
        _uiState.value = _uiState.value.copy(isProjectManagerOpen = open)
    }

    fun openSettingsDialog(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSettingsDialogOpen = open)
    }

    fun updateSyncPassphrase(passphrase: String) {
        _uiState.value = _uiState.value.copy(syncPassphrase = passphrase)
        p2pServer.syncPassphrase = passphrase
    }

    fun updatePeerIpInput(ip: String) {
        _uiState.value = _uiState.value.copy(peerIpInput = ip)
    }

    // Quick Add Task from bottom bar
    fun quickAddTask(
        title: String,
        priority: TaskPriority = TaskPriority.P3_MEDIUM,
        dueHourOffset: Int = 1,
        isAlarm: Boolean = false,
        projectId: Long = 1
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.time = _uiState.value.selectedDate
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            cal.set(Calendar.HOUR_OF_DAY, (currentHour + dueHourOffset).coerceIn(0, 23))
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)

            val proj = allProjects.value.find { it.id == projectId } ?: allProjects.value.firstOrNull()
            val task = TaskEntity(
                title = title.trim(),
                description = "",
                dueTimestamp = cal.timeInMillis,
                dueDateString = dateFormat.format(cal.time),
                dueTimeString = timeFormat.format(cal.time),
                priority = priority,
                projectId = proj?.id ?: 1,
                projectName = proj?.name ?: "General",
                projectColorHex = proj?.colorHex ?: "#6366F1",
                isAlarmEnabled = isAlarm || (priority == TaskPriority.P1_URGENT),
                alarmSound = AlarmSoundType.ZEN_CHIME.id
            )
            val newId = repository.insertTask(task)
            val savedTask = task.copy(id = newId)
            if (savedTask.isAlarmEnabled) {
                AlarmScheduler.scheduleTaskAlarm(getApplication(), savedTask)
            }
            ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    // Save full task from editor
    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            if (task.id == 0L) {
                val newId = repository.insertTask(task)
                val savedTask = task.copy(id = newId)
                if (savedTask.isAlarmEnabled) {
                    AlarmScheduler.scheduleTaskAlarm(getApplication(), savedTask)
                }
            } else {
                repository.updateTask(task)
                if (task.isAlarmEnabled && !task.isCompleted) {
                    AlarmScheduler.scheduleTaskAlarm(getApplication(), task)
                } else {
                    AlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
                }
            }
            closeTaskEditor()
            ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            repository.completeTask(task, newCompleted)
            if (newCompleted) {
                AlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
            }
            ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            AlarmScheduler.cancelTaskAlarm(getApplication(), taskId)
            closeTaskEditor()
            ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun snoozeTask(taskId: Long, minutes: Int) {
        viewModelScope.launch {
            repository.snoozeTask(taskId, minutes)
            val updated = repository.getTaskById(taskId)
            if (updated != null) {
                AlarmScheduler.scheduleTaskAlarm(getApplication(), updated)
            }
            ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun snoozeRingingAlarm(minutes: Int) {
        val task = _uiState.value.ringingAlarmTask
        AlarmAudioPlayer.stopSound()
        if (task != null) {
            viewModelScope.launch {
                repository.snoozeTask(task.id, minutes)
                val updated = repository.getTaskById(task.id)
                if (updated != null) {
                    AlarmScheduler.scheduleTaskAlarm(getApplication(), updated)
                }
                _uiState.value = _uiState.value.copy(ringingAlarmTask = null)
                ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
            }
        } else {
            _uiState.value = _uiState.value.copy(ringingAlarmTask = null)
        }
    }

    fun dismissRingingAlarm(markCompleted: Boolean) {
        val task = _uiState.value.ringingAlarmTask
        AlarmAudioPlayer.stopSound()
        if (task != null) {
            viewModelScope.launch {
                if (markCompleted) {
                    repository.completeTask(task, true)
                }
                AlarmScheduler.cancelTaskAlarm(getApplication(), task.id)
                _uiState.value = _uiState.value.copy(ringingAlarmTask = null)
                ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
            }
        } else {
            _uiState.value = _uiState.value.copy(ringingAlarmTask = null)
        }
    }

    // Projects CRUD
    fun addProject(name: String, colorHex: String, iconName: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertProject(
                ProjectEntity(name = name.trim(), colorHex = colorHex, iconName = iconName)
            )
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    // P2P Server & Client Actions
    fun toggleP2PServer() {
        if (p2pServer.isServerRunning.value) {
            p2pServer.stopServer()
        } else {
            p2pServer.syncPassphrase = _uiState.value.syncPassphrase
            p2pServer.startServer()
        }
    }

    fun syncWithPeerNow() {
        val peerIp = _uiState.value.peerIpInput.trim()
        if (peerIp.isBlank()) {
            _uiState.value = _uiState.value.copy(syncStatusMessage = "Please enter peer IP address")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncStatusMessage = "Connecting to peer $peerIp:8989...")
            val result = p2pClient.syncWithPeer(
                peerHost = peerIp,
                port = 8989,
                passphrase = _uiState.value.syncPassphrase
            )
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncStatusMessage = "✅ Successfully synced & merged $count changes from peer!"
                )
                ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncStatusMessage = "❌ Sync failed: ${err.message}"
                )
            }
        }
    }

    fun exportEncryptedCode() {
        viewModelScope.launch {
            val code = p2pClient.exportEncryptedCode(_uiState.value.syncPassphrase)
            _uiState.value = _uiState.value.copy(exportedCodeText = code, syncStatusMessage = "Encrypted packet generated")
        }
    }

    fun importEncryptedCode(code: String) {
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            val result = p2pClient.importEncryptedCode(code, _uiState.value.syncPassphrase)
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncStatusMessage = "✅ Decrypted & merged $count items successfully!"
                )
                ChronoTaskWidgetProvider.updateAllWidgets(getApplication())
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncStatusMessage = "❌ Import error: ${err.message}"
                )
            }
        }
    }

    // Sound preview testing
    fun playSoundPreview(soundType: AlarmSoundType) {
        AlarmAudioPlayer.playSound(soundType, loop = false)
    }

    fun stopSoundPreview() {
        AlarmAudioPlayer.stopSound()
    }
}
