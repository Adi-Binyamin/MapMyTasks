package com.example.mapmytasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

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
        AppUtils.setupCategorySpinner(this, categorySpinner)
        setupDateTimePicker()
        setupLocationPicker()
        setupSaveButton()

        findViewById<Button>(R.id.backBtn).setOnClickListener { finish() }
        checkPermissions()
    }

    private fun loadPartners() {
        val myEmail = TaskManager.getCurrentUserEmail() ?: return
        val partnersList = mutableListOf("My Tasks")

        TaskManager.getPartners(myEmail, onSuccess = { partners ->
            partnersList.addAll(partners)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, partnersList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            assignToSpinner.adapter = adapter
        }, onFailure = {
            toast("Error loading partners")
        })
    }

    private fun setupSaveButton() {
        saveTaskBtn.setOnClickListener {
            // 1. איסוף הנתונים מהשדות
            val taskName = taskNameInput.text.toString().trim()
            val selectedOption = assignToSpinner.selectedItem?.toString() ?: "My Tasks"

            // 2. בדיקת תקינות מקיפה (ולידציה)
            // בדקנו: שם, תאריך, תיאור המיקום, וקואורדינטות (לוודא שבאמת נבחר מיקום מהרשימה)
            if (taskName.isEmpty() || selectedDateTime.isEmpty() || selectedLocation.isEmpty() || selectedLat == 0.0) {
                // תורגם לאנגלית
                toast("Please fill in all details, including selecting a location from the list")
                return@setOnClickListener // עוצר כאן ולא ממשיך לשמירה
            }

            // 3. מניעת לחיצות כפולות - נעילת הכפתור
            saveTaskBtn.isEnabled = false

            val myUid = TaskManager.getCurrentUserId() ?: run {
                saveTaskBtn.isEnabled = true
                return@setOnClickListener
            }
            val myEmail = TaskManager.getCurrentUserEmail() ?: "Unknown"
            val targetAssignTo = if (selectedOption == "My Tasks") myEmail else selectedOption

            val task = Task(
                name = taskName,
                category = categorySpinner.selectedItem.toString(),
                dateTime = selectedDateTime,
                location = selectedLocation,
                latitude = selectedLat,
                longitude = selectedLng,
                createdBy = myEmail,
                assignTo = targetAssignTo
            )

            // 4. תהליך השמירה
            if (selectedOption == "My Tasks") {
                saveToFirestore(myUid, task, "My Tasks")
                // הערה: בתוך saveToFirestore, אם הכל מצליח, אתה בטח סוגר את המסך (finish)
                // אם יש שם שגיאה, כדאי להוסיף שם saveTaskBtn.isEnabled = true
            } else {
                val partnerEmail = selectedOption.lowercase()
                TaskManager.getUserIdByEmail(partnerEmail, onSuccess = { partnerUid ->
                    if (partnerUid != null) {
                        saveToFirestore(partnerUid, task, partnerEmail)
                    } else {
                        // תורגם לאנגלית
                        toast("Partner not found in the system")
                        saveTaskBtn.isEnabled = true // משחררים את הכפתור לתיקון
                    }
                }, onFailure = {
                    // תורגם לאנגלית
                    toast("Error searching for partner")
                    saveTaskBtn.isEnabled = true // משחררים את הכפתור לניסיון חוזר
                })
            }
        }
    }

    private fun saveToFirestore(targetUid: String, task: Task, targetName: String) {
        TaskManager.addTask(targetUid, task, onSuccess = { updatedTask ->
            toast("Task saved and sent to $targetName")
            // הנה התיקון שלנו! משתמשים במחלקה החדשה במקום בפונקציה שנמחקה מ-AppUtils
            WeatherWorker.scheduleWeatherWorker(this, updatedTask)
            finish()
        }, onFailure = { e ->
            toast("Error: ${e.message}")
            saveTaskBtn.isEnabled = true // חשוב: אם השמירה ל-Firestore נכשלה, נשחרר את הכפתור כדי שהמשתמש יוכל לנסות שוב
        })
    }

    private fun setupDateTimePicker() {
        dateTimeBtn.setOnClickListener {
            DateTimeUtils.showDateTimePicker(this) { formattedDateTime, year, month, day ->
                selectedDateTime = formattedDateTime
                dateTimeBtn.text = selectedDateTime

                val selectedCategory = categorySpinner.selectedItem.toString()

                // קריאות למחלקות העזר המרכזיות שלנו:
                AppUtils.checkProductivityWarning(this, selectedCategory, selectedDateTime)
                AppUtils.checkHolidayHebcal(this, year, month, day)
            }
        }
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
}