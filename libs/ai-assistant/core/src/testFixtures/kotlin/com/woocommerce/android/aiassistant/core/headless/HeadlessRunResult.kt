package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class HeadlessRunResult(
    val scenarioId: String,
    val turns: List<HeadlessTurnResult>,
)

@Serializable
data class HeadlessSuiteRunResult(
    val metadata: HeadlessRunMetadata,
    val scenarios: List<HeadlessScenarioRunResult>,
)

@Serializable
data class HeadlessRunMetadata(
    val modelId: String,
    val promptVersion: String,
    val toolCatalogVersion: String,
    val startedAtIso8601: String,
    val chatServiceClass: String,
    val jwtProviderClass: String,
    val toolRegistryClass: String,
    val safetyPolicy: String,
    val smokeStoreLabel: String,
    val credentialSource: String,
)

@Serializable
data class HeadlessScenarioRunResult(
    val scenarioId: String,
    val category: HeadlessScenarioCategory,
    val result: HeadlessRunResult,
    val hardCheckResults: List<HeadlessHardCheckResult>,
    val status: HeadlessScenarioStatus,
)

@Serializable
enum class HeadlessScenarioStatus {
    PASS,
    FAIL,
}

@Serializable
data class HeadlessTurnResult(
    val turnIndex: Int,
    val userMessage: String,
    val assistantText: String,
    val outcome: LoopOutcome,
    val toolCalls: List<HeadlessToolCallTrace>,
    val confirmationRequests: List<HeadlessConfirmationRequestTrace> = emptyList(),
    val confirmationResults: List<HeadlessConfirmationResultTrace> = emptyList(),
    val errors: List<String> = emptyList(),
)

@Serializable
data class HeadlessToolCallTrace(
    val id: String,
    val name: String,
    val arguments: JsonObject,
    val safetyLevel: ToolSafetyLevel,
    val resultKind: HeadlessToolResultKind,
)

@Serializable
data class HeadlessConfirmationRequestTrace(
    val id: String,
    val toolCallId: String,
    val toolName: String,
    val arguments: JsonObject,
    val safetyLevel: ToolSafetyLevel,
)

@Serializable
data class HeadlessConfirmationResultTrace(
    val requestId: String,
    val decision: String,
)

@Serializable
enum class HeadlessToolResultKind {
    SUCCESS,
    VALIDATION_ERROR,
    TRANSPORT_ERROR,
    REJECTED_BY_SAFETY,
}
