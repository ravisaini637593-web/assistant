package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantEngine
import com.example.permissions.PermissionCoordinator
import com.example.ui.components.GlowingOrb
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen

@Composable
fun SetupWizardScreen(
    engine: AssistantEngine,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }
    var hasMicPermission by remember {
        mutableStateOf(PermissionCoordinator.hasPermission(context, Manifest.permission.RECORD_AUDIO))
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (currentStep == index) 24.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentStep == index) NeonCyan
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            // Step Content
            AnimatedContent(
                targetState = currentStep,
                label = "wizard_step"
            ) { step ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (step) {
                        0 -> {
                            // Step 1: Welcome & Voice Activation
                            GlowingOrb(
                                state = com.example.assistant.EngineState.IDLE,
                                rmsLevel = 0f,
                                onClick = {}
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Welcome to Aura",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your personal Siri-style voice assistant for Android, equipped with both offline device tools and online Gemini AI.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        1 -> {
                            // Step 2: Microphone & Speech Engine
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Voice Input Access",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aura uses on-device speech recognition to listen when you press the microphone button. Your audio is processed locally on your phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (!hasMicPermission) {
                                Button(
                                    onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                    modifier = Modifier.testTag("setup_grant_mic")
                                ) {
                                    Text("Grant Microphone Access")
                                }
                            } else {
                                Text(
                                    "✓ Microphone Access Granted",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        2 -> {
                            // Step 3: Text to Speech
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Spoken Answers (TTS)",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aura speaks responses using your device's built-in Text-To-Speech engine. You can adjust speed and pitch anytime in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { engine.ttsEngine.speak("Aura voice engine ready.") }
                            ) {
                                Text("Test Assistant Voice")
                            }
                        }

                        3 -> {
                            // Step 4: Privacy & Least-Privilege
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Privacy & Local First",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All phone commands (timers, apps, flashlight, volume, battery) execute strictly on-device without cloud transmission. High-risk actions require explicit user confirmation.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(60.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.testTag("wizard_next_button")
                ) {
                    Text(if (currentStep == 3) "Get Started" else "Next")
                }
            }
        }
    }
}
