package com.example.mapmytasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    private var selectedDateTime: String = ""
    private var selectedLocation: String = ""
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    companion object {
        private const val LOCATION_REQUEST_CODE = 1001
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2001
    }

    // Set up the screen and find all the views
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

    // Open pickers to change the date and time of the task
    private fun setupDateTimePicker() {
        dateTimeBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                TimePickerDialog(this, { _, hour, minute ->
                    selectedDateTime = String.format("%02d/%02d/%04d %02d:%02d", day, month + 1, year, hour, minute)
                    dateTimeBtn.text = selectedDateTime
                    checkHolidayHebcal(year, month + 1, day)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    // Check if the selected date has a Jewish holiday
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

    // Get the current task details from Firebase to show on screen
    private fun loadTaskData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("tasks").document(taskId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    taskNameEdit.setText(doc.getString("name"))
                    selectedDateTime = doc.getString("dateTime") ?: ""
                    dateTimeBtn.text = selectedDateTime
                    selectedLocation = doc.getString("location") ?: ""
                    selectedLat = doc.getDouble("latitude") ?: 0.0
                    selectedLng = doc.getDouble("longitude") ?: 0.0
                    locationBtn.text = selectedLocation
                    val category = doc.getString("category")
                    val spinnerPosition = (categorySpinner.adapter as ArrayAdapter<String>).getPosition(category)
                    categorySpinner.setSelection(spinnerPosition)
                }
            }
    }

    // Update the task in Firebase with the new information
    private fun saveTaskChanges() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
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
            isActive = true
        )

        FirebaseFirestore.getInstance().collection("users").document(userId).collection("tasks").document(taskId)
            .set(updatedTask)
            .addOnSuccessListener {
                toast("Task saved successfully")

                scheduleSmartWeatherCheck(updatedTask)

                setResult(RESULT_OK)
                finish()
            }
    }

    // Reset the weather check timer after editing the task
    private fun scheduleSmartWeatherCheck(task: Task) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val taskDate = sdf.parse(task.dateTime) ?: return
            val now = System.currentTimeMillis()

            val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L
            val delay = (taskDate.time - twentyFourHoursInMillis) - now
            val finalDelay = if (delay > 0) delay else 0L

            val data = workDataOf(
                "lat" to task.latitude,
                "lon" to task.longitude,
                "taskName" to task.name
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

    // Delete this task from Firebase
    private fun deleteTask() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("tasks").document(taskId)
            .delete()
            .addOnSuccessListener {
                WorkManager.getInstance(this).cancelUniqueWork("weather_$taskId")
                setResult(RESULT_OK)
                finish()
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}