package com.example.mapmytasks

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import com.google.firebase.firestore.ListenerRegistration

class PartnerManagement : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var grantBtn: Button
    private lateinit var partnersListView: ListView

    private val partnerEmails = mutableListOf<String>()
    private val permissionDocIds = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var listenerRegistration: ListenerRegistration? = null

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
            if (friendEmail.isEmpty()) { toast("אנא הזן אימייל"); return@setOnClickListener }
            if (friendEmail == TaskManager.getCurrentUserEmail()) { toast("אינך יכול לתת הרשאה לעצמך"); return@setOnClickListener }

            TaskManager.addPermission(friendEmail,
                onSuccess = {
                    toast("הצלחה! $friendEmail נוסף לרשימה")
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

    private fun setupListView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, partnerEmails)
        partnersListView.adapter = adapter

        partnersListView.setOnItemLongClickListener { _, _, position, _ ->
            val docId = permissionDocIds[position]
            val email = partnerEmails[position]

            TaskManager.deletePermission(docId,
                onSuccess = { toast("ההרשאה עבור $email הוסרה") },
                onFailure = { toast("שגיאה במחיקה") }
            )
            true
        }
    }

    private fun loadExistingPermissions() {
        listenerRegistration = TaskManager.listenToMyPermissions { emails, ids ->
            partnerEmails.clear()
            permissionDocIds.clear()
            partnerEmails.addAll(emails)
            permissionDocIds.addAll(ids)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove() // ניקוי המאזין כשהמסך נסגר
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}