package com.example.mapmytasks

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TasksAdapter(
    private var tasks: List<Task>,
    private val onEditClick: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.taskItemContainer)
        val cardView: LinearLayout = view.findViewById(R.id.taskCardView)
        val dateHeader: TextView = view.findViewById(R.id.dateHeader)
        val divider: View = view.findViewById(R.id.innerDivider)

        val timeOnlyText: TextView = view.findViewById(R.id.taskTimeOnly)
        val nameText: TextView = view.findViewById(R.id.taskName)
        val locationText: TextView = view.findViewById(R.id.taskLocation)
        val createdByText: TextView = view.findViewById(R.id.taskCreatedBy)
        val editBtn: ImageButton = view.findViewById(R.id.editTaskBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int = tasks.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val prevTask = if (position > 0) tasks[position - 1] else null
        val nextTask = if (position < tasks.size - 1) tasks[position + 1] else null

        // טיפול בזמן ותאריך
        val dateTimeParts = task.dateTime.split(" ")
        val currentDate = dateTimeParts.getOrNull(0) ?: ""
        val currentTime = dateTimeParts.getOrNull(1) ?: "--:--"

        val prevDate = prevTask?.dateTime?.split(" ")?.getOrNull(0)
        val nextDate = nextTask?.dateTime?.split(" ")?.getOrNull(0)

        // ניהול צבעי המטריצה
        val darkPastels = listOf("#AED6F1", "#A9DFBF", "#F5B7B1", "#D7BDE2", "#F9E79F")
        val lightPastels = listOf("#D1EAFF", "#D1F2EB", "#FADBD8", "#E8DAEF", "#FCF3CF")

        val idx = task.colorIndex % darkPastels.size
        holder.container.setBackgroundColor(Color.parseColor(darkPastels[idx]))
        holder.cardView.setBackgroundColor(Color.parseColor(lightPastels[idx]))

        // הצגת כותרת תאריך
        if (currentDate != prevDate) {
            holder.dateHeader.text = currentDate
            holder.dateHeader.visibility = View.VISIBLE
        } else {
            holder.dateHeader.visibility = View.GONE
        }

        // רווחים
        val layoutParams = holder.container.layoutParams as ViewGroup.MarginLayoutParams
        if (currentDate != nextDate) {
            layoutParams.bottomMargin = 32
            holder.divider.visibility = View.GONE
        } else {
            layoutParams.bottomMargin = 0
            holder.divider.visibility = View.VISIBLE
        }
        holder.container.layoutParams = layoutParams

        holder.timeOnlyText.text = currentTime
        holder.nameText.text = task.name
        holder.locationText.text = task.location

        // שימוש ב-TaskManager במקום FirebaseAuth ישירות
        val myEmail = TaskManager.getCurrentUserEmail()
        if (!task.createdBy.isNullOrEmpty() && task.createdBy != myEmail) {
            holder.createdByText.text = "From: ${task.createdBy}"
            holder.createdByText.visibility = View.VISIBLE
        } else {
            holder.createdByText.visibility = View.GONE
        }

        holder.editBtn.setOnClickListener { onEditClick(task) }
    }
}