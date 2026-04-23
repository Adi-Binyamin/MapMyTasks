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

    private val timeSlots = listOf("Morning", "Afternoon", "Evening", "Night")

    // הרשימה המעודכנת שזכרתי עבורך (11 קטגוריות)
    private val categories = listOf(
        "Work", "Study", "Personal", "Shopping", "Health",
        "Finance", "Hobby", "Travel", "School", "Chores", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_summary)

        chartDoneTimeSlots = findViewById(R.id.chartDoneTime)
        chartPendingTimeSlots = findViewById(R.id.chartPendingTime)
        chartDoneCategory = findViewById(R.id.chartDoneCategory)
        chartPendingCategory = findViewById(R.id.chartPendingCategory)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        fetchTasks()
    }

    private fun fetchTasks() {
        val user = Firebase.auth.currentUser ?: return
        val db = Firebase.firestore

        val lastWeekDates = getLastFullWeekDates()
        // פורמט התאריכים כאן צריך להתאים ל-dd/MM/yyyy
        val weekStart = parseDateTime("${lastWeekDates.first()} 00:00")
        val weekEnd = parseDateTime("${lastWeekDates.last()} 23:59")

        val doneSlots = FloatArray(4) { 0f }
        val pendingSlots = FloatArray(4) { 0f }

        val doneByCategory = mutableMapOf<String, Float>()
        val pendingByCategory = mutableMapOf<String, Float>()

        // אתחול המפות עם כל הקטגוריות מהרשימה החדשה
        categories.forEach {
            doneByCategory[it] = 0f
            pendingByCategory[it] = 0f
        }

        db.collection("users").document(user.uid).collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val task = doc.toObject(Task::class.java)
                    val cal = parseDateTime(task.dateTime) ?: continue

                    if (weekStart != null && weekEnd != null) {
                        if (cal.before(weekStart) || cal.after(weekEnd)) continue
                    }

                    // מציאת אינדקס זמן (בוקר, צהריים וכו')
                    val slotIndex = getTimeSlotIndex(cal)

                    // מציאת קטגוריה תואמת מהרשימה (מתעלם מרווחים ואותיות גדולות)
                    val categoryKey = categories.find { it.equals(task.category.trim(), ignoreCase = true) } ?: "Other"

                    if (task.status == TaskStatus.DONE) {
                        doneSlots[slotIndex]++
                        doneByCategory[categoryKey] = doneByCategory.getOrDefault(categoryKey, 0f) + 1
                    } else {
                        pendingSlots[slotIndex]++
                        pendingByCategory[categoryKey] = pendingByCategory.getOrDefault(categoryKey, 0f) + 1
                    }
                }

                val weekLabel = "${lastWeekDates.first()} - ${lastWeekDates.last()}"
                displayCharts(doneSlots, pendingSlots, doneByCategory, pendingByCategory, weekLabel)
            }
    }

    private fun displayCharts(
        doneSlots: FloatArray,
        pendingSlots: FloatArray,
        doneByCategory: Map<String, Float>,
        pendingByCategory: Map<String, Float>,
        weekLabel: String
    ) {
        setupBarChart(chartDoneTimeSlots, doneSlots.toList(), timeSlots, "Done Tasks ($weekLabel)", android.R.color.holo_green_light)
        setupBarChart(chartPendingTimeSlots, pendingSlots.toList(), timeSlots, "Pending Tasks ($weekLabel)", android.R.color.holo_red_light)

        // בגרפים של הקטגוריות אנחנו שולחים את הערכים לפי הסדר של רשימת ה-categories שלנו
        val doneValues = categories.map { doneByCategory[it] ?: 0f }
        val pendingValues = categories.map { pendingByCategory[it] ?: 0f }

        setupBarChart(chartDoneCategory, doneValues, categories, "Done by Category", android.R.color.holo_green_light)
        setupBarChart(chartPendingCategory, pendingValues, categories, "Pending by Category", android.R.color.holo_red_light)
    }

    private fun getTimeSlotIndex(cal: Calendar): Int {
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..11 -> 0      // Morning
            in 12..17 -> 1     // Afternoon
            in 18..21 -> 2     // Evening
            else -> 3          // Night
        }
    }

    private fun setupBarChart(chart: BarChart, values: List<Float>, labels: List<String>, descriptionText: String, colorRes: Int) {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val dataSet = BarDataSet(entries, "")
        dataSet.color = resources.getColor(colorRes, null)
        dataSet.valueTextSize = 10f

        chart.data = BarData(dataSet)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.xAxis.labelRotationAngle = -45f

        chart.axisRight.isEnabled = false
        chart.axisLeft.axisMinimum = 0f
        chart.description.text = descriptionText
        chart.animateY(1000)
        chart.invalidate()
    }

    private fun getLastFullWeekDates(): List<String> {
        val calendar = Calendar.getInstance()
        // הולכים אחורה ליום ראשון של השבוע שעבר
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return List(7) {
            val dateStr = formatter.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            dateStr
        }
    }

    private fun parseDateTime(dateTime: String): Calendar? {
        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = formatter.parse(dateTime) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) { null }
    }
}