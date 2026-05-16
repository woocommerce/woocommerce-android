package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooAiSmokeConfigTest {
    @Test
    fun `given live no device config, when artifact layout is configured, then per run directories are enabled`() {
        val config = WooAiSmokeConfig(
            baselineMode = WooAiSmokeBaselineMode.CHECK,
            scenarioResourceName = "live-scenarios.json",
            baselineResourceName = "live-baseline.json",
            approvedBaselineFileName = "approved-live-baseline.json",
            usePerRunDirectory = true,
        )

        assertThat(config.usePerRunDirectory).isTrue()
    }

    @Test
    fun `given deterministic support config, when artifact layout is configured, then latest output stays stable`() {
        val config = WooAiSmokeConfig(
            baselineMode = WooAiSmokeBaselineMode.CHECK,
            scenarioResourceName = "support-scenarios.json",
            baselineResourceName = "support-baseline.json",
            approvedBaselineFileName = "approved-baseline.json",
            usePerRunDirectory = false,
        )

        assertThat(config.usePerRunDirectory).isFalse()
    }
}
