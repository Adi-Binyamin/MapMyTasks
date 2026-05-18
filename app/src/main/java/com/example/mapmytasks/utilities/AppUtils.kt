package com.example.mapmytasks.utilities

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.example.mapmytasks.data.TaskManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// פונקציית הרחבה (Extension) ל-Context: עכשיו אפשר לכתוב ()toast פשוט מכל מסך!
fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

object AppUtils {

    val CATEGORIES = listOf(
        "Work", "Study", "Personal", "Shopping", "Health",
        "Finance", "Hobby", "Travel", "School", "Chores", "Other"
    )

    fun setupCategorySpinner(context: Context, spinner: Spinner) {
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, CATEGORIES)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    fun checkProductivityWarning(context: Context, category: String, dateTime: String) {
        val userId = TaskManager.getCurrentUserId() ?: return
        val timeIndex = DateTimeUtils.getTimeIndexFromDateTime(dateTime)
        if (timeIndex == -1) return

        TaskManager.getProductivityStats(userId, category, timeIndex) { total, done ->
            if (total >= 3 && (done.toFloat() / total) < 0.5) {
                val timeNames = listOf("in the morning", "in the afternoon", "in the evening", "at night")
                context.toast("⚠️ Heads up: You usually don't finish $category tasks ${timeNames[timeIndex]}. Consider changing the time?")
            }
        }
    }

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
                        Handler(Looper.getMainLooper()).post {
                            context.toast("Heads up: $holidayName falls on this date")
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    // --- הפונקציה החדשה שמתזמנת את ההתראה ---
    fun scheduleTaskAlarm(context: Context, task: com.example.mapmytasks.models.Task, userId: String) {
        try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            val date = sdf.parse(task.dateTime) ?: return

            if (date.time <= System.currentTimeMillis()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(context, com.example.mapmytasks.receivers.TaskReminderReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_NAME", task.name)
                putExtra("USER_ID", userId)
            }

            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                task.id.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, date.time, pendingIntent)
                } else {
                    alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, date.time, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, date.time, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}