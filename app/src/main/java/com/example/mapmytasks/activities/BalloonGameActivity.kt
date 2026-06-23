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
        "Take 3 deep breaths and relax \uD83C\uDF38",
        "Stretch your back and neck for a moment ✨",
        "Drink a glass of cold water \uD83D\uDCA7",
        "Play your favorite song and dance! \uD83C\uDFB6",
        "Send a kind message to someone you love \uD83D\uDC96",
        "Look in the mirror and smile, you're amazing! \uD83D\uDE0A",
        "Close your eyes and think of something happy \uD83C\uDF08",
        "Give yourself a big hug for your hard work \uD83E\uDD17",
        "Go outside for 2 minutes of fresh air \uD83C\uDF43",
        "You are doing a great job! Keep it up \uD83D\uDE80"
    )

    // Initializes the 3x3 balloon grid and dynamically adds the balloons to the screen.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balloon_game)

        val grid = findViewById<GridLayout>(R.id.balloonGrid)

        grid.columnCount = 3
        grid.rowCount = 3

        for (i in 0 until 9) {
            val balloon = ImageView(this)
            balloon.setImageResource(R.drawable.ic_balloon)

            val params = GridLayout.LayoutParams()

            // Using layout weights (1f) with 0 width/height so the grid distributes the balloons evenly across the available space.
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.width = 0
            params.height = 0
            params.setMargins(15, 15, 15, 15)

            balloon.layoutParams = params
            balloon.scaleType = ImageView.ScaleType.FIT_CENTER

            balloon.setOnClickListener {
                if (!isPopped) {
                    isPopped = true
                    popBalloon(balloon)
                }
            }

            grid.addView(balloon)
        }
    }

    // Plays the popping animation, reveals a random message, and closes the activity.
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

                // Delays the closing of the activity by 5 seconds to let the user read the message.
                Handler(Looper.getMainLooper()).postDelayed({
                    finish()
                }, 5000)
            }

            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }
}