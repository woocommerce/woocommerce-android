package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

class WooAiSmokeLiveEnvRuleTest {
    @Test
    fun `given no live opt in, when rule evaluates, then inner statement is not evaluated`() {
        var evaluated = false
        val rule = WooAiSmokeLiveEnvRule(emptyMap(), File("build/outputs/woo-ai-smoke/live/latest"))

        assertThatThrownBy {
            rule.apply(
                object : Statement() {
                    override fun evaluate() {
                        evaluated = true
                    }
                },
                Description.EMPTY,
            ).evaluate()
        }.isInstanceOf(AssumptionViolatedException::class.java)

        assertThat(evaluated).isFalse
    }

    @Test
    fun `given live env missing credentials, when rule evaluates, then only missing names are reported`() {
        val rule = WooAiSmokeLiveEnvRule(
            mapOf("WOO_AI_SMOKE_RUN_LIVE" to "true"),
            File("build/outputs/woo-ai-smoke/live/latest"),
        )

        assertThatThrownBy {
            rule.apply(noOpStatement(), Description.EMPTY).evaluate()
        }.hasMessageContaining("WOO_SITE_URL")
            .hasMessageContaining("WOO_SITE_ID")
            .hasMessageContaining("WOO_USERNAME")
            .hasMessageContaining("WOO_APP_PASSWORD")
    }

    @Test
    fun `given invalid site id, when rule evaluates, then validation fails`() {
        val rule = WooAiSmokeLiveEnvRule(
            validEnvironment() + ("WOO_SITE_ID" to "-1"),
            File("build/outputs/woo-ai-smoke/live/latest"),
        )

        assertThatThrownBy {
            rule.apply(noOpStatement(), Description.EMPTY).evaluate()
        }.hasMessageContaining("WOO_SITE_ID must be a positive numeric remote site id")
    }

    @Test
    fun `given valid live env, when rule evaluates, then credentials are available`() {
        val rule = WooAiSmokeLiveEnvRule(
            validEnvironment(),
            File("build/outputs/woo-ai-smoke/live/latest"),
        )

        rule.apply(noOpStatement(), Description.EMPTY).evaluate()

        assertThat(rule.requireValidCredentials().siteId).isEqualTo(2922L)
    }

    private fun noOpStatement() = object : Statement() {
        override fun evaluate() = Unit
    }

    private fun validEnvironment() = mapOf(
        "WOO_AI_SMOKE_RUN_LIVE" to "true",
        "WOO_SITE_URL" to "https://store.example",
        "WOO_SITE_ID" to "2922",
        "WOO_USERNAME" to "merchant@example.com",
        "WOO_APP_PASSWORD" to "app password",
    )
}
