package com.securevault.app.presentation.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.app.domain.model.Note
import com.securevault.app.domain.model.NoteColor
import com.securevault.app.domain.usecase.note.DeleteNoteUseCase
import com.securevault.app.domain.usecase.note.GetNoteByIdUseCase
import com.securevault.app.domain.usecase.note.SaveNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailUiState(
    val noteId: Long = -1L,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isSensitive: Boolean = false,
    val color: NoteColor = NoteColor.DEFAULT,
    val expiresAt: Long? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase
) : ViewModel() {

    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L

    private val _uiState = MutableStateFlow(NoteDetailUiState(noteId = noteId))
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    init {
        if (noteId > 0L) loadNote(noteId)
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { getNoteByIdUseCase(id) }
                .onSuccess { note ->
                    note?.let { n ->
                        _uiState.update { it.copy(
                            title       = n.title,
                            content     = n.content,
                            isPinned    = n.isPinned,
                            isLocked    = n.isLocked,
                            isSensitive = n.isSensitive,
                            color       = n.color,
                            expiresAt   = n.expiresAt,
                            isLoading   = false
                        )}
                    } ?: _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun onTitleChange(title: String)     = _uiState.update { it.copy(title = title) }
    fun onContentChange(content: String) = _uiState.update { it.copy(content = content) }
    fun onPinnedChange(pinned: Boolean)  = _uiState.update { it.copy(isPinned = pinned) }
    fun onLockedChange(locked: Boolean)  = _uiState.update { it.copy(isLocked = locked) }
    fun onSensitiveChange(s: Boolean)    = _uiState.update { it.copy(isSensitive = s) }
    fun onColorChange(c: NoteColor)      = _uiState.update { it.copy(color = c) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Title cannot be empty") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                saveNoteUseCase(
                    Note(
                        id          = state.noteId.takeIf { it > 0L } ?: 0L,
                        title       = state.title.trim(),
                        content     = state.content,
                        isPinned    = state.isPinned,
                        isLocked    = state.isLocked,
                        isSensitive = state.isSensitive,
                        color       = state.color,
                        expiresAt   = state.expiresAt
                    )
                )
            }
            .onSuccess { _uiState.update { it.copy(isLoading = false, isSaved = true) } }
            .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun delete() {
        val state = _uiState.value
        if (state.noteId <= 0L) return
        viewModelScope.launch {
            deleteNoteUseCase(
                Note(id = state.noteId, title = state.title, content = state.content)
            )
            _uiState.update { it.copy(isSaved = true) } // reuse flag to navigate back
        }
    }
}
