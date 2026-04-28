package com.securevault.app.domain.usecase.task

import com.securevault.app.domain.model.Task
import com.securevault.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(query: String): Flow<List<Task>> =
        if (query.isBlank()) repository.getAllTasks()
        else repository.searchTasks(query.trim())
}
