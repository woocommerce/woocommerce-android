@file:Suppress("FunctionNaming", "MagicNumber")

package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class WooAiSmokeCredentialConfigTest {
    @Test
    fun `given no Gradle live opt in, when parsing credentials, then result is skipped`() {
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment(),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
            runLiveEnabled = false,
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Skipped::class.java)
    }

    @Test
    fun `given live env missing credentials, when parsing credentials, then only key names are reported`() {
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = emptyMap(),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
            runLiveEnabled = true,
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
            runLiveEnabled = true,
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Invalid::class.java)
        assertThat((result as WooAiSmokeCredentialParseResult.Invalid).message)
            .contains("WOO_SITE_ID must be a positive numeric remote site id")
    }

    @Test
    fun `given valid env, when parsing credentials, then parser applies non-secret defaults`() {
        val outputDirectory = File("build/woo-ai-smoke/live/latest")
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment(),
            defaultOutputDirectory = outputDirectory,
            runLiveEnabled = true,
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Valid::class.java)
        val config = (result as WooAiSmokeCredentialParseResult.Valid).config
        assertThat(config.siteUrl).isEqualTo("https://store.example")
        assertThat(config.siteId).isEqualTo(2922L)
        assertThat(config.username).isEqualTo("merchant@example.com")
        assertThat(config.appPassword).isEqualTo("app password")
        assertThat(config.storeLabel).isEqualTo("redacted-store")
        assertThat(config.outputDirectory).isEqualTo(outputDirectory)
        assertThat(config.sampleCount).isEqualTo(1)
        assertThat(config.scenarioIds).isEmpty()
    }

    @Test
    fun `given output directory env, when parsing credentials, then default output directory is used`() {
        val outputDirectory = File("build/woo-ai-smoke/live/latest")
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment() + ("WOO_AI_SMOKE_OUTPUT_DIR" to "/tmp/not-used"),
            defaultOutputDirectory = outputDirectory,
            runLiveEnabled = true,
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Valid::class.java)
        assertThat((result as WooAiSmokeCredentialParseResult.Valid).config.outputDirectory).isEqualTo(outputDirectory)
    }

    @Test
    fun `given valid env with scenario filter, when parsing credentials, then filter is applied`() {
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment() +
                ("WOO_AI_SMOKE_SCENARIO_ID" to "recent_orders, orders_with_email"),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
            runLiveEnabled = true,
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Valid::class.java)
        val config = (result as WooAiSmokeCredentialParseResult.Valid).config
        assertThat(config.scenarioIds).containsExactly("recent_orders", "orders_with_email")
    }

    @Test
    fun `given valid env with sample count, when parsing credentials, then sample count is applied`() {
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment() + ("WOO_AI_SMOKE_SAMPLES" to "3"),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
            runLiveEnabled = true,
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Valid::class.java)
        assertThat((result as WooAiSmokeCredentialParseResult.Valid).config.sampleCount).isEqualTo(3)
    }

    @Test
    fun `given invalid sample counts, when parsing credentials, then parsing fails before network access`() {
        listOf("0", "-1", "4", "not-a-number").forEach { sampleCount ->
            val result = WooAiSmokeCredentialSource.fromEnvironment(
                environment = validEnvironment() + ("WOO_AI_SMOKE_SAMPLES" to sampleCount),
                defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
                runLiveEnabled = true,
            )

            assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Invalid::class.java)
            assertThat((result as WooAiSmokeCredentialParseResult.Invalid).message)
                .contains("WOO_AI_SMOKE_SAMPLES")
        }
    }

    @Test
    fun `given app password contains spaces, when parsing credentials, then password is preserved exactly`() {
        val password = " abcd efgh ijkl mnop "
        val result = WooAiSmokeCredentialSource.fromEnvironment(
            environment = validEnvironment() + ("WOO_APP_PASSWORD" to password),
            defaultOutputDirectory = File("build/woo-ai-smoke/live/latest"),
            runLiveEnabled = true,
        )

        assertThat(result).isInstanceOf(WooAiSmokeCredentialParseResult.Valid::class.java)
        assertThat((result as WooAiSmokeCredentialParseResult.Valid).config.appPassword).isEqualTo(password)
    }

    private fun validEnvironment() = mapOf(
        "WOO_SITE_URL" to "https://store.example",
        "WOO_SITE_ID" to "2922",
        "WOO_USERNAME" to "merchant@example.com",
        "WOO_APP_PASSWORD" to "app password",
    )
}
