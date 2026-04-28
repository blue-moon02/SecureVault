package com.securevault.app.presentation.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.app.domain.model.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onBack() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete Task") },
            text   = { Text("This task will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId <= 0L) "New Task" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    if (taskId > 0L) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Save, null,
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value         = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label         = { Text("Title *") },
                modifier      = Modifier.fillMaxWidth(),
                isError       = uiState.error?.contains("Title") == true,
                singleLine    = true
            )

            OutlinedTextField(
                value         = uiState.description,
                onValueChange = viewModel::onDescChange,
                label         = { Text("Description (optional)") },
                modifier      = Modifier.fillMaxWidth().defaultMinSize(minHeight = 100.dp),
                maxLines      = 6
            )

            // Priority selector
            Text("Priority", style = MaterialTheme.typography.labelLarge,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.values().forEach { p ->
                    FilterChip(
                        selected = uiState.priority == p,
                        onClick  = { viewModel.onPriorityChange(p) },
                        label    = { Text(p.label) }
                    )
                }
            }

            // Sensitive toggle
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Sensitive Task", style = MaterialTheme.typography.bodyMedium)
                    Text("Hidden in panic mode",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked         = uiState.isSensitive,
                    onCheckedChange = viewModel::onSensitiveChange
                )
            }

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                     style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}
