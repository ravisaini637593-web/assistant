package com.example.assistant

import com.example.tools.RiskLevel
import com.example.tools.ToolResult

enum class EngineState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    AWAITING_CONFIRMATION
}

enum class ModePreference {
    AUTO,
    PREFER_OFFLINE,
    PREFER_ONLINE
}

data class PendingAction(
    val toolName: String,
    val arguments: Map<String, String>,
    val riskLevel: RiskLevel,
    val explanation: String
)

data class UiMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "assistant"
    val text: String,
    val toolName: String? = null,
    val toolResult: ToolResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AssistantUiState(
    val state: EngineState = EngineState.IDLE,
    val currentMode: String = "AUTO",
    val isOnline: Boolean = true,
    val partialTranscript: String = "",
    val lastResponse: String = "",
    val rmsLevel: Float = 0f,
    val pendingAction: PendingAction? = null,
    val messages: List<UiMessage> = emptyList(),
    val activeTimersCount: Int = 0,
    val activeRemindersCount: Int = 0
)
