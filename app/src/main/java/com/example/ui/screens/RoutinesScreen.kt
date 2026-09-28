package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.AppDatabase
import com.example.data.Routine
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(
    engine: AssistantEngine,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val routines by db.routineDao().getAll().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newTrigger by remember { mutableStateOf("") }
    var newActions by remember { mutableStateOf("") }

    // Seed default routines if empty
    LaunchedEffect(Unit) {
        val current = db.routineDao().getEnabledRoutines()
        if (current.isEmpty()) {
            db.routineDao().insert(
                Routine(
                    title = "Good Morning",
                    triggerPhrase = "good morning",
                    description = "Reads battery status and opens display settings",
                    actionsJson = "battery status; open display settings"
                )
            )
            db.routineDao().insert(
                Routine(
                    title = "Bedtime Routine",
                    triggerPhrase = "good night",
                    description = "Turns off flashlight and mutes media volume",
                    actionsJson = "turn off flashlight; mute volume"
                )
            )
            db.routineDao().insert(
                Routine(
                    title = "5-Minute Focus Session",
                    triggerPhrase = "start focus session",
                    description = "Sets a 5-minute countdown timer",
                    actionsJson = "set timer for 300 seconds"
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant Routines", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("routines_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_routine_button")) {
                        Icon(Icons.Default.Add, contentDescription = "Add Routine", tint = ElectricBlue)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Automate multi-step actions with a single spoken phrase or button tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(routines) { routine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_card_${routine.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = routine.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = {
                                    engine.executeRoutine(routine)
                                    onNavigateBack()
                                },
                                modifier = Modifier.testTag("run_routine_${routine.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricBlue,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = routine.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Say: \"${routine.triggerPhrase}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Create Routine") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Routine Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_routine_title")
                        )
                        OutlinedTextField(
                            value = newTrigger,
                            onValueChange = { newTrigger = it },
                            label = { Text("Trigger phrase (e.g. 'work time')") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_routine_trigger")
                        )
                        OutlinedTextField(
                            value = newActions,
                            onValueChange = { newActions = it },
                            label = { Text("Commands separated by ';' (e.g. 'turn on flashlight; set volume to 50%')") },
                            modifier = Modifier.fillMaxWidth().testTag("new_routine_actions")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank() && newActions.isNotBlank()) {
                                coroutineScope.launch {
                                    db.routineDao().insert(
                                        Routine(
                                            title = newTitle,
                                            triggerPhrase = if (newTrigger.isNotBlank()) newTrigger else newTitle.lowercase(),
                                            description = "Custom routine",
                                            actionsJson = newActions
                                        )
                                    )
                                    showAddDialog = false
                                    newTitle = ""
                                    newTrigger = ""
                                    newActions = ""
                                }
                            }
                        },
                        modifier = Modifier.testTag("save_routine_button")
                    ) {
                        Text("Save Routine")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
