package com.securevault.app.presentation.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.app.domain.repository.NoteRepository
import com.securevault.app.domain.repository.SettingsRepository
import com.securevault.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultStats(
    val noteCount: Int = 0,
    val taskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val isPanicMode: Boolean = false
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val stats: StateFlow<VaultStats> = combine(
        noteRepository.getAllNotes(),
        taskRepository.getAllTasks(),
        settingsRepository.getSettings()
    ) { notes, tasks, settings ->
        VaultStats(
            noteCount          = notes.size,
            taskCount          = tasks.size,
            completedTaskCount = tasks.count { it.isCompleted },
            isPanicMode        = settings.panicModeEnabled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultStats())
}
