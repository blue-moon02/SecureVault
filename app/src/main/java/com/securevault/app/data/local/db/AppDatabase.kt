package com.securevault.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.securevault.app.data.local.db.dao.DocumentDao
import com.securevault.app.data.local.db.dao.NoteDao
import com.securevault.app.data.local.db.dao.TaskDao
import com.securevault.app.data.local.db.entity.DocumentEntity
import com.securevault.app.data.local.db.entity.NoteEntity
import com.securevault.app.data.local.db.entity.TaskEntity

@Database(
    entities = [NoteEntity::class, TaskEntity::class, DocumentEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun documentDao(): DocumentDao

    companion object {
        const val DATABASE_NAME = "securevault.db"
    }
}
