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

        // קבלת תאריך המשימה המקורי (למשל: 20/02/2024 10:00)
        val taskDateTime = inputData.getString("taskDateTime") ?: ""

        if (lat == 0.0 || lon == 0.0 || taskDateTime.isEmpty()) return Result.success()

        try {
            // המרת התאריך לפורמט של ה-API (yyyy-MM-dd) כדי שנדע איזה יום לחפש בתחזית
            val taskDateOnly = try {
                val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                formatter.format(parser.parse(taskDateTime.split(" ")[0])!!)
            } catch (e: Exception) { "" }

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

            // סריקת התחזית וחיפוש נתונים שמתאימים ליום המשימה
            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val dtText = item.getString("dt_txt") // פורמט: "2024-02-20 12:00:00"

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

            // אם מצאנו נתונים ויש תנאי קיצוני - שולחים התראה (אנחנו עכשיו 24 שעות לפני המשימה)
            if (foundData && (isRain || maxTemp >= 30)) {
                sendNotification(taskName, isRain, maxTemp)
            }

            return Result.success()
        } catch (e: Exception) {
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
