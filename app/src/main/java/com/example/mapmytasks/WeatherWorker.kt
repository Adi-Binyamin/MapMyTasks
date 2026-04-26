package com.example.mapmytasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WeatherWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lat = inputData.getDouble("lat", 0.0)
        val lon = inputData.getDouble("lon", 0.0)
        val taskName = inputData.getString("taskName") ?: "משימה"
        val taskDateTime = inputData.getString("taskDateTime") ?: ""

        if (lat == 0.0 || lon == 0.0 || taskDateTime.isEmpty()) return Result.success()

        try {
            val taskDateOnly = try {
                val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                formatter.format(parser.parse(taskDateTime.split(" ")[0])!!)
            } catch (e: Exception) { "" }

            // שימוש ב-API Key שלך
            val url = "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&units=metric&appid=9daee5d8fbe2b45d8827dac821dd20d9"

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return Result.success()

            val json = JSONObject(body)
            val list = json.getJSONArray("list")

            var isRain = false
            var maxTemp = Double.MIN_VALUE
            var foundData = false

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val dtText = item.getString("dt_txt")

                if (dtText.startsWith(taskDateOnly)) {
                    foundData = true
                    val temp = item.getJSONObject("main").getDouble("temp")
                    maxTemp = maxOf(maxTemp, temp)

                    val weatherArray = item.getJSONArray("weather")
                    for (j in 0 until weatherArray.length()) {
                        if (weatherArray.getJSONObject(j).getString("main").contains("Rain", true)) {
                            isRain = true
                        }
                    }
                }
            }

            if (foundData && (isRain || maxTemp >= 30)) {
                sendNotification(taskName, isRain, maxTemp)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun sendNotification(taskName: String, isRain: Boolean, maxTemp: Double) {
        val channelId = "weather_alerts"
        val message = when {
            isRain && maxTemp >= 30 -> "מחר צפוי גשם וחום (${maxTemp.toInt()}°)"
            isRain -> "מחר צפוי גשם"
            else -> "מחר חם מאוד (${maxTemp.toInt()}°)"
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "התראות מזג אוויר", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("התראת מזג אוויר – $taskName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        fun scheduleWeatherWorker(context: Context, task: Task) {
            val data = workDataOf(
                "lat" to task.latitude,
                "lon" to task.longitude,
                "taskName" to task.name,
                "taskDateTime" to task.dateTime
            )

            // חישוב השהייה של 24 שעות לפני המשימה
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val taskDate = sdf.parse(task.dateTime) ?: return
            val delay = taskDate.time - System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            val finalDelay = if (delay > 0) delay else 0L

            val workRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(data)
                .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "weather_${task.id}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}