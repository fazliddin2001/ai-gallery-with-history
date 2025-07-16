
package com.google.ai.edge.gallery.ui.llmchat

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.gallery.data.ConfigKey
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.TASK_LLM_CHAT
import com.google.ai.edge.gallery.data.TASK_LLM_ASK_IMAGE
import com.google.ai.edge.gallery.data.Task

import com.google.ai.edge.gallery.db.ChatInteractionDao
import com.google.ai.edge.gallery.db.ChatInteractionEntity

import com.google.ai.edge.gallery.ui.common.chat.ChatMessageBenchmarkLlmResult
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageLoading
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageText
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageType
import com.google.ai.edge.gallery.ui.common.chat.ChatMessageWarning
import com.google.ai.edge.gallery.ui.common.chat.ChatSide
import com.google.ai.edge.gallery.ui.common.chat.ChatViewModel
import com.google.ai.edge.gallery.ui.common.chat.Stat
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.text.append


private const val TAG = "AGLlmChatViewModel"
private val STATS = listOf(
  Stat(id = "time_to_first_token", label = "1st token", unit = "sec"),
  Stat(id = "prefill_speed", label = "Prefill speed", unit = "tokens/s"),
  Stat(id = "decode_speed", label = "Decode speed", unit = "tokens/s"),
  Stat(id = "latency", label = "Latency", unit = "sec")
)

// Placeholder for your actual DAO and Entity.
// You'll need to define these and your Room database setup.
interface ChatInteractionDao {
  suspend fun insertInteraction(interaction: ChatInteractionEntity): Long
  suspend fun updateLlmResponse(id: Long, responseText: String, isProcessing: Boolean)
  suspend fun getInteractionById(id: Long): ChatInteractionEntity? // For error handling if needed
}

data class ChatInteractionEntity(
  val id: Long = 0,
  val timestamp: Long,
  val userRequestText: String?,
  val userRequestImageUri: String?,
  var llmResponseText: String?,
  var isProcessing: Boolean = true
)
// End Placeholder

@HiltViewModel
open class LlmChatViewModel @Inject constructor(val chatInteractionDao: ChatInteractionDao) : ChatViewModel(task = TASK_LLM_CHAT){
  private var currentDbInteractionId: Long? = null // To store the ID of the current DB interaction
  private var accumulatedResponse: StringBuilder = StringBuilder()
  fun generateResponse(model: Model, input: String, image: Bitmap? = null, onError: () -> Unit) {
    val accelerator = model.getStringConfigValue(key = ConfigKey.ACCELERATOR, defaultValue = "")
    viewModelScope.launch(Dispatchers.Default) {
      setInProgress(true)
      setPreparing(true)
      currentDbInteractionId = null // Reset for new response
      accumulatedResponse.clear()
      // --- Database: Create initial interaction entry ---
      val imageUriString = null // TODO: Convert bitmap to URI and save if needed
      val newInteraction = ChatInteractionEntity(
        timestamp = System.currentTimeMillis(),
        userRequestText = input,
        userRequestImageUri = imageUriString,
        llmResponseText = "",
        isProcessing = true
      )
      // Store the ID of the newly inserted interaction
      currentDbInteractionId = withContext(Dispatchers.IO) {
        chatInteractionDao.insertInteraction(newInteraction)
      }
      // --- End Database ---

      // Loading.
      addMessage(
        model = model,
        message = ChatMessageLoading(accelerator = accelerator),
      )

      // Wait for instance to be initialized.
      while (model.instance == null) {
        delay(100)
      }
      delay(500)

      // Run inference.
      val instance = model.instance as LlmModelInstance
      var prefillTokens = instance.session.sizeInTokens(input)
      if (image != null) {
        prefillTokens += 257
      }

      var firstRun = true
      var timeToFirstToken = 0f
      var firstTokenTs = 0L
      var decodeTokens = 0
      var prefillSpeed = 0f
      var decodeSpeed: Float
      val start = System.currentTimeMillis()

      try {
        LlmChatModelHelper.runInference(model = model,
          input = input,
          image = image,
          resultListener = { partialResult, done ->
            val curTs = System.currentTimeMillis()

            if (firstRun) {
              firstTokenTs = System.currentTimeMillis()
              timeToFirstToken = (firstTokenTs - start) / 1000f
              prefillSpeed = prefillTokens / timeToFirstToken
              firstRun = false
              setPreparing(false)
            } else {
              decodeTokens++
            }


            // Append partial result to our accumulator
            accumulatedResponse.append(partialResult)

            // --- Database: Incremental Update ---
            currentDbInteractionId?.let { dbId ->
              // Update the database with the current accumulated response.
              // Still mark as processing = true until 'done'.
              viewModelScope.launch(Dispatchers.IO) {
                chatInteractionDao.updateLlmResponse(
                  id = dbId,
                  responseText = accumulatedResponse.toString(),
                  isProcessing = true // Still processing
                )
              }
            }
            // Remove the last message if it is a "loading" message.
            // This will only be done once.
            val lastMessage = getLastMessage(model = model)
            if (lastMessage?.type == ChatMessageType.LOADING) {
              removeLastMessage(model = model)

              // Add an empty message that will receive streaming results.
              addMessage(
                model = model,
                message = ChatMessageText(
                  content = "",
                  side = ChatSide.AGENT,
                  accelerator = accelerator
                )
              )
            }

            // Incrementally update the streamed partial results.
            val latencyMs: Long = if (done) System.currentTimeMillis() - start else -1
            updateLastTextMessageContentIncrementally(
              model = model, partialContent = partialResult, latencyMs = latencyMs.toFloat()
            )
            if (done) {
              setInProgress(false)

              decodeSpeed = decodeTokens / ((curTs - firstTokenTs) / 1000f)
              if (decodeSpeed.isNaN()) {
                decodeSpeed = 0f
              }

              // ... (decodeSpeed logic) ...

              // --- Database: Final Update (Mark as not processing) ---
              currentDbInteractionId?.let { dbId ->
                viewModelScope.launch(Dispatchers.IO) {
                  chatInteractionDao.updateLlmResponse(
                    id = dbId,
                    responseText = accumulatedResponse.toString(), // Final complete response
                    isProcessing = false // Now it's done processing
                  )
                }
              }
              // --- End Final Database Update ---

              if (lastMessage is ChatMessageText) {
                updateLastTextMessageLlmBenchmarkResult(
                  model = model, llmBenchmarkResult = ChatMessageBenchmarkLlmResult(
                    orderedStats = STATS,
                    statValues = mutableMapOf(
                      "prefill_speed" to prefillSpeed,
                      "decode_speed" to decodeSpeed,
                      "time_to_first_token" to timeToFirstToken,
                      "latency" to (curTs - start).toFloat() / 1000f,
                    ),
                    running = false,
                    latencyMs = -1f,
                    accelerator = accelerator,
                  )
                )
              }
            }
          },
          cleanUpListener = {
            setInProgress(false)
            setPreparing(false)
          })
      } catch (e: Exception) {
        Log.e(TAG, "Error occurred while running inference", e)
        setInProgress(false)
        setPreparing(false)
        onError()
      }
    }
  }

  fun stopResponse(model: Model) {
    Log.d(TAG, "Stopping response for model ${model.name}...")
    if (getLastMessage(model = model) is ChatMessageLoading) {
      removeLastMessage(model = model)
    }
    viewModelScope.launch(Dispatchers.Default) {
      setInProgress(false)
      val instance = model.instance as LlmModelInstance
      instance.session.cancelGenerateResponseAsync()
    }
  }

  fun resetSession(model: Model) {
    viewModelScope.launch(Dispatchers.Default) {
      setIsResettingSession(true)
      clearAllMessages(model = model)
      stopResponse(model = model)

      while (true) {
        try {
          LlmChatModelHelper.resetSession(model = model)
          break
        } catch (e: Exception) {
          Log.d(TAG, "Failed to reset session. Trying again")
        }
        delay(200)
      }
      setIsResettingSession(false)
    }
  }

  fun runAgain(model: Model, message: ChatMessageText, onError: () -> Unit) {
    viewModelScope.launch(Dispatchers.Default) {
      // Wait for model to be initialized.
      while (model.instance == null) {
        delay(100)
      }

      // Clone the clicked message and add it.
      addMessage(model = model, message = message.clone())

      // Run inference.
      generateResponse(
        model = model, input = message.content, onError = onError
      )
    }
  }

  fun handleError(
    context: Context,
    model: Model,
    modelManagerViewModel: ModelManagerViewModel,
    triggeredMessage: ChatMessageText,
  ) {
    // Clean up.
    modelManagerViewModel.cleanupModel(task = task, model = model)

    // Remove the "loading" message.
    if (getLastMessage(model = model) is ChatMessageLoading) {
      removeLastMessage(model = model)
    }

    // Remove the last Text message.
    if (getLastMessage(model = model) == triggeredMessage) {
      removeLastMessage(model = model)
    }

    // Add a warning message for re-initializing the session.
    addMessage(
      model = model,
      message = ChatMessageWarning(content = "Error occurred. Re-initializing the session.")
    )

    // Add the triggered message back.
    addMessage(model = model, message = triggeredMessage)

    // Re-initialize the session/engine.
    modelManagerViewModel.initializeModel(
      context = context, task = task, model = model
    )

    // Re-generate the response automatically.
    generateResponse(model = model, input = triggeredMessage.content, onError = {})
  }
}

@HiltViewModel
class LlmAskImageViewModel @Inject constructor(
  injectedDao: ChatInteractionDao
) : LlmChatViewModel(chatInteractionDao = injectedDao) {
}