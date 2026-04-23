package com.example.mapmytasks

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
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
    private lateinit var assignToSpinner: Spinner

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        taskNameInput = findViewById(R.id.taskNameInput)
        categorySpinner = findViewById(R.id.categorySpinner)
        dateTimeBtn = findViewById(R.id.dateTimeBtn)
        locationBtn = findViewById(R.id.locationBtn)
        saveTaskBtn = findViewById(R.id.saveTaskBtn)
        assignToSpinner = findViewById(R.id.assignToSpinner)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, placesApiKey)
        }

        loadPartners()
        setupCategorySpinner()
        setupDateTimePicker()
        setupLocationPicker()
        setupSaveButton()

        findViewById<Button>(R.id.backBtn).setOnClickListener { finish() }
        checkPermissions()
    }

    private fun loadPartners() {
        val myEmail = Firebase.auth.currentUser?.email ?: return
        val partnersList = mutableListOf("My Tasks")

        FirebaseFirestore.getInstance().collection("permissions")
            .whereEqualTo("allowedEditorEmail", myEmail)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val ownerEmail = doc.getString("ownerEmail") ?: continue
                    if (!partnersList.contains(ownerEmail)) {
                        partnersList.add(ownerEmail)
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, partnersList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                assignToSpinner.adapter = adapter
            }
            .addOnFailureListener { toast("Error loading partners") }
    }

    // קטע קוד מתוך CreateTask.kt המעודכן
    private fun setupSaveButton() {
        saveTaskBtn.setOnClickListener {
            val taskName = taskNameInput.text.toString().trim()
            if (taskName.isEmpty()) { toast("Please enter task name"); return@setOnClickListener }
            if (selectedDateTime.isEmpty()) { toast("Please choose date"); return@setOnClickListener }

            val selectedOption = assignToSpinner.selectedItem?.toString() ?: "My Tasks"
            val myUid = Firebase.auth.currentUser?.uid ?: return@setOnClickListener
            val myEmail = Firebase.auth.currentUser?.email ?: "Unknown"

            // קביעת מי הנמען של המשימה
            // אם בחרתי "My Tasks", הנמען הוא אני. אחרת, הנמען הוא המייל של השותף מה-Spinner.
            val targetAssignTo = if (selectedOption == "My Tasks") myEmail else selectedOption

            val task = Task(
                name = taskName,
                category = categorySpinner.selectedItem.toString(),
                dateTime = selectedDateTime,
                location = selectedLocation,
                latitude = selectedLat,
                longitude = selectedLng,
                createdBy = myEmail,
                assignTo = targetAssignTo // השדה החדש שהוספת!
            )

            if (selectedOption == "My Tasks") {
                saveToFirestore(myUid, task, "My Tasks")
            } else {
                val partnerEmail = selectedOption.lowercase()
                FirebaseFirestore.getInstance().collection("users")
                    .whereEqualTo("email", partnerEmail)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val partnerUid = documents.documents[0].id
                            saveToFirestore(partnerUid, task, partnerEmail)
                        } else {
                            toast("Partner not found in system")
                        }
                    }
            }
        }
    }

    private fun saveToFirestore(targetUid: String, task: Task, targetName: String) {
        FirebaseFirestore.getInstance().collection("users")
            .document(targetUid)
            .collection("tasks")
            .add(task)
            .addOnSuccessListener { doc ->
                doc.update("id", doc.id)
                toast("Task saved and sent to $targetName")
                scheduleSmartWeatherCheck(task)
                finish()
            }
            .addOnFailureListener { e -> toast("Error: ${e.message}") }
    }

    private fun checkProductivityWarning(category: String, dateTime: String) {
        val user = Firebase.auth.currentUser ?: return
        val hour = try { dateTime.split(" ")[1].split(":")[0].toInt() } catch (e: Exception) { return }

        val timeIndex = when (hour) {
            in 6..11 -> 0
            in 12..17 -> 1
            in 18..21 -> 2
            else -> 3
        }

        FirebaseFirestore.getInstance().collection("users")
            .document(user.uid).collection("tasks")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { result ->
                var total = 0
                var done = 0
                for (doc in result) {
                    val taskDateTime = doc.getString("dateTime") ?: continue
                    val taskHour = try { taskDateTime.split(" ")[1].split(":")[0].toInt() } catch(e:Exception) { 0 }
                    val taskIndex = when (taskHour) {
                        in 6..11 -> 0
                        in 12..17 -> 1
                        in 18..21 -> 2
                        else -> 3
                    }
                    if (taskIndex == timeIndex) {
                        total++
                        if (doc.getString("status") == "DONE") done++
                    }
                }
                if (total >= 3 && (done.toFloat() / total) < 0.5) {
                    val timeNames = listOf("בבוקר", "בצהריים", "בערב", "בלילה")
                    toast("⚠️ שים לב: בדרך כלל את נוטה לא לסיים משימות $category ${timeNames[timeIndex]}. אולי כדאי לשנות שעה?")
                }
            }
    }

    private fun setupDateTimePicker() {
        dateTimeBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                TimePickerDialog(this, { _, h, min ->
                    selectedDateTime = String.format("%02d/%02d/%04d %02d:%02d", d, m + 1, y, h, min)
                    dateTimeBtn.text = selectedDateTime

                    val selectedCategory = categorySpinner.selectedItem.toString()
                    checkProductivityWarning(selectedCategory, selectedDateTime)
                    checkHolidayHebcal(y, m + 1, d)

                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun scheduleSmartWeatherCheck(task: Task) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val taskDate = sdf.parse(task.dateTime) ?: return
            val now = System.currentTimeMillis()
            val targetTime = taskDate.time - (24 * 60 * 60 * 1000L)
            val delay = if (targetTime - now > 0) targetTime - now else 0L

            val data = workDataOf(
                "lat" to task.latitude,
                "lon" to task.longitude,
                "taskName" to task.name,
                "taskDateTime" to task.dateTime
            )

            val workRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
                .setInputData(data)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(this).enqueue(workRequest)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun checkHolidayHebcal(year: Int, month: Int, day: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = "https://www.hebcal.com/hebcal?v=1&cfg=json&maj=on&min=on&mod=on&nx=on&year=$year&month=$month&ss=on&mf=on"
                val response = OkHttpClient().newCall(Request.Builder().url(url).build()).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val items = JSONObject(body).optJSONArray("items")
                    val selectedDateStr = String.format("%04d-%02d-%02d", year, month, day)
                    var holidayName: String? = null
                    if (items != null) {
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            if (item.optString("date").startsWith(selectedDateStr)) {
                                holidayName = item.optString("title")
                                break
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (holidayName != null) toast("שים לב: בתאריך זה חל $holidayName")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Work", "Study", "Personal", "Shopping", "Health", "Finance", "Hobby", "Travel", "School", "Chores", "Other")
        categorySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
    }

    private fun setupLocationPicker() {
        locationBtn.setOnClickListener {
            val fields = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this)
            placePickerLauncher.launch(intent)
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}