package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.assistant.AssistantEngine
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    SETUP_WIZARD,
    ASSISTANT,
    HISTORY,
    SETTINGS,
    PERMISSIONS,
    ROUTINES,
    DIAGNOSTICS
}

class MainActivity : ComponentActivity() {

    private lateinit var engine: AssistantEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        engine = AssistantEngine(this, lifecycleScope)

        // Check if first run
        val prefs = getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember {
                        mutableStateOf(if (isFirstRun) Screen.SETUP_WIZARD else Screen.ASSISTANT)
                    }

                    when (currentScreen) {
                        Screen.SETUP_WIZARD -> {
                            SetupWizardScreen(
                                engine = engine,
                                onComplete = {
                                    prefs.edit().putBoolean("is_first_run", false).apply()
                                    currentScreen = Screen.ASSISTANT
                                }
                            )
                        }

                        Screen.ASSISTANT -> {
                            AssistantScreen(
                                engine = engine,
                                onNavigateToHistory = { currentScreen = Screen.HISTORY },
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                onNavigateToPermissions = { currentScreen = Screen.PERMISSIONS },
                                onNavigateToRoutines = { currentScreen = Screen.ROUTINES },
                                onNavigateToDiagnostics = { currentScreen = Screen.DIAGNOSTICS }
                            )
                        }

                        Screen.HISTORY -> {
                            BackHandler { currentScreen = Screen.ASSISTANT }
                            ConversationHistoryScreen(
                                engine = engine,
                                onNavigateBack = { currentScreen = Screen.ASSISTANT }
                            )
                        }

                        Screen.SETTINGS -> {
                            BackHandler { currentScreen = Screen.ASSISTANT }
                            SettingsScreen(
                                engine = engine,
                                onNavigateBack = { currentScreen = Screen.ASSISTANT },
                                onNavigateToDiagnostics = { currentScreen = Screen.DIAGNOSTICS }
                            )
                        }

                        Screen.PERMISSIONS -> {
                            BackHandler { currentScreen = Screen.ASSISTANT }
                            PermissionsScreen(
                                onNavigateBack = { currentScreen = Screen.ASSISTANT }
                            )
                        }

                        Screen.ROUTINES -> {
                            BackHandler { currentScreen = Screen.ASSISTANT }
                            RoutinesScreen(
                                engine = engine,
                                onNavigateBack = { currentScreen = Screen.ASSISTANT }
                            )
                        }

                        Screen.DIAGNOSTICS -> {
                            BackHandler { currentScreen = Screen.SETTINGS }
                            DiagnosticsScreen(
                                engine = engine,
                                onNavigateBack = { currentScreen = Screen.SETTINGS }
                            )
                        }
                    }
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("TRIGGER_VOICE", false) ||
            intent.action == Intent.ACTION_ASSIST
        ) {
            engine.startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine.ttsEngine.shutdown()
        engine.speechAdapter.destroy()
    }
}
