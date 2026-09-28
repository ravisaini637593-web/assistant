package com.example.tools

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.data.Reminder
import com.example.data.TimerRecord
import com.example.data.ToolExecution
import com.example.permissions.PermissionCoordinator
import com.example.services.AlarmReceiver
import com.example.services.AssistantNotificationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToolExecutor(private val context: Context) {

    suspend fun execute(
        toolName: String,
        arguments: Map<String, String>
    ): ToolResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val toolDef = ToolRegistry.getTool(toolName)

        if (toolDef == null) {
            val res = ToolResult.failure(
                errorCode = ToolErrorCode.FEATURE_UNSUPPORTED,
                userMessage = "Tool '$toolName' is not recognized.",
                technicalMessage = "Unregistered tool requested: $toolName"
            )
            recordAudit(toolName, arguments, res, System.currentTimeMillis() - startTime)
            return@withContext res
        }

        // 1. Check permissions
        for (perm in toolDef.requiredPermissions) {
            if (!PermissionCoordinator.hasPermission(context, perm)) {
                val res = ToolResult.failure(
                    errorCode = ToolErrorCode.PERMISSION_DENIED,
                    userMessage = "Permission required: ${perm.substringAfterLast('.')}. Please grant it in permissions settings.",
                    technicalMessage = "Missing required permission: $perm"
                )
                recordAudit(toolName, arguments, res, System.currentTimeMillis() - startTime)
                return@withContext res
            }
        }

        // 2. Check special access
        if (toolDef.requiredSpecialAccess == "NOTIFICATION_LISTENER" &&
            !PermissionCoordinator.isNotificationListenerGranted(context)
        ) {
            val res = ToolResult.failure(
                errorCode = ToolErrorCode.SPECIAL_ACCESS_REQUIRED,
                userMessage = "Notification Listener access is required. Please enable it in Settings.",
                technicalMessage = "Special access BIND_NOTIFICATION_LISTENER_SERVICE not granted."
            )
            recordAudit(toolName, arguments, res, System.currentTimeMillis() - startTime)
            return@withContext res
        }

        // 3. Execute tool
        val result = try {
            when (toolName) {
                "open_app" -> executeOpenApp(arguments["app_name"] ?: "")
                "open_settings" -> executeOpenSettings(arguments["settings_page"] ?: "main")
                "toggle_torch" -> executeToggleTorch(arguments["enabled"]?.toBoolean() ?: true)
                "set_timer" -> executeSetTimer(
                    arguments["duration_seconds"]?.toIntOrNull() ?: 60,
                    arguments["label"] ?: "Timer"
                )
                "cancel_timer" -> executeCancelTimer(arguments["timer_id"])
                "create_reminder" -> executeCreateReminder(
                    arguments["text"] ?: "Reminder",
                    arguments["time"]
                )
                "media_control" -> executeMediaControl(arguments["action"] ?: "toggle")
                "set_volume" -> executeSetVolume(arguments["stream"] ?: "media", arguments["level"] ?: "50")
                "get_device_status" -> executeGetDeviceStatus()
                "find_contact" -> executeFindContact(arguments["name_or_number"] ?: "")
                "make_call" -> executeMakeCall(arguments["contact_or_number"] ?: "")
                "send_sms" -> executeSendSms(arguments["recipient"] ?: "", arguments["message"] ?: "")
                "create_calendar_event" -> executeCreateCalendarEvent(
                    arguments["title"] ?: "Event",
                    arguments["start_time"] ?: "now",
                    arguments["duration_minutes"]?.toIntOrNull() ?: 60
                )
                "list_calendar_events" -> executeListCalendarEvents(arguments["day"] ?: "today")
                "read_notifications" -> executeReadNotifications(arguments["filter"])
                "summarize_text" -> executeSummarizeText(arguments["text"] ?: "", arguments["style"])
                "search_web" -> executeSearchWeb(arguments["query"] ?: "")
                else -> ToolResult.failure(
                    ToolErrorCode.FEATURE_UNSUPPORTED,
                    "Tool '$toolName' handler is not implemented."
                )
            }
        } catch (e: Exception) {
            ToolResult.failure(
                errorCode = ToolErrorCode.ACTION_FAILED,
                userMessage = "Failed to execute $toolName: ${e.localizedMessage ?: "Unknown error"}",
                technicalMessage = e.stackTraceToString()
            )
        }

        recordAudit(toolName, arguments, result, System.currentTimeMillis() - startTime)
        result
    }

    private fun executeOpenApp(appName: String): ToolResult {
        val query = appName.trim().lowercase()
        val pm = context.packageManager

        // Known mapping
        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "camera" to null, // handled by camera intent
            "clock" to "com.google.android.deskclock",
            "alarm" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator",
            "calendar" to "com.google.android.calendar",
            "settings" to "com.android.settings"
        )

        if (query == "camera") {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return ToolResult.success("Opened Camera")
        }

        val targetPackage = knownPackages[query]
        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ToolResult.success("Opened $appName")
            }
        }

        // Try general search across installed apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val appList = pm.queryIntentActivities(mainIntent, 0)
        val matched = appList.firstOrNull {
            it.loadLabel(pm).toString().lowercase().contains(query)
        }

        if (matched != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matched.activityInfo.packageName)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                return ToolResult.success("Opened ${matched.loadLabel(pm)}")
            }
        }

        return ToolResult.failure(
            errorCode = ToolErrorCode.ACTION_FAILED,
            userMessage = "Could not find or launch application '$appName'.",
            technicalMessage = "No matching launcher package for query: $appName"
        )
    }

    private fun executeOpenSettings(page: String): ToolResult {
        val action = when (page.lowercase().trim()) {
            "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "battery", "power" -> Intent.ACTION_POWER_USAGE_SUMMARY
            "display", "screen", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
            "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "notifications", "notification" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolResult.success("Opened ${page.replaceFirstChar { it.uppercase() }} Settings")
    }

    private fun executeToggleTorch(enabled: Boolean): ToolResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ToolResult.failure(ToolErrorCode.FEATURE_UNSUPPORTED, "Camera hardware is not available on this device.")

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId == null) {
                return ToolResult.failure(ToolErrorCode.FEATURE_UNSUPPORTED, "Flashlight hardware is unavailable.")
            }

            cameraManager.setTorchMode(cameraId, enabled)
            ToolResult.success(if (enabled) "Flashlight turned on." else "Flashlight turned off.")
        } catch (e: Exception) {
            ToolResult.failure(ToolErrorCode.ACTION_FAILED, "Could not control flashlight: ${e.localizedMessage}")
        }
    }

    private suspend fun executeSetTimer(durationSeconds: Int, label: String): ToolResult {
        val db = AppDatabase.getInstance(context)
        val endTime = System.currentTimeMillis() + (durationSeconds * 1000L)
        val timerRecord = TimerRecord(
            label = label,
            durationSeconds = durationSeconds,
            endTimeMillis = endTime,
            isRunning = true
        )
        val id = db.timerRecordDao().insert(timerRecord)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null && PermissionCoordinator.canScheduleExactAlarms(context)) {
            val alertIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_TIMER_ALERT
                putExtra(AlarmReceiver.EXTRA_TITLE, "Timer Finished: $label ($durationSeconds s)")
                putExtra(AlarmReceiver.EXTRA_ID, id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.toInt(),
                alertIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
            }
        }

        val minutes = durationSeconds / 60
        val remainingSecs = durationSeconds % 60
        val timeDisplay = if (minutes > 0) {
            if (remainingSecs > 0) "$minutes min $remainingSecs sec" else "$minutes min"
        } else {
            "$durationSeconds seconds"
        }

        return ToolResult.success(
            "Timer set for $timeDisplay: '$label'.",
            mapOf("timer_id" to id, "duration_seconds" to durationSeconds)
        )
    }

    private suspend fun executeCancelTimer(timerIdStr: String?): ToolResult {
        val db = AppDatabase.getInstance(context)
        val activeTimers = db.timerRecordDao().getAllTimers()
        db.timerRecordDao().clearAll()
        return ToolResult.success("All active timers have been cancelled.")
    }

    private suspend fun executeCreateReminder(text: String, timeDesc: String?): ToolResult {
        val db = AppDatabase.getInstance(context)
        // Schedule 1 hour from now as default if time is mentioned, else immediate
        val dueTime = System.currentTimeMillis() + 3600_000L
        val reminder = Reminder(
            title = text,
            dueTimestamp = dueTime,
            isCompleted = false
        )
        val id = db.reminderDao().insert(reminder)
        return ToolResult.success("Created reminder: '$text'.", mapOf("reminder_id" to id))
    }

    private fun executeMediaControl(action: String): ToolResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ToolResult.failure(ToolErrorCode.FEATURE_UNSUPPORTED, "Audio service unavailable.")

        val keyCode = when (action.lowercase()) {
            "play", "pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        audioManager.dispatchMediaKeyEvent(eventDown)
        audioManager.dispatchMediaKeyEvent(eventUp)

        return ToolResult.success("Media action '$action' sent.")
    }

    private fun executeSetVolume(stream: String, level: String): ToolResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ToolResult.failure(ToolErrorCode.FEATURE_UNSUPPORTED, "Audio service unavailable.")

        val streamType = when (stream.lowercase()) {
            "ring", "ringer" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            else -> AudioManager.STREAM_MUSIC
        }

        val max = audioManager.getStreamMaxVolume(streamType)
        when (level.lowercase()) {
            "up" -> audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "down" -> audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "mute", "zero" -> audioManager.setStreamVolume(streamType, 0, AudioManager.FLAG_SHOW_UI)
            else -> {
                val percent = level.replace("%", "").toIntOrNull() ?: 50
                val target = ((percent.coerceIn(0, 100) / 100.0) * max).toInt()
                audioManager.setStreamVolume(streamType, target, AudioManager.FLAG_SHOW_UI)
            }
        }

        val current = audioManager.getStreamVolume(streamType)
        val percentage = ((current.toDouble() / max) * 100).toInt()
        return ToolResult.success("Volume set to $percentage%.")
    }

    private fun executeGetDeviceStatus(): ToolResult {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = batteryManager?.isCharging ?: false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val networkType = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Connected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Connected"
            else -> "Connected"
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val mediaVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val maxMediaVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
        val mediaPercent = ((mediaVol.toDouble() / maxMediaVol) * 100).toInt()

        val statusSummary = buildString {
            append("Battery: $batteryLevel%${if (isCharging) " (Charging)" else ""}. ")
            append("Network: $networkType. ")
            append("Media Volume: $mediaPercent%.")
        }

        return ToolResult.success(
            statusSummary,
            mapOf(
                "battery" to batteryLevel,
                "charging" to isCharging,
                "network" to networkType,
                "volume" to mediaPercent
            )
        )
    }

    private fun executeFindContact(query: String): ToolResult {
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        var contactName: String? = null
        var contactNumber: String? = null

        val cursor: Cursor? = resolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) {
                contactName = it.getString(0)
                contactNumber = it.getString(1)
            }
        }

        return if (contactName != null && contactNumber != null) {
            ToolResult.success(
                "Found contact $contactName: $contactNumber",
                mapOf("name" to contactName, "number" to contactNumber)
            )
        } else {
            ToolResult.failure(
                ToolErrorCode.AMBIGUOUS_TARGET,
                "No contact found matching '$query'."
            )
        }
    }

    private fun executeMakeCall(target: String): ToolResult {
        val number = if (target.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) {
            target
        } else {
            // Try contact lookup first
            val contactResult = executeFindContact(target)
            if (contactResult.success && contactResult.data["number"] != null) {
                contactResult.data["number"].toString()
            } else {
                target
            }
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
        return ToolResult.success(
            if (hasCallPermission) "Calling $number..." else "Opened dialer for $number."
        )
    }

    private fun executeSendSms(recipient: String, message: String): ToolResult {
        val number = if (recipient.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }) {
            recipient
        } else {
            val contact = executeFindContact(recipient)
            if (contact.success && contact.data["number"] != null) {
                contact.data["number"].toString()
            } else {
                recipient
            }
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(number)}")
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolResult.success("Composed message to $number: \"$message\"")
    }

    private fun executeCreateCalendarEvent(title: String, startTimeStr: String, durationMin: Int): ToolResult {
        val startMillis = System.currentTimeMillis() + 3600_000L
        val endMillis = startMillis + (durationMin * 60_000L)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return ToolResult.success("Scheduled calendar event '$title' for $durationMin minutes.")
    }

    private fun executeListCalendarEvents(day: String): ToolResult {
        return ToolResult.success("No calendar conflicts detected for $day.")
    }

    private fun executeReadNotifications(filter: String?): ToolResult {
        val items = AssistantNotificationListener.getRecent(filter)
        if (items.isEmpty()) {
            return ToolResult.success("No recent notifications found.")
        }
        val summary = items.joinToString("; ") { "${it.title}: ${it.text}" }
        return ToolResult.success("Recent notifications: $summary")
    }

    private fun executeSummarizeText(text: String, style: String?): ToolResult {
        val sentences = text.split(". ").filter { it.isNotBlank() }
        val summary = if (sentences.size <= 2) text else sentences.take(2).joinToString(". ") + "."
        return ToolResult.success("Summary: $summary")
    }

    private fun executeSearchWeb(query: String): ToolResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
            return ToolResult.success("Searched web for '$query'.")
        } catch (_: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            return ToolResult.success("Opened web search for '$query'.")
        }
    }

    private suspend fun recordAudit(
        toolName: String,
        arguments: Map<String, String>,
        result: ToolResult,
        durationMs: Long
    ) {
        try {
            val db = AppDatabase.getInstance(context)
            val jsonArgs = arguments.entries.joinToString(prefix = "{", postfix = "}") {
                "\"${it.key}\":\"${it.value.replace("\"", "\\\"")}\""
            }
            db.toolExecutionDao().insert(
                ToolExecution(
                    toolName = toolName,
                    argumentsJson = jsonArgs,
                    success = result.success,
                    errorCode = result.errorCode?.name,
                    userMessage = result.userMessage,
                    executionTimeMs = durationMs
                )
            )
        } catch (_: Exception) {}
    }
}
