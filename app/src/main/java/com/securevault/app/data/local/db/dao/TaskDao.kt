package com.securevault.app.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.securevault.app.data.local.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("""
        SELECT * FROM tasks
        ORDER BY isCompleted ASC,
                 CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
                 updatedAt DESC
    """)
    fun getTasksPaged(): PagingSource<Int, TaskEntity>

    @Query("""
        SELECT * FROM tasks
        ORDER BY isCompleted ASC,
                 CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END ASC,
                 updatedAt DESC
    """)
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("""
        SELECT * FROM tasks
        WHERE title LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = NOT isCompleted, updatedAt = :now WHERE id = :id")
    suspend fun toggleTask(id: Long, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}
