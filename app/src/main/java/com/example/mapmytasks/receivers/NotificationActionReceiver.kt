package com.example.mapmytasks.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.mapmytasks.data.TaskManager
import com.example.mapmytasks.models.TaskStatus

/**
 * NotificationActionReceiver listens for broadcast intents triggered by notification action buttons.
 * It handles user interactions directly from the notification tray, such as marking a task as done or dismissing it.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    // Processes the action intents from notification buttons and updates the task status in Firestore accordingly.
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val userId = intent.getStringExtra("USER_ID") ?: return

        // Closes the notification immediately when a button is clicked to provide instant UI feedback.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.hashCode())

        when (intent.action) {
            "MARK_DONE" -> {
                TaskManager.updateTaskStatus(userId, taskId, TaskStatus.DONE,
                    onSuccess = {
                        Toast.makeText(context, "Task completed ✅", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "Update error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            "DISMISS" -> {
                TaskManager.updateTaskStatus(userId, taskId, TaskStatus.MISSED,
                    onSuccess = {
                        Toast.makeText(context, "Task dismissed ❌", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "Update error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}