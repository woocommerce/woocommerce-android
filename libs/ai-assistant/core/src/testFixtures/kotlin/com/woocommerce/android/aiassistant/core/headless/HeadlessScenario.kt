@file:Suppress("SpacingBetweenDeclarationsWithAnnotations")

package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.history.AssistantSessionHistory
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import kotlinx.serialization.Serializable

@Serializable
data class HeadlessBaseline(
    val scenarios: List<HeadlessScenarioSpec>,
)

@Serializable
data class HeadlessScenarioSpec(
    val id: String,
    val turns: List<HeadlessTurnSpec>,
    val category: String,
    val scope: ToolScope,
    val hardChecks: List<HeadlessHardCheck>,
)

data class HeadlessScenario(
    val id: String,
    val turns: List<HeadlessTurnSpec>,
    val systemPrompt: String,
    val initialSessionHistory: AssistantSessionHistory,
    val context: SessionContext,
)

@Serializable
data class HeadlessTurnSpec(
    val userMessage: String,
    val hardChecks: List<HeadlessHardCheck>,
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
    NO_FAILED_OUTCOME,
    NO_TURN_ERRORS,
    ASSISTANT_TEXT_NOT_BLANK,
}
