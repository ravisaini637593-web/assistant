package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tools.ToolResult
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.WarningAmber

@Composable
fun ToolCard(
    toolName: String,
    result: ToolResult?,
    modifier: Modifier = Modifier
) {
    val icon = getToolIcon(toolName)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tool_card_$toolName"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (result?.success == true) NeonGreen.copy(alpha = 0.5f)
                else if (result?.success == false) DangerRed.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outline
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = formatToolTitle(toolName),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status chip
                if (result != null) {
                    val statusText = if (result.success) "SUCCESS" else (result.errorCode?.name ?: "FAILED")
                    val statusColor = if (result.success) NeonGreen else DangerRed

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (result != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.userMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getToolIcon(name: String): ImageVector {
    return when (name) {
        "open_app" -> Icons.Default.Apps
        "open_settings" -> Icons.Default.Settings
        "toggle_torch" -> Icons.Default.FlashlightOn
        "set_timer", "cancel_timer" -> Icons.Default.Timer
        "create_reminder" -> Icons.Default.NotificationAdd
        "media_control" -> Icons.Default.PlayArrow
        "set_volume" -> Icons.AutoMirrored.Filled.VolumeUp
        "get_device_status" -> Icons.Default.BatteryChargingFull
        "find_contact" -> Icons.Default.Contacts
        "make_call" -> Icons.Default.Call
        "send_sms" -> Icons.Default.Sms
        "create_calendar_event", "list_calendar_events" -> Icons.Default.Event
        "read_notifications" -> Icons.Default.Notifications
        "search_web" -> Icons.Default.Search
        "summarize_text" -> Icons.AutoMirrored.Filled.Subject
        else -> Icons.Default.SmartToy
    }
}

fun formatToolTitle(name: String): String {
    return name.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
