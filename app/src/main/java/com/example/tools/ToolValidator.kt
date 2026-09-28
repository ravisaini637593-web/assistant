package com.example.tools

object ToolValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validate(toolName: String, args: Map<String, String>): ValidationResult {
        val tool = ToolRegistry.getTool(toolName)
            ?: return ValidationResult(false, "Unknown tool: '$toolName'")

        when (tool.name) {
            "open_app" -> {
                val appName = args["app_name"] ?: args["package"]
                if (appName.isNullOrBlank()) {
                    return ValidationResult(false, "Missing required argument 'app_name'")
                }
            }
            "make_call" -> {
                val target = args["contact_or_number"] ?: args["number"] ?: args["contact"]
                if (target.isNullOrBlank()) {
                    return ValidationResult(false, "Missing phone number or contact name to call")
                }
            }
            "send_sms" -> {
                val recipient = args["recipient"] ?: args["contact"] ?: args["number"]
                val message = args["message"] ?: args["text"]
                if (recipient.isNullOrBlank()) {
                    return ValidationResult(false, "Missing recipient for SMS")
                }
                if (message.isNullOrBlank()) {
                    return ValidationResult(false, "Missing message text for SMS")
                }
            }
            "set_timer" -> {
                val duration = args["duration_seconds"]?.toIntOrNull()
                    ?: args["duration"]?.toIntOrNull()
                if (duration == null || duration <= 0) {
                    return ValidationResult(false, "Invalid or missing 'duration_seconds' (must be positive integer)")
                }
            }
            "toggle_torch" -> {
                val enabled = args["enabled"]
                if (enabled == null || (enabled != "true" && enabled != "false")) {
                    return ValidationResult(false, "Argument 'enabled' must be 'true' or 'false'")
                }
            }
            "find_contact" -> {
                val query = args["name_or_number"] ?: args["name"] ?: args["query"]
                if (query.isNullOrBlank()) {
                    return ValidationResult(false, "Missing contact name or search query")
                }
            }
            "create_reminder" -> {
                val text = args["text"] ?: args["reminder"] ?: args["title"]
                if (text.isNullOrBlank()) {
                    return ValidationResult(false, "Missing reminder text")
                }
            }
        }

        return ValidationResult(true)
    }
}
