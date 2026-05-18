package com.woocommerce.android.aiassistant.headless

import java.io.File
import java.net.URI

object WooAiSmokeCredentialSource {
    private val requiredKeys = listOf(
        "WOO_SITE_URL",
        "WOO_SITE_ID",
        "WOO_USERNAME",
        "WOO_APP_PASSWORD",
    )

    fun fromEnvironment(
        environment: Map<String, String>,
        defaultOutputDirectory: File,
        runLiveEnabled: Boolean,
    ): WooAiSmokeCredentialParseResult {
        if (!runLiveEnabled) {
            return WooAiSmokeCredentialParseResult.Skipped(
                WooAiSmokeLiveRunGate.DISABLED_REASON
            )
        }

        val missingKeys = requiredKeys.filter { environment[it].isNullOrBlank() }
        if (missingKeys.isNotEmpty()) {
            return WooAiSmokeCredentialParseResult.Invalid(
                "Missing required Woo AI smoke environment variables: ${missingKeys.joinToString()}"
            )
        }

        val siteUrl = environment.getValue("WOO_SITE_URL").trim().trimEnd('/')
        val siteId = environment.getValue("WOO_SITE_ID").toLongOrNull()
        val sampleCount = parseSampleCount(environment["WOO_AI_SMOKE_SAMPLES"]).getOrElse { error ->
            return WooAiSmokeCredentialParseResult.Invalid(error.message ?: "Invalid WOO_AI_SMOKE_SAMPLES.")
        }
        val scenarioIds = parseScenarioIds(environment["WOO_AI_SMOKE_SCENARIO_ID"]).getOrElse { error ->
            return WooAiSmokeCredentialParseResult.Invalid(error.message ?: "Invalid WOO_AI_SMOKE_SCENARIO_ID.")
        }
        val outputDirectory = defaultOutputDirectory

        return when {
            siteId == null || siteId <= 0L -> WooAiSmokeCredentialParseResult.Invalid(
                "WOO_SITE_ID must be a positive numeric remote site id."
            )
            !siteUrl.isHttpsUrl() -> WooAiSmokeCredentialParseResult.Invalid(
                "WOO_SITE_URL must be an HTTPS URL."
            )
            else -> runCatching {
                WooAiSmokeCredentialParseResult.Valid(
                    WooAiSmokeCredentialConfig(
                        siteUrl = siteUrl,
                        siteId = siteId,
                        username = environment.getValue("WOO_USERNAME"),
                        appPassword = environment.getValue("WOO_APP_PASSWORD"),
                        storeLabel = environment["WOO_AI_SMOKE_STORE_LABEL"]?.ifBlank { null } ?: "redacted-store",
                        outputDirectory = outputDirectory,
                        credentialSource = "environment",
                        sampleCount = sampleCount,
                        scenarioIds = scenarioIds,
                    )
                )
            }.getOrElse { error ->
                WooAiSmokeCredentialParseResult.Invalid(error.message ?: "Invalid Woo AI smoke credentials.")
            }
        }
    }

    private fun String.isHttpsUrl(): Boolean =
        runCatching { URI(this) }.getOrNull()?.scheme.equals("https", ignoreCase = true)

    private fun parseSampleCount(rawValue: String?): Result<Int> = runCatching {
        val value = rawValue?.ifBlank { null } ?: return@runCatching 1
        val sampleCount = value.toIntOrNull()
            ?: error("WOO_AI_SMOKE_SAMPLES must be a number between 1 and $MAX_SAMPLE_COUNT.")
        require(sampleCount in 1..MAX_SAMPLE_COUNT) {
            "WOO_AI_SMOKE_SAMPLES must be a number between 1 and $MAX_SAMPLE_COUNT."
        }
        sampleCount
    }

    private fun parseScenarioIds(rawValue: String?): Result<Set<String>> = runCatching {
        val value = rawValue?.ifBlank { null } ?: return@runCatching emptySet<String>()
        val ids = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        require(ids.isNotEmpty()) {
            "WOO_AI_SMOKE_SCENARIO_ID must contain at least one scenario id."
        }
        ids.toCollection(LinkedHashSet())
    }

    private const val MAX_SAMPLE_COUNT = 3
}

sealed interface WooAiSmokeCredentialParseResult {
    data class Skipped(val reason: String) : WooAiSmokeCredentialParseResult
    data class Invalid(val message: String) : WooAiSmokeCredentialParseResult
    data class Valid(val config: WooAiSmokeCredentialConfig) : WooAiSmokeCredentialParseResult
}
