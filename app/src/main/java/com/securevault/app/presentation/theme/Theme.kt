package com.securevault.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary              = VaultPrimary,
    onPrimary            = VaultOnPrimary,
    primaryContainer     = VaultPrimaryContainer,
    onPrimaryContainer   = VaultOnPrimaryContainer,
    secondary            = VaultSecondary,
    onSecondary          = VaultOnSecondary,
    secondaryContainer   = VaultSecondaryContainer,
    onSecondaryContainer = VaultOnSecondaryContainer,
    tertiary             = VaultTertiary,
    onTertiary           = VaultOnTertiary,
    tertiaryContainer    = VaultTertiaryContainer,
    onTertiaryContainer  = VaultOnTertiaryContainer,
    background           = VaultBackground,
    onBackground         = VaultOnBackground,
    surface              = VaultSurface,
    onSurface            = VaultOnSurface,
    surfaceVariant       = VaultSurfaceVariant,
    onSurfaceVariant     = VaultOnSurfaceVariant,
    outline              = VaultOutline,
    outlineVariant       = VaultOutlineVariant,
    error                = VaultError,
    onError              = VaultOnError,
    errorContainer       = VaultErrorContainer,
    onErrorContainer     = VaultOnErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary    = VaultPrimaryLight,
    background = VaultBackgroundLight,
    surface    = VaultSurfaceLight,
    onSurface  = VaultOnSurfaceLight,
)

@Composable
fun SecureVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Material You dynamic colors (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Edge-to-edge + status bar tinting
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = VaultTypography,
        content     = content
    )
}
