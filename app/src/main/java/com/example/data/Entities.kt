package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val triggerPhrase: String,
    val description: String,
    val actionsJson: String, // List of tool calls serialized
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueTimestamp: Long,
    val isCompleted: Boolean = false,
    val recurrence: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "timers")
data class TimerRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val durationSeconds: Int,
    val endTimeMillis: Long,
    val isRunning: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversation_sessions")
data class ConversationSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val mode: String, // "OFFLINE", "ONLINE", "AUTO"
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

@Entity(tableName = "conversation_messages")
data class ConversationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user", "assistant", "system"
    val text: String,
    val toolCallJson: String? = null,
    val toolResultJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tool_executions")
data class ToolExecution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val toolName: String,
    val argumentsJson: String,
    val success: Boolean,
    val errorCode: String? = null,
    val userMessage: String,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "permission_snapshots")
data class PermissionStateSnapshot(
    @PrimaryKey val permissionKey: String,
    val isGranted: Boolean,
    val lastChecked: Long = System.currentTimeMillis()
)
