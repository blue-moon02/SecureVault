package com.securevault.app.presentation.ui.notes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.app.domain.model.NoteColor
import com.securevault.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentFocus = remember { FocusRequester() }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Navigate back after save or delete
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    // Auto-focus content for new notes
    LaunchedEffect(noteId) {
        if (noteId <= 0L) {
            kotlinx.coroutines.delay(200)
            runCatching { contentFocus.requestFocus() }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete Note") },
            text   = { Text("This note will be permanently deleted.") },
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

    val cardBg = noteColorToDetailBg(uiState.color)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { viewModel.save(); /* save on back */ }) {
                        Icon(Icons.Default.ArrowBack, "Back & Save")
                    }
                },
                actions = {
                    // Pin toggle
                    IconButton(onClick = { viewModel.onPinnedChange(!uiState.isPinned) }) {
                        Icon(
                            if (uiState.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Toggle Pin",
                            tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Lock toggle
                    IconButton(onClick = { viewModel.onLockedChange(!uiState.isLocked) }) {
                        Icon(
                            if (uiState.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Toggle Lock",
                            tint = if (uiState.isLocked) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Color picker
                    IconButton(onClick = { showColorPicker = !showColorPicker }) {
                        Icon(Icons.Default.Palette, contentDescription = "Color")
                    }
                    // Delete (only for existing notes)
                    if (noteId > 0L) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                 tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    // Save
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Default.Save, contentDescription = "Save",
                             tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        },
        containerColor = cardBg
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Colour picker row
            AnimatedVisibility(
                visible = showColorPicker,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                NoteColorPicker(
                    selected    = uiState.color,
                    onColorPick = { viewModel.onColorChange(it); showColorPicker = false }
                )
            }

            // Sensitive toggle chip
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilterChip(
                    selected = uiState.isSensitive,
                    onClick  = { viewModel.onSensitiveChange(!uiState.isSensitive) },
                    label    = { Text("Sensitive") },
                    leadingIcon = {
                        if (uiState.isSensitive) Icon(Icons.Default.VisibilityOff, null,
                            Modifier.size(16.dp))
                        else Icon(Icons.Default.Visibility, null, Modifier.size(16.dp))
                    }
                )
            }

            // Title field
            BasicTextField(
                value        = uiState.title,
                onValueChange = viewModel::onTitleChange,
                textStyle    = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { inner ->
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        if (uiState.title.isEmpty()) {
                            Text("Title", style = MaterialTheme.typography.headlineMedium,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Content field
            BasicTextField(
                value         = uiState.content,
                onValueChange = viewModel::onContentChange,
                textStyle     = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { inner ->
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        if (uiState.content.isEmpty()) {
                            Text("Write your note here…",
                                 style = MaterialTheme.typography.bodyLarge,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp)
                    .focusRequester(contentFocus)
            )

            // Error
            uiState.error?.let { err ->
                Text(
                    text     = err,
                    color    = MaterialTheme.colorScheme.error,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun NoteColorPicker(
    selected: NoteColor,
    onColorPick: (NoteColor) -> Unit
) {
    val colors = NoteColor.values()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        colors.forEach { noteColor ->
            val composeColor = noteColorToDetailBg(noteColor)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(composeColor)
                    .then(
                        if (selected == noteColor)
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier
                    )
                    .clickable { onColorPick(noteColor) }
            )
        }
    }
}

// Make the inner text field work — re-export as public alias
@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    decorationBox: @Composable (@Composable () -> Unit) -> Unit = { it() }
) {
    androidx.compose.foundation.text.BasicTextField(
        value          = value,
        onValueChange  = onValueChange,
        modifier       = modifier,
        textStyle      = textStyle,
        decorationBox  = decorationBox
    )
}

private fun noteColorToDetailBg(color: NoteColor): Color = when (color) {
    NoteColor.DEFAULT -> Color(0xFF161B22)
    NoteColor.RED     -> Color(0xFF2D1515)
    NoteColor.GREEN   -> Color(0xFF0D2318)
    NoteColor.BLUE    -> Color(0xFF0D1629)
    NoteColor.PURPLE  -> Color(0xFF1A0D33)
    NoteColor.YELLOW  -> Color(0xFF332B0D)
}
