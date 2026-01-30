package com.example.mapmytasks

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
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class LocationTaskService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val notifiedTasks = mutableSetOf<String>()

    companion object {
        private const val FOREGROUND_ID = 1
        private const val CHANNEL_ID = "task_service_channel"
        private const val CHANNEL_NAME = "Task Location Service"
        private const val CHECK_INTERVAL_MS = 5000L
        private const val GEOFENCE_RADIUS_METERS = 1000f
        private const val TAG = "TASK_NOTIFY"
    }

    // Start the background service and setup notification channel
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

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
                checkTasks(location.latitude, location.longitude)
            }
        }

        startLocationUpdates()
    }

    // Ask for location and start tracking the user position
    private fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            interval = CHECK_INTERVAL_MS
            fastestInterval = 2000L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        if (fineLocationGranted && backgroundLocationGranted) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
            Log.d(TAG, "Location updates started")
        } else {
            Log.d(TAG, "No location permissions or background location denied")
        }
    }

    // Check all tasks from Firebase to see if we are close to a task location
    private fun checkTasks(lat: Double, lng: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val tasksRef = db.collection("users").document(userId).collection("tasks")

        tasksRef.get().addOnSuccessListener { tasks ->
            val now = Calendar.getInstance()

            for (doc in tasks) {
                val task = doc.toObject(Task::class.java)
                val taskId = doc.id
                if (notifiedTasks.contains(taskId)) continue
                if (task.dateTime.isEmpty()) continue

                val taskLat = task.latitude
                val taskLng = task.longitude
                if (taskLat == 0.0 && taskLng == 0.0) continue

                val distance = FloatArray(1)
                android.location.Location.distanceBetween(lat, lng, taskLat, taskLng, distance)
                if (distance[0] > GEOFENCE_RADIUS_METERS) continue

                val parts = task.dateTime.split(" ", "/", ":")
                if (parts.size < 5) continue

                val taskCal = Calendar.getInstance().apply {
                    set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(),
                        parts[3].toInt(), parts[4].toInt(), 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val diff = now.timeInMillis - taskCal.timeInMillis
                if (diff in 0..30_000) {
                    showNotification(task.name, taskId, userId)
                    notifiedTasks.add(taskId)
                }
            }
        }.addOnFailureListener {
            Log.e(TAG, "Error getting tasks: ${it.message}")
        }
    }

    // Show a notification alert when the user is at the right place and time
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
            this, 0, markDoneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
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

    // Stop tracking location when the service is closed
    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}