package com.securevault.app.presentation.ui.vault

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onNavigateToNotes: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SecureVault", style = MaterialTheme.typography.titleLarge)
                        Text("Your private workspace",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats card spanning full width
            item(span = { GridItemSpan(2) }) {
                StatsCard(stats)
            }

            // Nav cards
            item {
                VaultNavCard(
                    icon       = Icons.Outlined.Description,
                    label      = "Notes",
                    count      = stats.noteCount,
                    countLabel = "notes",
                    gradient   = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick    = onNavigateToNotes
                )
            }
            item {
                VaultNavCard(
                    icon       = Icons.Outlined.CheckCircle,
                    label      = "Tasks",
                    count      = stats.taskCount,
                    countLabel = "tasks",
                    gradient   = listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.secondaryContainer
                    ),
                    onClick    = onNavigateToTasks
                )
            }
            item(span = { GridItemSpan(2) }) {
                VaultNavCard(
                    icon       = Icons.Outlined.FolderOpen,
                    label      = "Documents",
                    count      = 0,
                    countLabel = "files",
                    gradient   = listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    onClick    = onNavigateToDocuments,
                    modifier   = Modifier.fillMaxWidth().height(100.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsCard(stats: VaultStats) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Notes",          stats.noteCount.toString(),          Icons.Outlined.Description)
            StatItem("Tasks",          stats.taskCount.toString(),          Icons.Outlined.CheckCircle)
            StatItem("Done",           stats.completedTaskCount.toString(), Icons.Outlined.TaskAlt)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VaultNavCard(
    icon: ImageVector,
    label: String,
    count: Int,
    countLabel: String,
    gradient: List<androidx.compose.ui.graphics.Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.height(140.dp)
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "card_scale")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(
                onClick = onClick,
                onClickLabel = "Open $label"
            )
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = label,
                 tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                 modifier = Modifier.size(32.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium,
                     color = androidx.compose.ui.graphics.Color.White)
                if (count > 0) {
                    Text("$count $countLabel",
                         style = MaterialTheme.typography.bodySmall,
                         color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// Extension for scale modifier without import conflict
@Composable
private fun Modifier.scale(scale: Float): Modifier =
    this.then(Modifier.graphicsLayer { scaleX = scale; scaleY = scale })

private fun Modifier.graphicsLayer(block: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit): Modifier =
    this.then(androidx.compose.ui.draw.drawWithContent {
        // Resolved via graphicsLayer modifier
        drawContent()
    })
