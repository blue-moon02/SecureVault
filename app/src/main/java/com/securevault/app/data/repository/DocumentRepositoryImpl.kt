package com.securevault.app.data.repository

import com.securevault.app.data.local.db.AppDatabase
import com.securevault.app.data.local.db.entity.DocumentEntity
import com.securevault.app.domain.model.Document
import com.securevault.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : DocumentRepository {

    private val dao = db.documentDao()

    override fun getAllDocuments(): Flow<List<Document>> =
        dao.getAllDocuments().map { list -> list.map { it.toDomain() } }

    override suspend fun getDocumentById(id: Long): Document? =
        dao.getDocumentById(id)?.toDomain()

    override suspend fun saveDocument(document: Document): Long =
        dao.insertDocument(DocumentEntity.fromDomain(document))

    override suspend fun updateDocument(document: Document) =
        dao.updateDocument(DocumentEntity.fromDomain(document))

    override suspend fun deleteDocument(document: Document) =
        dao.deleteDocument(DocumentEntity.fromDomain(document))

    override fun searchDocuments(query: String): Flow<List<Document>> =
        dao.searchDocuments(query).map { list -> list.map { it.toDomain() } }
}
