package com.securevault.app.domain.usecase.note

import com.securevault.app.domain.model.Note
import com.securevault.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(query: String): Flow<List<Note>> =
        if (query.isBlank()) repository.getAllNotes()
        else repository.searchNotes(query.trim())
}
