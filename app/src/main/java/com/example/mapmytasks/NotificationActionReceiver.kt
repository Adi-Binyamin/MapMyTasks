package com.example.mapmytasks

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {

    // Handles actions from notification buttons
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val userId = intent.getStringExtra("USER_ID") ?: return

        // סגירת ההתראה מיד כשלוחצים על אחד הכפתורים
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.hashCode())

        when (intent.action) {
            "MARK_DONE" -> {
                TaskManager.updateTaskStatus(userId, taskId, TaskStatus.DONE,
                    onSuccess = {
                        Toast.makeText(context, "משימה בוצעה ✅", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "שגיאה בעדכון: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            "DISMISS" -> {
                TaskManager.updateTaskStatus(userId, taskId, TaskStatus.MISSED,
                    onSuccess = {
                        Toast.makeText(context, "משימה נדחתה ❌", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(context, "שגיאה בעדכון: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}