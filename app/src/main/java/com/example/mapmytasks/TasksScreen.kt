package com.example.mapmytasks

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
            intent.putExtra("OWNER_ID", TaskManager.getCurrentUserId())
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

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                fetchTasks(sortBy = if (position == 0) "date" else "category")
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchTasks(sortBy: String = "date") {
        val userId = TaskManager.getCurrentUserId() ?: return

        TaskManager.getTasksForUser(userId, onSuccess = { fetchedTasks ->
            tasksList.clear()
            val now = Calendar.getInstance()

            for (task in fetchedTasks) {
                val taskCal = parseTaskDateTime(task.dateTime)
                task.isActive = taskCal?.after(now) ?: true

                if (task.isActive) {
                    tasksList.add(task)
                }
            }

            // מיון לפי תאריך/קטגוריה
            if (sortBy == "date") {
                tasksList.sortWith(compareBy({ parseTaskDateTime(it.dateTime)?.time }))
            } else {
                tasksList.sortBy { it.category }
            }

            // חישוב צבעים ציקליים לפי ימים
            var dayGroupIndex = -1
            var lastDateString = ""

            for (task in tasksList) {
                val dateOnly = task.dateTime.split(" ")[0]
                if (dateOnly != lastDateString) {
                    dayGroupIndex++
                    lastDateString = dateOnly
                }
                task.colorIndex = dayGroupIndex % 5
            }

            tasksAdapter.notifyDataSetChanged()
        }, onFailure = {
            toast("Failed to load tasks")
        })
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
        } catch (e: Exception) { null }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}