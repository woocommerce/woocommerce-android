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
    val smokeStoreLabel: String? = null,
)

@Serializable
data class HeadlessApprovedScenarioBaseline(
    val scenarioId: String,
    val category: HeadlessScenarioCategory,
    val approvedHardChecks: List<HeadlessApprovedHardCheck>,
    val knownFailure: HeadlessKnownFailure? = null,
)

@Serializable
data class HeadlessApprovedHardCheck(
    val type: HeadlessHardCheckType,
    val value: String,
)

@Serializable
data class HeadlessKnownFailure(
    val reason: String,
    val issue: String? = null,
    val expectedFailedHardChecks: List<HeadlessApprovedHardCheck>,
)
