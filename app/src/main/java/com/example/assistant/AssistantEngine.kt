package com.example.assistant

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.ai.offline.AndroidTtsEngine
import com.example.ai.offline.CommandRouter
import com.example.ai.offline.OnDeviceModelAdapter
import com.example.ai.offline.OnDeviceSpeechAdapter
import com.example.ai.online.GeminiApiClient
import com.example.ai.online.GeminiResponse
import com.example.data.AppDatabase
import com.example.data.ConversationMessage
import com.example.data.ConversationSession
import com.example.data.Routine
import com.example.tools.RiskLevel
import com.example.tools.ToolExecutor
import com.example.tools.ToolRegistry
import com.example.tools.ToolResult
import com.example.tools.ToolValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val db = AppDatabase.getInstance(context)
    val toolExecutor = ToolExecutor(context)
    val speechAdapter = OnDeviceSpeechAdapter(context)
    val ttsEngine = AndroidTtsEngine(context)
    val geminiClient = GeminiApiClient(context)
    val onDeviceModel = OnDeviceModelAdapter(context)

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var currentSessionId: Long = 0
    var modePreference: ModePreference = ModePreference.AUTO
        private set

    init {
        ttsEngine.setSpeakingListener { isSpeaking ->
            _uiState.update { current ->
                if (isSpeaking) {
                    current.copy(state = EngineState.SPEAKING)
                } else {
                    if (current.state == EngineState.SPEAKING) {
                        current.copy(state = EngineState.IDLE)
                    } else current
                }
            }
        }

        // Initialize session
        scope.launch(Dispatchers.IO) {
            val session = ConversationSession(
                title = "Assistant Session",
                mode = modePreference.name
            )
            currentSessionId = db.conversationSessionDao().insert(session)
            checkNetworkStatus()
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun checkNetworkStatus() {
        val online = isOnline()
        _uiState.update { it.copy(isOnline = online) }
    }

    fun setModePreference(mode: ModePreference) {
        modePreference = mode
        _uiState.update { it.copy(currentMode = mode.name) }
    }

    fun startListening() {
        stopSpeaking() // Barge-in: immediately silence any ongoing speech

        if (_uiState.value.state == EngineState.AWAITING_CONFIRMATION) {
            // Can be listening for confirmation
        }

        _uiState.update {
            it.copy(
                state = EngineState.LISTENING,
                partialTranscript = ""
            )
        }

        val preferOffline = (modePreference == ModePreference.PREFER_OFFLINE) || !isOnline()
        speechAdapter.startListening(
            preferOffline = preferOffline,
            onRmsChanged = { rms ->
                _uiState.update { it.copy(rmsLevel = rms) }
            },
            onPartialResult = { partial ->
                _uiState.update { it.copy(partialTranscript = partial) }
            },
            onFinalResult = { final ->
                _uiState.update { it.copy(partialTranscript = final) }
                processUserInput(final)
            },
            onError = { err ->
                _uiState.update {
                    it.copy(
                        state = EngineState.IDLE,
                        partialTranscript = ""
                    )
                }
                appendAssistantMessage("Speech input error: $err")
            }
        )
    }

    fun stopListening() {
        speechAdapter.stopListening()
        _uiState.update { it.copy(state = EngineState.IDLE) }
    }

    fun stopSpeaking() {
        ttsEngine.stop()
        _uiState.update {
            if (it.state == EngineState.SPEAKING) it.copy(state = EngineState.IDLE) else it
        }
    }

    fun processUserInput(rawText: String) {
        val input = rawText.trim()
        if (input.isBlank()) {
            _uiState.update { it.copy(state = EngineState.IDLE) }
            return
        }

        // If currently awaiting confirmation, check for verbal "yes" / "confirm" / "cancel"
        if (_uiState.value.state == EngineState.AWAITING_CONFIRMATION) {
            val lower = input.lowercase()
            if (lower.contains("yes") || lower.contains("confirm") || lower.contains("proceed") || lower.contains("call") || lower.contains("send")) {
                confirmPendingAction()
                return
            } else if (lower.contains("no") || lower.contains("cancel") || lower.contains("stop")) {
                cancelPendingAction()
                return
            }
        }

        // Add user message to UI and DB
        appendUserMessage(input)
        _uiState.update { it.copy(state = EngineState.PROCESSING, partialTranscript = "") }

        scope.launch(Dispatchers.IO) {
            checkNetworkStatus()
            val online = isOnline()
            val shouldTryOfflineFirst = modePreference == ModePreference.PREFER_OFFLINE ||
                    !online ||
                    modePreference == ModePreference.AUTO

            // 1. Try local deterministic command router
            val localMatch = CommandRouter.match(input)
            if (localMatch != null) {
                handleMatchedTool(localMatch.toolName, localMatch.arguments, localMatch.explanation)
                return@launch
            }

            // 2. If user prefers offline or no internet connection
            if (!online || modePreference == ModePreference.PREFER_OFFLINE) {
                // Try on-device model if available
                val localAnswer = onDeviceModel.generateLocalResponse(input)
                if (localAnswer != null) {
                    speakAndDisplay(localAnswer)
                } else {
                    val offlineMsg = if (!online) {
                        "You are currently offline. I can execute device commands like timers, flashlight, alarms, apps, and volume, but live web queries require internet."
                    } else {
                        "Offline mode is preferred. Command was not recognized by the local intent parser. Switch to Online or Auto mode for conversational queries."
                    }
                    speakAndDisplay(offlineMsg)
                }
                return@launch
            }

            // 3. Online Gemini route
            executeOnlineGemini(input)
        }
    }

    private suspend fun executeOnlineGemini(input: String) {
        val history = _uiState.value.messages.takeLast(6).map {
            it.role to it.text
        }

        when (val response = geminiClient.query(input, history)) {
            is GeminiResponse.FunctionCallResponse -> {
                handleMatchedTool(response.functionName, response.arguments, "Tool call: ${response.functionName}")
            }
            is GeminiResponse.TextResponse -> {
                speakAndDisplay(response.text)
            }
            is GeminiResponse.ErrorResponse -> {
                // Fallback to local search or explanation
                speakAndDisplay("Online request could not be completed: ${response.message}")
            }
        }
    }

    private suspend fun handleMatchedTool(
        toolName: String,
        arguments: Map<String, String>,
        explanation: String
    ) {
        // Validate
        val validation = ToolValidator.validate(toolName, arguments)
        if (!validation.isValid) {
            speakAndDisplay("Cannot execute $toolName: ${validation.errorMessage}")
            return
        }

        // Check ActionPolicy / ConfirmationGate
        val toolDef = ToolRegistry.getTool(toolName)
        val risk = toolDef?.riskLevel ?: RiskLevel.LOW
        if (ActionPolicy.requiresConfirmation(toolName, arguments)) {
            val confirmMsg = ActionPolicy.getConfirmationMessage(toolName, arguments)
            _uiState.update {
                it.copy(
                    state = EngineState.AWAITING_CONFIRMATION,
                    pendingAction = PendingAction(
                        toolName = toolName,
                        arguments = arguments,
                        riskLevel = risk,
                        explanation = confirmMsg
                    )
                )
            }
            speakAndDisplay(confirmMsg, autoSpeak = true)
            return
        }

        // Execute directly
        executeToolDirectly(toolName, arguments)
    }

    fun confirmPendingAction() {
        val pending = _uiState.value.pendingAction ?: return
        _uiState.update { it.copy(state = EngineState.PROCESSING, pendingAction = null) }
        scope.launch(Dispatchers.IO) {
            executeToolDirectly(pending.toolName, pending.arguments)
        }
    }

    fun cancelPendingAction() {
        _uiState.update { it.copy(state = EngineState.IDLE, pendingAction = null) }
        speakAndDisplay("Action cancelled.")
    }

    private suspend fun executeToolDirectly(toolName: String, arguments: Map<String, String>) {
        val result = toolExecutor.execute(toolName, arguments)

        val messageText = result.userMessage
        appendAssistantMessage(
            text = messageText,
            toolName = toolName,
            toolResult = result
        )
        ttsEngine.speak(messageText)
        _uiState.update {
            it.copy(
                state = EngineState.IDLE,
                lastResponse = messageText
            )
        }
    }

    fun executeRoutine(routine: Routine) {
        scope.launch(Dispatchers.IO) {
            appendUserMessage("Run routine: ${routine.title}")
            // Execute routine actions sequentially
            val parts = routine.actionsJson.split(";").map { it.trim() }.filter { it.isNotBlank() }
            for (actionStr in parts) {
                val match = CommandRouter.match(actionStr)
                if (match != null) {
                    val result = toolExecutor.execute(match.toolName, match.arguments)
                    appendAssistantMessage(result.userMessage, toolName = match.toolName, toolResult = result)
                }
            }
            ttsEngine.speak("Finished routine: ${routine.title}")
        }
    }

    private fun speakAndDisplay(text: String, autoSpeak: Boolean = true) {
        appendAssistantMessage(text)
        if (autoSpeak) {
            ttsEngine.speak(text)
        }
        _uiState.update {
            it.copy(
                state = if (it.state == EngineState.AWAITING_CONFIRMATION) EngineState.AWAITING_CONFIRMATION else EngineState.IDLE,
                lastResponse = text
            )
        }
    }

    private fun appendUserMessage(text: String) {
        val msg = UiMessage(role = "user", text = text)
        _uiState.update { it.copy(messages = it.messages + msg) }
        scope.launch(Dispatchers.IO) {
            try {
                db.conversationMessageDao().insert(
                    ConversationMessage(
                        sessionId = currentSessionId,
                        role = "user",
                        text = text
                    )
                )
            } catch (_: Exception) {}
        }
    }

    private fun appendAssistantMessage(
        text: String,
        toolName: String? = null,
        toolResult: ToolResult? = null
    ) {
        val msg = UiMessage(
            role = "assistant",
            text = text,
            toolName = toolName,
            toolResult = toolResult
        )
        _uiState.update { it.copy(messages = it.messages + msg) }
        scope.launch(Dispatchers.IO) {
            try {
                db.conversationMessageDao().insert(
                    ConversationMessage(
                        sessionId = currentSessionId,
                        role = "assistant",
                        text = text,
                        toolCallJson = toolName,
                        toolResultJson = toolResult?.userMessage
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun clearHistory() {
        scope.launch(Dispatchers.IO) {
            db.conversationMessageDao().clearAll()
            db.conversationSessionDao().clearAll()
            db.toolExecutionDao().clearAll()
            _uiState.update { it.copy(messages = emptyList()) }
        }
    }
}
