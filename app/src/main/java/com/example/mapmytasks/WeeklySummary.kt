package com.example.mapmytasks

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Button


class WeeklySummary : AppCompatActivity() {

    private lateinit var chartDoneTimeSlots: BarChart
    private lateinit var chartPendingTimeSlots: BarChart
    private lateinit var chartDoneCategory: BarChart
    private lateinit var chartPendingCategory: BarChart

    private val timeSlots = listOf("Night", "Morning", "Afternoon", "Evening") // 0-3
    private val categories = listOf(
        "Work", "Study", "Personal", "Health", "Fitness",
        "Hobby", "Chores", "Errands", "Social", "Misc"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_weekly_summary)

        chartDoneTimeSlots = findViewById(R.id.chartDoneTime)
        chartPendingTimeSlots = findViewById(R.id.chartPendingTime)
        chartDoneCategory = findViewById(R.id.chartDoneCategory)
        chartPendingCategory = findViewById(R.id.chartPendingCategory)

        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Close the screen and return to the previous one
        }

        fetchTasks()
    }

    // Fetch tasks from Firestore and compute weekly statistics
    private fun fetchTasks() {
        val user = Firebase.auth.currentUser ?: return
        val db = Firebase.firestore

        // Last full week (Sunday to Saturday)
        val lastWeekDates = getLastFullWeekDates()
        val weekStart = parseDateTime(lastWeekDates.first() + " 00:00")
        val weekEnd = parseDateTime(lastWeekDates.last() + " 23:59")

        // Arrays for time slots
        val doneSlots = FloatArray(4) { 0f }
        val pendingSlots = FloatArray(4) { 0f }

        // Maps for categories
        val doneByCategory = mutableMapOf<String, Float>()
        val pendingByCategory = mutableMapOf<String, Float>()
        categories.forEach {
            doneByCategory[it] = 0f
            pendingByCategory[it] = 0f
        }

        val weekLabel = "${lastWeekDates.first()} - ${lastWeekDates.last()}"

        if (weekStart == null || weekEnd == null) {
            // No data - display empty charts
            displayCharts(doneSlots, pendingSlots, doneByCategory, pendingByCategory, weekLabel)
            return
        }

        db.collection("users").document(user.uid).collection("tasks")
            .get()
            .addOnSuccessListener { result ->

                for (doc in result) {
                    val task = doc.toObject(Task::class.java)
                    val cal = parseDateTime(task.dateTime) ?: continue
                    if (cal.before(weekStart) || cal.after(weekEnd)) continue

                    // Determine time slot
                    val slotIndex = getTimeSlotIndex(cal)

                    if (task.status == TaskStatus.DONE) {
                        doneSlots[slotIndex]++
                        doneByCategory[task.category] = doneByCategory.getOrDefault(task.category, 0f) + 1
                    } else {
                        pendingSlots[slotIndex]++
                        pendingByCategory[task.category] = pendingByCategory.getOrDefault(task.category, 0f) + 1
                    }
                }

                displayCharts(doneSlots, pendingSlots, doneByCategory, pendingByCategory, weekLabel)
            }
            .addOnFailureListener {
                displayCharts(doneSlots, pendingSlots, doneByCategory, pendingByCategory, weekLabel)
            }
    }

    // Display all charts on screen
    private fun displayCharts(
        doneSlots: FloatArray,
        pendingSlots: FloatArray,
        doneByCategory: Map<String, Float>,
        pendingByCategory: Map<String, Float>,
        weekLabel: String
    ) {
        // Time slot charts
        setupBarChart(chartDoneTimeSlots, doneSlots.toList(), timeSlots, "Done Tasks ($weekLabel)", android.R.color.holo_green_light)
        setupBarChart(chartPendingTimeSlots, pendingSlots.toList(), timeSlots, "Pending Tasks ($weekLabel)", android.R.color.holo_red_light)

        // Category charts
        setupBarChart(chartDoneCategory, doneByCategory.values.toList(), categories, "Done by Category ($weekLabel)", android.R.color.holo_green_light)
        setupBarChart(chartPendingCategory, pendingByCategory.values.toList(), categories, "Pending by Category ($weekLabel)", android.R.color.holo_red_light)
    }

    // Get time slot index by hour of day
    private fun getTimeSlotIndex(cal: Calendar): Int {
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..11 -> 0      // Morning
            in 12..17 -> 1     // Afternoon
            in 18..21 -> 2     // Evening
            else -> 3          // Night
        }
    }

    // Configure and show a bar chart
    private fun setupBarChart(
        chart: BarChart,
        values: List<Float>,
        labels: List<String>,
        descriptionText: String,
        colorRes: Int
    ) {
        val entries = labels.mapIndexed { index, _ ->
            BarEntry(index.toFloat(), if (index < values.size) values[index] else 0f)
        }

        val dataSet = BarDataSet(entries, "")
        dataSet.color = resources.getColor(colorRes, null)

        val data = BarData(dataSet)
        data.barWidth = 0.9f
        chart.data = data

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.granularity = 1f
        chart.xAxis.setDrawLabels(true)
        chart.xAxis.labelRotationAngle = -45f
        chart.xAxis.setAvoidFirstLastClipping(true)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        chart.axisLeft.axisMinimum = 0f
        chart.axisRight.isEnabled = false

        chart.description.text = descriptionText
        chart.setFitBars(true)
        chart.invalidate()
    }

    // Return dates of the last full week (Sunday-Saturday)
    private fun getLastFullWeekDates(): List<String> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val year = calendar.get(Calendar.YEAR)
        val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())

        return List(7) {
            val dateStr = formatter.format(calendar.time) + "/$year"
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            dateStr
        }
    }

    // Parse date string to Calendar
    private fun parseDateTime(dateTime: String): Calendar? {
        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = formatter.parse(dateTime) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            null
        }
    }
}
