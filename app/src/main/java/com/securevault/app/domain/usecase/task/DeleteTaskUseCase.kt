package com.securevault.app.domain.usecase.task

import com.securevault.app.domain.model.Task
import com.securevault.app.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) = repository.deleteTask(task)
}
