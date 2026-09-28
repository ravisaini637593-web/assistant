package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantEngine
import com.example.assistant.ModePreference
import com.example.ui.theme.ElectricBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    engine: AssistantEngine,
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val uiState by engine.uiState.collectAsState()
    var selectedMode by remember { mutableStateOf(engine.modePreference) }
    var apiKeyInput by remember { mutableStateOf(engine.geminiClient.customApiKey ?: "") }
    var selectedModel by remember { mutableStateOf(engine.geminiClient.selectedModel) }
    var speechRate by remember { mutableFloatStateOf(engine.ttsEngine.speechRate) }
    var pitch by remember { mutableFloatStateOf(engine.ttsEngine.pitch) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDiagnostics, modifier = Modifier.testTag("diagnostics_button")) {
                        Icon(Icons.Default.Info, contentDescription = "Diagnostics", tint = ElectricBlue)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Preference
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Operating Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Controls how requests are routed between offline device tools and online Gemini reasoning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ModePreference.values().forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = {
                                    selectedMode = mode
                                    engine.setModePreference(mode)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (mode) {
                                        ModePreference.AUTO -> "Auto (Recommended)"
                                        ModePreference.PREFER_OFFLINE -> "Prefer Offline"
                                        ModePreference.PREFER_ONLINE -> "Prefer Online"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = when (mode) {
                                        ModePreference.AUTO -> "Executes phone actions locally, uses Gemini for chat"
                                        ModePreference.PREFER_OFFLINE -> "Zero network usage for commands, max privacy"
                                        ModePreference.PREFER_ONLINE -> "Routes complex reasoning through cloud model"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Voice & Speech synthesis settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Speech & Voice (TTS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Speech Rate: ${String.format("%.1f", speechRate)}x", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            engine.ttsEngine.speechRate = it
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pitch: ${String.format("%.1f", pitch)}x", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = pitch,
                        onValueChange = {
                            pitch = it
                            engine.ttsEngine.pitch = it
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            engine.ttsEngine.speak("Hello! I am Aura, your personal device assistant.")
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Voice")
                    }
                }
            }

            // Gemini API settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gemini AI Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Used for natural conversation, general questions, and dynamic tool orchestration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            engine.geminiClient.customApiKey = it.ifBlank { null }
                        },
                        label = { Text("Custom Gemini API Key (Optional)") },
                        placeholder = { Text("Inject from Secrets or enter here") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Model Selection:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    val models = listOf("gemini-2.5-flash", "gemini-3.5-flash", "gemini-3.1-pro-preview")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (model in models) {
                            FilterChip(
                                selected = selectedModel == model,
                                onClick = {
                                    selectedModel = model
                                    engine.geminiClient.selectedModel = model
                                },
                                label = { Text(model.removePrefix("gemini-"), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Diagnostics navigation card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                onClick = onNavigateToDiagnostics
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("System Diagnostics & Telemetry", fontWeight = FontWeight.Bold)
                        Text("Inspect STT, TTS voices, network, and test tools", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
