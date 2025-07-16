package com.google.ai.edge.gallery.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_interactions") // This is the table name in SQLite
data class ChatInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Auto-generated primary key

    val timestamp: Long, // When the user made the request

    val userRequestText: String?, // Text of the user's query

    val userRequestImageUri: String?, // URI for the image, if any

    var llmResponseText: String?, // Text of the LLM's response

    var isProcessing: Boolean = true // To track if LLM is still generating a response
)