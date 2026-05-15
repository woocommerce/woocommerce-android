package com.woocommerce.android.aiassistant.core.headless

import kotlinx.serialization.Serializable

@Serializable
data class HeadlessApprovedBaseline(
    val version: Int,
    val metadata: HeadlessBaselineMetadata,
    val scenarios: List<HeadlessApprovedScenarioBaseline>,
)

@Serializable
data class HeadlessBaselineMetadata(
    val modelId: String,
    val promptVersion: String,
    val toolCatalogVersion: String,
)

@Serializable
data class HeadlessApprovedScenarioBaseline(
    val scenarioId: String,
    val category: HeadlessScenarioCategory,
    val approvedStatus: HeadlessScenarioStatus,
    val approvedHardChecks: List<HeadlessApprovedHardCheck>,
)

@Serializable
data class HeadlessApprovedHardCheck(
    val type: HeadlessHardCheckType,
    val value: String,
)
