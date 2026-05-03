package com.example.mapmytasks.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mapmytasks.R

class BalloonGameActivity : AppCompatActivity() {

    private var isPopped = false

    private val cuteTasks = listOf(
        "Take 3 deep breaths and relax 🌸",
        "Stretch your back and neck for a moment ✨",
        "Drink a glass of cold water 💧",
        "Play your favorite song and dance! 🎶",
        "Send a kind message to someone you love 💖",
        "Look in the mirror and smile, you're amazing! 😊",
        "Close your eyes and think of something happy 🌈",
        "Give yourself a big hug for your hard work 🤗",
        "Go outside for 2 minutes of fresh air 🍃",
        "You are doing a great job! Keep it up 🚀"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balloon_game)

        val grid = findViewById<GridLayout>(R.id.balloonGrid)

        // חשוב: הגדרת כמות העמודות והשורות בגריד
        grid.columnCount = 3
        grid.rowCount = 3

        for (i in 0 until 9) {
            val balloon = ImageView(this)
            balloon.setImageResource(R.drawable.ic_balloon)

            // הגדרת משקל (Weight) כדי שהבלונים יתפרסו על כל המסך
            val params = GridLayout.LayoutParams()

            // הגדרת העמודה והשורה עם משקל 1f (חלוקה שווה)
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)

            // גובה ורוחב - נשתמש ב-0 כדי שהמשקל יקבע את הגודל
            params.width = 0
            params.height = 0

            params.setMargins(15, 15, 15, 15)
            balloon.layoutParams = params
            balloon.scaleType = ImageView.ScaleType.FIT_CENTER // שומר על פרופורציות הבלון

            balloon.setOnClickListener {
                if (!isPopped) {
                    isPopped = true
                    popBalloon(balloon)
                }
            }

            grid.addView(balloon)
        }
    }

    private fun popBalloon(balloon: View) {
        val taskView = findViewById<TextView>(R.id.taskResultText)
        val popAnim = AnimationUtils.loadAnimation(this, R.anim.balloon_pop)
        val appearAnim = AnimationUtils.loadAnimation(this, R.anim.task_appear)

        balloon.startAnimation(popAnim)

        popAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation?) {
                balloon.visibility = View.INVISIBLE

                taskView.text = cuteTasks.random()
                taskView.visibility = View.VISIBLE
                taskView.startAnimation(appearAnim)

                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 5000)
            }
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }
}