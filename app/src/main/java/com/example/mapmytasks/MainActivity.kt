
package com.example.mapmytasks


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val createTaskBtn: Button = findViewById(R.id.createTaskBtn)
        val assignTaskBtn: Button = findViewById(R.id.assignTaskBtn)
        val weeklyStatsBtn: Button = findViewById(R.id.weeklyStatsBtn)
        val myTasksBtn: Button = findViewById(R.id.myTasksBtn)
        val productivityBtn: Button = findViewById(R.id.productivityBtn)

        createTaskBtn.setOnClickListener {
            val intent = Intent(this, CreateTask::class.java)
            startActivity(intent)
        }

        assignTaskBtn.setOnClickListener {
            val intent = Intent(this, EditTask::class.java)
            startActivity(intent)
        }

        weeklyStatsBtn.setOnClickListener {
            val intent = Intent(this, WeeklySummary::class.java)
            startActivity(intent)
        }

        myTasksBtn.setOnClickListener {
            val intent = Intent(this, TasksScreen::class.java)
            startActivity(intent)
        }

        productivityBtn.setOnClickListener {
            val intent = Intent(this, Productivity::class.java)
            startActivity(intent)
        }
    }
}
