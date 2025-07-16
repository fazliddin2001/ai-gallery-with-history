// Create a new file, e.g., com/google/ai/edge/gallery/ui/history/ChatHistoryScreen.kt

package com.google.ai.edge.gallery.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api // Add this import

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.db.ChatInteractionEntity
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class) // Or the specific experimental annotation needed
@Composable
fun ChatHistoryScreen(
    viewModel: ChatHistoryViewModel = hiltViewModel() // Get ViewModel via Hilt
) {
    val chatHistory by viewModel.chatHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chat History") })
        }
    ) { paddingValues ->
        if (chatHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No chat history yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 8.dp),
                reverseLayout = false // True would show newest at bottom and auto-scroll
                // Set to false for typical top-to-bottom reading of history
                // The DAO already sorts recent to old, so items[0] is newest.
            ) {
                items(chatHistory, key = { it.id }) { interaction ->
                    ChatItemCard(interaction = interaction)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ChatItemCard(interaction: ChatInteractionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Format timestamp for display
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
            val dateString = sdf.format(Date(interaction.timestamp))

            Text(
                text = "On: $dateString",
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.height(4.dp))

            interaction.userRequestText?.let {
                Text(
                    text = "You: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // You might want to display the image URI or a thumbnail if interaction.userRequestImageUri is present
            Spacer(modifier = Modifier.height(8.dp))
            interaction.llmResponseText?.let {
                if (it.isNotEmpty()) {
                    Text(
                        text = "LLM: $it",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (interaction.isProcessing) {
                    Text(
                        text = "LLM: (Still processing...)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "LLM: (No response)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            if (interaction.isProcessing && interaction.llmResponseText.isNullOrEmpty()) {
                Text(
                    text = "LLM: (Processing...)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
