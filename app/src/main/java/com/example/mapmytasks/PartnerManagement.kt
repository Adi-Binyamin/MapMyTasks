package com.example.mapmytasks

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.content.Intent

class PartnerManagement : AppCompatActivity() {

    private lateinit var emailInput: EditText
    private lateinit var grantBtn: Button
    private lateinit var partnersListView: ListView

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // רשימות למעקב אחרי הנתונים
    private val partnerEmails = mutableListOf<String>()
    private val permissionDocIds = mutableListOf<String>() // נשמור את ה-ID של המסמך כדי למחוק אותו בקלות
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner_management)

        emailInput = findViewById(R.id.partnerEmailInput)
        grantBtn = findViewById(R.id.grantPermissionBtn)
        partnersListView = findViewById(R.id.partnersListView) // ודאי שהוספת ListView ב-XML

        setupListView()
        loadExistingPermissions()

        grantBtn.setOnClickListener {
            val friendEmail = emailInput.text.toString().trim().lowercase()
            if (friendEmail.isEmpty()) { toast("אנא הזן אימייל"); return@setOnClickListener }
            if (friendEmail == auth.currentUser?.email) { toast("אינך יכול לתת הרשאה לעצמך"); return@setOnClickListener }

            checkIfUserExistsAndGrant(friendEmail)
        }

        findViewById<Button>(R.id.backBtn).setOnClickListener { finish() }
        val btnGoToSentTasks: Button = findViewById(R.id.btnGoToSentTasks)
        btnGoToSentTasks.setOnClickListener {
            val intent = Intent(this, SentTasksActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupListView() {
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, partnerEmails)
        partnersListView.adapter = adapter

        // לחיצה ארוכה על פריט ברשימה תמחק אותו
        partnersListView.setOnItemLongClickListener { _, _, position, _ ->
            val docId = permissionDocIds[position]
            val email = partnerEmails[position]

            deletePermission(docId, email, position)
            true
        }

        // הערה: כדאי להוסיף הודעה למשתמש ש"לחיצה ארוכה מוחקת שותף"
    }

    private fun loadExistingPermissions() {
        val currentUser = auth.currentUser ?: return

        db.collection("permissions")
            .whereEqualTo("ownerId", currentUser.uid)
            .addSnapshotListener { value, error -> // SnapshotListener מעדכן את הרשימה אוטומטית כשיש שינוי
                if (error != null) return@addSnapshotListener

                partnerEmails.clear()
                permissionDocIds.clear()

                for (doc in value!!) {
                    val email = doc.getString("allowedEditorEmail") ?: continue
                    partnerEmails.add(email)
                    permissionDocIds.add(doc.id)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun checkIfUserExistsAndGrant(friendEmail: String) {
        val currentUser = auth.currentUser ?: return

        db.collection("users")
            .whereEqualTo("email", friendEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    toast("משתמש זה לא רשום באפליקציה")
                } else {
                    val permission = hashMapOf(
                        "ownerId" to currentUser.uid,
                        "ownerEmail" to currentUser.email,
                        "allowedEditorEmail" to friendEmail
                    )

                    db.collection("permissions")
                        .add(permission)
                        .addOnSuccessListener {
                            toast("הצלחה! $friendEmail נוסף לרשימה")
                            emailInput.text.clear()
                        }
                }
            }
    }

    private fun deletePermission(docId: String, email: String, position: Int) {
        db.collection("permissions").document(docId)
            .delete()
            .addOnSuccessListener {
                toast("ההרשאה עבור $email הוסרה")
            }
            .addOnFailureListener { toast("שגיאה במחיקה") }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}