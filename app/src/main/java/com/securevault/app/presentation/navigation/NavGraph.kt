package com.securevault.app.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.securevault.app.presentation.ui.auth.AuthScreen
import com.securevault.app.presentation.ui.documents.DocumentsScreen
import com.securevault.app.presentation.ui.notes.NoteDetailScreen
import com.securevault.app.presentation.ui.notes.NotesScreen
import com.securevault.app.presentation.ui.settings.SettingsScreen
import com.securevault.app.presentation.ui.tasks.TaskDetailScreen
import com.securevault.app.presentation.ui.tasks.TasksScreen
import com.securevault.app.presentation.ui.vault.VaultScreen

@Composable
fun VaultNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Auth.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220))
        },
        exitTransition = {
            fadeOut(tween(180)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(180))
        },
        popEnterTransition = {
            fadeIn(tween(220)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220))
        },
        popExitTransition = {
            fadeOut(tween(180)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(180))
        }
    ) {
        // Auth screen
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Screen.Vault.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // Vault (home)
        composable(Screen.Vault.route) {
            VaultScreen(
                onNavigateToNotes     = { navController.navigate(Screen.Notes.route) },
                onNavigateToTasks     = { navController.navigate(Screen.Tasks.route) },
                onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                onNavigateToSettings  = { navController.navigate(Screen.Settings.route) }
            )
        }

        // Notes list
        composable(Screen.Notes.route) {
            NotesScreen(
                onNavigateToDetail = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Note detail / editor
        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
            NoteDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }

        // Tasks list
        composable(Screen.Tasks.route) {
            TasksScreen(
                onNavigateToDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Task detail / editor
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
            TaskDetailScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() }
            )
        }

        // Documents
        composable(Screen.Documents.route) {
            DocumentsScreen(onBack = { navController.popBackStack() })
        }

        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
