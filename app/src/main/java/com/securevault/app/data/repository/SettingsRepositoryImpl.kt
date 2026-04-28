package com.securevault.app.data.repository

import com.securevault.app.data.local.datastore.UserPreferencesDataStore
import com.securevault.app.domain.model.AppSettings
import com.securevault.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) : SettingsRepository {

    override fun getSettings(): Flow<AppSettings> = dataStore.appSettings

    override suspend fun updateSettings(settings: AppSettings) =
        dataStore.updateSettings(settings)

    override suspend fun updateLastUnlocked(timestamp: Long) =
        dataStore.updateLastUnlocked(timestamp)

    override suspend fun isLocked(timeoutSeconds: Int): Boolean {
        val lastUnlocked = dataStore.appSettings.first().lastUnlockedAt
        if (lastUnlocked == 0L) return true
        val elapsed = (System.currentTimeMillis() - lastUnlocked) / 1000L
        return elapsed >= timeoutSeconds
    }
}
