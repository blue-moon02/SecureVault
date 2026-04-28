package com.securevault.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevault.app.domain.model.Document

@Entity(
    tableName = "documents",
    indices = [Index(value = ["updatedAt"])]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val mimeType: String = "application/pdf",
    val sizeBytes: Long = 0,
    val isSensitive: Boolean = false,
    val tags: String = "",   // Comma-separated
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Document(
        id = id,
        name = name,
        filePath = filePath,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        isSensitive = isSensitive,
        tags = tags.split(",").filter { it.isNotBlank() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(doc: Document) = DocumentEntity(
            id = doc.id,
            name = doc.name,
            filePath = doc.filePath,
            mimeType = doc.mimeType,
            sizeBytes = doc.sizeBytes,
            isSensitive = doc.isSensitive,
            tags = doc.tags.joinToString(","),
            createdAt = doc.createdAt,
            updatedAt = doc.updatedAt
        )
    }
}
