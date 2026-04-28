package com.securevault.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevault.app.domain.model.Note
import com.securevault.app.domain.model.NoteColor

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["isPinned"]),
        Index(value = ["isSensitive"])
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isSensitive: Boolean = false,
    val color: String = NoteColor.DEFAULT.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
) {
    fun toDomain() = Note(
        id = id,
        title = title,
        content = content,
        isPinned = isPinned,
        isLocked = isLocked,
        isSensitive = isSensitive,
        color = runCatching { NoteColor.valueOf(color) }.getOrDefault(NoteColor.DEFAULT),
        createdAt = createdAt,
        updatedAt = updatedAt,
        expiresAt = expiresAt
    )

    companion object {
        fun fromDomain(note: Note) = NoteEntity(
            id = note.id,
            title = note.title,
            content = note.content,
            isPinned = note.isPinned,
            isLocked = note.isLocked,
            isSensitive = note.isSensitive,
            color = note.color.name,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            expiresAt = note.expiresAt
        )
    }
}
