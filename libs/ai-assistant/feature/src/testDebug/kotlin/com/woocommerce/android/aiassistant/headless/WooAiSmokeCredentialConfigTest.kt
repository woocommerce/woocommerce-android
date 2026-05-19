@file:Suppress("FunctionNaming", "MagicNumber")

package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class WooAiSmokeCredentialConfigTest {
    @Test
    fun `given no live env, when parsing credentials, then result is skipped`() {
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = emptyMap(),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Skipped::class.java)
    }

    @Test
    fun `given live env missing credentials, when parsing credentials, then only key names are reported`() {
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = mapOf("WOO_AI_SMOKE_RUN_LIVE" to "true"),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Invalid::class.java)
        val message = (result as WooAiSmokeCredentialParseResult.Invalid).message
        assertThat(message).contains("WOO_SITE_URL")
        assertThat(message).contains("WOO_SITE_ID")
        assertThat(message).contains("WOO_USERNAME")
        assertThat(message).contains("WOO_APP_PASSWORD")
        assertThat(message).doesNotContain("username")
        assertThat(message).doesNotContain("password123")
    }

    @Test
    fun `given invalid site id, when parsing credentials, then parsing fails before network access`() {
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment() + ("WOO_SITE_ID" to "not-a-number"),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Invalid::class.java)
        assertThat((result as WooAiSmokeCredentialParseResult.Invalid).message)
            .contains("WOO_SITE_ID must be a positive numeric remote site id")
    }

    @Test
    fun `given invalid smoke mode, when parsing credentials, then error points to WOO_AI_SMOKE_MODE`() {
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment() + ("WOO_AI_SMOKE_MODE" to "surprise"),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Invalid::class.java)
        assertThat((result as WooAiSmokeCredentialParseResult.Invalid).message)
            .contains("WOO_AI_SMOKE_MODE")
            .contains("surprise")
    }

    @Test
    fun `given valid env, when parsing credentials, then parser applies non-secret defaults`() {
        val outputDirectory = File("build/woo-ai-smoke/live/latest")
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment(),
            defaultOutputDirectory = outputDirectory,
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Valid::class.java)
        val config = (result as WooAiSmokeCredentialParseResult.Valid).config
        assertThat(config.siteUrl).isEqualTo("https://store.example")
        assertThat(config.siteId).isEqualTo(2922L)
        assertThat(config.username).isEqualTo("merchant@example.com")
        assertThat(config.appPassword).isEqualTo("app password")
        assertThat(config.mode).isEqualTo(WooAiSmokeBaselineMode.CHECK)
        assertThat(config.storeLabel).isEqualTo("redacted-store")
        assertThat(config.outputDirectory).isEqualTo(outputDirectory)
    }

    private fun validEnvironment() = mapOf(
        "WOO_AI_SMOKE_RUN_LIVE" to "true",
        "WOO_SITE_URL" to "https://store.example",
        "WOO_SITE_ID" to "2922",
        "WOO_USERNAME" to "merchant@example.com",
        "WOO_APP_PASSWORD" to "app password",
    )
}
