package com.securevault.app.domain.repository

import com.securevault.app.domain.model.Document
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<Document>>
    suspend fun getDocumentById(id: Long): Document?
    suspend fun saveDocument(document: Document): Long
    suspend fun updateDocument(document: Document)
    suspend fun deleteDocument(document: Document)
    fun searchDocuments(query: String): Flow<List<Document>>
}
