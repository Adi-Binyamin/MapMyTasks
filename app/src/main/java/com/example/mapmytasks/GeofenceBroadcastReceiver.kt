package com.example.mapmytasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "task_channel"
        private const val CHANNEL_NAME = "Task Notifications"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("TASK_NAME") ?: "Task Nearby"
        val taskId = intent.getStringExtra("TASK_ID") ?: return

        // שימוש ב-TaskManager המרכזי במקום פנייה ישירה ל-Firebase
        val currentUserId = TaskManager.getCurrentUserId() ?: return

        showNotification(context, taskName, taskId, currentUserId)
    }

    private fun showNotification(context: Context, taskName: String, taskId: String, userId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifications for tasks at locations"
            notificationManager.createNotificationChannel(channel)
        }

        // כפתור "בוצע"
        val markDoneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "MARK_DONE"
            putExtra("TASK_ID", taskId)
            putExtra("USER_ID", userId)
            putExtra("TASK_NAME", taskName)
        }

        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(), // שימוש ב-hashCode מונע דריסה של התראות שונות
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // כפתור "ביטול"
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "DISMISS"
            putExtra("TASK_ID", taskId)
            putExtra("USER_ID", userId)
            putExtra("TASK_NAME", taskName)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 1, // קוד בקשה ייחודי
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("MapMyTasks Reminder")
            .setContentText("You are near: $taskName")
            .setSmallIcon(android.R.drawable.ic_dialog_map) // אייקון של מפה מתאים יותר
            .addAction(android.R.drawable.checkbox_on_background, "✅ Done", markDonePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "❌ Dismiss", dismissPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // הפעלה של רטט וצליל
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}