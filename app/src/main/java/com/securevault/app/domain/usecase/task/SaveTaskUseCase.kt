package com.securevault.app.domain.usecase.task

import com.securevault.app.domain.model.Task
import com.securevault.app.domain.repository.TaskRepository
import javax.inject.Inject

class SaveTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Long {
        require(task.title.isNotBlank()) { "Task title cannot be blank" }
        return if (task.id == 0L) {
            repository.saveTask(task)
        } else {
            repository.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
            task.id
        }
    }
}
