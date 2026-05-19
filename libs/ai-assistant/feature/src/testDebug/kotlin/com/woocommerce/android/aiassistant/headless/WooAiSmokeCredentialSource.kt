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
        val outputDirectory = environment["WOO_AI_SMOKE_OUTPUT_DIR"]
            ?.ifBlank { null }
            ?.let(::File)
            ?: defaultOutputDirectory

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
                    )
                )
            }.getOrElse { error ->
                WooAiSmokeCredentialParseResult.Invalid(error.message ?: "Invalid Woo AI smoke credentials.")
            }
        }
    }

    private fun String.isHttpsUrl(): Boolean =
        runCatching { URI(this) }.getOrNull()?.scheme.equals("https", ignoreCase = true)
}

sealed interface WooAiSmokeCredentialParseResult {
    data class Skipped(val reason: String) : WooAiSmokeCredentialParseResult
    data class Invalid(val message: String) : WooAiSmokeCredentialParseResult
    data class Valid(val config: WooAiSmokeCredentialConfig) : WooAiSmokeCredentialParseResult
}
