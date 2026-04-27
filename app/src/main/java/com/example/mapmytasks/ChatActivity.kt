package com.example.mapmytasks

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

    // המפתח המעודכן שלך מה-AI Studio
    private val geminiApiKey = "AIzaSyBR0DalSIZd2UuLXDktU8dv8vc0PJVBzCQ"

    // הגדרת המודל - שימוש בגרסה 2.0
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash", // זה השם המדויק שמופיע לך ב-Logcat
        apiKey = geminiApiKey,
        systemInstruction = content {
            text("""
                אתה העוזר הווירטואלי של אפליקציית MapMyTasks.
                
                מבט-על על האפליקציה:
                MapMyTasks היא מערכת חכמה לניהול משימות וזמן. היא מאפשרת למשתמשים ליצור משימות, להצמיד אותן למיקומים גיאוגרפיים (כדי לקבל התראה כשהם מתקרבים למקום), לסנכרן את הלו"ז מול יומן גוגל (Google Calendar), ולראות את ההתקדמות שלהם דרך מסך סטטיסטיקות וגרפים.
                
                התפקיד שלך:
                לענות על שאלות של משתמשים, לעזור להם לארגן את המשימות שלהם, לייעץ על ניהול זמן, ולהסביר להם איך להשתמש באפליקציה אם הם מסתבכים.
                
                סגנון:
                ענה בעברית טבעית, ברורה וידידותית. שמור על תשובות קצרות ונוחות לקריאה במסך נייד (מומלץ להשתמש באימוג'י ונקודות כשצריך).
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
            addMessageToChat(ChatMessage("היי! אני העוזר של MapMyTasks. איך אוכל לעזור?", false))
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
            addMessageToChat(ChatMessage("שגיאה: אין חיבור לאינטרנט.", false))
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
                        addMessageToChat(ChatMessage("קיבלתי תגובה ריקה מגוגל.", false))
                    }
                    sendButton.isEnabled = true
                }

            } catch (e: Exception) {
                Log.e(TAG, "🛑 חריגה (Exception) בזמן התקשורת!")
                Log.e(TAG, "Exception Type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Full Message: ${e.message}")
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    val displayError = when {
                        e.message?.contains("404") == true -> "שגיאה 404: המודל לא נמצא."
                        e.message?.contains("429") == true -> "שגיאה 429: חריגה ממכסת הבקשות."
                        e.message?.contains("401") == true -> "שגיאה 401: בעיית הרשאה במפתח ה-API."
                        else -> "שגיאה טכנית בתקשורת."
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