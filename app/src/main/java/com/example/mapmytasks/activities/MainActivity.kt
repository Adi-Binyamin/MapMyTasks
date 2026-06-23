package com.example.mapmytasks.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.example.mapmytasks.R
import com.example.mapmytasks.data.TaskManager

/**
 * MainActivity serves as the central hub of the app, providing navigation
 * buttons to access all main features and screens.
 */
class MainActivity : AppCompatActivity() {

    // Initializes the main dashboard and sets up click listeners for navigation routing.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val createTaskBtn: Button = findViewById(R.id.createTaskBtn)
        val partnerManagementBtn: Button = findViewById(R.id.partnerManagementBtn)
        val weeklyStatsBtn: Button = findViewById(R.id.weeklyStatsBtn)
        val myTasksBtn: Button = findViewById(R.id.myTasksBtn)
        val productivityBtn: Button = findViewById(R.id.productivityBtn)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        val gameBtn: Button = findViewById(R.id.gameBtn)

        val chatBtn: Button = findViewById(R.id.chatBtn)

        chatBtn.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        createTaskBtn.setOnClickListener {
            val intent = Intent(this, CreateTask::class.java)
            startActivity(intent)
        }

        partnerManagementBtn.setOnClickListener {
            val intent = Intent(this, PartnerManagement::class.java)
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

        gameBtn.setOnClickListener {
            val intent = Intent(this, BalloonGameActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            // Logs out the user, clears the activity back stack to prevent returning, and navigates to Login.
            TaskManager.logoutUser()

            val intent = Intent(this, LogIn::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }
    }
}