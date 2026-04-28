package com.securevault.app.domain.usecase.document

import com.securevault.app.domain.model.Document
import com.securevault.app.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDocumentsUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    operator fun invoke(): Flow<List<Document>> = repository.getAllDocuments()
}
