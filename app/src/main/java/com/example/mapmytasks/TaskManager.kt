package com.example.mapmytasks

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object TaskManager {

    private val db = FirebaseFirestore.getInstance()
    private val tasks = mutableListOf<Task>()

    // משיכת כל המשימות של המשתמש
    fun fetchTasks(userId: String, onComplete: (() -> Unit)? = null) {
        tasks.clear()
        db.collection("users")
            .document(userId)
            .collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result.documents) {
                    // חשוב: copy(id = doc.id) מוודא שה-ID מה-Firebase נכנס לאובייקט
                    val task = doc.toObject(Task::class.java)?.copy(id = doc.id)
                    task?.let { tasks.add(it) }
                }
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e("TaskManager", "Error fetching tasks", e)
            }
    }

    // הוספת משימה - שים לב שהוספנו עדכון ל-ID מיד לאחר השמירה
    fun addTask(userId: String, task: Task, onComplete: (() -> Unit)? = null) {
        db.collection("users")
            .document(userId)
            .collection("tasks")
            .add(task)
            .addOnSuccessListener { docRef ->
                // עדכון ה-ID בתוך המסמך עצמו ב-Firebase כדי שיהיה מסונכרן
                docRef.update("id", docRef.id)

                val savedTask = task.copy(id = docRef.id)
                tasks.add(savedTask)
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e("TaskManager", "Error adding task", e)
            }
    }

    // עדכון סטטוס משימה ל-"בוצע"
    fun markTaskDone(userId: String, taskId: String) {
        updateStatus(userId, taskId, TaskStatus.DONE)
    }

    // עדכון סטטוס משימה ל-"פוספס"
    fun markTaskMissed(userId: String, taskId: String) {
        updateStatus(userId, taskId, TaskStatus.MISSED)
    }

    // פונקציית עזר פרטית למניעת חזרתיות בקוד
    private fun updateStatus(userId: String, taskId: String, newStatus: TaskStatus) {
        val task = tasks.find { it.id == taskId }
        task?.status = newStatus

        db.collection("users")
            .document(userId)
            .collection("tasks")
            .document(taskId)
            .update("status", newStatus.name)
            .addOnFailureListener { e ->
                Log.e("TaskManager", "Error updating status", e)
            }
    }

    fun getTasks(): List<Task> = tasks

    fun getTasksByCategory(category: String): List<Task> =
        tasks.filter { it.category == category }

    fun getTasksByStatus(status: TaskStatus): List<Task> =
        tasks.filter { it.status == status }
}