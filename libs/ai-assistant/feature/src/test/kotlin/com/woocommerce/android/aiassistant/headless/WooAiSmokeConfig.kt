package com.woocommerce.android.aiassistant.headless

data class WooAiSmokeConfig(
    val scenarioResourceName: String,
    val baseline: WooAiSmokeBaselineConfig?,
    val usePerRunDirectory: Boolean,
    val sampleCount: Int = 1,
    val scenarioIds: Set<String> = emptySet(),
)

data class WooAiSmokeBaselineConfig(
    val mode: WooAiSmokeBaselineMode,
    val resourceName: String,
    val approvedFileName: String,
)

enum class WooAiSmokeBaselineMode {
    CHECK,
    APPROVE,
}
