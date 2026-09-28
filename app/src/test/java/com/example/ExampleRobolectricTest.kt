package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.offline.CommandRouter
import com.example.assistant.ActionPolicy
import com.example.data.AppDatabase
import com.example.data.Routine
import com.example.tools.ToolRegistry
import com.example.tools.ToolValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Assistant", appName)
    }

    @Test
    fun `command router parses flashlight command`() {
        val parsed = CommandRouter.match("turn on flashlight")
        assertNotNull(parsed)
        assertEquals("toggle_torch", parsed!!.toolName)
        assertEquals("true", parsed.arguments["enabled"])

        val parsedOff = CommandRouter.match("turn off the torch")
        assertNotNull(parsedOff)
        assertEquals("toggle_torch", parsedOff!!.toolName)
        assertEquals("false", parsedOff.arguments["enabled"])
    }

    @Test
    fun `command router parses timer command`() {
        val parsed = CommandRouter.match("set a timer for 5 minutes")
        assertNotNull(parsed)
        assertEquals("set_timer", parsed!!.toolName)
        assertEquals("300", parsed.arguments["duration_seconds"])
    }

    @Test
    fun `command router parses battery status command`() {
        val parsed = CommandRouter.match("check my battery status")
        assertNotNull(parsed)
        assertEquals("get_device_status", parsed!!.toolName)
    }

    @Test
    fun `tool validator checks required arguments`() {
        val invalidApp = ToolValidator.validate("open_app", emptyMap())
        assertFalse(invalidApp.isValid)

        val validApp = ToolValidator.validate("open_app", mapOf("app_name" to "YouTube"))
        assertTrue(validApp.isValid)

        val invalidTimer = ToolValidator.validate("set_timer", mapOf("duration_seconds" to "-5"))
        assertFalse(invalidTimer.isValid)

        val validTimer = ToolValidator.validate("set_timer", mapOf("duration_seconds" to "120"))
        assertTrue(validTimer.isValid)
    }

    @Test
    fun `action policy flags high risk tools for confirmation`() {
        assertTrue(ActionPolicy.requiresConfirmation("make_call", mapOf("contact" to "John")))
        assertTrue(ActionPolicy.requiresConfirmation("send_sms", mapOf("recipient" to "Mom", "message" to "Hi")))
        assertFalse(ActionPolicy.requiresConfirmation("set_timer", mapOf("duration_seconds" to "60")))
        assertFalse(ActionPolicy.requiresConfirmation("toggle_torch", mapOf("enabled" to "true")))
    }

    @Test
    fun `database routine insertion and query`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val id = db.routineDao().insert(
            Routine(
                title = "Test Routine",
                triggerPhrase = "test me",
                description = "Robolectric test",
                actionsJson = "turn on flashlight"
            )
        )
        assertTrue(id > 0)
        val routine = db.routineDao().getById(id)
        assertNotNull(routine)
        assertEquals("Test Routine", routine?.title)
    }
}
