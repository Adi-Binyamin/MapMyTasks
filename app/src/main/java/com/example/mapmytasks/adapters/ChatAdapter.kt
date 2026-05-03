package com.example.mapmytasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmytasks.R


// מודל הנתונים: טקסט ההודעה, והאם היא נשלחה על ידי המשתמש (true) או הבוט (false)
data class ChatMessage(val text: String, val isUser: Boolean)

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userMessageText: TextView = view.findViewById(R.id.userMessageText)
        val botMessageText: TextView = view.findViewById(R.id.botMessageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            // מציגים את הבועה הכחולה ומסתירים את האפורה
            holder.userMessageText.text = message.text
            holder.userMessageText.visibility = View.VISIBLE
            holder.botMessageText.visibility = View.GONE
        } else {
            // מציגים את הבועה האפורה ומסתירים את הכחולה
            holder.botMessageText.text = message.text
            holder.botMessageText.visibility = View.VISIBLE
            holder.userMessageText.visibility = View.GONE
        }
    }
}