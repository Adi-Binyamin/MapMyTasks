package com.example.mapmytasks

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
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
                // Match the English default text
                if (myEmail != null && selectedPartner != "Select partner from permissions list...") {
                    startListeningToTasks(myEmail, selectedPartner)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadPartners(myEmail: String) {
        TaskManager.getPartners(myEmail, onSuccess = { partners ->
            partnersList.clear()
            // Translated default option
            partnersList.add("Select partner from permissions list...")
            partnersList.addAll(partners)
            partnersAdapter.notifyDataSetChanged()
        }, onFailure = {
            toast("Error loading partners")
        })
    }

    private fun startListeningToTasks(myEmail: String, partnerEmail: String) {
        // Clean up previous listener if exists
        sentTasksListener?.remove()

        sentTasksListener = TaskManager.listenToSentTasksForPartner(myEmail, partnerEmail) { tasks ->
            tasksList.clear()
            if (tasks.isEmpty()) {
                // Translated empty state
                tasksList.add("No tasks found sent to $partnerEmail")
            } else {
                for (task in tasks) {
                    val icon = if (task.status == TaskStatus.DONE) "✅" else "⏳"
                    // Translated task details layout
                    val taskDetails = """
                        $icon Task: ${task.name}
                        📁 Category: ${task.category}
                        📅 Time: ${task.dateTime}
                        📍 Location: ${task.location}
                        📊 Status: ${task.status}
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