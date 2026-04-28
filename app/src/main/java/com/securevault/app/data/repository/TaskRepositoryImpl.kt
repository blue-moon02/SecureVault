package com.securevault.app.data.repository

import androidx.paging.*
import com.securevault.app.data.local.db.AppDatabase
import com.securevault.app.data.local.db.entity.TaskEntity
import com.securevault.app.domain.model.Task
import com.securevault.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val db: AppDatabase
) : TaskRepository {

    private val dao = db.taskDao()

    override fun getTasksPaged(): Flow<PagingData<Task>> =
        Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false),
            pagingSourceFactory = { dao.getTasksPaged() }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun getAllTasks(): Flow<List<Task>> =
        dao.getAllTasks().map { list -> list.map { it.toDomain() } }

    override suspend fun getTaskById(id: Long): Task? =
        dao.getTaskById(id)?.toDomain()

    override fun searchTasks(query: String): Flow<List<Task>> =
        dao.searchTasks(query).map { list -> list.map { it.toDomain() } }

    override suspend fun saveTask(task: Task): Long =
        dao.insertTask(TaskEntity.fromDomain(task))

    override suspend fun updateTask(task: Task) =
        dao.updateTask(TaskEntity.fromDomain(task))

    override suspend fun toggleTask(id: Long) = dao.toggleTask(id)

    override suspend fun deleteTask(task: Task) =
        dao.deleteTask(TaskEntity.fromDomain(task))
}
