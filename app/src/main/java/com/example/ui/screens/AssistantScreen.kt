package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantEngine
import com.example.assistant.EngineState
import com.example.assistant.ModePreference
import com.example.ui.components.ConfirmationCard
import com.example.ui.components.GlowingOrb
import com.example.ui.components.ToolCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    engine: AssistantEngine,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val uiState by engine.uiState.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Aura",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        // Mode badge
                        val modeText = uiState.currentMode
                        val isOnline = uiState.isOnline
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isOnline) NeonGreen.copy(alpha = 0.15f)
                                    else WarningAmber.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isOnline) NeonGreen.copy(alpha = 0.4f)
                                    else WarningAmber.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isOnline) "ONLINE ($modeText)" else "OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) NeonGreen else WarningAmber
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToRoutines,
                        modifier = Modifier.testTag("nav_routines_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Routines", tint = ElectricBlue)
                    }
                    IconButton(
                        onClick = onNavigateToPermissions,
                        modifier = Modifier.testTag("nav_permissions_button")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = "Permissions", tint = ElectricBlue)
                    }
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("nav_history_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = ElectricBlue)
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ElectricBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content: either welcome suggestions or message stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty()) {
                    // Empty state with quick suggestion chips
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "What can I do for you?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Speak naturally or tap any shortcut below",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val quickCommands = listOf(
                            "Turn on flashlight" to Icons.Default.FlashlightOn,
                            "Set timer for 5 minutes" to Icons.Default.Timer,
                            "How much battery left?" to Icons.Default.BatteryChargingFull,
                            "Open YouTube" to Icons.Default.Apps,
                            "Open Wi-Fi settings" to Icons.Default.Wifi,
                            "Remind me to drink water" to Icons.Default.NotificationAdd
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for ((cmd, icon) in quickCommands) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quick_cmd_${cmd.take(10)}")
                                        .clickable { engine.processUserInput(cmd) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(cmd, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.messages) { msg ->
                            if (msg.role == "user") {
                                // User bubble
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                                            .background(ElectricBlue)
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = msg.text,
                                            color = Color.Black,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                // Assistant response
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    if (msg.text.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .widthIn(max = 300.dp)
                                                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    // Tool execution card if present
                                    if (msg.toolName != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        ToolCard(
                                            toolName = msg.toolName,
                                            result = msg.toolResult
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pending Confirmation Card if awaiting
            AnimatedVisibility(
                visible = uiState.state == EngineState.AWAITING_CONFIRMATION && uiState.pendingAction != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                uiState.pendingAction?.let { pending ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ConfirmationCard(
                            pendingAction = pending,
                            onConfirm = { engine.confirmPendingAction() },
                            onCancel = { engine.cancelPendingAction() }
                        )
                    }
                }
            }

            // Status label / Live transcript
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val statusString = when (uiState.state) {
                    EngineState.LISTENING -> {
                        if (uiState.partialTranscript.isNotBlank()) "\"${uiState.partialTranscript}\""
                        else "Listening to your voice..."
                    }
                    EngineState.PROCESSING -> "Analyzing request..."
                    EngineState.SPEAKING -> "Speaking... (tap orb or stop to barge in)"
                    EngineState.AWAITING_CONFIRMATION -> "Waiting for your confirmation"
                    EngineState.IDLE -> "Tap orb to speak or type below"
                }

                Text(
                    text = statusString,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (uiState.state) {
                        EngineState.LISTENING -> NeonCyan
                        EngineState.SPEAKING -> ElectricBlue
                        EngineState.AWAITING_CONFIRMATION -> WarningAmber
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            // Glowing Orb Interactive Voice Center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                GlowingOrb(
                    state = uiState.state,
                    rmsLevel = uiState.rmsLevel,
                    onClick = {
                        when (uiState.state) {
                            EngineState.SPEAKING -> engine.stopSpeaking()
                            EngineState.LISTENING -> engine.stopListening()
                            else -> engine.startListening()
                        }
                    }
                )

                // Barge-in Stop button when speaking
                if (uiState.state == EngineState.SPEAKING) {
                    IconButton(
                        onClick = { engine.stopSpeaking() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp)
                            .testTag("stop_speaking_button")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop speaking", tint = Color.Red)
                    }
                }
            }

            // Text input bar (push-to-type)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask Aura or enter phone command...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("assistant_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                val text = textInput
                                textInput = ""
                                engine.processUserInput(text)
                            }
                        },
                        modifier = Modifier
                            .testTag("assistant_send_button")
                            .clip(CircleShape)
                            .background(ElectricBlue)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send command",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
