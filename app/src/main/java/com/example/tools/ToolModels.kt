package com.example.tools

enum class ToolErrorCode {
    PERMISSION_DENIED,
    ROLE_NOT_HELD,
    SPECIAL_ACCESS_REQUIRED,
    NETWORK_UNAVAILABLE,
    MODEL_UNAVAILABLE,
    FEATURE_UNSUPPORTED,
    INVALID_ARGUMENTS,
    USER_CANCELLED,
    TIMEOUT,
    ACTION_FAILED,
    AMBIGUOUS_TARGET
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNSUPPORTED
}

data class ToolResult(
    val success: Boolean,
    val errorCode: ToolErrorCode? = null,
    val userMessage: String,
    val technicalMessage: String? = null,
    val data: Map<String, Any?> = emptyMap()
) {
    companion object {
        fun success(userMessage: String, data: Map<String, Any?> = emptyMap()): ToolResult =
            ToolResult(success = true, userMessage = userMessage, data = data)

        fun failure(
            errorCode: ToolErrorCode,
            userMessage: String,
            technicalMessage: String? = null,
            data: Map<String, Any?> = emptyMap()
        ): ToolResult = ToolResult(
            success = false,
            errorCode = errorCode,
            userMessage = userMessage,
            technicalMessage = technicalMessage,
            data = data
        )
    }
}

data class ToolDefinition(
    val name: String,
    val purpose: String,
    val riskLevel: RiskLevel,
    val requiresConfirmation: Boolean,
    val requiredPermissions: List<String> = emptyList(),
    val requiredSpecialAccess: String? = null,
    val parametersSchema: Map<String, String> = emptyMap(), // paramName to description
    val userExplanation: String
)
