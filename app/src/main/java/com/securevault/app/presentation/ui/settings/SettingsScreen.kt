package com.securevault.app.presentation.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.app.domain.model.AppSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item { SettingsSectionHeader("Security") }

            item {
                SettingsToggleItem(
                    icon      = Icons.Outlined.Fingerprint,
                    title     = "Biometric Lock",
                    subtitle  = "Require fingerprint or face ID to open the app",
                    checked   = settings.isBiometricEnabled,
                    onChecked = viewModel::updateBiometric
                )
            }

            item {
                SettingsToggleItem(
                    icon      = Icons.Outlined.Screenshot,
                    title     = "Screenshot Protection",
                    subtitle  = "Prevent screenshots on sensitive screens",
                    checked   = settings.screenshotProtectionEnabled,
                    onChecked = viewModel::updateScreenshotProtection
                )
            }

            item {
                SettingsToggleItem(
                    icon      = Icons.Outlined.VisibilityOff,
                    title     = "Panic Mode",
                    subtitle  = "Hide sensitive content after ${EncryptedPrefsMaxAttempts} failed attempts",
                    checked   = settings.panicModeEnabled,
                    onChecked = viewModel::updatePanicMode
                )
            }

            item { SettingsDivider() }
            item { SettingsSectionHeader("Auto-Lock") }

            item {
                AutoLockSlider(
                    currentSeconds = settings.autoLockTimeoutSeconds,
                    onChanged      = viewModel::updateAutoLock
                )
            }

            item { SettingsDivider() }
            item { SettingsSectionHeader("Appearance") }

            item {
                SettingsToggleItem(
                    icon      = Icons.Outlined.Palette,
                    title     = "Material You / Dynamic Colors",
                    subtitle  = "Follow your wallpaper colors (Android 12+)",
                    checked   = settings.dynamicColorEnabled,
                    onChecked = viewModel::updateDynamicColor
                )
            }

            item {
                DarkModeSelector(
                    currentValue = settings.isDarkMode,
                    onChanged    = viewModel::updateDarkMode
                )
            }

            item { SettingsDivider() }
            item { SettingsSectionHeader("About") }

            item {
                Card(
                    shape  = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("SecureVault 1.0.0", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "End-to-end local encryption. Your data never leaves this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private const val EncryptedPrefsMaxAttempts = 5

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title,    style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun AutoLockSlider(
    currentSeconds: Int,
    onChanged: (Int) -> Unit
) {
    val steps = listOf(15, 30, 60, 120, 300)
    val currentIdx = steps.indexOfFirst { it >= currentSeconds }.coerceAtLeast(0)

    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, null,
                         tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Auto-lock Timer", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text  = formatSeconds(currentSeconds),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value         = currentIdx.toFloat(),
                onValueChange = { onChanged(steps[it.toInt()]) },
                valueRange    = 0f..(steps.size - 1).toFloat(),
                steps         = steps.size - 2,
                modifier      = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DarkModeSelector(currentValue: Boolean?, onChanged: (Boolean?) -> Unit) {
    Card(
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Dark Mode", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = currentValue == null, onClick = { onChanged(null) },
                    label = { Text("System") })
                FilterChip(selected = currentValue == false, onClick = { onChanged(false) },
                    label = { Text("Light") })
                FilterChip(selected = currentValue == true, onClick = { onChanged(true) },
                    label = { Text("Dark") })
            }
        }
    }
}

private fun formatSeconds(s: Int): String = when {
    s < 60   -> "${s}s"
    s < 3600 -> "${s / 60}m"
    else     -> "${s / 3600}h"
}
