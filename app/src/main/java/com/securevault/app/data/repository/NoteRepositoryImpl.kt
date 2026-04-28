package com.securevault.app.data.repository

import androidx.paging.*
import com.securevault.app.data.local.db.AppDatabase
import com.securevault.app.data.local.db.entity.NoteEntity
import com.securevault.app.domain.model.Note
import com.securevault.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : NoteRepository {

    private val dao = db.noteDao()

    override fun getNotesPaged(): Flow<PagingData<Note>> =
        Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false),
            pagingSourceFactory = { dao.getNotesPaged() }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun getAllNotes(): Flow<List<Note>> =
        dao.getAllNotes().map { list -> list.map { it.toDomain() } }

    override suspend fun getNoteById(id: Long): Note? =
        dao.getNoteById(id)?.toDomain()

    override fun searchNotes(query: String): Flow<List<Note>> =
        dao.searchNotes(query).map { list -> list.map { it.toDomain() } }

    override suspend fun saveNote(note: Note): Long =
        dao.insertNote(NoteEntity.fromDomain(note))

    override suspend fun updateNote(note: Note) =
        dao.updateNote(NoteEntity.fromDomain(note))

    override suspend fun deleteNote(note: Note) =
        dao.deleteNote(NoteEntity.fromDomain(note))

    override suspend fun deleteExpiredNotes(): Int =
        dao.deleteExpiredNotes()

    override suspend fun getNonSensitiveNotes(): List<Note> =
        dao.getNonSensitiveNotes().map { it.toDomain() }
}
