package com.woocommerce.android.aiassistant.headless

data class WooAiSmokeConfig(
    val baselineMode: WooAiSmokeBaselineMode,
    val scenarioResourceName: String,
    val baselineResourceName: String,
    val approvedBaselineFileName: String,
    val usePerRunDirectory: Boolean,
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
