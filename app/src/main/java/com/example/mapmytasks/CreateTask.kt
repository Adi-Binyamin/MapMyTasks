package com.example.mapmytasks

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CreateTask : AppCompatActivity() {

    private lateinit var taskNameInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var dateTimeBtn: Button
    private lateinit var locationBtn: Button
    private lateinit var saveTaskBtn: Button

    private var selectedDateTime = ""
    private var selectedLocation = ""
    private var selectedLat = 0.0
    private var selectedLng = 0.0

    private val placesApiKey = "AIzaSyC6eMCaoX_s7YQICNdfXPhpt16TH_QxQzk"

    private val requestPermissionLauncher =

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val placePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val place = Autocomplete.getPlaceFromIntent(data)
                if (place.latLng != null) {
                    selectedLocation = place.address ?: place.name ?: "Selected location"
                    selectedLat = place.latLng!!.latitude
                    selectedLng = place.latLng!!.longitude
                    locationBtn.text = selectedLocation
                } else {
                    toast("Location not valid")
                }
            }
        }

    // Start everything and find the views
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, placesApiKey)
        }


        taskNameInput = findViewById(R.id.taskNameInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        dateTimeBtn = findViewById(R.id.dateTimeBtn)
        locationBtn = findViewById(R.id.locationBtn)
        saveTaskBtn = findViewById(R.id.saveTaskBtn)

        setupCategorySpinner()
        setupDateTimePicker()
        setupLocationPicker()
        setupSaveButton()

        findViewById<Button>(R.id.backBtn).setOnClickListener { finish() }
        checkPermissions()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Work", "Study", "Personal", "Shopping", "Health", "Finance", "Hobby", "Travel", "School", "Chores", "Other")
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
    }

    // Opens date and time picker to choose when the task is
    private fun setupDateTimePicker() {
        dateTimeBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                TimePickerDialog(this, { _, h, min ->
                    selectedDateTime = String.format("%02d/%02d/%04d %02d:%02d", d, m + 1, y, h, min)
                    dateTimeBtn.text = selectedDateTime
                    checkHolidayHebcal(y, m + 1, d)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    // Get holiday info from Hebcal to see if today is a special day
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
                        if (foundHoliday != null) toast(" שים לב: בתאריך זה חל $foundHoliday")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupLocationPicker() {
        locationBtn.setOnClickListener {
            val fields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this)
            placePickerLauncher.launch(intent)
        }
    }

    // Saves the task info to Firebase if all fields are ok
    private fun setupSaveButton() {
        saveTaskBtn.setOnClickListener {
            val taskName = taskNameInput.text.toString().trim()
            if (taskName.isEmpty()) { toast("Please enter task name"); return@setOnClickListener }
            if (selectedDateTime.isEmpty()) { toast("Please choose date"); return@setOnClickListener }
            if (selectedLat == 0.0) { toast("Please choose location"); return@setOnClickListener }

            val userId = Firebase.auth.currentUser?.uid ?: return@setOnClickListener

            val task = Task(
                name = taskName,
                category = categorySpinner.selectedItem.toString(),
                dateTime = selectedDateTime,
                location = selectedLocation,
                latitude = selectedLat,
                longitude = selectedLng
            )

            FirebaseFirestore.getInstance().collection("users")
                .document(userId).collection("tasks")
                .add(task)
                .addOnSuccessListener { doc ->
                    doc.update("id", doc.id)
                    toast("Task saved!")

                    scheduleSmartWeatherCheck(task)

                    finish()
                }
                .addOnFailureListener { e -> toast("Error: ${e.message}") }
        }
    }

    // Calculate time to check the weather before the task starts
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
                "taskName" to task.name
            )

            val workRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(data)
                .setInitialDelay(finalDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(this).enqueue(workRequest)

            Log.d("WeatherSchedule", "Worker scheduled with delay of ${finalDelay/1000/60} minutes")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}