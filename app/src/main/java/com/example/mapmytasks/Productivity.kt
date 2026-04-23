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

    // הרשימה המעודכנת שתואמת ל-CreateTask ו-EditTask
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

    private val doneMap = mutableMapOf<String, FloatArray>()
    private val totalMap = mutableMapOf<String, IntArray>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productivity)

        // אתחול רכיבי UI
        spinner = findViewById(R.id.categorySpinner)
        progressMorning = findViewById(R.id.progressMorning)
        progressAfternoon = findViewById(R.id.progressAfternoon)
        progressEvening = findViewById(R.id.progressEvening)
        progressNight = findViewById(R.id.progressNight)

        tvMorningPercent = findViewById(R.id.tvMorningPercent)
        tvAfternoonPercent = findViewById(R.id.tvAfternoonPercent)
        tvEveningPercent = findViewById(R.id.tvEveningPercent)
        tvNightPercent = findViewById(R.id.tvNightPercent)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // הגדרת הספינר עם הרשימה החדשה
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = adapter

        fetchDataFromFirebase()
    }

    private fun fetchDataFromFirebase() {
        val user = Firebase.auth.currentUser ?: return

        // איפוס מפות הנתונים לפי הרשימה החדשה
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
                    val task = doc.toObject(Task::class.java)

                    // חיפוש חכם של הקטגוריה כדי למנוע טעויות הקלדה ב-Database
                    val category = categories.find { it.equals(task.category.trim(), ignoreCase = true) } ?: "Other"

                    val taskCal = parseDateTime(task.dateTime) ?: continue

                    // מחשבים פרודוקטיביות רק על משימות שעבר זמן היעד שלהן
                    if (taskCal.after(now)) continue

                    val hour = taskCal.get(Calendar.HOUR_OF_DAY)
                    val timeIndex = when (hour) {
                        in 6..11 -> 0   // בוקר
                        in 12..17 -> 1  // צהריים
                        in 18..21 -> 2  // ערב
                        else -> 3       // לילה
                    }

                    totalMap[category]?.let { it[timeIndex]++ }
                    if (task.status == TaskStatus.DONE) {
                        doneMap[category]?.let { it[timeIndex]++ }
                    }
                }

                // עדכון ראשוני של הגרפים לפי הקטגוריה הראשונה (Work)
                updateProgressBars(categories[0])

                // הגדרת מאזין לשינויים בספינר
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

        // עדכון גרפים וטקסט אחוזים
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