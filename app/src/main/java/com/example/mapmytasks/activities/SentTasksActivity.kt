package com.example.mapmytasks.activities

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mapmytasks.R
import com.example.mapmytasks.data.TaskManager
import com.example.mapmytasks.models.TaskStatus
import com.example.mapmytasks.utilities.toast

import com.google.firebase.firestore.ListenerRegistration

/**
 * SentTasksActivity allows the user to view tasks they have assigned to other authorized partners.
 * It uses a spinner to select a partner and a list view to display real-time task updates.
 */
class SentTasksActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnBack: Button
    private lateinit var spinnerPartners: Spinner

    private val tasksList = mutableListOf<String>()
    private lateinit var tasksAdapter: ArrayAdapter<String>

    private val partnersList = mutableListOf<String>()
    private lateinit var partnersAdapter: ArrayAdapter<String>

    private var sentTasksListener: ListenerRegistration? = null

    // Initializes the UI components, sets up adapters, and triggers the loading of partners.
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
                if (myEmail != null && selectedPartner != "Select partner from permissions list...") {
                    startListeningToTasks(myEmail, selectedPartner)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // Fetches the list of authorized partners from Firestore and populates the spinner.
    private fun loadPartners(myEmail: String) {
        TaskManager.getPartners(myEmail, onSuccess = { partners ->
            partnersList.clear()
            partnersList.add("Select partner from permissions list...")
            partnersList.addAll(partners)
            partnersAdapter.notifyDataSetChanged()
        }, onFailure = {
            toast("Error loading partners")

        })
    }

    // Attaches a real-time listener to fetch and format tasks assigned to the selected partner.
    private fun startListeningToTasks(myEmail: String, partnerEmail: String) {
        // Removes any existing listener before starting a new one to avoid duplicate data streams.
        sentTasksListener?.remove()

        sentTasksListener =
            TaskManager.listenToSentTasksForPartner(myEmail, partnerEmail) { tasks ->
                tasksList.clear()
                if (tasks.isEmpty()) {
                    tasksList.add("No tasks found sent to $partnerEmail")
                } else {
                    for (task in tasks) {
                        val icon = if (task.status == TaskStatus.DONE) "✅" else "⏳"
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

    // Cleans up the Firestore listener to prevent memory leaks when the activity is closed.
    override fun onDestroy() {
        super.onDestroy()
        sentTasksListener?.remove()
    }
}