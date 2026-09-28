package com.example.ai.online

import android.content.Context
import com.example.BuildConfig
import com.example.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResponse {
    data class TextResponse(val text: String) : GeminiResponse()
    data class FunctionCallResponse(
        val functionName: String,
        val arguments: Map<String, String>
    ) : GeminiResponse()
    data class ErrorResponse(val message: String) : GeminiResponse()
}

class GeminiApiClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    var customApiKey: String? = null
    var selectedModel: String = "gemini-2.5-flash"

    private fun getApiKey(): String {
        return if (!customApiKey.isNullOrBlank()) {
            customApiKey!!
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) {
                ""
            }
        }
    }

    suspend fun query(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResponse.ErrorResponse(
                "Gemini API key is not configured. Please add your key in Assistant Settings or AI Studio Secrets."
            )
        }

        try {
            val root = JSONObject()

            // 1. System instruction
            val systemInstruction = JSONObject()
            val systemParts = JSONArray()
            val sysText = """
                You are Aura, an intelligent Android mobile personal assistant.
                You can execute tasks on the user's phone via function calling.
                Available tools: open_app, open_settings, toggle_torch, set_timer, cancel_timer,
                create_reminder, media_control, set_volume, get_device_status, find_contact,
                make_call, send_sms, create_calendar_event, list_calendar_events, read_notifications,
                summarize_text, search_web.
                If the user wants to perform an action on their phone, output a function call.
                Never invent tool results. Keep spoken text responses warm, natural, and concise (1-2 sentences).
            """.trimIndent()
            systemParts.put(JSONObject().put("text", sysText))
            systemInstruction.put("parts", systemParts)
            root.put("systemInstruction", systemInstruction)

            // 2. Contents / Conversation history
            val contents = JSONArray()
            for ((role, text) in conversationHistory.takeLast(6)) {
                val contentObj = JSONObject()
                contentObj.put("role", if (role == "assistant") "model" else "user")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", text))
                contentObj.put("parts", parts)
                contents.put(contentObj)
            }
            // Add current prompt
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", userPrompt))
            currentTurn.put("parts", currentParts)
            contents.put(currentTurn)
            root.put("contents", contents)

            // 3. Tools definitions
            val toolsArray = JSONArray()
            val functionDeclarations = JSONArray()

            for ((name, def) in ToolRegistry.tools) {
                val funcObj = JSONObject()
                funcObj.put("name", name)
                funcObj.put("description", def.purpose)

                val paramsObj = JSONObject()
                paramsObj.put("type", "OBJECT")
                val propertiesObj = JSONObject()
                val requiredArray = JSONArray()

                for ((paramName, desc) in def.parametersSchema) {
                    val prop = JSONObject()
                    prop.put("type", "STRING")
                    prop.put("description", desc)
                    propertiesObj.put(paramName, prop)
                }
                paramsObj.put("properties", propertiesObj)
                funcObj.put("parameters", paramsObj)
                functionDeclarations.put(funcObj)
            }

            val toolContainer = JSONObject()
            toolContainer.put("functionDeclarations", functionDeclarations)
            toolsArray.put(toolContainer)
            root.put("tools", toolsArray)

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$selectedModel:generateContent?key=$apiKey"
            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMessage = try {
                    JSONObject(responseBody).getJSONObject("error").getString("message")
                } catch (_: Exception) {
                    "API Error code: ${response.code}"
                }
                return@withContext GeminiResponse.ErrorResponse("Gemini API error: $errMessage")
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts != null && parts.length() > 0) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue

                    // Check for functionCall
                    if (part.has("functionCall")) {
                        val fn = part.getJSONObject("functionCall")
                        val fnName = fn.getString("name")
                        val argsObj = fn.optJSONObject("args")
                        val argsMap = mutableMapOf<String, String>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                argsMap[key] = argsObj.optString(key, "")
                            }
                        }
                        return@withContext GeminiResponse.FunctionCallResponse(fnName, argsMap)
                    }

                    // Check for text
                    if (part.has("text")) {
                        val text = part.getString("text")
                        return@withContext GeminiResponse.TextResponse(text)
                    }
                }
            }

            GeminiResponse.ErrorResponse("Received empty response from Gemini.")
        } catch (e: Exception) {
            GeminiResponse.ErrorResponse("Connection failed: ${e.localizedMessage}")
        }
    }
}
