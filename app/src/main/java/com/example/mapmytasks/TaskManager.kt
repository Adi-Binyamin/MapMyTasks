package com.example.mapmytasks

import com.google.firebase.firestore.FirebaseFirestore

object TaskManager {

    private val db = FirebaseFirestore.getInstance()
    private val tasks = mutableListOf<Task>()

    // Gets all tasks from Firestore
    fun fetchTasks(userId: String, onComplete: (() -> Unit)? = null) {
        tasks.clear()
        db.collection("users")
            .document(userId)
            .collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result.documents) {
                    val task = doc.toObject(Task::class.java)?.copy(id = doc.id)
                    task?.let { tasks.add(it) }
                }
                onComplete?.invoke()
            }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    // Adds a new task to Firestore and local list
    fun addTask(userId: String, task: Task, onComplete: (() -> Unit)? = null) {
        db.collection("users")
            .document(userId)
            .collection("tasks")
            .add(task)
            .addOnSuccessListener { docRef ->
                val savedTask = task.copy(id = docRef.id)
                tasks.add(savedTask)
                onComplete?.invoke()
            }
            .addOnFailureListener { e -> e.printStackTrace() }
    }

    // Marks a task as done
    fun markTaskDone(userId: String, taskId: String) {
        val task = tasks.find { it.id == taskId } ?: return
        task.status = TaskStatus.DONE

        db.collection("users")
            .document(userId)
            .collection("tasks")
            .document(taskId)
            .update("status", TaskStatus.DONE.name)
    }

    // Marks a task as missed
    fun markTaskMissed(userId: String, taskId: String) {
        val task = tasks.find { it.id == taskId } ?: return
        task.status = TaskStatus.MISSED

        db.collection("users")
            .document(userId)
            .collection("tasks")
            .document(taskId)
            .update("status", TaskStatus.MISSED.name)
    }

    // Returns all tasks
    fun getTasks(): List<Task> = tasks

    // Returns tasks in a specific category
    fun getTasksByCategory(category: String): List<Task> =
        tasks.filter { it.category == category }

    // Returns tasks with a specific status
    fun getTasksByStatus(status: TaskStatus): List<Task> =
        tasks.filter { it.status == status }
}
