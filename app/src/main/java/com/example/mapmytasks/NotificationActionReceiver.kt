package com.example.mapmytasks

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class NotificationActionReceiver : BroadcastReceiver() {

    // Handles actions from notification buttons
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val userId = intent.getStringExtra("USER_ID") ?: return

        when (intent.action) {
            "MARK_DONE" -> {
                updateTaskStatus(context, userId, taskId, TaskStatus.DONE)
                Toast.makeText(context, "משימה בוצעה ✅", Toast.LENGTH_SHORT).show()
            }
            "DISMISS" -> {
                updateTaskStatus(context, userId, taskId, TaskStatus.MISSED)
                Toast.makeText(context, "משימה נדחתה ❌", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Updates the task status in Firebase
    private fun updateTaskStatus(context: Context, userId: String, taskId: String, status: TaskStatus) {
        val db = FirebaseFirestore.getInstance()
        val taskRef = db.collection("users").document(userId).collection("tasks").document(taskId)
        taskRef.update("status", status.name)
            .addOnSuccessListener {
                Toast.makeText(context, "Task status updated to ${status.name}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error updating task status: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
