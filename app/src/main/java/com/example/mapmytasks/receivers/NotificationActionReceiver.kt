package com.example.mapmytasks.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.mapmytasks.data.TaskManager
import com.example.mapmytasks.models.TaskStatus

class NotificationActionReceiver : BroadcastReceiver() {

    // Handles actions from notification buttons
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val userId = intent.getStringExtra("USER_ID") ?: return

        // Close the notification immediately when a button is clicked
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