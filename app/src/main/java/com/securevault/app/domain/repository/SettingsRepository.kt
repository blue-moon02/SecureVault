package com.securevault.app.domain.repository

import com.securevault.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateLastUnlocked(timestamp: Long)
    suspend fun isLocked(timeoutSeconds: Int): Boolean
}
