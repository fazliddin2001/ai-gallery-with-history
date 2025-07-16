package com.google.ai.edge.gallery.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // For reactive updates, if you display history lists

@Dao
interface ChatInteractionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Or ABORT, depending on desired behavior
    suspend fun insertInteraction(interaction: ChatInteractionEntity): Long // Returns the new row ID

    @Update
    suspend fun updateInteraction(interaction: ChatInteractionEntity)

    // Specific update for when the LLM response is ready
    @Query("UPDATE chat_interactions SET llmResponseText = :responseText, isProcessing = :isProcessing WHERE id = :id")
    suspend fun updateLlmResponse(id: Long, responseText: String?, isProcessing: Boolean)

    @Query("SELECT * FROM chat_interactions WHERE id = :id")
    suspend fun getInteractionById(id: Long): ChatInteractionEntity?

    @Query("SELECT * FROM chat_interactions ORDER BY timestamp DESC")
    fun getAllInteractions(): Flow<List<ChatInteractionEntity>> // For displaying history

    @Query("DELETE FROM chat_interactions WHERE id = :id")
    suspend fun deleteInteractionById(id: Long)

    @Query("DELETE FROM chat_interactions") // To clear all history
    suspend fun clearAllInteractions()

    // You might also want a query to get interactions that were still processing,
    // e.g., if the app closed unexpectedly.
    @Query("SELECT * FROM chat_interactions WHERE isProcessing = 1 ORDER BY timestamp DESC")
    suspend fun getPendingInteractions(): List<ChatInteractionEntity>
    @Query("SELECT * FROM chat_interactions ORDER BY timestamp DESC")
    fun getAllInteractionsDesc(): Flow<List<ChatInteractionEntity>>
}