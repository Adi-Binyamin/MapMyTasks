package com.example.mapmytasks.models

// Data model representing a single task with all its properties, including scheduling, location, and assignment details.
data class Task(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val dateTime: String = "",     // Expected format: "dd/MM/yyyy HH:mm"
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var status: TaskStatus = TaskStatus.PENDING,
    var isActive: Boolean = false,
    val createdBy: String = "",
    val assignTo: String = "",
    var colorIndex: Int = 0
)

// Represents the possible lifecycle states of a task.
enum class TaskStatus {
    PENDING,
    DONE,
    MISSED
}