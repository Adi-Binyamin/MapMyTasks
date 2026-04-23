package com.example.mapmytasks

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class TasksScreen : AppCompatActivity() {

    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var sortSpinner: Spinner
    private val tasksList = mutableListOf<Task>()
    private lateinit var tasksAdapter: TasksAdapter

    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchTasks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks_screen)

        val serviceIntent = Intent(this, LocationTaskService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        tasksRecyclerView = findViewById(R.id.tasksRecyclerView)
        sortSpinner = findViewById(R.id.sortSpinner)

        tasksAdapter = TasksAdapter(tasksList) { task ->
            val intent = Intent(this, EditTask::class.java)
            intent.putExtra("TASK_ID", task.id)
            val myUid = FirebaseAuth.getInstance().currentUser?.uid
            intent.putExtra("OWNER_ID", myUid)
            editTaskLauncher.launch(intent)
        }

        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.adapter = tasksAdapter

        setupSortSpinner()
        fetchTasks()

        findViewById<Button>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }

    private fun setupSortSpinner() {
        val options = resources.getStringArray(R.array.sort_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter

        sortSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> fetchTasks(sortBy = "date")
                    1 -> fetchTasks(sortBy = "category")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        })
    }

    private fun fetchTasks(sortBy: String = "date") {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("tasks")
            .get()
            .addOnSuccessListener { documents ->
                tasksList.clear()
                val now = Calendar.getInstance()

                for (doc in documents) {
                    val task = doc.toObject(Task::class.java)?.copy(id = doc.id) ?: continue
                    val taskCal = parseTaskDateTime(task.dateTime)
                    task.isActive = taskCal?.after(now) ?: true

                    if (task.isActive) {
                        tasksList.add(task)
                    }
                }

                // --- שלב המיון (קריטי למטריצה) ---
                tasksList.sortWith(compareBy({ parseTaskDateTime(it.dateTime)?.time }))

                // --- שלב הצביעה הציקלית (פותר את הבעיה שהכל כחול) ---
                var dayGroupIndex = -1
                var lastDateString = ""

                for (task in tasksList) {
                    val dateOnly = task.dateTime.split(" ")[0] // מחלץ למשל "24/03/2026"

                    if (dateOnly != lastDateString) {
                        // אם הגענו לתאריך חדש ברשימה הממוינת, מעלים את האינדקס
                        dayGroupIndex++
                        lastDateString = dateOnly
                    }

                    // קובע למשימה אינדקס צבע (0 עד 4) שיישאר קבוע לכל המשימות באותו יום
                    task.colorIndex = dayGroupIndex % 5
                }

                // עדכון האדפטר עם הרשימה ה"צבועה"
                tasksAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("TasksScreen", "Error fetching tasks", exception)
                toast("Failed to load tasks")
            }
    }

    private fun parseTaskDateTime(dateTime: String): Calendar? {
        return try {
            val parts = dateTime.split(" ", "/", ":")
            if (parts.size < 5) return null
            Calendar.getInstance().apply {
                set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(),
                    parts[3].toInt(), parts[4].toInt(), 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}