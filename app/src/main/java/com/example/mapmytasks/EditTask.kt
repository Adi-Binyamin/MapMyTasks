package com.example.mapmytasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

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

        setupCategorySpinner()
        setupDateTimePicker()
        setupLocationPicker()
        loadTaskData()

        saveTaskBtn.setOnClickListener { saveTaskChanges() }
        deleteTaskBtn.setOnClickListener { deleteTask() }
        backBtn.setOnClickListener { finish() }
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Work", "Study", "Personal", "Shopping", "Health", "Finance", "Hobby", "Travel", "School", "Chores", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun checkProductivityWarning(category: String, dateTime: String) {
        val userId = TaskManager.getCurrentUserId() ?: return

        val hour = try {
            dateTime.split(" ")[1].split(":")[0].toInt()
        } catch (e: Exception) { return }

        val timeIndex = when (hour) {
            in 6..11 -> 0
            in 12..17 -> 1
            in 18..21 -> 2
            else -> 3
        }

        TaskManager.getProductivityStats(userId, category, timeIndex) { total, done ->
            if (total >= 3 && (done.toFloat() / total) < 0.5) {
                val timeNames = listOf("בבוקר", "בצהריים", "בערב", "בלילה")
                toast("⚠️ שים לב: בדרך כלל את נוטה לא לסיים משימות $category ${timeNames[timeIndex]}. אולי כדאי לשנות שעה?")
            }
        }
    }

    private fun setupDateTimePicker() {
        dateTimeBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->
                    selectedDateTime = String.format("%02d/%02d/%04d %02d:%02d", day, month + 1, year, hour, minute)
                    dateTimeBtn.text = selectedDateTime

                    checkProductivityWarning(categorySpinner.selectedItem.toString(), selectedDateTime)
                    checkHolidayHebcal(year, month + 1, day)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun checkHolidayHebcal(year: Int, month: Int, day: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://www.hebcal.com/hebcal?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=$year&month=$month&ss=on&mf=on"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    val items = json.optJSONArray("items")
                    val selectedDateStr = String.format("%04d-%02d-%02d", year, month, day)
                    var foundHoliday: String? = null

                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            if (item.optString("date").startsWith(selectedDateStr)) {
                                foundHoliday = item.optString("title")
                                break
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (foundHoliday != null) toast("שים לב: בתאריך זה חל $foundHoliday")
                    }
                }
            } catch (e: Exception) { Log.e("HolidayCheck", "Error: ${e.message}") }
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
            scheduleSmartWeatherCheck(updatedTask)
            setResult(RESULT_OK)
            finish()
        }, onFailure = { e ->
            toast("Failed to update task")
        })
    }

    private fun scheduleSmartWeatherCheck(task: Task) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val taskDate = sdf.parse(task.dateTime) ?: return
            val now = System.currentTimeMillis()

            val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L
            val targetTime = taskDate.time - twentyFourHoursInMillis
            val delay = targetTime - now
            val finalDelay = if (delay > 0) delay else 0L

            val data = workDataOf(
                "lat" to task.latitude,
                "lon" to task.longitude,
                "taskName" to task.name,
                "taskDateTime" to task.dateTime
            )

            val workRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(data)
                .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "weather_$taskId",
                androidx.work.ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } catch (e: Exception) { e.printStackTrace() }
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

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}