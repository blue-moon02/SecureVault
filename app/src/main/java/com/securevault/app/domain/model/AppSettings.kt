package com.securevault.app.domain.model

data class AppSettings(
    val isBiometricEnabled: Boolean = true,
    val autoLockTimeoutSeconds: Int = 30,
    val panicModeEnabled: Boolean = false,
    val dynamicColorEnabled: Boolean = true,
    val screenshotProtectionEnabled: Boolean = true,
    val lastUnlockedAt: Long = 0L,
    val isDarkMode: Boolean? = null
)
