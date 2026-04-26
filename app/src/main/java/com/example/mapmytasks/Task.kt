package com.example.mapmytasks

data class Task(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val dateTime: String = "",     // פורמט: "dd/MM/yyyy HH:mm"
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var status: TaskStatus = TaskStatus.PENDING,
    var isActive: Boolean = false,
    val createdBy: String = "",
    val assignTo: String = "",
    var colorIndex: Int = 0
)

enum class TaskStatus {
    PENDING,
    DONE,
    MISSED
}