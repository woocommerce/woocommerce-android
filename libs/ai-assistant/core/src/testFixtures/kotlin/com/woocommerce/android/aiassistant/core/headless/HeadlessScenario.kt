package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.loop.SessionContext
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
    val hardChecks: List<HeadlessHardCheck> = emptyList(),
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
    val hardChecks: List<HeadlessHardCheck> = emptyList(),
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
    TOOL_CALLED,
    TOOL_NOT_CALLED,
}
