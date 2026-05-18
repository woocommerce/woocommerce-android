package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooAiSmokeConfigTest {
    @Test
    fun `given live no device config, when artifact layout is configured, then per run directories are enabled`() {
        val config = WooAiSmokeConfig(
            scenarioResourceName = "live-scenarios.json",
            baseline = WooAiSmokeBaselineConfig(
                mode = WooAiSmokeBaselineMode.CHECK,
                resourceName = "live-baseline.json",
                approvedFileName = "approved-live-baseline.json",
            ),
            usePerRunDirectory = true,
        )

        assertThat(config.usePerRunDirectory).isTrue()
    }

    @Test
    fun `given deterministic support config, when artifact layout is configured, then latest output stays stable`() {
        val config = WooAiSmokeConfig(
            scenarioResourceName = "deterministic-scenarios.json",
            baseline = null,
            usePerRunDirectory = false,
        )

        assertThat(config.usePerRunDirectory).isFalse()
    }
}
