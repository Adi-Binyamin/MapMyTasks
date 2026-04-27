package com.example.mapmytasks

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
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

    // שומרים את מצב המיון הנוכחי
    private var currentSortBy: String = "date"

    private val editTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchTasks(currentSortBy)
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
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSortBy = if (position == 0) "date" else "category"
                fetchTasks(sortBy = currentSortBy)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchTasks(sortBy: String = currentSortBy) {
        val userId = TaskManager.getCurrentUserId() ?: return

        TaskManager.getTasksForUser(userId, onSuccess = { fetchedTasks ->
            tasksList.clear()
            val now = Calendar.getInstance()

            for (task in fetchedTasks) {
                val taskCal = DateTimeUtils.parseDateTime(task.dateTime)
                task.isActive = taskCal?.after(now) ?: true

                if (task.isActive) {
                    tasksList.add(task)
                }
            }

            // העדכון החשוב! השתמשנו בשם המשתנה החדש של האדפטר:
            tasksAdapter.currentSortMethod = sortBy

            // ביצוע המיון בפועל - משתמשים ב-sortWith כמו קודם
            if (sortBy == "date") {
                tasksList.sortWith(compareBy { DateTimeUtils.parseDateTime(it.dateTime)?.time })
            } else {
                tasksList.sortWith(compareBy({ it.category.lowercase() }, { DateTimeUtils.parseDateTime(it.dateTime)?.time }))
            }

            // חישוב צבעים ציקליים לפי הקבוצה
            var groupIndex = -1
            var lastGroupKey = ""

            for (task in tasksList) {
                val currentGroupKey = if (sortBy == "date") task.dateTime.split(" ")[0] else task.category
                if (currentGroupKey != lastGroupKey) {
                    groupIndex++
                    lastGroupKey = currentGroupKey
                }
                task.colorIndex = groupIndex % 5
            }

            tasksAdapter.notifyDataSetChanged()
        }, onFailure = {
            toast("Failed to load tasks")
        })
    }
}