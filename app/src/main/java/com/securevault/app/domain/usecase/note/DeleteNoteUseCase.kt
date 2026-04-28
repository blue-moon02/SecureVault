package com.securevault.app.domain.usecase.note

import com.securevault.app.domain.model.Note
import com.securevault.app.domain.repository.NoteRepository
import javax.inject.Inject

class DeleteNoteUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    suspend operator fun invoke(note: Note) = repository.deleteNote(note)
}
