package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeadlessBaseline(
    val version: Int,
    val scenarios: List<HeadlessScenarioSpec>,
)

@Serializable
data class HeadlessScenarioSpec(
    val id: String,
    val turns: List<HeadlessTurnSpec>,
    val category: HeadlessScenarioCategory,
    val scope: ToolScope,
    val hardChecks: List<HeadlessHardCheck>,
    val smokeFixture: HeadlessSmokeFixture?,
)

data class HeadlessScenario(
    val id: String,
    val turns: List<HeadlessTurnSpec>,
    val initialHistory: List<AssistantMessage>,
    val context: SessionContext,
)

@Serializable
data class HeadlessTurnSpec(
    val userMessage: String,
    val hardChecks: List<HeadlessHardCheck>,
)

@Serializable
enum class HeadlessScenarioCategory {
    LEGACY_SCRIPTED,
    ORDERS_READ,
    PRODUCTS_READ,
    ANALYTICS_READ,
    WRITE_CONFIRMATION,
    OFF_DOMAIN_REFUSAL,
    @SerialName("read")
    READ,
    @SerialName("analytics")
    ANALYTICS,
    @SerialName("write")
    WRITE,
    @SerialName("search")
    SEARCH,
    @SerialName("limits")
    LIMITS,
    @SerialName("edge")
    EDGE,
    @SerialName("robustness")
    ROBUSTNESS,
    @SerialName("memory")
    MEMORY,
    @SerialName("safety")
    SAFETY,
}

@Serializable
data class HeadlessSmokeFixture(
    val ownerTag: String,
    val allowApproval: Boolean,
)

@Serializable
data class HeadlessHardCheck(
    val type: HeadlessHardCheckType,
    val value: String,
)

@Serializable
enum class HeadlessHardCheckType {
    OUTCOME_EQUALS,
    ASSISTANT_TEXT_CONTAINS,
    ASSISTANT_TEXT_NOT_CONTAINS,
    ASSISTANT_REFUSAL,
    TOOL_CALLED,
    TOOL_NOT_CALLED,
    TOOL_CALL_COUNT_AT_MOST,
    TOOL_RESULT_KIND_EQUALS,
    CONFIRMATION_DECISION_EQUALS,
    TOOL_ARGUMENT_JSON_CONTAINS,
    TOOL_CALLED_ANY,
    TOTAL_TOOL_CALL_COUNT_AT_MOST,
    ASSISTANT_TEXT_CONTAINS_ANY,
    TOOL_ARGUMENT_NOT_CONTAINS,
}
