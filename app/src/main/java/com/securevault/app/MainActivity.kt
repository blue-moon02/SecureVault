package com.securevault.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.securevault.app.data.local.security.AutoLockManager
import com.securevault.app.domain.repository.SettingsRepository
import com.securevault.app.presentation.navigation.Screen
import com.securevault.app.presentation.navigation.VaultNavGraph
import com.securevault.app.presentation.theme.SecureVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var autoLockManager: AutoLockManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply screenshot protection based on persisted setting
        applyScreenshotProtection()

        setContent {
            val settings by settingsRepository.getSettings().collectAsState(
                initial = runBlocking { settingsRepository.getSettings().first() }
            )

            SecureVaultTheme(
                darkTheme    = settings.isDarkMode ?: isSystemInDarkTheme(),
                dynamicColor = settings.dynamicColorEnabled
            ) {
                val navController = rememberNavController()

                // Observe auto-lock state — redirect to Auth when locked
                val isLocked by autoLockManager.isLocked.collectAsState()
                LaunchedEffect(isLocked) {
                    if (isLocked && settings.isBiometricEnabled) {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        }
                    }
                }

                VaultNavGraph(
                    navController    = navController,
                    startDestination = if (settings.isBiometricEnabled)
                        Screen.Auth.route
                    else
                        Screen.Vault.route
                )
            }
        }
    }

    private fun applyScreenshotProtection() {
        // Read synchronously once at startup (DataStore is fast on first read)
        val screenshotProtectionEnabled = runBlocking {
            runCatching {
                settingsRepository.getSettings().first().screenshotProtectionEnabled
            }.getOrDefault(true)  // Default to protected
        }

        if (screenshotProtectionEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
