package com.securevault.app.domain.usecase.note

import com.securevault.app.domain.model.Note
import com.securevault.app.domain.repository.NoteRepository
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Long {
        require(note.title.isNotBlank()) { "Note title cannot be blank" }
        return if (note.id == 0L) {
            repository.saveNote(note)
        } else {
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
    }
}
