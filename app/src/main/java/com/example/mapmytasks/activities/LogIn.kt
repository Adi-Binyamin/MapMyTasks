package com.example.mapmytasks.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mapmytasks.data.LocationTaskService
import com.example.mapmytasks.data.TaskManager
import com.example.mapmytasks.databinding.ActivityLogInBinding

/**
 * LogIn Activity handles user authentication.
 * It provides functionality for both registering new users and logging in existing ones using Firebase.
 */
class LogIn : AppCompatActivity() {

    private lateinit var binding: ActivityLogInBinding

    // Initializes view binding and sets up click listeners for the authentication buttons.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener {
            registerUser()
        }

        binding.loginButton.setOnClickListener {
            loginUser()
        }
    }

    // Validates credentials and attempts to register a new user account via the TaskManager.
    private fun registerUser() {
        val email = binding.emailEditText.text.toString().trim().lowercase()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        TaskManager.registerUser(
            email, password,
            onSuccess = {
                navigateToMain()
            },
            onFailure = { e ->
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Validates credentials and attempts to log in an existing user via the TaskManager.
    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim().lowercase()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        TaskManager.loginUser(
            email, password,
            onSuccess = {
                navigateToMain()
            },
            onFailure = { e ->
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Starts the continuous location tracking service and transitions the user to the MainActivity.
    private fun navigateToMain() {
        val serviceIntent = Intent(this, LocationTaskService::class.java)

        // Ensures the service is started correctly based on the Android version (Foreground for Android 8.0+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}