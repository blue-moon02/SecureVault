package com.securevault.app.domain.usecase.document

import com.securevault.app.domain.model.Document
import com.securevault.app.domain.repository.DocumentRepository
import javax.inject.Inject

class DeleteDocumentUseCase @Inject constructor(
    private val repository: DocumentRepository
) {
    suspend operator fun invoke(document: Document) = repository.deleteDocument(document)
}
