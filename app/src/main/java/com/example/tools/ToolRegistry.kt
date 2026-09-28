package com.example.tools

import android.Manifest

object ToolRegistry {
    val tools: Map<String, ToolDefinition> = listOf(
        ToolDefinition(
            name = "open_app",
            purpose = "Launch an installed Android application by name or keyword",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf("app_name" to "Name of the app to launch (e.g. YouTube, Maps, Chrome, Camera)"),
            userExplanation = "Opening requested application"
        ),
        ToolDefinition(
            name = "open_settings",
            purpose = "Open an Android settings screen (Wi-Fi, Bluetooth, Battery, Display, Sound, Apps, etc.)",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf("settings_page" to "Page to open (wifi, bluetooth, battery, display, sound, apps, accessibility, notifications, system)"),
            userExplanation = "Opening device settings screen"
        ),
        ToolDefinition(
            name = "toggle_torch",
            purpose = "Turn the flashlight torch on or off",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            requiredPermissions = listOf(Manifest.permission.CAMERA),
            parametersSchema = mapOf("enabled" to "true to turn torch on, false to turn it off"),
            userExplanation = "Controlling device flashlight"
        ),
        ToolDefinition(
            name = "set_timer",
            purpose = "Set a countdown timer on the device",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf(
                "duration_seconds" to "Duration in seconds (e.g. 300 for 5 minutes)",
                "label" to "Optional label for the timer"
            ),
            userExplanation = "Setting device countdown timer"
        ),
        ToolDefinition(
            name = "cancel_timer",
            purpose = "Cancel an active timer",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf("timer_id" to "Optional ID or label of the timer to cancel"),
            userExplanation = "Cancelling countdown timer"
        ),
        ToolDefinition(
            name = "create_reminder",
            purpose = "Create a reminder item with optional due time",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf(
                "text" to "What to remember",
                "time" to "When to remind (e.g. in 1 hour, tomorrow 9am, or timestamp)"
            ),
            userExplanation = "Creating a local reminder"
        ),
        ToolDefinition(
            name = "media_control",
            purpose = "Control media playback (play, pause, next, previous)",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf("action" to "Playback action: play, pause, toggle, next, previous"),
            userExplanation = "Controlling media playback"
        ),
        ToolDefinition(
            name = "set_volume",
            purpose = "Adjust audio volume level",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf(
                "level" to "Volume level percentage (0 to 100) or direction (up/down)",
                "stream" to "media, ring, notification, or alarm"
            ),
            userExplanation = "Adjusting audio volume"
        ),
        ToolDefinition(
            name = "get_device_status",
            purpose = "Query battery percentage, network connection, volume, or storage status",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf("fields" to "battery, network, volume, memory, all"),
            userExplanation = "Reading device diagnostics and status"
        ),
        ToolDefinition(
            name = "find_contact",
            purpose = "Look up a contact name and phone number",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            requiredPermissions = listOf(Manifest.permission.READ_CONTACTS),
            parametersSchema = mapOf("name_or_number" to "Contact name or phone number fragment"),
            userExplanation = "Searching device contacts"
        ),
        ToolDefinition(
            name = "make_call",
            purpose = "Place a telephone call to a contact or phone number",
            riskLevel = RiskLevel.HIGH,
            requiresConfirmation = true,
            requiredPermissions = listOf(Manifest.permission.CALL_PHONE),
            parametersSchema = mapOf("contact_or_number" to "Phone number or contact name to call"),
            userExplanation = "Placing phone call"
        ),
        ToolDefinition(
            name = "send_sms",
            purpose = "Compose or send an SMS text message to a contact or phone number",
            riskLevel = RiskLevel.HIGH,
            requiresConfirmation = true,
            parametersSchema = mapOf(
                "recipient" to "Phone number or contact name",
                "message" to "Text content to send"
            ),
            userExplanation = "Sending text message"
        ),
        ToolDefinition(
            name = "create_calendar_event",
            purpose = "Add an event or appointment to the device calendar",
            riskLevel = RiskLevel.MEDIUM,
            requiresConfirmation = false,
            requiredPermissions = listOf(Manifest.permission.WRITE_CALENDAR),
            parametersSchema = mapOf(
                "title" to "Event title",
                "start_time" to "Event start time description",
                "duration_minutes" to "Duration in minutes"
            ),
            userExplanation = "Adding event to calendar"
        ),
        ToolDefinition(
            name = "list_calendar_events",
            purpose = "Read upcoming events from the device calendar",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            requiredPermissions = listOf(Manifest.permission.READ_CALENDAR),
            parametersSchema = mapOf("day" to "today, tomorrow, or upcoming"),
            userExplanation = "Reading calendar schedule"
        ),
        ToolDefinition(
            name = "read_notifications",
            purpose = "Read recent notifications with user-granted Notification Listener access",
            riskLevel = RiskLevel.MEDIUM,
            requiresConfirmation = false,
            requiredSpecialAccess = "NOTIFICATION_LISTENER",
            parametersSchema = mapOf("filter" to "Optional app name or filter query"),
            userExplanation = "Reading active notifications"
        ),
        ToolDefinition(
            name = "search_web",
            purpose = "Search the web for up-to-date information (online only)",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf("query" to "Search query to look up"),
            userExplanation = "Searching web information"
        ),
        ToolDefinition(
            name = "summarize_text",
            purpose = "Condense and summarize provided text into concise key points",
            riskLevel = RiskLevel.LOW,
            requiresConfirmation = false,
            parametersSchema = mapOf("text" to "Text to summarize", "style" to "bullet_points or paragraph"),
            userExplanation = "Summarizing text"
        )
    ).associateBy { it.name }

    fun getTool(name: String): ToolDefinition? = tools[name]
}
