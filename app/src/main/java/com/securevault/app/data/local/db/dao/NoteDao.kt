package com.securevault.app.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.securevault.app.data.local.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // Paging: ordered pinned-first, then by updatedAt desc
    @Query("""
        SELECT * FROM notes
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun getNotesPaged(): PagingSource<Int, NoteEntity>

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("""
        SELECT * FROM notes
        WHERE title LIKE '%' || :query || '%'
           OR content LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredNotes(now: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM notes WHERE isSensitive = 0")
    suspend fun getNonSensitiveNotes(): List<NoteEntity>
}
