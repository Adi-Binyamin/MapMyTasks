package com.example.mapmytasks.utilities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.util.Calendar

object DateTimeUtils {

    // Parses a formatted date-time string into a Calendar object for use across various screens.
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

    // Determines the time of day index (0=Morning, 1=Afternoon, 2=Evening, 3=Night) based on the extracted hour.
    fun getTimeIndexFromDateTime(dateTime: String): Int {
        val hour = try { dateTime.split(" ")[1].split(":")[0].toInt() } catch (e: Exception) { return -1 }
        return when (hour) {
            in 6..11 -> 0
            in 12..17 -> 1
            in 18..21 -> 2
            else -> 3
        }
    }

    // Opens a combined Date and Time picker dialog sequence, ensuring only future dates/times can be selected.
    fun showDateTimePicker(
        context: Context,
        onDateTimeSelected: (formattedDateTime: String, year: Int, month: Int, day: Int) -> Unit
    ) {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->

                // Checks if the selected date and time are in the past.
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, day, hour, minute, 0)
                }

                if (selectedCalendar.timeInMillis < System.currentTimeMillis()) {
                    // The selected time is in the past - show an error and discard the selection.
                    context.toast("Please select a future date and time")
                } else {
                    // The selected time is valid (in the future) - format and return the result.
                    val formattedStr = String.format("%02d/%02d/%04d %02d:%02d", day, month + 1, year, hour, minute)
                    onDateTimeSelected(formattedStr, year, month + 1, day)
                }

            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        // Prevents the user from selecting past dates in the calendar dialog.
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000

        datePickerDialog.show()
    }
}