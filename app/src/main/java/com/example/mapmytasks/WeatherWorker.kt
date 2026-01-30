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

    // Checks weather and sends notification if needed
    override suspend fun doWork(): Result {
        val lat = inputData.getDouble("lat", 0.0)
        val lon = inputData.getDouble("lon", 0.0)
        val taskName = inputData.getString("taskName") ?: "משימה"

        if (lat == 0.0 || lon == 0.0) return Result.success()

        try {
            val url =
                "https://api.openweathermap.org/data/2.5/forecast" +
                        "?lat=$lat&lon=$lon" +
                        "&units=metric" +
                        "&appid=9daee5d8fbe2b45d8827dac821dd20d9"

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return Result.success()

            val json = JSONObject(body)
            val list = json.getJSONArray("list")

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val tomorrowStr = dateFormat.format(calendar.time)

            var isRain = false
            var maxTemp = Double.MIN_VALUE

            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val dateText = item.getString("dt_txt")

                if (dateText.startsWith(tomorrowStr)) {
                    val temp = item.getJSONObject("main").getDouble("temp")
                    maxTemp = maxOf(maxTemp, temp)

                    val weatherArray = item.getJSONArray("weather")
                    for (j in 0 until weatherArray.length()) {
                        val main = weatherArray.getJSONObject(j).getString("main")
                        if (main.contains("Rain")) {
                            isRain = true
                        }
                    }
                }
            }

            if (isRain || maxTemp >= 30) {
                sendNotification(taskName, isRain, maxTemp)
            }

            return Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    // Sends a notification about tomorrow's weather
    private fun sendNotification(
        taskName: String,
        isRain: Boolean,
        maxTemp: Double
    ) {
        val channelId = "weather_alerts"

        val message = when {
            isRain && maxTemp >= 30 ->
                "מחר צפוי גשם וחום (${maxTemp.toInt()}°)"
            isRain ->
                "מחר צפוי גשם"
            else ->
                "מחר חם מאוד (${maxTemp.toInt()}°)"
        }

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "התראות מזג אוויר",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("התראת מזג אוויר – $taskName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {

        // Calculates delay until tomorrow at 7 AM
        fun calculateDelayForTomorrow(): Long {
            val now = Calendar.getInstance()
            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return tomorrow.timeInMillis - now.timeInMillis
        }

        // Schedules the worker to run tomorrow
        fun scheduleWeatherWorker(context: Context, task: Task) {
            val data = workDataOf(
                "lat" to task.latitude,
                "lon" to task.longitude,
                "taskName" to task.name
            )

            val workRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(data)
                .setInitialDelay(calculateDelayForTomorrow(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
