// Create a new file, e.g., com/google/ai/edge/gallery/ui/history/ChatHistoryViewModel.kt

package com.google.ai.edge.gallery.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.db.ChatInteractionDao
import com.google.ai.edge.gallery.db.ChatInteractionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatHistoryViewModel @Inject constructor(
    private val chatInteractionDao: ChatInteractionDao
) : ViewModel() {

    // Expose the chat history as a StateFlow for the UI to observe
    val chatHistory: StateFlow<List<ChatInteractionEntity>> =
        chatInteractionDao.getAllInteractionsDesc()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Keep active 5s after last subscriber
                initialValue = emptyList() // Initial empty list
            )

    // Optional: Function to clear history
    fun clearHistory() {
        viewModelScope.launch {
            chatInteractionDao.clearAllInteractions()
        }
    }
}