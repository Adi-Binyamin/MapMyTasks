package com.example.mapmytasks.data

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.mapmytasks.models.TaskStatus
import com.example.mapmytasks.receivers.NotificationActionReceiver
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * LocationTaskService is a foreground service that continuously monitors the user's location
 * and the current time to trigger geofence and time-based task notifications.
 */
class LocationTaskService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val notifiedTasks = mutableSetOf<String>()

    private var lastLat: Double = 0.0
    private var lastLng: Double = 0.0
    private val handler = Handler(Looper.getMainLooper())
    private var lastSentTaskCheck = 0L

    // A repeating task that checks for due notifications at regular intervals.
    private val timeCheckRunnable = object : Runnable {
        override fun run() {
            // 1. Check personal tasks based on location and time (runs every 10 seconds).
            if (lastLat != 0.0 && lastLng != 0.0) {
                checkTasks(lastLat, lastLng)
            }

            // 2. Check tasks assigned to others based on time (polls the server only once a minute).
            val now = System.currentTimeMillis()
            if (now - lastSentTaskCheck >= 60000L) {
                checkSentTasksDirectly()
                lastSentTaskCheck = now
            }

            handler.postDelayed(this, 10000L)
        }
    }

    // Fetches tasks assigned to others directly from the server without keeping them in a global list.
    private fun checkSentTasksDirectly() {
        val myEmail = TaskManager.getCurrentUserEmail() ?: return
        val now = Calendar.getInstance()

        FirebaseFirestore.getInstance().collectionGroup("tasks")
            .whereEqualTo("createdBy", myEmail)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val task = doc.toObject(com.example.mapmytasks.models.Task::class.java)?.copy(id = doc.id) ?: continue

                    // Skips tasks that are actually assigned to yourself.
                    if (task.assignTo == myEmail) continue
                    if (task.status != TaskStatus.PENDING) continue
                    if (task.dateTime.isEmpty()) continue

                    val parts = task.dateTime.split(" ", "/", ":")
                    if (parts.size < 5) continue

                    val taskCal = Calendar.getInstance().apply {
                        set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(),
                            parts[3].toInt(), parts[4].toInt(), 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val diff = now.timeInMillis - taskCal.timeInMillis
                    val isSameMinute = diff in 0..59999

                    if (isSameMinute) {
                        val uniqueKey = "${task.id}_sent"
                        if (!notifiedTasks.contains(uniqueKey)) {
                            showSentTaskNotification(task)
                            notifiedTasks.add(uniqueKey)
                        }
                    }
                }
            }
    }

    // Displays a clean notification without action buttons, showing only the task info and target email.
    private fun showSentTaskNotification(task: com.example.mapmytasks.models.Task) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifChannelId = "sent_tasks_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notifChannelId, "Sent Tasks Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, notifChannelId)
            .setContentTitle("Reminder: Task assigned to partner")
            .setContentText("The task '${task.name}' is due for ${task.assignTo}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Task details: ${task.name}\n" +
                        "Category: ${task.category}\n" +
                        "Task location: ${task.location}\n " +
                        "Sent to partner: ${task.assignTo}"
            ))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Uses a modified hashCode offset (+2) to prevent collisions with regular personal task notifications.
        notificationManager.notify(task.id.hashCode() + 2, notification)
    }

    companion object {
        private const val FOREGROUND_ID = 1
        private const val CHANNEL_ID = "task_service_channel"
        private const val CHANNEL_NAME = "Task Location Service"
        private const val CHECK_INTERVAL_MS = 5000L
        private const val GEOFENCE_RADIUS_METERS = 1000f
    }

    // Initializes location services, sets up the foreground notification, and starts the loops.
    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoring tasks by location")
            .setContentText("Service running in background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(FOREGROUND_ID, notification)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                lastLat = location.latitude
                lastLng = location.longitude
                checkTasks(lastLat, lastLng)
            }
        }

        startLocationUpdates()
        handler.post(timeCheckRunnable)

    }

    // Requests continuous location updates if the required permission is granted.
    private fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            interval = CHECK_INTERVAL_MS //ask for user location every 5 sec
            fastestInterval = 2000L // min time to update user location is 2 sec
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f //dont sent update if the user didnt move at least 1 meter
        }

        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Relies on the location permission that the user grants during the task creation flow.
        if (fineLocationGranted) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
        }
    }

    // Evaluates personal pending tasks to see if the current time and location match the task's criteria.
    private fun checkTasks(lat: Double, lng: Double) {
        val userId = TaskManager.getCurrentUserId() ?: return

        TaskManager.getTasksForUser(userId, onSuccess = { tasks ->
            val now = Calendar.getInstance()

            for (task in tasks) {
                val taskId = task.id

                if (task.status != TaskStatus.PENDING) continue
                if (task.dateTime.isEmpty()) continue

                val parts = task.dateTime.split(" ", "/", ":")
                if (parts.size < 5) continue

                val taskCal = Calendar.getInstance().apply {
                    set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(),
                        parts[3].toInt(), parts[4].toInt(), 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val diff = now.timeInMillis - taskCal.timeInMillis

                if (diff >= 60000) {
                    TaskManager.updateTaskStatus(userId, taskId, TaskStatus.MISSED)
                    continue
                }

                val isSameMinute = diff in 0..59999

                if (isSameMinute) {
                    val taskLat = task.latitude
                    val taskLng = task.longitude
                    if (taskLat == 0.0 && taskLng == 0.0) continue

                    val distance = FloatArray(1)
                    android.location.Location.distanceBetween(lat, lng, taskLat, taskLng, distance)

                    if (distance[0] <= GEOFENCE_RADIUS_METERS) {
                        if (!notifiedTasks.contains(taskId)) {
                            showNotification(task.name, taskId, userId)
                            notifiedTasks.add(taskId)
                        }
                    }
                }
            }
        }, onFailure = {
        })
    }

    // Displays an actionable notification allowing the user to mark a personal task as done or dismiss it.
    private fun showNotification(taskName: String, taskId: String, userId: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifChannelId = "task_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notifChannelId, "Task Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val markDoneIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "MARK_DONE"
            putExtra("TASK_ID", taskId)
            putExtra("USER_ID", userId)
        }

        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "DISMISS"
            putExtra("TASK_ID", taskId)
            putExtra("USER_ID", userId)
        }

        val markDonePendingIntent = PendingIntent.getBroadcast(
            this, taskId.hashCode(), markDoneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, taskId.hashCode() + 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(this, notifChannelId)
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

    override fun onBind(intent: Intent?): IBinder? = null

    // Cleans up the location updates and background thread callbacks when the service is stopped.
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(timeCheckRunnable)
    }
}