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

    // שינינו את השם כדי למנוע התנגשות עם הפונקציה של קוטלין!
    var currentSortMethod: String = "date"

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

        // שימוש בשם החדש - currentSortMethod
        val currentGroupKey = if (currentSortMethod == "date") task.dateTime.split(" ").getOrNull(0) ?: "" else task.category

        val prevGroupKey = if (prevTask != null) {
            if (currentSortMethod == "date") prevTask.dateTime.split(" ").getOrNull(0) ?: "" else prevTask.category
        } else null

        val nextGroupKey = if (nextTask != null) {
            if (currentSortMethod == "date") nextTask.dateTime.split(" ").getOrNull(0) ?: "" else nextTask.category
        } else null

        // ניהול צבעי המטריצה
        val darkPastels = listOf("#AED6F1", "#A9DFBF", "#F5B7B1", "#D7BDE2", "#F9E79F")
        val lightPastels = listOf("#D1EAFF", "#D1F2EB", "#FADBD8", "#E8DAEF", "#FCF3CF")

        val idx = task.colorIndex % darkPastels.size
        holder.container.setBackgroundColor(Color.parseColor(darkPastels[idx]))
        holder.cardView.setBackgroundColor(Color.parseColor(lightPastels[idx]))

        // הצגת הכותרת הנכונה (Header)
        if (currentGroupKey != prevGroupKey) {
            holder.dateHeader.text = currentGroupKey
            holder.dateHeader.visibility = View.VISIBLE
        } else {
            holder.dateHeader.visibility = View.GONE
        }

        // ניהול המרווחים
        val layoutParams = holder.container.layoutParams as ViewGroup.MarginLayoutParams
        if (currentGroupKey != nextGroupKey) {
            layoutParams.bottomMargin = 32
            holder.divider.visibility = View.GONE
        } else {
            layoutParams.bottomMargin = 0
            holder.divider.visibility = View.VISIBLE
        }
        holder.container.layoutParams = layoutParams

        // הצגת נתוני המשימה
        val currentTime = task.dateTime.split(" ").getOrNull(1) ?: "--:--"
        holder.timeOnlyText.text = currentTime
        holder.nameText.text = task.name
        holder.locationText.text = task.location

        // שימוש ב-TaskManager
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