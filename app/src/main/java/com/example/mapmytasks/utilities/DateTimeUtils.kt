package com.example.mapmytasks.utilities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

object DateTimeUtils {

    // המרת מחרוזת תאריך לאובייקט Calendar (משמש את המסכים השונים)
    fun parseDateTime(dateTime: String): Calendar? {
        return try {
            val parts = dateTime.split(" ", "/", ":")
            if (parts.size < 5) return null
            Calendar.getInstance().apply {
                set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(),
                    parts[3].toInt(), parts[4].toInt(), 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) { null }
    }

    // מציאת "חלק היום" מתוך שעה (0=בוקר, 1=צהריים, 2=ערב, 3=לילה)
    fun getTimeIndexFromDateTime(dateTime: String): Int {
        val hour = try { dateTime.split(" ")[1].split(":")[0].toInt() } catch (e: Exception) { return -1 }
        return when (hour) {
            in 6..11 -> 0
            in 12..17 -> 1
            in 18..21 -> 2
            else -> 3
        }
    }

    // פתיחת דיאלוג משולב של תאריך ושעה
    fun showDateTimePicker(
        context: Context,
        onDateTimeSelected: (formattedDateTime: String, year: Int, month: Int, day: Int) -> Unit
    ) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->

                // בדיקה אם הזמן והשעה שנבחרו קטנים מהרגע הנוכחי
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                }

                if (selectedCalendar.timeInMillis < System.currentTimeMillis()) {
                    // הזמן כבר עבר - מקפיצים הודעה ולא שומרים את הבחירה!
                    context.toast("Please select a future date and time")
                } else {
                    // הזמן תקין (בעתיד) - מעבירים הלאה
                    val formattedStr = String.format("%02d/%02d/%04d %02d:%02d", day, month + 1, year, hour, minute)
                    onDateTimeSelected(formattedStr, year, month + 1, day)
                }

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        // --- התוספת שלנו: מונע לחיצה על ימים בעבר בלוח השנה ---
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000

        datePickerDialog.show()
    }
}