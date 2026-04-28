package com.securevault.app.domain.usecase.task

import com.securevault.app.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(id: Long) = repository.toggleTask(id)
}
