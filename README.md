# Aura Assistant - Siri-Style Android Personal Assistant

Aura is a production-grade, native Android voice and phone-task assistant engineered for smartphones. It combines a local-first offline execution engine for core phone tasks with Google Gemini API for online conversational reasoning, current knowledge, and function calling.

## Architecture

```
User Voice / Text Input
       │
       ▼
┌───────────────────────────────┐
│     AssistantEngine           │
│  (State: Idle/Listen/Speak)   │
└──────────────┬────────────────┘
               │
       ┌───────┴────────────────┐
       ▼                        ▼
┌─────────────────────┐  ┌─────────────────────┐
│  Offline Route      │  │  Online Gemini Route│
│  CommandRouter      │  │  GeminiApiClient    │
│  (Zero network)     │  │  (Function Calling) │
└──────────┬──────────┘  └──────────┬──────────┘
           │                        │
           └───────────┬────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│           ActionPolicy & Gate                │
│  - ToolValidator                             │
│  - PermissionCoordinator                     │
│  - ConfirmationGate (High-Risk calls/SMS)    │
└──────────────────────┬───────────────────────┘
                       ▼
┌──────────────────────────────────────────────┐
│               ToolExecutor                   │
│  - App Launch & Settings Intents             │
│  - CameraManager (Torch)                     │
│  - AlarmManager & Room DB (Timers/Reminders) │
│  - AudioManager (Media & Volume)             │
│  - BatteryManager & ConnectivityManager      │
│  - Telephony & Contacts Provider             │
└──────────────────────────────────────────────┘
```

## Feature Matrix

| Feature | Execution Mode | Permissions / Requirements | Risk Level |
|---|---|---|---|
| **Voice Activation (Push-to-Talk)** | BOTH | `RECORD_AUDIO` (On-Device STT where available) | LOW |
| **Speech Output (TTS)** | OFFLINE | Local Android TextToSpeech engine | LOW |
| **App Launching** | OFFLINE | Target app launchable via `<queries>` / Intent | LOW |
| **Settings Navigation** | OFFLINE | Standard public Settings intents | LOW |
| **Flashlight / Torch Control** | OFFLINE | `CAMERA` (CameraManager setTorchMode) | LOW |
| **Countdown Timers** | OFFLINE | `SCHEDULE_EXACT_ALARM`, AlarmManager + Room DB | LOW |
| **Reminders & Tasks** | OFFLINE | Room DB + Scheduled Notifications | LOW |
| **Media Playback Control** | OFFLINE | `AudioManager.dispatchMediaKeyEvent` | LOW |
| **Volume Adjustment** | OFFLINE | `AudioManager.setStreamVolume` | LOW |
| **Device Diagnostics (Battery/Net)** | OFFLINE | BatteryManager, ConnectivityManager | LOW |
| **Contact Search** | OFFLINE | `READ_CONTACTS` | LOW |
| **Direct Calling** | OFFLINE | `CALL_PHONE` (or fallback to `ACTION_DIAL`) | HIGH (Requires Confirmation) |
| **SMS Composition / Send** | OFFLINE | Standard user-visible SMS intent | HIGH (Requires Confirmation) |
| **Calendar Scheduling** | OFFLINE | `READ_CALENDAR`, `WRITE_CALENDAR` | MEDIUM |
| **Notification Summarization** | OFFLINE | `BIND_NOTIFICATION_LISTENER_SERVICE` | MEDIUM |
| **Automated Multi-Step Routines** | OFFLINE | Room DB Routine records | LOW |
| **Conversational Intelligence** | ONLINE | Gemini API (`gemini-2.5-flash` / `gemini-3.5-flash`) | LOW |
| **Tool Calling / Reasoning** | ONLINE | Gemini function calling | Handled by Gate |
| **Web Search Queries** | ONLINE | Google Web Search intent / Gemini | LOW |
| **Accessibility Shortcuts** | ROLE/SPECIAL | `BIND_ACCESSIBILITY_SERVICE` (Deterministic) | OPTIONAL |
| **Default Assistant Role** | ROLE | `RoleManager.ROLE_ASSISTANT` | OPTIONAL |

## Permissions & Least-Privilege Design

- **Contextual Requesting**: Runtime permissions are requested at the moment of use, never upfront in bulk.
- **Confirmation Gate**: High-risk operations (e.g. phone calls, SMS) trigger a user-visible confirmation card before execution.
- **Privacy First**: Sensitive data (contacts, SMS, notifications) are not transmitted to the cloud. Local commands are intercepted by the deterministic `CommandRouter` and executed immediately with zero latency and zero data egress.
- **Audit Log**: Every executed tool is recorded locally in Room DB (`tool_executions`) with timestamp, arguments, execution duration, and success status.

## Google AI Studio Configuration

1. In AI Studio, configure the `GEMINI_API_KEY` under the Secrets panel.
2. The key is automatically injected via the Secrets Gradle Plugin into `BuildConfig.GEMINI_API_KEY`.
3. Users can also enter a custom key directly in Assistant Settings.
