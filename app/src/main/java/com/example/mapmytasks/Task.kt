package com.example.mapmytasks

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

data class Task(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val dateTime: String = "",     // פורמט מצופה: "dd/MM/yyyy HH:mm"
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var status: TaskStatus = TaskStatus.PENDING,
    var isActive: Boolean = false,
    val createdBy: String = "",
    val assignTo: String = "",

    // השדה החדש שישמש אותנו לצביעת המטריצה היומית
    // אנחנו לא קובעים אותו ב-Firebase, אלא מחשבים אותו באפליקציה לפי סדר הימים
    var colorIndex: Int = 0
)

enum class TaskStatus {
    PENDING,
    DONE,
    MISSED
}

/**
 * פונקציה לעדכון סטטוס המשימה ב-Firebase
 */
fun updateTaskStatus(userId: String, taskId: String, newStatus: TaskStatus) {
    val db = FirebaseFirestore.getInstance()

    val taskRef = db.collection("users")
        .document(userId)
        .collection("tasks")
        .document(taskId)

    taskRef.update("status", newStatus.name)
        .addOnSuccessListener {
            Log.d("TaskUpdate", "Task status updated to $newStatus")
        }
        .addOnFailureListener { e ->
            Log.e("TaskUpdate", "Failed to update task status", e)
        }
}