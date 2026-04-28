package com.securevault.app.domain.repository

import androidx.paging.PagingData
import com.securevault.app.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotesPaged(): Flow<PagingData<Note>>
    fun getAllNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun saveNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun deleteExpiredNotes(): Int
    suspend fun getNonSensitiveNotes(): List<Note>
}
