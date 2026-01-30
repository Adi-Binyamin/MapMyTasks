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

    // This runs when the phone gets a signal that we are near a task location
    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("TASK_NAME") ?: return
        val taskId = intent.getStringExtra("TASK_ID") ?: ""
        val userId = intent.getStringExtra("USER_ID") ?: ""

        showNotification(context, taskName, taskId, userId)
    }

    // Create and show a notification with "Done" and "Dismiss" buttons
    private fun showNotification(context: Context, taskName: String, taskId: String, userId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifications for tasks at locations"
            notificationManager.createNotificationChannel(channel)
        }

        val markDoneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "MARK_DONE"
            putExtra("TASK_ID", taskId)
            putExtra("USER_ID", userId)
            putExtra("TASK_NAME", taskName)
        }

        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "DISMISS"
            putExtra("TASK_ID", taskId)
            putExtra("USER_ID", userId)
            putExtra("TASK_NAME", taskName)
        }

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Task Reminder")
            .setContentText(taskName)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(android.R.drawable.checkbox_on_background, "✅ Done", markDonePendingIntent)
            .addAction(android.R.drawable.ic_delete, "❌ Dismiss", dismissPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }
}