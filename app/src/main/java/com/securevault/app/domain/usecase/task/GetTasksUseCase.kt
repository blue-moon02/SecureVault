package com.securevault.app.domain.usecase.task

import androidx.paging.PagingData
import com.securevault.app.domain.model.Task
import com.securevault.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<PagingData<Task>> = repository.getTasksPaged()
}
