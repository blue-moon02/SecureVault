package com.securevault.app.presentation.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.securevault.app.domain.model.Task
import com.securevault.app.domain.usecase.task.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val deleteTarget: Task? = null,
    val snackMessage: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val searchTasksUseCase: SearchTasksUseCase,
    private val toggleTaskUseCase: ToggleTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    val tasksPaged: Flow<PagingData<Task>> = getTasksUseCase()
        .cachedIn(viewModelScope)

    val searchResults: StateFlow<List<Task>> = _uiState
        .map { it.searchQuery }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { searchTasksUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(q: String) {
        _uiState.update { it.copy(searchQuery = q, isSearchActive = q.isNotEmpty()) }
    }

    fun clearSearch() = _uiState.update { it.copy(searchQuery = "", isSearchActive = false) }

    fun toggleTask(id: Long) {
        viewModelScope.launch { toggleTaskUseCase(id) }
    }

    fun requestDelete(task: Task) = _uiState.update { it.copy(deleteTarget = task) }

    fun confirmDelete() {
        val task = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            deleteTaskUseCase(task)
            _uiState.update { it.copy(deleteTarget = null, snackMessage = "Task deleted") }
        }
    }

    fun cancelDelete() = _uiState.update { it.copy(deleteTarget = null) }
    fun clearSnack()   = _uiState.update { it.copy(snackMessage = null) }
}
