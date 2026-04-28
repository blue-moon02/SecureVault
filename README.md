# SecureVault — Architecture Reference

## Project Overview

SecureVault is a **privacy-first task and document manager** targeting mid-level Android engineer portfolios.  
It demonstrates **product polish**, **framework mastery**, and **real-world security patterns**.

---

## Tech Stack

| Layer           | Technology                                                          |
|-----------------|---------------------------------------------------------------------|
| **Language**    | Kotlin 2.1.20                                                       |
| **UI**          | Jetpack Compose + Material 3 + Dynamic Colors                       |
| **DI**          | Hilt 2.56                                                           |
| **Navigation**  | Navigation Compose (type-safe routes)                               |
| **Database**    | Room 2.7 + SQLCipher 4.5 (AES-256 encrypted DB)                     |
| **Async**       | Kotlin Coroutines + Flow                                            |
| **Paging**      | Paging 3 (cachedIn + staggered grid)                                |
| **Background**  | WorkManager (Hilt worker factory)                                   |
| **Preferences** | DataStore Preferences                                               |
| **Security**    | BiometricPrompt API + Android Keystore + EncryptedSharedPreferences |
| **PDF**         | iText7 (AES-256 password-protected export)                          |
| **Build**       | AGP 8.10 + KSP 2.1.20 + Version Catalogs                            |

---

## Architecture: Clean Architecture + MVVM (Feature-based)

```
app/
├── di/                          ← Hilt modules (Database, Repository, Worker)
├── data/
│   ├── local/
│   │   ├── db/                  ← Room database, DAOs, entities
│   │   │   ├── entity/          ← NoteEntity, TaskEntity, DocumentEntity
│   │   │   ├── dao/             ← NoteDao, TaskDao, DocumentDao
│   │   │   └── AppDatabase.kt   ← @Database with SQLCipher SupportFactory
│   │   ├── security/
│   │   │   ├── CryptoManager.kt           ← AES-256-GCM via Android Keystore
│   │   │   ├── BiometricHelper.kt         ← BiometricPrompt Flow wrapper
│   │   │   ├── EncryptedPreferencesManager.kt ← EncryptedSharedPreferences
│   │   │   ├── AutoLockManager.kt         ← ProcessLifecycle auto-lock
│   │   │   └── PdfExporter.kt             ← iText7 AES-256 PDF export
│   │   └── datastore/
│   │       └── UserPreferencesDataStore.kt ← DataStore<Preferences>
│   ├── repository/              ← Repository implementations
│   └── worker/
│       ├── CleanupWorker.kt     ← Deletes expired notes (weekly)
│       └── BackupWorker.kt      ← Local backup (daily)
├── domain/
│   ├── model/                   ← Pure Kotlin data classes (no Android deps)
│   ├── repository/              ← Repository interfaces
│   └── usecase/                 ← One use case per action
│       ├── note/
│       ├── task/
│       └── document/
└── presentation/
    ├── navigation/              ← Screen.kt sealed class + NavGraph.kt
    ├── theme/                   ← Color.kt, Type.kt, Theme.kt (M3 + Dynamic Color)
    └── ui/
        ├── auth/                ← BiometricPrompt, panic mode, failed-attempt counter
        ├── vault/               ← Home dashboard with stats
        ├── notes/               ← Paging 3 staggered grid, search, color picker
        ├── tasks/               ← Priority-sorted list, swipe-to-toggle
        ├── documents/           ← PDF import via SAF, encrypted storage
        ├── settings/            ← Biometric, auto-lock, Material You, screenshot protection
        └── components/          ← VaultSearchBar, EmptyState, PriorityChip, ConfirmDeleteDialog
```

---

## Key Security Patterns

### 1. Database Encryption (SQLCipher)
The Room database is opened with a **SQLCipher SupportFactory**. The 32-byte passphrase is
derived by encrypting a fixed nonce with an AES-256 key stored exclusively in the Android Keystore.
The key never leaves secure hardware; the passphrase is zeroed from memory immediately after use.

```kotlin
// DatabaseModule.kt
val masterKey = cryptoManager.getOrCreateDbKey()          // Keystore key
val encrypted = cryptoManager.encrypt(nonce, masterKey)    // AES-256-GCM
val passphrase = encrypted.ciphertext.copyOf(32)           // 32-byte DB key
val factory = SupportFactory(passphrase)
// ... open Room with factory ...
passphrase.fill(0)                                         // Zero memory
```

### 2. Biometric Authentication
`BiometricHelper` wraps `BiometricPrompt` in a `callbackFlow<BiometricResult>` that emits
exactly one terminal result per prompt. The UI collects it in a `LaunchedEffect`.

```kotlin
biometricHelper.authenticate(activity).collect { result ->
    when (result) {
        is BiometricResult.Success       -> viewModel.onAuthenticationSuccess()
        is BiometricResult.Failed        -> viewModel.onAuthenticationFailed()  // count + panic
        is BiometricResult.Error         -> viewModel.onAuthError(result.code, result.message)
        is BiometricResult.CryptoSuccess -> viewModel.onAuthenticationSuccess() // crypto unlock
    }
}
```

### 3. Panic Mode
After **5 consecutive biometric failures**, `AuthViewModel` sets a `panic_mode_enabled` flag in
`EncryptedSharedPreferences`. Screens that display sensitive content check this flag and
replace it with a placeholder before rendering.

### 4. Auto-Lock Timer
`AutoLockManager` implements `DefaultLifecycleObserver` and registers with
`ProcessLifecycleOwner`. On `onStop` it records a timestamp; on `onStart` it compares elapsed
time against the user's configured timeout. If exceeded, it emits `isLocked = true`, and
`MainActivity` navigates back to `AuthScreen`.

### 5. Screenshot Protection
`MainActivity.applyScreenshotProtection()` reads the DataStore setting synchronously on startup
(single cold-read) and calls `WindowManager.LayoutParams.FLAG_SECURE` before any content is
rendered.

### 6. Password-Protected PDF Export
`PdfExporter` uses iText7's `WriterProperties.setStandardEncryption()` with
`ENCRYPTION_AES_256`. The **user password** is required to open the PDF; the **owner password**
restricts editing. Exported files land in the app's private `files/exports/` directory.

---

## Paging 3 Pattern

Notes and Tasks both use **Paging 3** with Room's `PagingSource`:

```kotlin
// NoteDao
@Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
fun getNotesPaged(): PagingSource<Int, NoteEntity>

// NoteRepositoryImpl
Pager(PagingConfig(pageSize = 20)) { dao.getNotesPaged() }
    .flow
    .map { pagingData -> pagingData.map { it.toDomain() } }

// NotesViewModel — cachedIn survives rotation
val notesPaged = getNotesUseCase().cachedIn(viewModelScope)
```

Notes are displayed in a **`LazyVerticalStaggeredGrid`** (Compose Foundation); tasks use a
**`LazyColumn`** with priority-sorted ordering done in SQL.

---

## Search

All search uses **Flow debounce (300ms) + distinctUntilChanged** to avoid unnecessary DB hits:

```kotlin
_uiState
    .map { it.searchQuery }
    .debounce(300)
    .distinctUntilChanged()
    .flatMapLatest { searchNotesUseCase(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

SQL uses `LIKE '%' || :query || '%'` across title **and** content — giving you full-text search
without FTS5 setup overhead (add FTS5 for production if note bodies grow large).

---

## WorkManager Jobs

| Worker          | Schedule | Purpose                                                                               |
|-----------------|----------|---------------------------------------------------------------------------------------|
| `CleanupWorker` | Weekly   | Deletes notes whose `expiresAt` has passed                                            |
| `BackupWorker`  | Daily    | Creates a local JSON snapshot of non-sensitive notes in `files/backups/` (max 5 kept) |

Both workers use `@HiltWorker` + `@AssistedInject` and are registered via a custom
`Configuration.Provider` in `SecureVaultApp`.

---

## "Hire Me" Differentiators — Quick Reference

| Feature                    | File                                   | API                                                  |
|----------------------------|----------------------------------------|------------------------------------------------------|
| SQLCipher DB encryption    | `DatabaseModule.kt`                    | `net.sqlcipher.database.SupportFactory`              |
| Android Keystore key gen   | `CryptoManager.kt`                     | `KeyGenParameterSpec` + AES-256-GCM                  |
| Biometric prompt Flow      | `BiometricHelper.kt`                   | `BiometricPrompt` + `callbackFlow`                   |
| EncryptedSharedPreferences | `EncryptedPreferencesManager.kt`       | `androidx.security.crypto`                           |
| Auto-lock on background    | `AutoLockManager.kt`                   | `ProcessLifecycleOwner`                              |
| Screenshot block           | `MainActivity.kt`                      | `FLAG_SECURE`                                        |
| Panic mode                 | `AuthViewModel.kt`                     | Encrypted prefs counter                              |
| AES-256 PDF export         | `PdfExporter.kt`                       | iText7 `WriterProperties`                            |
| Material You               | `Theme.kt`                             | `dynamicDarkColorScheme` / `dynamicLightColorScheme` |
| Paging 3                   | `NoteRepositoryImpl`, `NotesViewModel` | `Pager`, `cachedIn`, `LazyPagingItems`               |
| Staggered grid             | `NotesScreen.kt`                       | `LazyVerticalStaggeredGrid`                          |
| Debounced search           | `NotesViewModel`, `TasksViewModel`     | `debounce` + `flatMapLatest`                         |
| WorkManager (Hilt)         | `CleanupWorker`, `BackupWorker`        | `@HiltWorker`, `@AssistedInject`                     |
| DataStore                  | `UserPreferencesDataStore.kt`          | `dataStore.edit {}`, typed keys                      |
