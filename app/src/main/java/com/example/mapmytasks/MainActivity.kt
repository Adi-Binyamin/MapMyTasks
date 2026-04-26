package com.example.mapmytasks

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // הגדרת כל הכפתורים מה-XML
        val createTaskBtn: Button = findViewById(R.id.createTaskBtn)
        val partnerManagementBtn: Button = findViewById(R.id.partnerManagementBtn)
        val weeklyStatsBtn: Button = findViewById(R.id.weeklyStatsBtn)
        val myTasksBtn: Button = findViewById(R.id.myTasksBtn)
        val productivityBtn: Button = findViewById(R.id.productivityBtn)
        val btnLogout: Button = findViewById(R.id.btnLogout)

        // הכפתור החדש של המשחק
        val gameBtn: Button = findViewById(R.id.gameBtn)

        // מעבר למסך יצירת משימה
        createTaskBtn.setOnClickListener {
            val intent = Intent(this, CreateTask::class.java)
            startActivity(intent)
        }

        // מעבר לניהול שותפים
        partnerManagementBtn.setOnClickListener {
            val intent = Intent(this, PartnerManagement::class.java)
            startActivity(intent)
        }

        // מעבר לסיכום שבועי
        weeklyStatsBtn.setOnClickListener {
            val intent = Intent(this, WeeklySummary::class.java)
            startActivity(intent)
        }

        // מעבר למסך המשימות שלי
        myTasksBtn.setOnClickListener {
            val intent = Intent(this, TasksScreen::class.java)
            startActivity(intent)
        }

        // מעבר למסך פרודוקטיביות
        productivityBtn.setOnClickListener {
            val intent = Intent(this, Productivity::class.java)
            startActivity(intent)
        }

        // מעבר למסך המשחק (הבלונים)
        gameBtn.setOnClickListener {
            val intent = Intent(this, BalloonGameActivity::class.java)
            startActivity(intent)
        }

        // התנתקות מהמערכת
        btnLogout.setOnClickListener {
            // 1. ניתוק המשתמש מ-Firebase דרך ה-TaskManager
            TaskManager.logoutUser()

            // 2. מעבר למסך ההתחברות (LogIn)
            val intent = Intent(this, LogIn::class.java)

            // 3. מחיקת היסטוריית המסכים כדי שלא יהיה ניתן לחזור אחורה
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            // 4. סגירת המסך הנוכחי
            finish()
        }
    }
}