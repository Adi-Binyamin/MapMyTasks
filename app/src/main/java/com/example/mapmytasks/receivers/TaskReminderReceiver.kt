package com.example.mapmytasks.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mapmytasks.data.TaskManager

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val taskName = intent.getStringExtra("TASK_NAME") ?: "Task"
        val userId = intent.getStringExtra("USER_ID") ?: return
        val createdBy = intent.getStringExtra("CREATED_BY") ?: ""

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        // בדיקה: האם המשתמש המחובר כרגע במכשיר זה הוא היוצר של המשימה?
        val myEmail = TaskManager.getCurrentUserEmail()
        val isCreator = (myEmail != null && myEmail.lowercase() == createdBy.lowercase())

        // בניית בסיס ההתראה המשותף לשני המקרים
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // שים לב שאייקון זה קיים אצלך או תחליף לשלך
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (isCreator) {
            // --- מקרה 1: אתה יוצר המשימה (התראה למידע בלבד, בלי כפתורים) ---
            notificationBuilder
                .setContentTitle("A task you sent is underway 📤")
                .setContentText("Reminder: The task '$taskName' was sent to your partner.")

            // לא מוסיפים שום addAction, אז לא יהיו כפתורי אישור/סירוב
        } else {
            // --- מקרה 2: השותף קיבל את המשימה (ההתראה הרגילה עם הכפתורים) ---
            notificationBuilder
                .setContentTitle("New task for you 📅")
                .setContentText(taskName)

            // כפתור בוצע
            val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "MARK_DONE"
                putExtra("TASK_ID", taskId)
                putExtra("USER_ID", userId)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context, taskId.hashCode() + 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // כפתור התעלם
            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = "DISMISS"
                putExtra("TASK_ID", taskId)
                putExtra("USER_ID", userId)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context, taskId.hashCode() + 2, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .addAction(android.R.drawable.checkbox_on_background, "Done ✅", donePendingIntent)
                .addAction(android.R.drawable.ic_delete, "Dismiss ❌", dismissPendingIntent)
        }

        // שליחת ההתראה למסך
        manager.notify(taskId.hashCode(), notificationBuilder.build())
    }
}