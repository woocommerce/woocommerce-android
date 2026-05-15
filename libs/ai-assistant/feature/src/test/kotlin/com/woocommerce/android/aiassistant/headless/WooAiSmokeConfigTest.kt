package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class WooAiSmokeConfigTest {
    @Test
    fun `given normal instrumentation args, when config parses, then non smoke args are ignored`() {
        val config = WooAiSmokeConfig.fromInstrumentationArguments(
            mapOf(
                "class" to "com.woocommerce.android.aiassistant.headless.WooAiSmokeAndroidTest",
                "clearPackageData" to "false",
                "API_URL" to "https://example.com",
                "API_EMAIL" to "merchant@example.com",
                "API_PASSWORD" to "not-read-by-smoke-config",
                "wooAiSmoke" to "true",
            )
        )

        assertThat(config.enabled).isTrue
        assertThat(config.baselineMode).isEqualTo(WooAiSmokeBaselineMode.CHECK)
        assertThat(config.outputDirectoryName).isEqualTo("woo-ai-smoke")
    }

    @Test
    fun `given credential-like smoke argument, when config parses, then it is rejected`() {
        assertThatThrownBy {
            WooAiSmokeConfig.fromInstrumentationArguments(
                mapOf(
                    "wooAiSmoke" to "true",
                    "wooAiSmokeToken" to "abc",
                )
            )
        }.hasMessageContaining("Smoke config does not accept credential-like wooAiSmoke arguments")
    }

    @Test
    fun `given write mode argument, when config parses, then only decline is accepted`() {
        val config = WooAiSmokeConfig.fromInstrumentationArguments(
            mapOf(
                "wooAiSmoke" to "true",
                "wooAiSmokeWriteMode" to "decline",
            )
        )

        assertThat(config.writeMode).isEqualTo(WooAiSmokeWriteMode.DECLINE)
    }

    @Test
    fun `given unsafe write approval mode, when config parses, then it is rejected`() {
        assertThatThrownBy {
            WooAiSmokeConfig.fromInstrumentationArguments(
                mapOf(
                    "wooAiSmoke" to "true",
                    "wooAiSmokeWriteMode" to "approve-smoke-fixture",
                )
            )
        }.hasMessageContaining("Only decline write mode is supported")
    }
}
