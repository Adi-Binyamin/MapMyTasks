package com.example.mapmytasks

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.AdapterView
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

    // Handles result from EditTask screen
    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchTasks()
        }
    }

    // Sets up UI and starts location service
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
            editTaskLauncher.launch(intent)
        }

        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.adapter = tasksAdapter

        setupSortSpinner()
        fetchTasks()

        val backBtn = findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }
    }

    // Sets up the sorting spinner
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

    // Fetches tasks from Firebase and sorts them
    private fun fetchTasks(sortBy: String = "date") {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).collection("tasks")
            .get()
            .addOnSuccessListener { documents ->
                tasksList.clear()
                val now = Calendar.getInstance()

                for (doc in documents) {
                    val task = doc.toObject(Task::class.java).copy(id = doc.id)

                    val taskCal = parseTaskDateTime(task.dateTime)
                    task.isActive = taskCal?.after(now) ?: true

                    if (task.isActive) {
                        tasksList.add(task)
                    }
                }

                when (sortBy) {
                    "date" -> tasksList.sortBy { parseTaskDateTime(it.dateTime)?.time }
                    "category" -> tasksList.sortBy { it.category }
                }

                tasksAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    // Parses task date string into a Calendar object
    private fun parseTaskDateTime(dateTime: String): Calendar? {
        val parts = dateTime.split(" ", "/", ":")
        if (parts.size < 5) return null
        return Calendar.getInstance().apply {
            set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(),
                parts[3].toInt(), parts[4].toInt(), 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
