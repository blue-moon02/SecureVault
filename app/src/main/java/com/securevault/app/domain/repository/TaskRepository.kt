package com.securevault.app.domain.repository

import androidx.paging.PagingData
import com.securevault.app.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasksPaged(): Flow<PagingData<Task>>
    fun getAllTasks(): Flow<List<Task>>
    suspend fun getTaskById(id: Long): Task?
    fun searchTasks(query: String): Flow<List<Task>>
    suspend fun saveTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun toggleTask(id: Long)
    suspend fun deleteTask(task: Task)
}
