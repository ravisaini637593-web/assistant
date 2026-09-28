package com.example.ai.offline

import android.content.Context
import android.os.Build

class OnDeviceModelAdapter(private val context: Context) {

    data class AICoreStatus(
        val isSupportedOnHardware: Boolean,
        val isServicePresent: Boolean,
        val modelStatus: String
    )

    fun checkStatus(): AICoreStatus {
        val isHardwareEligible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        // Check for AICore system package or service
        val pm = context.packageManager
        val aicorePackagePresent = try {
            pm.getPackageInfo("com.google.android.aicore", 0) != null
        } catch (_: Exception) {
            false
        }

        val statusText = when {
            aicorePackagePresent -> "AICore present on device (Gemini Nano ready)"
            isHardwareEligible -> "Hardware supported; AICore package not installed by OEM"
            else -> "Device requires Android 14+ and NPU support for on-device Gemini Nano"
        }

        return AICoreStatus(
            isSupportedOnHardware = isHardwareEligible,
            isServicePresent = aicorePackagePresent,
            modelStatus = statusText
        )
    }

    suspend fun generateLocalResponse(prompt: String): String? {
        val status = checkStatus()
        if (!status.isServicePresent) return null
        return null // Will fall back to local CommandRouter or Online Gemini
    }
}
