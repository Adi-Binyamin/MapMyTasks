package com.example.mapmytasks

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

object TaskManager {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val tasks = mutableListOf<Task>()

    // --- פעולות התחברות והרשמה (Auth) ---

    fun registerUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val userMap = hashMapOf("email" to email, "uid" to userId)
                    db.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onSuccess() }
                } else {
                    task.exception?.let { onFailure(it) }
                }
            }
    }

    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess() else task.exception?.let { onFailure(it) }
            }
    }

    fun logoutUser() {
        auth.signOut()
    }

    // --- פעולות כלליות (משתמשים והרשאות) ---

    fun getCurrentUserId(): String? = auth.currentUser?.uid
    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun listenToMyPermissions(onUpdate: (emails: List<String>, docIds: List<String>) -> Unit): ListenerRegistration? {
        val uid = getCurrentUserId() ?: return null
        return db.collection("permissions").whereEqualTo("ownerId", uid)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val emails = mutableListOf<String>()
                val ids = mutableListOf<String>()
                for (doc in value!!) {
                    val email = doc.getString("allowedEditorEmail") ?: continue
                    emails.add(email)
                    ids.add(doc.id)
                }
                onUpdate(emails, ids)
            }
    }

    fun addPermission(friendEmail: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val userEmail = getCurrentUserEmail() ?: return
        val userId = getCurrentUserId() ?: return
        db.collection("users").whereEqualTo("email", friendEmail).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onFailure("משתמש זה לא רשום באפליקציה")
                } else {
                    val permission = hashMapOf("ownerId" to userId, "ownerEmail" to userEmail, "allowedEditorEmail" to friendEmail)
                    db.collection("permissions").add(permission).addOnSuccessListener { onSuccess() }
                }
            }
    }

    fun deletePermission(docId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("permissions").document(docId).delete().addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it) }
    }

    fun getPartners(myEmail: String, onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("permissions").whereEqualTo("allowedEditorEmail", myEmail).get()
            .addOnSuccessListener { result ->
                val partnersList = result.documents.mapNotNull { it.getString("ownerEmail") }.distinct()
                onSuccess(partnersList)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getUserIdByEmail(email: String, onSuccess: (String?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { onSuccess(if (it.isEmpty) null else it.documents[0].id) }
            .addOnFailureListener { onFailure(it) }
    }

    // --- פעולות משימות (Tasks) ---

    fun listenToSentTasksForPartner(myEmail: String, partnerEmail: String, onUpdate: (List<Task>) -> Unit): ListenerRegistration {
        return db.collectionGroup("tasks").whereEqualTo("createdBy", myEmail).whereEqualTo("assignTo", partnerEmail)
            .addSnapshotListener { value, error ->
                val fetchedTasks = value?.documents?.mapNotNull { doc -> doc.toObject(Task::class.java)?.copy(id = doc.id) } ?: emptyList()
                onUpdate(fetchedTasks)
            }
    }

    fun getTasksForUser(userId: String, onSuccess: (List<Task>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId).collection("tasks").get()
            .addOnSuccessListener { result ->
                onSuccess(result.documents.mapNotNull { it.toObject(Task::class.java)?.copy(id = it.id) })
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun fetchTasks(userId: String, onComplete: (() -> Unit)? = null) {
        tasks.clear()
        getTasksForUser(userId, onSuccess = {
            tasks.addAll(it)
            onComplete?.invoke()
        }, onFailure = { Log.e("TaskManager", "Error", it) })
    }

    fun getTask(userId: String, taskId: String, onSuccess: (Task?) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId).collection("tasks").document(taskId).get()
            .addOnSuccessListener { onSuccess(it.toObject(Task::class.java)) }
            .addOnFailureListener { onFailure(it) }
    }

    fun addTask(userId: String, task: Task, onSuccess: ((Task) -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        db.collection("users").document(userId).collection("tasks").add(task)
            .addOnSuccessListener { doc ->
                doc.update("id", doc.id)
                val savedTask = task.copy(id = doc.id)
                tasks.add(savedTask)
                onSuccess?.invoke(savedTask)
            }
            .addOnFailureListener { onFailure?.invoke(it) }
    }

    fun updateTask(userId: String, taskId: String, updatedTask: Task, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId).collection("tasks").document(taskId).set(updatedTask)
            .addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it) }
    }

    fun deleteTask(userId: String, taskId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId).collection("tasks").document(taskId).delete()
            .addOnSuccessListener { onSuccess() }.addOnFailureListener { onFailure(it) }
    }

    fun updateTaskStatus(userId: String, taskId: String, newStatus: TaskStatus, onSuccess: (() -> Unit)? = null, onFailure: ((Exception) -> Unit)? = null) {
        db.collection("users").document(userId).collection("tasks").document(taskId).update("status", newStatus.name)
            .addOnSuccessListener {
                tasks.find { it.id == taskId }?.status = newStatus
                onSuccess?.invoke()
            }
            .addOnFailureListener { onFailure?.invoke(it) }
    }

    fun markTaskDone(userId: String, taskId: String) = updateTaskStatus(userId, taskId, TaskStatus.DONE)
    fun markTaskMissed(userId: String, taskId: String) = updateTaskStatus(userId, taskId, TaskStatus.MISSED)

    // --- פעולות סטטיסטיקה ---

    fun getAllProductivityStats(userId: String, categories: List<String>, onSuccess: (doneMap: Map<String, FloatArray>, totalMap: Map<String, IntArray>) -> Unit) {
        val doneMap = mutableMapOf<String, FloatArray>()
        val totalMap = mutableMapOf<String, IntArray>()
        categories.forEach { doneMap[it] = FloatArray(4); totalMap[it] = IntArray(4) }

        db.collection("users").document(userId).collection("tasks").get().addOnSuccessListener { result ->
            val now = Calendar.getInstance()
            for (doc in result) {
                val task = doc.toObject(Task::class.java)
                val category = categories.find { it.equals(task.category.trim(), ignoreCase = true) } ?: "Other"
                val taskCal = try {
                    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(task.dateTime)
                    Calendar.getInstance().apply { time = date!! }
                } catch (e: Exception) { null } ?: continue

                if (taskCal.after(now)) continue
                val timeIndex = when (taskCal.get(Calendar.HOUR_OF_DAY)) { in 6..11 -> 0; in 12..17 -> 1; in 18..21 -> 2; else -> 3 }
                totalMap[category]?.let { it[timeIndex]++ }
                if (task.status == TaskStatus.DONE) doneMap[category]?.let { it[timeIndex]++ }
            }
            onSuccess(doneMap, totalMap)
        }
    }

    fun getProductivityStats(userId: String, category: String, timeIndex: Int, onSuccess: (total: Int, done: Int) -> Unit) {
        db.collection("users").document(userId).collection("tasks").whereEqualTo("category", category).get()
            .addOnSuccessListener { result ->
                var total = 0
                var done = 0
                for (doc in result) {
                    val status = doc.getString("status")
                    val dateTime = doc.getString("dateTime") ?: continue
                    try {
                        val hour = dateTime.split(" ")[1].split(":")[0].toInt()
                        val currentTaskIndex = when (hour) { in 6..11 -> 0; in 12..17 -> 1; in 18..21 -> 2; else -> 3 }
                        if (currentTaskIndex == timeIndex) {
                            total++
                            if (status == "DONE") done++
                        }
                    } catch (e: Exception) { }
                }
                onSuccess(total, done)
            }
    }

    fun getWeeklySummaryStats(
        userId: String,
        categories: List<String>,
        onSuccess: (doneSlots: FloatArray, pendingSlots: FloatArray, doneByCat: Map<String, Float>, pendingByCat: Map<String, Float>, weekDates: List<String>) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val weekDates = List(7) {
            val d = formatter.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            d
        }

        val weekStart = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("${weekDates.first()} 00:00")
        val weekEnd = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse("${weekDates.last()} 23:59")

        val doneSlots = FloatArray(4) { 0f }
        val pendingSlots = FloatArray(4) { 0f }
        val doneByCat = categories.associateWith { 0f }.toMutableMap()
        val pendingByCat = categories.associateWith { 0f }.toMutableMap()

        db.collection("users").document(userId).collection("tasks").get().addOnSuccessListener { result ->
            for (doc in result) {
                val task = doc.toObject(Task::class.java)
                val cal = try {
                    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(task.dateTime)
                    Calendar.getInstance().apply { time = date!! }
                } catch (e: Exception) { null } ?: continue

                if (cal.time.before(weekStart) || cal.time.after(weekEnd)) continue

                val slotIndex = when (cal.get(Calendar.HOUR_OF_DAY)) { in 6..11 -> 0; in 12..17 -> 1; in 18..21 -> 2; else -> 3 }
                val catKey = categories.find { it.equals(task.category.trim(), ignoreCase = true) } ?: "Other"

                if (task.status == TaskStatus.DONE) {
                    doneSlots[slotIndex]++
                    doneByCat[catKey] = (doneByCat[catKey] ?: 0f) + 1
                } else {
                    pendingSlots[slotIndex]++
                    pendingByCat[catKey] = (pendingByCat[catKey] ?: 0f) + 1
                }
            }
            onSuccess(doneSlots, pendingSlots, doneByCat, pendingByCat, weekDates)
        }
    }

    fun getTasks(): List<Task> = tasks
    fun getTasksByCategory(category: String): List<Task> = tasks.filter { it.category == category }
    fun getTasksByStatus(status: TaskStatus): List<Task> = tasks.filter { it.status == status }
}