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
    ): WooAiSmokeCredentialParseResult =
        if (runLiveEnabled) {
            parseLiveEnvironment(environment, defaultOutputDirectory)
        } else {
            WooAiSmokeCredentialParseResult.Skipped(
                WooAiSmokeLiveRunGate.DISABLED_REASON
            )
        }

    private fun parseLiveEnvironment(
        environment: Map<String, String>,
        defaultOutputDirectory: File,
    ): WooAiSmokeCredentialParseResult {
        val missingKeys = requiredKeys.filter { environment[it].isNullOrBlank() }
        if (missingKeys.isNotEmpty()) {
            return WooAiSmokeCredentialParseResult.Invalid(
                "Missing required Woo AI smoke environment variables: ${missingKeys.joinToString()}"
            )
        }

        return runCatching {
            environment.toCredentialConfig(defaultOutputDirectory)
        }.fold(
            onSuccess = { WooAiSmokeCredentialParseResult.Valid(it) },
            onFailure = { error ->
                WooAiSmokeCredentialParseResult.Invalid(error.message ?: "Invalid Woo AI smoke credentials.")
            }
        )
    }

    private fun Map<String, String>.toCredentialConfig(outputDirectory: File): WooAiSmokeCredentialConfig {
        val siteUrl = getValue("WOO_SITE_URL").trim().trimEnd('/')
        val siteId = parseSiteId()
        val scenarioIds = parseScenarioIds(this["WOO_AI_SMOKE_SCENARIO_ID"]).getOrThrow()

        require(siteUrl.isHttpsUrl()) {
            "WOO_SITE_URL must be an HTTPS URL."
        }

        return WooAiSmokeCredentialConfig(
            siteUrl = siteUrl,
            siteId = siteId,
            username = getValue("WOO_USERNAME"),
            appPassword = getValue("WOO_APP_PASSWORD"),
            storeLabel = this["WOO_AI_SMOKE_STORE_LABEL"]?.ifBlank { null } ?: "redacted-store",
            outputDirectory = outputDirectory,
            credentialSource = "environment",
            sampleCount = parseSampleCount(this["WOO_AI_SMOKE_SAMPLES"]).getOrThrow(),
            scenarioIds = scenarioIds,
        )
    }

    private fun Map<String, String>.parseSiteId(): Long {
        val siteId = getValue("WOO_SITE_ID").toLongOrNull()
        require(siteId != null && siteId > 0L) {
            "WOO_SITE_ID must be a positive numeric remote site id."
        }
        return siteId
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
