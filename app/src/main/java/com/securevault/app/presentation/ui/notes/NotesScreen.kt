package com.securevault.app.presentation.ui.notes

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.securevault.app.domain.model.Note
import com.securevault.app.domain.model.NoteColor
import com.securevault.app.presentation.ui.components.ConfirmDeleteDialog
import com.securevault.app.presentation.ui.components.EmptyState
import com.securevault.app.presentation.ui.components.VaultSearchBar
import com.securevault.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    onNavigateToDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState     by viewModel.uiState.collectAsState()
    val pagedNotes   = viewModel.notesPaged.collectAsLazyPagingItems()
    val searchResults by viewModel.searchResults.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar
    LaunchedEffect(uiState.snackMessage) {
        uiState.snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnack()
        }
    }

    // Delete dialog
    uiState.deleteTarget?.let { note ->
        ConfirmDeleteDialog(
            title   = "Delete Note",
            message = ""${note.title}" will be permanently deleted.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::cancelDelete
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title        = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
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
                text    = { Text("New Note") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            VaultSearchBar(
                query         = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder   = "Search notes…"
            )

            if (uiState.isSearchActive) {
                // Search results as a simple list
                if (searchResults.isEmpty()) {
                    EmptyState(
                        icon     = Icons.Outlined.SearchOff,
                        title    = "No Results",
                        subtitle = "No notes match "${uiState.searchQuery}""
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults.size) { i ->
                            NoteCard(
                                note      = searchResults[i],
                                onClick   = { onNavigateToDetail(searchResults[i].id) },
                                onDelete  = { viewModel.requestDelete(searchResults[i]) }
                            )
                        }
                    }
                }
            } else {
                // Staggered grid from Paging 3
                when {
                    pagedNotes.loadState.refresh is LoadState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    pagedNotes.itemCount == 0 -> {
                        EmptyState(
                            icon     = Icons.Outlined.Description,
                            title    = "No Notes Yet",
                            subtitle = "Tap + to create your first secure note"
                        )
                    }
                    else -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(pagedNotes.itemCount) { index ->
                                pagedNotes[index]?.let { note ->
                                    NoteCard(
                                        note     = note,
                                        onClick  = { onNavigateToDetail(note.id) },
                                        onDelete = { viewModel.requestDelete(note) },
                                        modifier = Modifier.animateItemPlacement(tween(250))
                                    )
                                }
                            }
                            if (pagedNotes.loadState.append is LoadState.Loading) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = androidx.compose.ui.Alignment.Center) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = noteColorToComposeColor(note.color)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pin indicator
                if (note.isPinned) {
                    Icon(Icons.Default.PushPin, null,
                         modifier = Modifier.size(14.dp),
                         tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.weight(1f))
                // Lock indicator
                if (note.isLocked) {
                    Icon(Icons.Default.Lock, null,
                         modifier = Modifier.size(14.dp),
                         tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(
                        onClick  = { showMenu = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text    = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text      = note.title,
                style     = MaterialTheme.typography.titleSmall,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
                color     = MaterialTheme.colorScheme.onSurface
            )

            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text      = note.content,
                    style     = MaterialTheme.typography.bodySmall,
                    maxLines  = 4,
                    overflow  = TextOverflow.Ellipsis,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sensitive badge
            if (note.isSensitive) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Sensitive",
                         style = MaterialTheme.typography.labelSmall,
                         modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                         color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun noteColorToComposeColor(color: NoteColor): Color = when (color) {
    NoteColor.DEFAULT -> Color(0xFF21262D)
    NoteColor.RED     -> NoteRed
    NoteColor.GREEN   -> NoteGreen
    NoteColor.BLUE    -> NoteBlue
    NoteColor.PURPLE  -> NotePurple
    NoteColor.YELLOW  -> NoteYellow
}
