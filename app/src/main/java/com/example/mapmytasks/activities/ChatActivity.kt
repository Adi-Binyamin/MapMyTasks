package com.example.mapmytasks.activities

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton

    private val messagesList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val TAG = "GEMINI_TRACE"

    // APIkey is private
    private val geminiApiKey = ""

    // הגדרת המודל - שימוש בגרסה 2.0
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash", // זה השם המדויק שמופיע לך ב-Logcat
        apiKey = geminiApiKey,
        systemInstruction = content {
            text("""
                You are the virtual assistant for the MapMyTasks app.
                
                App Overview:
                MapMyTasks is a smart task and time management system. It allows users to create tasks, pin them to geographical locations (to get an alert when they approach the place), sync the schedule with Google Calendar, and view their progress through a statistics and graphs screen.
                
                Your Role:
                Answer user questions, help them organize their tasks, advise on time management, and explain how to use the app if they get stuck.
                
                Style:
                Answer in natural, clear, and friendly English. Keep answers short and easy to read on a mobile screen (feel free to use emojis and bullet points when needed).
            """.trimIndent())        }
    )

    // אתחול הצ'אט עם זיכרון
    private val chat = generativeModel.startChat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        Log.i(TAG, "==========================================")
        Log.i(TAG, "🚀 אתחול ChatActivity עבור MapMyTasks")
        Log.d(TAG, "Model configured: gemini-2.0-flash")

        // קריאה לפונקציית בדיקת המודלים הזמינים
        printAvailableModels()

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        chatAdapter = ChatAdapter(messagesList)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        if (messagesList.isEmpty()) {
            Log.d(TAG, "מציג הודעת פתיחה דיפולטיבית")
            // תורגם לאנגלית
            addMessageToChat(ChatMessage("Hi! I'm the MapMyTasks assistant. How can I help you?", false))
        }

        sendButton.setOnClickListener {
            val userText = messageInput.text.toString().trim()
            Log.d(TAG, "🖱️ לחיצה על כפתור שליחה. טקסט: '$userText'")

            if (userText.isNotEmpty()) {
                checkAndSend(userText)
            } else {
                Log.w(TAG, "⚠️ ניסיון שליחה של טקסט ריק - בוטל")
            }
        }
    }

    /**
     * פונקציה לבדיקת רשימת המודלים הזמינים עבור ה-API Key הנוכחי
     * מדמה את הבדיקה שנעשית ב-Python כדי לוודא תאימות
     */
    private fun printAvailableModels() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("GEMINI_MODELS", "🔍 מתחיל סריקת מודלים זמינים...")

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models?key=$geminiApiKey")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val modelsArray = jsonObject.getJSONArray("models")

                    Log.i("GEMINI_MODELS", "✅ נמצאו ${modelsArray.length()} מודלים זמינים עבור המפתח שלך:")

                    for (i in 0 until modelsArray.length()) {
                        val model = modelsArray.getJSONObject(i)
                        val name = model.getString("name")
                        val displayName = model.optString("displayName", "N/A")
                        Log.i("GEMINI_MODELS", "🔹 מודל: $name | שם תצוגה: $displayName")
                    }
                } else {
                    Log.e("GEMINI_MODELS", "❌ שגיאה בקבלת המודלים! קוד שגיאה: ${response.code}")
                    Log.e("GEMINI_MODELS", "Response: $responseBody")
                }
            } catch (e: Exception) {
                Log.e("GEMINI_MODELS", "❌ תקלה בתקשורת בזמן בדיקת המודלים: ${e.message}")
            }
        }
    }

    private fun checkAndSend(userText: String) {
        Log.d(TAG, "🔎 בודק חיבור לאינטרנט...")
        if (!isNetworkAvailable()) {
            Log.e(TAG, "❌ אין חיבור אינטרנט פעיל במכשיר!")
            // תורגם לאנגלית
            addMessageToChat(ChatMessage("Error: No internet connection.", false))
            return
        }
        Log.d(TAG, "✅ אינטרנט תקין. עובר לשליחה לג'מיני.")
        sendMessageToGemini(userText)
    }

    private fun sendMessageToGemini(userText: String) {
        Log.d(TAG, "----------------------------------")
        Log.i(TAG, "📤 שולח הודעת משתמש: $userText")

        addMessageToChat(ChatMessage(userText, true))
        messageInput.text.clear()
        sendButton.isEnabled = false

        Log.d(TAG, "📊 היסטוריית שיחה נוכחית: ${chat.history.size} הודעות")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "📡 פנייה ל-API (Gemini 2.0 Flash)...")
                val startTime = System.currentTimeMillis()

                val response = chat.sendMessage(userText)

                val endTime = System.currentTimeMillis()
                Log.d(TAG, "⏱️ זמן תגובה מהשרת: ${endTime - startTime}ms")

                val botReply = response.text

                withContext(Dispatchers.Main) {
                    if (botReply != null) {
                        Log.i(TAG, "✅ תגובה התקבלה: $botReply")
                        addMessageToChat(ChatMessage(botReply, false))
                    } else {
                        Log.w(TAG, "⚠️ אזהרה: response.text חזר כ-null!")
                        // תורגם לאנגלית
                        addMessageToChat(ChatMessage("Received an empty response from Google.", false))
                    }
                    sendButton.isEnabled = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "🛑 חריגה (Exception) בזמן התקשורת!")
                Log.e(TAG, "Exception Type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Full Message: ${e.message}")
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    // כל השגיאות תורגמו לאנגלית
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }

    private fun addMessageToChat(message: ChatMessage) {
        Log.v(TAG, "עדכון UI: הוספת הודעה לרשימה (${if (message.isUser) "משתמש" else "בוט"})")
        messagesList.add(message)
        chatAdapter.notifyItemInserted(messagesList.size - 1)
        chatRecyclerView.scrollToPosition(messagesList.size - 1)
    }
}