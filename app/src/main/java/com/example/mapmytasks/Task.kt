package com.example.mapmytasks

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

data class Task(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val dateTime: String = "",     // "dd/MM/yyyy HH:mm"
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var status: TaskStatus = TaskStatus.PENDING,
    var isActive: Boolean = false
)

enum class TaskStatus {
    PENDING,
    DONE,
    MISSED
}

// Updates task status in Firebase
fun updateTaskStatus(taskId: String, newStatus: TaskStatus) {
    val db = FirebaseFirestore.getInstance()
    val taskRef = db.collection("tasks").document(taskId)

    taskRef.update("status", newStatus.name)
        .addOnSuccessListener {
            Log.d("TaskUpdate", "Task status updated to $newStatus")
        }
        .addOnFailureListener { e ->
            Log.e("TaskUpdate", "Failed to update task status", e)
        }
}
