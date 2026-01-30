package com.example.mapmytasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TasksAdapter(
    private var tasks: List<Task>,
    private val onEditClick: (Task) -> Unit
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.taskName)
        val categoryText: TextView = view.findViewById(R.id.taskCategory)
        val dateTimeText: TextView = view.findViewById(R.id.taskDateTime)
        val locationText: TextView = view.findViewById(R.id.taskLocation)
        val editBtn: Button = view.findViewById(R.id.editTaskBtn)
    }

    // Updates the list of tasks in the adapter
    fun updateTasks(newTasks: List<Task>) {
        this.tasks = newTasks
        notifyDataSetChanged()
    }

    // Creates a new view holder for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int = tasks.size

    // Binds data to the view holder
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.nameText.text = task.name
        holder.categoryText.text = task.category
        holder.dateTimeText.text = task.dateTime
        holder.locationText.text = task.location

        holder.editBtn.setOnClickListener {
            onEditClick(task)
        }
    }
}
