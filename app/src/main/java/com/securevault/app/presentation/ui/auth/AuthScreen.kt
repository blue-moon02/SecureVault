package com.securevault.app.presentation.ui.auth

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.securevault.app.data.local.security.BiometricAvailability
import com.securevault.app.data.local.security.BiometricHelper
import com.securevault.app.data.local.security.BiometricResult
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    biometricHelper: BiometricHelper = androidx.hilt.navigation.compose.hiltViewModel<AuthViewModel>()
        .let { hiltViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Pulse animation for the lock icon
    val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )

    // Collect events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AuthEvent.AuthSuccess    -> onAuthenticated()
                is AuthEvent.PanicActivated -> { /* Show panic UI */ }
                is AuthEvent.ShowError      -> { /* Show snackbar */ }
            }
        }
    }

    // Auto-trigger biometric on entry
    LaunchedEffect(uiState.biometricAvailability) {
        if (uiState.biometricAvailability == BiometricAvailability.AVAILABLE) {
            triggerBiometric(context, viewModel)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated vault icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "SecureVault",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp).scale(scale)
            )

            Text(
                text = "SecureVault",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Your private space for sensitive notes,
tasks, and documents.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Failed attempts warning
            AnimatedVisibility(visible = uiState.failedAttempts > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null,
                            tint = MaterialTheme.colorScheme.error)
                        Text(
                            "${uiState.failedAttempts} failed attempt(s). " +
                            "${5 - uiState.failedAttempts} remaining before panic mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Biometric unlock button
            Button(
                onClick = {
                    coroutineScope.launch { triggerBiometric(context, viewModel) }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState.biometricAvailability == BiometricAvailability.AVAILABLE
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Unlock with Biometric")
            }

            if (uiState.biometricAvailability != BiometricAvailability.AVAILABLE) {
                Text(
                    text = when (uiState.biometricAvailability) {
                        BiometricAvailability.NONE_ENROLLED   -> "No biometric enrolled. Please set up fingerprint or face unlock in system settings."
                        BiometricAvailability.NO_HARDWARE     -> "No biometric hardware available."
                        else -> "Biometric authentication unavailable."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private suspend fun triggerBiometric(context: android.content.Context, viewModel: AuthViewModel) {
    val activity = context as? FragmentActivity ?: return
    val biometricHelper = BiometricHelper(context)

    biometricHelper.authenticate(
        activity = activity,
        title    = "Unlock SecureVault",
        subtitle = "Confirm your identity"
    ).collect { result ->
        when (result) {
            is BiometricResult.Success       -> viewModel.onAuthenticationSuccess()
            is BiometricResult.CryptoSuccess -> viewModel.onAuthenticationSuccess()
            is BiometricResult.Failed        -> viewModel.onAuthenticationFailed()
            is BiometricResult.Error         -> viewModel.onAuthError(result.code, result.message)
        }
    }
}
