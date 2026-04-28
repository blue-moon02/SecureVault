package com.securevault.app.presentation.ui.tasks

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.securevault.app.domain.model.Priority
import com.securevault.app.domain.model.Task
import com.securevault.app.presentation.theme.*
import com.securevault.app.presentation.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedTasks = viewModel.tasksPaged.collectAsLazyPagingItems()
    val searchResults by viewModel.searchResults.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearSnack() }
    }

    uiState.deleteTarget?.let { task ->
        ConfirmDeleteDialog(
            title     = "Delete Task",
            message   = ""${task.title}" will be permanently deleted.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToDetail(-1L) },
                icon    = { Icon(Icons.Default.Add, null) },
                text    = { Text("New Task") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            VaultSearchBar(
                query         = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder   = "Search tasks…"
            )

            val displayList: List<Task> = if (uiState.isSearchActive) {
                searchResults
            } else emptyList()

            if (uiState.isSearchActive) {
                if (displayList.isEmpty()) {
                    EmptyState(Icons.Outlined.SearchOff, "No Results",
                        "No tasks match "${uiState.searchQuery}"")
                } else {
                    TaskList(
                        tasks    = displayList,
                        onToggle = viewModel::toggleTask,
                        onClick  = { onNavigateToDetail(it.id) },
                        onDelete = viewModel::requestDelete
                    )
                }
            } else {
                when {
                    pagedTasks.loadState.refresh is LoadState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    pagedTasks.itemCount == 0 -> {
                        EmptyState(Icons.Outlined.CheckCircle, "No Tasks Yet",
                            "Tap + to create your first task")
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pagedTasks.itemCount) { index ->
                                pagedTasks[index]?.let { task ->
                                    TaskItem(
                                        task     = task,
                                        onToggle = { viewModel.toggleTask(task.id) },
                                        onClick  = { onNavigateToDetail(task.id) },
                                        onDelete = { viewModel.requestDelete(task) },
                                        modifier = Modifier.animateItemPlacement(tween(220))
                                    )
                                }
                            }
                            if (pagedTasks.loadState.append is LoadState.Loading) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(8.dp),
                                        contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskList(
    tasks: List<Task>,
    onToggle: (Long) -> Unit,
    onClick: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tasks) { task ->
            TaskItem(task = task, onToggle = { onToggle(task.id) },
                onClick = { onClick(task) }, onDelete = { onDelete(task) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked         = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text  = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    ),
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text     = task.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                PriorityChip(priority = task.priority)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, null, Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}
