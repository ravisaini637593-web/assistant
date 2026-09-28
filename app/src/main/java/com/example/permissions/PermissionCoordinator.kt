package com.example.permissions

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val permissionString: String?,
    val isSpecialAccess: Boolean = false,
    val isGranted: Boolean,
    val settingsAction: String? = null
)

object PermissionCoordinator {

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAllPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    fun isNotificationListenerGranted(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    fun isAccessibilityGranted(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val expected = "${context.packageName}/com.example.services.AssistantAccessibilityService"
        return enabledServices.contains(expected)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    fun getPermissionList(context: Context): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone
        list.add(
            PermissionItem(
                id = "RECORD_AUDIO",
                title = "Microphone",
                description = "Required for on-device voice recognition and live speech input",
                permissionString = Manifest.permission.RECORD_AUDIO,
                isGranted = hasPermission(context, Manifest.permission.RECORD_AUDIO)
            )
        )

        // 2. Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(
                PermissionItem(
                    id = "POST_NOTIFICATIONS",
                    title = "Post Notifications",
                    description = "Required to notify you when timers and reminders finish",
                    permissionString = Manifest.permission.POST_NOTIFICATIONS,
                    isGranted = hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                )
            )
        }

        // 3. Contacts
        list.add(
            PermissionItem(
                id = "READ_CONTACTS",
                title = "Contacts",
                description = "Required to search contacts and resolve names for calling or messaging",
                permissionString = Manifest.permission.READ_CONTACTS,
                isGranted = hasPermission(context, Manifest.permission.READ_CONTACTS)
            )
        )

        // 4. Phone
        list.add(
            PermissionItem(
                id = "CALL_PHONE",
                title = "Direct Calling",
                description = "Allows calling numbers directly upon confirmation (otherwise falls back to dialer)",
                permissionString = Manifest.permission.CALL_PHONE,
                isGranted = hasPermission(context, Manifest.permission.CALL_PHONE)
            )
        )

        // 5. SMS
        list.add(
            PermissionItem(
                id = "SEND_SMS",
                title = "Send SMS",
                description = "Allows sending quick text messages with explicit confirmation",
                permissionString = Manifest.permission.SEND_SMS,
                isGranted = hasPermission(context, Manifest.permission.SEND_SMS)
            )
        )

        // 6. Calendar
        list.add(
            PermissionItem(
                id = "CALENDAR",
                title = "Calendar",
                description = "Required to read upcoming events and schedule new appointments",
                permissionString = Manifest.permission.READ_CALENDAR,
                isGranted = hasPermission(context, Manifest.permission.READ_CALENDAR) &&
                        hasPermission(context, Manifest.permission.WRITE_CALENDAR)
            )
        )

        // 7. Camera (Flashlight)
        list.add(
            PermissionItem(
                id = "CAMERA",
                title = "Flashlight / Torch",
                description = "Required to toggle the device flashlight on and off",
                permissionString = Manifest.permission.CAMERA,
                isGranted = hasPermission(context, Manifest.permission.CAMERA)
            )
        )

        // 8. Notification Listener (Special Access)
        list.add(
            PermissionItem(
                id = "NOTIFICATION_LISTENER",
                title = "Notification Listener",
                description = "Optional special access to read and summarize incoming alerts",
                permissionString = null,
                isSpecialAccess = true,
                isGranted = isNotificationListenerGranted(context),
                settingsAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            )
        )

        // 9. Accessibility Service (Special Access)
        list.add(
            PermissionItem(
                id = "ACCESSIBILITY",
                title = "Accessibility Automation",
                description = "Optional deterministic assistance for back/home screen shortcuts",
                permissionString = null,
                isSpecialAccess = true,
                isGranted = isAccessibilityGranted(context),
                settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
            )
        )

        // 10. Exact Alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(
                PermissionItem(
                    id = "EXACT_ALARM",
                    title = "Exact Alarms",
                    description = "Ensures timers and reminders fire precisely to the second",
                    permissionString = null,
                    isSpecialAccess = true,
                    isGranted = canScheduleExactAlarms(context),
                    settingsAction = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                )
            )
        }

        return list
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openSpecialAccessSettings(context: Context, action: String) {
        try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppSettings(context)
        }
    }
}
