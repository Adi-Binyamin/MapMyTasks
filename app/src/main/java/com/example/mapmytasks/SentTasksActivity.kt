package com.example.mapmytasks

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ListenerRegistration

class SentTasksActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnBack: Button
    private lateinit var spinnerPartners: Spinner

    private val tasksList = mutableListOf<String>()
    private lateinit var tasksAdapter: ArrayAdapter<String>

    private val partnersList = mutableListOf<String>()
    private lateinit var partnersAdapter: ArrayAdapter<String>

    private var sentTasksListener: ListenerRegistration? = null

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

        val myEmail = TaskManager.getCurrentUserEmail()
        if (myEmail != null) {
            loadPartners(myEmail)
        }

        spinnerPartners.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedPartner = partnersList[position]
                if (myEmail != null && selectedPartner != "בחר שותף מרשימת ההרשאות...") {
                    startListeningToTasks(myEmail, selectedPartner)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadPartners(myEmail: String) {
        TaskManager.getPartners(myEmail, onSuccess = { partners ->
            partnersList.clear()
            partnersList.add("בחר שותף מרשימת ההרשאות...")
            partnersList.addAll(partners)
            partnersAdapter.notifyDataSetChanged()
        }, onFailure = {
            toast("Error loading partners")
        })
    }

    private fun startListeningToTasks(myEmail: String, partnerEmail: String) {
        // ניקוי מאזין קודם אם קיים
        sentTasksListener?.remove()

        sentTasksListener = TaskManager.listenToSentTasksForPartner(myEmail, partnerEmail) { tasks ->
            tasksList.clear()
            if (tasks.isEmpty()) {
                tasksList.add("לא נמצאו משימות ששלחת ל-$partnerEmail")
            } else {
                for (task in tasks) {
                    val icon = if (task.status == TaskStatus.DONE) "✅" else "⏳"
                    val taskDetails = """
                        $icon משימה: ${task.name}
                        📁 קטגוריה: ${task.category}
                        📅 זמן: ${task.dateTime}
                        📍 מיקום: ${task.location}
                        📊 סטטוס: ${task.status}
                    """.trimIndent()
                    tasksList.add(taskDetails)
                }
            }
            tasksAdapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sentTasksListener?.remove()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}