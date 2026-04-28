package com.securevault.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.securevault.app.domain.model.Priority
import com.securevault.app.domain.model.Task

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["updatedAt"]),
        Index(value = ["isCompleted"]),
        Index(value = ["dueDate"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = Priority.MEDIUM.name,
    val dueDate: Long? = null,
    val isSensitive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Task(
        id = id,
        title = title,
        description = description,
        isCompleted = isCompleted,
        priority = runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.MEDIUM),
        dueDate = dueDate,
        isSensitive = isSensitive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(task: Task) = TaskEntity(
            id = task.id,
            title = task.title,
            description = task.description,
            isCompleted = task.isCompleted,
            priority = task.priority.name,
            dueDate = task.dueDate,
            isSensitive = task.isSensitive,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt
        )
    }
}
