package com.example.mapmytasks

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class Productivity : AppCompatActivity() {

    private val categories = listOf(
        "Work", "Study", "Personal", "Shopping", "Health",
        "Finance", "Hobby", "Travel", "School", "Chores", "Other"
    )

    private lateinit var spinner: Spinner
    private lateinit var progressMorning: ProgressBar
    private lateinit var progressAfternoon: ProgressBar
    private lateinit var progressEvening: ProgressBar
    private lateinit var progressNight: ProgressBar

    private lateinit var tvMorningPercent: TextView
    private lateinit var tvAfternoonPercent: TextView
    private lateinit var tvEveningPercent: TextView
    private lateinit var tvNightPercent: TextView

    private var doneMap: Map<String, FloatArray> = emptyMap()
    private var totalMap: Map<String, IntArray> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productivity)

        spinner = findViewById(R.id.categorySpinner)
        progressMorning = findViewById(R.id.progressMorning)
        progressAfternoon = findViewById(R.id.progressAfternoon)
        progressEvening = findViewById(R.id.progressEvening)
        progressNight = findViewById(R.id.progressNight)

        tvMorningPercent = findViewById(R.id.tvMorningPercent)
        tvAfternoonPercent = findViewById(R.id.tvAfternoonPercent)
        tvEveningPercent = findViewById(R.id.tvEveningPercent)
        tvNightPercent = findViewById(R.id.tvNightPercent)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter

        fetchData()
    }

    private fun fetchData() {
        val userId = TaskManager.getCurrentUserId() ?: return

        TaskManager.getAllProductivityStats(userId, categories) { dMap, tMap ->
            doneMap = dMap
            totalMap = tMap

            // עדכון ראשוני
            updateProgressBars(categories[0])

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                    updateProgressBars(categories[pos])
                }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }
    }

    private fun updateProgressBars(category: String) {
        val done = doneMap[category] ?: FloatArray(4)
        val total = totalMap[category] ?: IntArray(4)

        fun percent(d: Float, t: Int): Int {
            return if (t <= 0) 0 else ((d / t) * 100).toInt()
        }

        val pMorning = percent(done[0], total[0])
        progressMorning.progress = pMorning
        tvMorningPercent.text = "$pMorning%"

        val pAfternoon = percent(done[1], total[1])
        progressAfternoon.progress = pAfternoon
        tvAfternoonPercent.text = "$pAfternoon%"

        val pEvening = percent(done[2], total[2])
        progressEvening.progress = pEvening
        tvEveningPercent.text = "$pEvening%"

        val pNight = percent(done[3], total[3])
        progressNight.progress = pNight
        tvNightPercent.text = "$pNight%"
    }
}