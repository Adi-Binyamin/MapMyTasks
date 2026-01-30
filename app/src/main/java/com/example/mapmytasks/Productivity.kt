package com.example.mapmytasks

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class Productivity : AppCompatActivity() {

    private val categories = listOf(
        "Work", "Study", "Personal", "Health", "Fitness",
        "Hobby", "Chores", "Errands", "Social", "Misc"
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

    private val doneMap = mutableMapOf<String, FloatArray>()
    private val totalMap = mutableMapOf<String, IntArray>()

    // Initializes UI elements and starts loading productivity data
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

        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        fetchDataFromFirebase()
    }

    // Loads tasks from Firebase and calculates productivity per time of day
    private fun fetchDataFromFirebase() {
        val user = Firebase.auth.currentUser ?: return

        categories.forEach {
            doneMap[it] = FloatArray(4)
            totalMap[it] = IntArray(4)
        }

        val now = Calendar.getInstance()

        Firebase.firestore.collection("users")
            .document(user.uid)
            .collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val rawCategory = doc.getString("category") ?: "Misc"
                    val category = if (categories.contains(rawCategory)) rawCategory else "Misc"

                    val status = try {
                        TaskStatus.valueOf(doc.getString("status") ?: "PENDING")
                    } catch (e: Exception) {
                        TaskStatus.PENDING
                    }

                    val dateTime = doc.getString("dateTime") ?: continue
                    val taskCal = parseDateTime(dateTime) ?: continue

                    if (taskCal.after(now)) continue

                    val hour = taskCal.get(Calendar.HOUR_OF_DAY)
                    val timeIndex = when (hour) {
                        in 6..11 -> 0
                        in 12..17 -> 1
                        in 18..21 -> 2
                        else -> 3
                    }

                    totalMap[category]!![timeIndex]++
                    if (status == TaskStatus.DONE) {
                        doneMap[category]!![timeIndex]++
                    }
                }

                updateProgressBars(categories[0])
                spinner.setSelection(0)

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        updateProgressBars(categories[position])
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
    }

    // Updates progress bars and percentage labels for the selected category
    private fun updateProgressBars(category: String) {
        val done = doneMap[category]!!
        val total = totalMap[category]!!

        fun percent(done: Float, total: Int): Int {
            return if (total == 0) 0 else ((done / total) * 100).toInt()
        }

        progressMorning.progress = percent(done[0], total[0])
        progressAfternoon.progress = percent(done[1], total[1])
        progressEvening.progress = percent(done[2], total[2])
        progressNight.progress = percent(done[3], total[3])

        tvMorningPercent.text = "${progressMorning.progress}%"
        tvAfternoonPercent.text = "${progressAfternoon.progress}%"
        tvEveningPercent.text = "${progressEvening.progress}%"
        tvNightPercent.text = "${progressNight.progress}%"
    }

    // Parses task date string into a Calendar object
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
