package com.securevault.app.di

import android.content.Context
import androidx.room.Room
import com.securevault.app.data.local.db.AppDatabase
import com.securevault.app.data.local.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): AppDatabase {
        // Derive a stable 32-byte passphrase from the Keystore-protected key.
        // The key material never leaves the secure element; we encrypt a fixed
        // nonce to get the DB passphrase deterministically.
        val masterKey = cryptoManager.getOrCreateDbKey()
        val nonce = "securevault_db_nonce_v1".toByteArray()
        val encrypted = cryptoManager.encrypt(nonce, masterKey)
        val passphrase = encrypted.ciphertext.copyOf(32) // 256-bit passphrase

        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigrationFrom()   // Replace with proper migrations in prod
            .build()
            .also { passphrase.fill(0) }            // Zero out passphrase from memory
    }
}
