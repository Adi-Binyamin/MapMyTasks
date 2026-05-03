package com.example.mapmytasks

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

class EditTask : AppCompatActivity() {

    private lateinit var taskId: String
    private lateinit var taskNameEdit: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var dateTimeBtn: Button
    private lateinit var locationBtn: Button
    private lateinit var saveTaskBtn: Button
    private lateinit var deleteTaskBtn: Button
    private lateinit var backBtn: Button

    private lateinit var createdByTv: TextView
    private var taskOwnerId: String = ""

    private var selectedDateTime: String = ""
    private var selectedLocation: String = ""
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    companion object {
        private const val LOCATION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_task)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyC6eMCaoX_s7YQICNdfXPhpt16TH_QxQzk")
        }

        taskNameEdit = findViewById(R.id.taskNameEdit)
        categorySpinner = findViewById(R.id.categorySpinner)
        dateTimeBtn = findViewById(R.id.dateTimeBtn)
        locationBtn = findViewById(R.id.locationBtn)
        saveTaskBtn = findViewById(R.id.saveTaskBtn)
        deleteTaskBtn = findViewById(R.id.deleteTaskBtn)
        backBtn = findViewById(R.id.backBtn)

        taskId = intent.getStringExtra("TASK_ID") ?: ""

        AppUtils.setupCategorySpinner(this, categorySpinner)
        setupDateTimePicker()
        setupLocationPicker()
        loadTaskData()

        saveTaskBtn.setOnClickListener { saveTaskChanges() }
        deleteTaskBtn.setOnClickListener { deleteTask() }
        backBtn.setOnClickListener { finish() }
    }

    private fun setupDateTimePicker() {
        dateTimeBtn.setOnClickListener {
            DateTimeUtils.showDateTimePicker(this) { formattedDateTime, year, month, day ->
                selectedDateTime = formattedDateTime
                dateTimeBtn.text = selectedDateTime

                val selectedCategory = categorySpinner.selectedItem.toString()

                // קריאות למחלקות העזר:
                AppUtils.checkProductivityWarning(this, selectedCategory, selectedDateTime)
                AppUtils.checkHolidayHebcal(this, year, month, day)
            }
        }
    }

    private fun setupLocationPicker() {
        locationBtn.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this)
            startActivityForResult(intent, LOCATION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val place = Autocomplete.getPlaceFromIntent(data)
            selectedLocation = place.address ?: "Unknown"
            selectedLat = place.latLng?.latitude ?: 0.0
            selectedLng = place.latLng?.longitude ?: 0.0
            locationBtn.text = selectedLocation
        }
    }

    private fun loadTaskData() {
        taskOwnerId = intent.getStringExtra("OWNER_ID") ?: TaskManager.getCurrentUserId() ?: return
        createdByTv = findViewById(R.id.createdByTv)

        TaskManager.getTask(taskOwnerId, taskId, onSuccess = { task ->
            if (task != null) {
                taskNameEdit.setText(task.name)
                selectedDateTime = task.dateTime
                dateTimeBtn.text = selectedDateTime
                selectedLocation = task.location
                selectedLat = task.latitude
                selectedLng = task.longitude
                locationBtn.text = selectedLocation

                val creator = if (task.createdBy.isNotEmpty()) task.createdBy else "You"
                createdByTv.text = "Created by: $creator"
                createdByTv.visibility = android.view.View.VISIBLE

                val spinnerPosition = (categorySpinner.adapter as ArrayAdapter<String>).getPosition(task.category)
                categorySpinner.setSelection(spinnerPosition)
            }
        }, onFailure = { e ->
            toast("Error loading task data")
        })
    }

    private fun saveTaskChanges() {
        if (taskOwnerId.isEmpty()) return

        val taskName = taskNameEdit.text.toString().trim()

        if (taskName.isEmpty() || selectedDateTime.isEmpty() || selectedLocation.isEmpty() || selectedLat == 0.0) {
            // תורגם לאנגלית
            toast("Please fill in all details before saving")
            return
        }

        saveTaskBtn.isEnabled = false

        val updatedTask = Task(
            id = taskId,
            name = taskName,
            category = categorySpinner.selectedItem.toString(),
            dateTime = selectedDateTime,
            location = selectedLocation,
            latitude = selectedLat,
            longitude = selectedLng,
            status = TaskStatus.PENDING,
            isActive = true,
            createdBy = createdByTv.text.toString().replace("Created by: ", "")
        )

        TaskManager.updateTask(taskOwnerId, taskId, updatedTask, onSuccess = {
            toast("Task updated successfully")
            // הנה התיקון החשוב שלנו! עכשיו משתמשים במחלקה החדשה והאפור נעלם.
            WeatherWorker.scheduleWeatherWorker(this, updatedTask)
            setResult(RESULT_OK)
            finish()
        }, onFailure = { e ->
            toast("Failed to update task")
            saveTaskBtn.isEnabled = true
        })
    }

    private fun deleteTask() {
        if (taskOwnerId.isEmpty()) return

        TaskManager.deleteTask(taskOwnerId, taskId, onSuccess = {
            WorkManager.getInstance(this).cancelUniqueWork("weather_$taskId")
            toast("Task deleted")
            setResult(RESULT_OK)
            finish()
        }, onFailure = { e ->
            toast("Failed to delete task")
        })
    }
}