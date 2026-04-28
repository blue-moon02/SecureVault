package com.securevault.app.presentation.ui.documents

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securevault.app.domain.model.Document
import com.securevault.app.domain.usecase.document.DeleteDocumentUseCase
import com.securevault.app.domain.usecase.document.GetDocumentsUseCase
import com.securevault.app.domain.usecase.document.SaveDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class DocumentsUiState(
    val documents: List<Document> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val deleteTarget: Document? = null,
    val snackMessage: String? = null
)

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getDocumentsUseCase: GetDocumentsUseCase,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentsUiState())
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDocumentsUseCase()
                .catch { e -> Timber.e(e, "DocumentsVM: Error loading documents") }
                .collectLatest { docs ->
                    val query = _uiState.value.searchQuery
                    val filtered = if (query.isBlank()) docs
                        else docs.filter { it.name.contains(query, ignoreCase = true) }
                    _uiState.update { it.copy(documents = filtered) }
                }
        }
    }

    fun onSearchQueryChange(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    /**
     * Import a PDF from an external Uri into the app's private files dir.
     * In production: also encrypt the bytes with CryptoManager.
     */
    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                val fileName = resolveFileName(uri) ?: "document_${System.currentTimeMillis()}.pdf"
                val docsDir  = File(context.filesDir, "documents").also { it.mkdirs() }
                val destFile = File(docsDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }

                val sizeBytes = destFile.length()
                saveDocumentUseCase(
                    Document(
                        name      = fileName,
                        filePath  = destFile.absolutePath,
                        sizeBytes = sizeBytes
                    )
                )
                Timber.i("DocumentsVM: Imported $fileName (${sizeBytes}B)")
            }
            .onSuccess {
                _uiState.update { it.copy(isLoading = false, snackMessage = "Document imported") }
            }
            .onFailure { e ->
                Timber.e(e, "DocumentsVM: Import failed")
                _uiState.update { it.copy(isLoading = false, snackMessage = "Import failed: ${e.message}") }
            }
        }
    }

    fun requestDelete(doc: Document) = _uiState.update { it.copy(deleteTarget = doc) }

    fun confirmDelete() {
        val doc = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            runCatching {
                deleteDocumentUseCase(doc)
                File(doc.filePath).delete()  // Remove from private storage
            }
            _uiState.update { it.copy(deleteTarget = null, snackMessage = "Document deleted") }
        }
    }

    fun cancelDelete()  = _uiState.update { it.copy(deleteTarget = null) }
    fun clearSnack()    = _uiState.update { it.copy(snackMessage = null) }

    private fun resolveFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
        }
        return uri.lastPathSegment
    }
}
