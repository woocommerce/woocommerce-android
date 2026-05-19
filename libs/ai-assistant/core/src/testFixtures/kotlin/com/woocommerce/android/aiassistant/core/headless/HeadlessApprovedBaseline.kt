package com.woocommerce.android.aiassistant.core.headless

import kotlinx.serialization.Serializable

@Serializable
data class HeadlessApprovedBaseline(
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
    val category: String,
    val approvedHardChecks: List<HeadlessHardCheck>,
    val knownFailure: HeadlessKnownFailure? = null,
)

@Serializable
data class HeadlessKnownFailure(
    val reason: String,
    val expectedFailedHardChecks: List<HeadlessHardCheck>,
)
