package com.woocommerce.android.aiassistant.headless

data class WooAiSmokeConfig(
    val scenarioResourceName: String,
    val baseline: WooAiSmokeBaselineConfig?,
    val usePerRunDirectory: Boolean,
)

data class WooAiSmokeBaselineConfig(
    val mode: WooAiSmokeBaselineMode,
    val resourceName: String,
    val approvedFileName: String,
)

enum class WooAiSmokeBaselineMode {
    CHECK,
    APPROVE;

    companion object {
        fun from(value: String): WooAiSmokeBaselineMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: error("Unsupported WOO_AI_SMOKE_MODE: $value")
    }
}
