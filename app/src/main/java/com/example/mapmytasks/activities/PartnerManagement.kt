package com.example.mapmytasks.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.example.mapmytasks.R
import com.example.mapmytasks.data.TaskManager
import com.google.firebase.firestore.ListenerRegistration

class PartnerManagement : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var grantBtn: Button
    private lateinit var partnersListView: ListView

    private val partnerEmails = mutableListOf<String>()
    private val permissionDocIds = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var listenerRegistration: ListenerRegistration? = null

    // Initializes the UI components, sets up the list view, and handles button clicks for navigation and granting permissions.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner_management)

        emailInput = findViewById(R.id.partnerEmailInput)
        grantBtn = findViewById(R.id.grantPermissionBtn)
        partnersListView = findViewById(R.id.partnersListView)

        setupListView()
        loadExistingPermissions()

        grantBtn.setOnClickListener {
            val friendEmail = emailInput.text.toString().trim().lowercase()

            // Validates the input to prevent empty submissions or self-assignments.
            if (friendEmail.isEmpty()) { toast("Please enter an email"); return@setOnClickListener }
            if (friendEmail == TaskManager.getCurrentUserEmail()) { toast("You cannot grant permission to yourself"); return@setOnClickListener }

            TaskManager.addPermission(
                friendEmail,
                onSuccess = {
                    toast("Success! $friendEmail added to the list")
                    emailInput.text.clear()
                },
                onFailure = { errorMsg ->
                    toast(errorMsg)
                }
            )
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnGoToSentTasks).setOnClickListener {
            startActivity(Intent(this, SentTasksActivity::class.java))
        }
    }

    // Configures the ListView adapter and sets a long-click listener to allow users to revoke permissions.
    private fun setupListView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, partnerEmails)
        partnersListView.adapter = adapter

        partnersListView.setOnItemLongClickListener { _, _, position, _ ->
            val docId = permissionDocIds[position]
            val email = partnerEmails[position]

            TaskManager.deletePermission(
                docId,
                onSuccess = { toast("Permission for $email removed") },
                onFailure = { toast("Error during deletion") }
            )
            true
        }
    }

    // Sets up a real-time Firestore listener to automatically update the list whenever permissions change.
    private fun loadExistingPermissions() {
        listenerRegistration = TaskManager.listenToMyPermissions { emails, ids ->
            partnerEmails.clear()
            permissionDocIds.clear()
            partnerEmails.addAll(emails)
            permissionDocIds.addAll(ids)
            adapter.notifyDataSetChanged()
        }
    }

    // Removes the real-time listener to prevent memory leaks when the activity is closed.
    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }

    // Helper function to display short toast messages.
    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}