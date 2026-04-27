package com.example.mapmytasks

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// פונקציית הרחבה (Extension) ל-Context: עכשיו אפשר לכתוב ()toast פשוט מכל מסך!
fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

object AppUtils {

    // רשימת הקטגוריות המרכזית של האפליקציה! שנה אותה כאן, והיא תשתנה בכל המסכים
    val CATEGORIES = listOf(
        "Work", "Study", "Personal", "Shopping", "Health",
        "Finance", "Hobby", "Travel", "School", "Chores", "Other"
    )

    // טעינת הספינר של הקטגוריות בסיבוב אחד
    fun setupCategorySpinner(context: Context, spinner: Spinner) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, CATEGORIES)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    // בדיקת יעילות והקפצת אזהרה
    fun checkProductivityWarning(context: Context, category: String, dateTime: String) {
        val userId = TaskManager.getCurrentUserId() ?: return
        val timeIndex = DateTimeUtils.getTimeIndexFromDateTime(dateTime)
        if (timeIndex == -1) return

        TaskManager.getProductivityStats(userId, category, timeIndex) { total, done ->
            if (total >= 3 && (done.toFloat() / total) < 0.5) {
                val timeNames = listOf("בבוקר", "בצהריים", "בערב", "בלילה")
                context.toast("⚠️ שים לב: בדרך כלל את נוטה לא לסיים משימות $category ${timeNames[timeIndex]}. אולי כדאי לשנות שעה?")
            }
        }
    }

    // בדיקת חגים של Hebcal
    fun checkHolidayHebcal(context: Context, year: Int, month: Int, day: Int) {
        Thread {
            try {
                val url = "https://www.hebcal.com/hebcal?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=$year&month=$month&ss=on&mf=on"
                val response = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val items = JSONObject(body).optJSONArray("items")
                    val selectedDateStr = String.format("%04d-%02d-%02d", year, month, day)
                    var holidayName: String? = null

                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            if (item.optString("date").startsWith(selectedDateStr)) {
                                holidayName = item.optString("title")
                                break
                            }
                        }
                    }

                    if (holidayName != null) {
                        // חזרה ל-Thread הראשי כדי להציג את ה-Toast
                        Handler(Looper.getMainLooper()).post {
                            context.toast("שים לב: בתאריך זה חל $holidayName")
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    // תזמון בדיקת מזג אוויר חכמה
    fun scheduleSmartWeatherCheck(context: Context, task: Task) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val taskDate = sdf.parse(task.dateTime) ?: return
            val now = System.currentTimeMillis()
            val targetTime = taskDate.time - (24 * 60 * 60 * 1000L)
            val delay = if (targetTime - now > 0) targetTime - now else 0L

            val data = workDataOf(
                "lat" to task.latitude,
                "lon" to task.longitude,
                "taskName" to task.name,
                "taskDateTime" to task.dateTime
            )

            val workRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(data)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "weather_${task.id}",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) { e.printStackTrace() }
    }
}