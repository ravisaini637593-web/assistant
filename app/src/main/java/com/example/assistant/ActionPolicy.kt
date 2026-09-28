package com.example.assistant

import com.example.tools.RiskLevel
import com.example.tools.ToolRegistry

object ActionPolicy {

    fun requiresConfirmation(toolName: String, args: Map<String, String>): Boolean {
        val toolDef = ToolRegistry.getTool(toolName) ?: return false
        if (toolDef.requiresConfirmation) return true
        if (toolDef.riskLevel == RiskLevel.HIGH) return true

        // Extra guardrails: if volume is set to 100% or extreme
        if (toolName == "set_volume" && args["level"] == "100") return true

        return false
    }

    fun getConfirmationMessage(toolName: String, args: Map<String, String>): String {
        return when (toolName) {
            "make_call" -> {
                val contact = args["contact_or_number"] ?: args["contact"] ?: "number"
                "Are you sure you want to call $contact?"
            }
            "send_sms" -> {
                val recipient = args["recipient"] ?: "recipient"
                val text = args["message"] ?: ""
                "Send text message to $recipient: \"$text\"?"
            }
            "set_volume" -> "Set volume to 100%?"
            else -> "Execute $toolName with arguments: $args?"
        }
    }
}
