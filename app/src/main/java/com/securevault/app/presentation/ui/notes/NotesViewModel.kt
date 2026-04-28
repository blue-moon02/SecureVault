package com.securevault.app.presentation.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.securevault.app.domain.model.Note
import com.securevault.app.domain.usecase.note.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val searchResults: List<Note> = emptyList(),
    val deleteTarget: Note? = null,
    val snackMessage: String? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val getNotesUseCase: GetNotesUseCase,
    private val searchNotesUseCase: SearchNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    // Paged notes for the main list
    val notesPaged: Flow<PagingData<Note>> = getNotesUseCase()
        .cachedIn(viewModelScope)

    // Search with 300ms debounce
    val searchResults: StateFlow<List<Note>> = _uiState
        .map { it.searchQuery }
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query -> searchNotesUseCase(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearchActive = query.isNotEmpty()) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearchActive = false) }
    }

    fun requestDelete(note: Note) {
        _uiState.update { it.copy(deleteTarget = note) }
    }

    fun confirmDelete() {
        val note = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            deleteNoteUseCase(note)
            _uiState.update { it.copy(deleteTarget = null, snackMessage = "Note deleted") }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteTarget = null) }
    }

    fun clearSnack() {
        _uiState.update { it.copy(snackMessage = null) }
    }
}
