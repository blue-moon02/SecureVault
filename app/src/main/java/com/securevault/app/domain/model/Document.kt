package com.securevault.app.domain.model

data class Document(
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val mimeType: String = "application/pdf",
    val sizeBytes: Long = 0,
    val isSensitive: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
