package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantEngine
import com.example.permissions.PermissionCoordinator
import com.example.tools.ToolRegistry
import com.example.tools.ToolResult
import com.example.ui.components.ToolCard
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    engine: AssistantEngine,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isOnline = remember { engine.isOnline() }
    val isSttAvailable = remember { engine.speechAdapter.isRecognitionAvailable }
    val isOnDeviceStt = remember { engine.speechAdapter.isOnDeviceRecognitionAvailable }
    val ttsVoicesCount = remember { engine.ttsEngine.getInstalledVoicesCount() }
    val aiCoreStatus = remember { engine.onDeviceModel.checkStatus() }
    val isNotifListener = remember { PermissionCoordinator.isNotificationListenerGranted(context) }
    val isAccessibility = remember { PermissionCoordinator.isAccessibilityGranted(context) }

    // Tool tester
    var selectedToolName by remember { mutableStateOf("get_device_status") }
    var testResult by remember { mutableStateOf<ToolResult?>(null) }
    var isExecuting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("diag_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Capabilities Checklist
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Telemetry & Capabilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    DiagRow("Internet Connectivity", if (isOnline) "Connected" else "Disconnected", isOnline)
                    DiagRow("Speech Recognition Engine", if (isSttAvailable) "Available" else "Not Present", isSttAvailable)
                    DiagRow("On-Device SpeechRecognizer", if (isOnDeviceStt) "Supported (API 31+)" else "Online STT Fallback", isOnDeviceStt)
                    DiagRow("TextToSpeech Voices", "$ttsVoicesCount voices installed", ttsVoicesCount > 0)
                    DiagRow("Android OS Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", true)
                    DiagRow("Notification Listener", if (isNotifListener) "Enabled" else "Disabled", isNotifListener)
                    DiagRow("Accessibility Helper", if (isAccessibility) "Enabled" else "Disabled", isAccessibility)
                    DiagRow("On-Device AICore / Nano", aiCoreStatus.modelStatus, aiCoreStatus.isServicePresent)
                }
            }

            // Interactive Tool Playground
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tool Sandbox & Verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Test any phone-control tool to verify truthful execution and response formatting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val testableTools = listOf("get_device_status", "toggle_torch", "set_timer", "media_control", "open_settings")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (t in testableTools) {
                            FilterChip(
                                selected = selectedToolName == t,
                                onClick = { selectedToolName = t },
                                label = { Text(t.replace("_", " "), fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isExecuting = true
                            coroutineScope.launch {
                                val args = when (selectedToolName) {
                                    "toggle_torch" -> mapOf("enabled" to "true")
                                    "set_timer" -> mapOf("duration_seconds" to "10", "label" to "Diag Test")
                                    "media_control" -> mapOf("action" to "play")
                                    "open_settings" -> mapOf("settings_page" to "battery")
                                    else -> mapOf("fields" to "all")
                                }
                                testResult = engine.toolExecutor.execute(selectedToolName, args)
                                isExecuting = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("execute_test_tool_button"),
                        enabled = !isExecuting
                    ) {
                        Text(if (isExecuting) "Executing..." else "Run Test: $selectedToolName")
                    }

                    if (testResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ToolCard(toolName = selectedToolName, result = testResult)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DiagRow(label: String, value: String, isOk: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isOk) NeonGreen else WarningAmber
        )
    }
}
