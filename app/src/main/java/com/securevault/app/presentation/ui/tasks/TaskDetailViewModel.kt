package com.securevault.app.presentation.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.app.domain.model.Priority
import com.securevault.app.domain.model.Task
import com.securevault.app.domain.usecase.task.DeleteTaskUseCase
import com.securevault.app.domain.usecase.task.GetTasksUseCase
import com.securevault.app.domain.usecase.task.SaveTaskUseCase
import com.securevault.app.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val taskId: Long = -1L,
    val title: String = "",
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long? = null,
    val isSensitive: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: -1L
    private val _uiState = MutableStateFlow(TaskDetailUiState(taskId = taskId))
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init { if (taskId > 0L) loadTask(taskId) }

    private fun loadTask(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            taskRepository.getTaskById(id)?.let { t ->
                _uiState.update { it.copy(
                    title       = t.title,
                    description = t.description,
                    priority    = t.priority,
                    dueDate     = t.dueDate,
                    isSensitive = t.isSensitive,
                    isLoading   = false
                )}
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onTitleChange(v: String)       = _uiState.update { it.copy(title = v) }
    fun onDescChange(v: String)        = _uiState.update { it.copy(description = v) }
    fun onPriorityChange(v: Priority)  = _uiState.update { it.copy(priority = v) }
    fun onSensitiveChange(v: Boolean)  = _uiState.update { it.copy(isSensitive = v) }

    fun save() {
        val s = _uiState.value
        if (s.title.isBlank()) { _uiState.update { it.copy(error = "Title required") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                saveTaskUseCase(Task(
                    id          = s.taskId.takeIf { it > 0L } ?: 0L,
                    title       = s.title.trim(),
                    description = s.description,
                    priority    = s.priority,
                    dueDate     = s.dueDate,
                    isSensitive = s.isSensitive
                ))
            }
            .onSuccess { _uiState.update { it.copy(isLoading = false, isSaved = true) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun delete() {
        val s = _uiState.value
        if (s.taskId <= 0L) return
        viewModelScope.launch {
            deleteTaskUseCase(Task(id = s.taskId, title = s.title))
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
