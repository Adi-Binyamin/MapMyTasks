package com.example.mapmytasks.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapmytasks.R


// Data model representing a single chat message and identifying its sender (user or bot).
data class ChatMessage(val text: String, val isUser: Boolean)

/**
 * ChatAdapter manages the RecyclerView display for the chat interface.
 * It toggles visibility between user and bot message bubbles based on the sender.
 */
class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userMessageText: TextView = view.findViewById(R.id.userMessageText)
        val botMessageText: TextView = view.findViewById(R.id.botMessageText)
    }

    // Inflates the shared layout containing both user and bot message views.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun getItemCount(): Int = messages.size

    // Binds the data to the views and toggles visibility depending on who sent the message.
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        if (message.isUser) {
            // Displays the user message bubble and hides the bot message bubble.
            holder.userMessageText.text = message.text
            holder.userMessageText.visibility = View.VISIBLE
            holder.botMessageText.visibility = View.GONE
        } else {
            // Displays the bot message bubble and hides the user message bubble.
            holder.botMessageText.text = message.text
            holder.botMessageText.visibility = View.VISIBLE
            holder.userMessageText.visibility = View.GONE
        }
    }
}