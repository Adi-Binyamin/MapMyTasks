package com.example.mapmytasks

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mapmytasks.databinding.ActivityLogInBinding
import com.google.firebase.auth.FirebaseAuth

class LogIn : AppCompatActivity() {

    private lateinit var binding: ActivityLogInBinding
    private lateinit var auth: FirebaseAuth

    // This function runs when the login screen is opened
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("LOGIN_DEBUG", "Login screen loaded")
        Toast.makeText(this, "Login screen loaded", Toast.LENGTH_SHORT).show()

        auth = FirebaseAuth.getInstance()

        binding.registerButton.setOnClickListener {
            Log.d("LOGIN_DEBUG", "Register button clicked")
            registerUser()
        }

        binding.loginButton.setOnClickListener {
            Log.d("LOGIN_DEBUG", "Login button clicked")
            loginUser()
        }
    }

    // This function creates a new user with email and password
    private fun registerUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
            Log.d("LOGIN_DEBUG", "Registration failed: empty email or password")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LOGIN_DEBUG", "Registration SUCCESS: ${auth.currentUser?.email}")
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Log.e("LOGIN_DEBUG", "Registration FAILED: ${task.exception}")
                    Toast.makeText(
                        this,
                        "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // This function logs in an existing user
    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
            Log.d("LOGIN_DEBUG", "Login failed: empty email or password")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LOGIN_DEBUG", "Login SUCCESS: ${auth.currentUser?.email}")
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Log.e("LOGIN_DEBUG", "Login FAILED: ${task.exception}")
                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // This function starts the service and moves to the main screen
    private fun navigateToMain() {
        val serviceIntent = Intent(this, LocationTaskService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
