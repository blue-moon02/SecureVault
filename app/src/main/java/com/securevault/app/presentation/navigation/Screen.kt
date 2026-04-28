package com.securevault.app.presentation.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Auth : Screen("auth")

    // Main sections
    data object Vault     : Screen("vault")
    data object Notes     : Screen("notes")
    data object Tasks     : Screen("tasks")
    data object Documents : Screen("documents")
    data object Settings  : Screen("settings")

    // Detail screens
    data object NoteDetail     : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long = -1L) = "note_detail/$noteId"
    }
    data object TaskDetail     : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Long = -1L) = "task_detail/$taskId"
    }
    data object DocumentDetail : Screen("document_detail/{documentId}") {
        fun createRoute(documentId: Long) = "document_detail/$documentId"
    }
}
