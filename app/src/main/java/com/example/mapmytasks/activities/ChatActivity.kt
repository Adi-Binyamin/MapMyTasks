package com.example.mapmytasks.activities

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmytasks.adapters.ChatAdapter
import com.example.mapmytasks.adapters.ChatMessage
import com.example.mapmytasks.R
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ChatActivity manages the virtual assistant interface within the app.
 * It uses the Google Generative AI API (Gemini) to provide contextual help
 * to users regarding app navigation and features based on predefined system instructions.
 */
class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var btnBack: Button

    private val messagesList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val geminiApiKey = ""

    // Initializes the Gemini generative model with specific system instructions
    // to restrict its knowledge solely to the app's functionality and layout.
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = geminiApiKey,
        systemInstruction = content {
            text("""
                You are the virtual assistant for the MapMyTasks app.
                
                App Overview:
                The MapMyTasks home screen features a top-left virtual assistant robot icon for navigation help 
    and a top-right heart icon for a positive thinking game. The central menu has 6 buttons 
    arranged in 3 rows with two buttons per row. 
    
    On the first row, the left button (plus icon) leads to the task creation screen where users 
    first select the recipient (themselves or someone who authorized them), then enter a task name, 
    choose a category (Work, Studies, Personal, Shopping, Health, Financial, Hobbies, Travel, 
    School, Chores, or Other), select a future date and time, choose a location, and tap the save 
    button at the bottom (these fields are arranged one under the other in the exact order mentioned); 
    the right button opens a screen to browse future tasks—sortable by date or category—where each 
    task displays its name, address, time, date, category, and the creator's name (if not created 
    by the user), alongside a pencil icon that leads to an edit screen which is exactly like the 
    creation screen but adds a delete button below the save button. 
    
    On the second row, the left button (eye icon) opens the "Productivity" screen where selecting 
    a category shows a user's lifetime progress bars and percentages (1% to 100%) for morning, 
    afternoon, evening, and night, based on all usage periods from the beginning; the right button 
    (clock icon) opens "Weekly Stats" to view data from the previous Sunday through Saturday 
    (for instance, if today is Monday, 1.6.26, the data covers Sunday, 24.5.26, to Saturday, 30.5.26) 
    via 4 graphs from top to bottom: completed tasks by time, uncompleted tasks by time, 
    completed tasks by category, and uncompleted tasks by category. 
    
    On the third row, the left button (share icon) manages task sharing where users can type the 
    email of another registered user in the top section and tap "Grant Permission" below the text 
    box to let them assign tasks; below a divider line, currently authorized emails are listed 
    (removable via long-press), and the bottom section contains a button leading to "Tasks Sent to 
    Others" where users select an authorized peer's email to view all past, present, and future tasks 
    sent to them, displayed in the exact same format as the personal tasks screen; the right button 
    (power icon) functions as the logout action. 
    
    Additionally, the app offers weather services, providing a notification the day before a task 
    scheduled during rain or temperatures above 30°C, an immediate alert when choosing a date that 
    falls on a holiday or calendar event, and a recommendation to reconsider the timing if the app 
    detects a user's habit of canceling tasks at a specific time slot when scheduling a new task 
    for that same slot. 
    
    Under strict guardrails, you answer only based on the facts explicitly listed here without 
    assuming outside knowledge; if the user asks an unrelated question or about behind-the-scenes 
    mechanics (like how notifications work technically), you must reply with exactly: "I can only answer 
    questions about navigating the app" (or the Hebrew equivalent if asked in Hebrew), matching 
    the user's language and keeping responses highly concise and short.
                
                 """.trimIndent())        }
    )

    private val chat = generativeModel.startChat()

    // Sets up the UI components, RecyclerView adapter, and handles button clicks.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        btnBack = findViewById(R.id.btnBack)

        chatAdapter = ChatAdapter(messagesList)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        btnBack.setOnClickListener {
            finish()
        }

        if (messagesList.isEmpty()) {
            addMessageToChat(ChatMessage("Hi! I'm the MapMyTasks assistant. How can I help you?", false))
        }

        sendButton.setOnClickListener {
            val userText = messageInput.text.toString().trim()
            if (userText.isNotEmpty()) {
                checkAndSend(userText)
            }
        }
    }

    // Validates network connectivity before attempting to send the message to the API.
    private fun checkAndSend(userText: String) {
        if (!isNetworkAvailable()) {
            addMessageToChat(ChatMessage("Error: No internet connection.", false))
            return
        }
        sendMessageToGemini(userText)
    }

    // Handles the asynchronous network call to the Gemini API and updates the UI with the response or error.
    private fun sendMessageToGemini(userText: String) {
        addMessageToChat(ChatMessage(userText, true))
        messageInput.text.clear()
        sendButton.isEnabled = false

        // Launches a coroutine on the background thread (IO) to prevent blocking the main UI thread during the API call.
        lifecycleScope.launch(Dispatchers.IO) {
            try {

                val response = chat.sendMessage(userText)
                val botReply = response.text

                // Switches back to the main thread to safely update the UI components (RecyclerView).
                withContext(Dispatchers.Main) {
                    if (botReply != null) {
                        addMessageToChat(ChatMessage(botReply, false))
                    } else {
                        addMessageToChat(ChatMessage("Received an empty response from Google.", false))
                    }
                    sendButton.isEnabled = true
                }

            } catch (e: Exception) {

                // Maps HTTP error codes from the API exception to user-friendly error messages.
                withContext(Dispatchers.Main) {
                    val displayError = when {
                        e.message?.contains("404") == true -> "Error 404: Model not found."
                        e.message?.contains("429") == true -> "Error 429: Quota exceeded."
                        e.message?.contains("401") == true -> "Error 401: API key authorization issue."
                        else -> "Technical communication error."
                    }
                    addMessageToChat(ChatMessage(displayError, false))
                    sendButton.isEnabled = true
                }
            }
        }
    }

    // Checks if the device has an active internet connection (Wi-Fi or Cellular).
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }

    // Helper method to add a new message to the list and smoothly scroll the view to the bottom.
    private fun addMessageToChat(message: ChatMessage) {
        messagesList.add(message)
        chatAdapter.notifyItemInserted(messagesList.size - 1)
        chatRecyclerView.scrollToPosition(messagesList.size - 1)
    }
}