package com.securevault.app.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.app.domain.model.Priority
import com.securevault.app.presentation.theme.*

@Composable
fun PriorityChip(priority: Priority, modifier: Modifier = Modifier) {
    val (label, color) = when (priority) {
        Priority.HIGH   -> "High"   to PriorityHigh
        Priority.MEDIUM -> "Medium" to PriorityMedium
        Priority.LOW    -> "Low"    to PriorityLow
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
