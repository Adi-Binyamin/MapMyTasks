package com.example.mapmytasks

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SentTasksActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnBack: Button
    private lateinit var spinnerPartners: Spinner

    private val tasksList = mutableListOf<String>()
    private lateinit var tasksAdapter: ArrayAdapter<String>

    private val partnersList = mutableListOf<String>()
    private lateinit var partnersAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sent_tasks)

        listView = findViewById(R.id.sentTasksListView)
        btnBack = findViewById(R.id.btnBackFromSent)
        spinnerPartners = findViewById(R.id.spinnerPartners)

        tasksAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tasksList)
        listView.adapter = tasksAdapter

        partnersAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, partnersList)
        spinnerPartners.adapter = partnersAdapter

        btnBack.setOnClickListener { finish() }

        val myEmail = Firebase.auth.currentUser?.email
        if (myEmail != null) {
            loadPartnersFromPermissions(myEmail)
        }

        spinnerPartners.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedPartner = partnersList[position]
                if (myEmail != null && selectedPartner != "בחר שותף מרשימת ההרשאות...") {
                    fetchTasksForPartner(myEmail, selectedPartner)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadPartnersFromPermissions(myEmail: String) {
        // חיפוש באוסף permissions לפי השדה שראינו בתמונה שלך
        Firebase.firestore.collection("permissions")
            .whereEqualTo("allowedEditorEmail", myEmail)
            .get()
            .addOnSuccessListener { documents ->
                partnersList.clear()
                partnersList.add("בחר שותף מרשימת ההרשאות...")

                for (doc in documents) {
                    val partnerEmail = doc.getString("ownerEmail")
                    if (partnerEmail != null) {
                        partnersList.add(partnerEmail)
                    }
                }
                partnersAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("CHECK_TASKS", "Error loading partners: ${e.message}")
            }
    }

    private fun fetchTasksForPartner(myEmail: String, partnerEmail: String) {
        Firebase.firestore.collectionGroup("tasks")
            .whereEqualTo("createdBy", myEmail)
            .whereEqualTo("assignTo", partnerEmail)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("CHECK_TASKS", "שגיאה בשליפת נתונים: ${error.message}")
                    return@addSnapshotListener
                }

                tasksList.clear()

                if (value == null || value.isEmpty) {
                    tasksList.add("לא נמצאו משימות ששלחת ל-$partnerEmail")
                } else {
                    for (doc in value) {
                        // שליפת כל הפרטים מהמסמך ב-Firestore
                        val name = doc.getString("name") ?: "ללא שם"
                        val status = doc.getString("status") ?: "PENDING"
                        val category = doc.getString("category") ?: "כללי"
                        val dateTime = doc.getString("dateTime") ?: "לא נקבע זמן"
                        val location = doc.getString("location") ?: "לא נקבע מיקום"

                        val icon = if (status == "DONE") "✅" else "⏳"

                        // יצירת טקסט עשיר שמציג את כל המידע
                        val taskDetails = """
                        $icon משימה: $name
                        📁 קטגוריה: $category
                        📅 זמן: $dateTime
                        📍 מיקום: $location
                        📊 סטטוס: $status
                    """.trimIndent()

                        tasksList.add(taskDetails)
                    }
                }
                tasksAdapter.notifyDataSetChanged()
            }
    }
}