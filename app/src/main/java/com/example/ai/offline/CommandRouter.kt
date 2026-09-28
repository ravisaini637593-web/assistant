package com.example.ai.offline

data class ParsedCommand(
    val toolName: String,
    val arguments: Map<String, String>,
    val confidence: Float,
    val explanation: String
)

object CommandRouter {

    fun match(input: String): ParsedCommand? {
        val clean = input.trim().lowercase()

        // 1. Flashlight / Torch
        if (clean.matches(Regex(".*(turn on|enable|switch on)\\s+(the\\s+)?(flashlight|torch).*")) ||
            clean.matches(Regex(".*(flashlight|torch)\\s+on.*"))
        ) {
            return ParsedCommand("toggle_torch", mapOf("enabled" to "true"), 0.95f, "Turn on flashlight")
        }
        if (clean.matches(Regex(".*(turn off|disable|switch off)\\s+(the\\s+)?(flashlight|torch).*")) ||
            clean.matches(Regex(".*(flashlight|torch)\\s+off.*"))
        ) {
            return ParsedCommand("toggle_torch", mapOf("enabled" to "false"), 0.95f, "Turn off flashlight")
        }

        // 2. Timers
        val timerSecMatch = Regex(".*(set|start)\\s+(a\\s+)?timer\\s+for\\s+(\\d+)\\s*(s|sec|second|seconds).*").find(clean)
        if (timerSecMatch != null) {
            val secs = timerSecMatch.groupValues[3]
            return ParsedCommand("set_timer", mapOf("duration_seconds" to secs, "label" to "Timer"), 0.95f, "Set $secs second timer")
        }
        val timerMinMatch = Regex(".*(set|start)\\s+(a\\s+)?timer\\s+for\\s+(\\d+)\\s*(m|min|minute|minutes).*").find(clean)
        if (timerMinMatch != null) {
            val mins = timerMinMatch.groupValues[3].toIntOrNull() ?: 1
            val totalSecs = (mins * 60).toString()
            return ParsedCommand("set_timer", mapOf("duration_seconds" to totalSecs, "label" to "Timer"), 0.95f, "Set $mins minute timer")
        }
        val timerGenericMatch = Regex(".*timer\\s+(\\d+)\\s*(min|m|minute|minutes).*").find(clean)
        if (timerGenericMatch != null) {
            val mins = timerGenericMatch.groupValues[1].toIntOrNull() ?: 1
            return ParsedCommand("set_timer", mapOf("duration_seconds" to (mins * 60).toString(), "label" to "Timer"), 0.9f, "Set $mins minute timer")
        }
        if (clean.contains("cancel timer") || clean.contains("stop timer")) {
            return ParsedCommand("cancel_timer", emptyMap(), 0.95f, "Cancel active timer")
        }

        // 3. Reminders
        val remindMatch = Regex(".*(remind me to|create reminder|remember to)\\s+(.+)").find(clean)
        if (remindMatch != null) {
            val task = remindMatch.groupValues[2].trim()
            return ParsedCommand("create_reminder", mapOf("text" to task), 0.9f, "Create reminder for $task")
        }

        // 4. Device Status / Battery
        if (clean.contains("battery") || clean.contains("charge") || clean.contains("power level")) {
            return ParsedCommand("get_device_status", mapOf("fields" to "battery"), 0.95f, "Check battery status")
        }
        if (clean.contains("device status") || clean.contains("phone status") || clean.contains("system status")) {
            return ParsedCommand("get_device_status", mapOf("fields" to "all"), 0.95f, "Check device status")
        }

        // 5. Volume Controls
        if (clean.contains("volume up") || clean.contains("louder") || clean.contains("increase volume")) {
            return ParsedCommand("set_volume", mapOf("stream" to "media", "level" to "up"), 0.9f, "Increase volume")
        }
        if (clean.contains("volume down") || clean.contains("softer") || clean.contains("decrease volume") || clean.contains("lower volume")) {
            return ParsedCommand("set_volume", mapOf("stream" to "media", "level" to "down"), 0.9f, "Decrease volume")
        }
        if (clean.contains("mute") || clean.contains("silence")) {
            return ParsedCommand("set_volume", mapOf("stream" to "media", "level" to "mute"), 0.9f, "Mute audio")
        }
        val volPercentMatch = Regex(".*set volume (to )?(\\d+)%?.*").find(clean)
        if (volPercentMatch != null) {
            val lvl = volPercentMatch.groupValues[2]
            return ParsedCommand("set_volume", mapOf("stream" to "media", "level" to lvl), 0.95f, "Set volume to $lvl%")
        }

        // 6. Media Controls
        if (clean.contains("play music") || clean.contains("resume music") || clean == "play") {
            return ParsedCommand("media_control", mapOf("action" to "play"), 0.9f, "Play music")
        }
        if (clean.contains("pause music") || clean == "pause" || clean.contains("stop music")) {
            return ParsedCommand("media_control", mapOf("action" to "pause"), 0.9f, "Pause music")
        }
        if (clean.contains("next song") || clean.contains("skip song") || clean.contains("next track")) {
            return ParsedCommand("media_control", mapOf("action" to "next"), 0.9f, "Next track")
        }
        if (clean.contains("previous song") || clean.contains("previous track")) {
            return ParsedCommand("media_control", mapOf("action" to "previous"), 0.9f, "Previous track")
        }

        // 7. Settings Navigation
        val settingsMatch = Regex(".*(open|show|go to)\\s+(wifi|wi-fi|bluetooth|battery|display|sound|audio|volume|applications|apps|accessibility|settings).*").find(clean)
        if (settingsMatch != null) {
            val page = settingsMatch.groupValues[2]
            return ParsedCommand("open_settings", mapOf("settings_page" to page), 0.95f, "Open $page settings")
        }

        // 8. Open Apps
        val appMatch = Regex(".*(open|launch|start)\\s+(the\\s+)?([a-z0-9\\s]+)").find(clean)
        if (appMatch != null) {
            val appName = appMatch.groupValues[3].trim()
            if (!appName.contains("timer") && !appName.contains("reminder") && !appName.contains("settings")) {
                return ParsedCommand("open_app", mapOf("app_name" to appName), 0.85f, "Open $appName")
            }
        }

        // 9. Calling
        val callMatch = Regex(".*(call|dial|phone)\\s+([a-z0-9\\+\\-\\s]+)").find(clean)
        if (callMatch != null) {
            val contact = callMatch.groupValues[2].trim()
            if (contact.isNotBlank()) {
                return ParsedCommand("make_call", mapOf("contact_or_number" to contact), 0.9f, "Call $contact")
            }
        }

        // 10. SMS
        val smsMatch = Regex(".*(text|send message to|sms)\\s+([a-z0-9\\+\\-]+)\\s+(that|saying)?\\s*(.+)").find(clean)
        if (smsMatch != null) {
            val recipient = smsMatch.groupValues[2].trim()
            val msg = smsMatch.groupValues[4].trim()
            if (recipient.isNotBlank() && msg.isNotBlank()) {
                return ParsedCommand("send_sms", mapOf("recipient" to recipient, "message" to msg), 0.9f, "Send SMS to $recipient")
            }
        }

        // 11. Notifications
        if (clean.contains("read notification") || clean.contains("check notification") || clean.contains("my notifications")) {
            return ParsedCommand("read_notifications", emptyMap(), 0.9f, "Read notifications")
        }

        // 12. Contacts
        val findContactMatch = Regex(".*(find contact|lookup|look up contact|who is)\\s+(.+)").find(clean)
        if (findContactMatch != null) {
            val target = findContactMatch.groupValues[2].trim()
            return ParsedCommand("find_contact", mapOf("name_or_number" to target), 0.85f, "Search for $target")
        }

        return null
    }
}
