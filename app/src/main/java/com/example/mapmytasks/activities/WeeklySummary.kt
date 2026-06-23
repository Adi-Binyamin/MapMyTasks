package com.example.mapmytasks.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.widget.Button
import com.example.mapmytasks.R
import com.example.mapmytasks.data.TaskManager
import com.example.mapmytasks.utilities.AppUtils

/**
 * WeeklySummary Activity displays statistical bar charts detailing the user's task completion
 * over the past week, broken down by time slots and categories.
 */
class WeeklySummary : AppCompatActivity() {

    private lateinit var chartDoneTimeSlots: BarChart
    private lateinit var chartPendingTimeSlots: BarChart
    private lateinit var chartDoneCategory: BarChart
    private lateinit var chartPendingCategory: BarChart

    private val timeSlots = listOf("Morning", "Afternoon", "Evening", "Night")

    // Initializes the chart views and triggers the data fetching process.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_summary)

        chartDoneTimeSlots = findViewById(R.id.chartDoneTime)
        chartPendingTimeSlots = findViewById(R.id.chartPendingTime)
        chartDoneCategory = findViewById(R.id.chartDoneCategory)
        chartPendingCategory = findViewById(R.id.chartPendingCategory)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        fetchData()
    }

    // Fetches the weekly statistics from Firestore and populates the four corresponding bar charts.
    private fun fetchData() {
        val userId = TaskManager.getCurrentUserId() ?: return

        TaskManager.getWeeklySummaryStats(
            userId,
            AppUtils.CATEGORIES
        ) { doneS, pendingS, doneC, pendingC, weekDates ->
            val weekLabel = "${weekDates.first()} - ${weekDates.last()}"

            setupBarChart(
                chartDoneTimeSlots,
                doneS.toList(),
                timeSlots,
                "Done Tasks ($weekLabel)",
                android.R.color.holo_green_light
            )
            setupBarChart(
                chartPendingTimeSlots,
                pendingS.toList(),
                timeSlots,
                "Pending Tasks ($weekLabel)",
                android.R.color.holo_red_light
            )

            // Maps the fetched category data to match the central categories list, ensuring no missing values.
            val doneValues = AppUtils.CATEGORIES.map { doneC[it] ?: 0f }
            val pendingValues = AppUtils.CATEGORIES.map { pendingC[it] ?: 0f }

            setupBarChart(
                chartDoneCategory,
                doneValues,
                AppUtils.CATEGORIES,
                "Done by Category",
                android.R.color.holo_green_light
            )
            setupBarChart(
                chartPendingCategory,
                pendingValues,
                AppUtils.CATEGORIES,
                "Pending by Category",
                android.R.color.holo_red_light
            )
        }
    }

    // Helper function to configure the visual appearance, labels, and animation for a given BarChart instance.
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
}