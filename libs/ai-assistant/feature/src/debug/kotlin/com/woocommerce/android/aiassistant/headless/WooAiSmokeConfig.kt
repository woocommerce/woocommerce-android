package com.woocommerce.android.aiassistant.headless

data class WooAiSmokeConfig(
    val enabled: Boolean,
    val baselineMode: WooAiSmokeBaselineMode,
    val writeMode: WooAiSmokeWriteMode,
    val outputDirectoryName: String,
) {
    companion object {
        fun fromInstrumentationArguments(arguments: Map<String, String?>): WooAiSmokeConfig {
            val smokeArguments = arguments.filterKeys { it.startsWith("wooAiSmoke") }
            rejectCredentialLikeSmokeArguments(smokeArguments)
            return fromSmokeArguments(smokeArguments)
        }

        private fun rejectCredentialLikeSmokeArguments(arguments: Map<String, String?>) {
            val forbidden = listOf("token", "password", "credential", "secret")
            val rejected = arguments.keys.firstOrNull { key ->
                forbidden.any { key.contains(it, ignoreCase = true) }
            }
            require(rejected == null) {
                "Smoke config does not accept credential-like wooAiSmoke arguments: $rejected"
            }
        }

        private fun fromSmokeArguments(arguments: Map<String, String?>): WooAiSmokeConfig {
            val writeMode = arguments["wooAiSmokeWriteMode"] ?: "decline"
            require(writeMode == "decline") {
                "Only decline write mode is supported for WOOMOB-2922"
            }
            return WooAiSmokeConfig(
                enabled = arguments["wooAiSmoke"].toBoolean(),
                baselineMode = WooAiSmokeBaselineMode.from(arguments["wooAiSmokeBaselineMode"] ?: "check"),
                writeMode = WooAiSmokeWriteMode.DECLINE,
                outputDirectoryName = arguments["wooAiSmokeOutputDir"] ?: "woo-ai-smoke",
            )
        }
    }
}

enum class WooAiSmokeBaselineMode {
    CHECK,
    APPROVE;

    companion object {
        fun from(value: String): WooAiSmokeBaselineMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: error("Unsupported wooAiSmokeBaselineMode: $value")
    }
}

enum class WooAiSmokeWriteMode {
    DECLINE,
}
