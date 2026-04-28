package com.securevault.app.domain.usecase.note

import androidx.paging.PagingData
import com.securevault.app.domain.model.Note
import com.securevault.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNotesUseCase @Inject constructor(
    private val repository: NoteRepository
) {
    operator fun invoke(): Flow<PagingData<Note>> = repository.getNotesPaged()
}
