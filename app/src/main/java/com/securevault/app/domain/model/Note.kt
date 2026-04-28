package com.securevault.app.domain.model

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val isSensitive: Boolean = false,
    val color: NoteColor = NoteColor.DEFAULT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)

enum class NoteColor(val argb: Long) {
    DEFAULT(0xFF1C1B1F),
    RED(0xFF6B2525),
    GREEN(0xFF1A4731),
    BLUE(0xFF1A2E5A),
    PURPLE(0xFF3B1A6B),
    YELLOW(0xFF6B5B1A),
}
