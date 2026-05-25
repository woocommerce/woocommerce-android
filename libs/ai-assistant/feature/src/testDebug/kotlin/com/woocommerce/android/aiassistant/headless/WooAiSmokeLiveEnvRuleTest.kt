@file:Suppress("FunctionNaming")

package com.woocommerce.android.aiassistant.headless

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

class WooAiSmokeLiveEnvRuleTest {
    @Test
    fun `given no Gradle live opt in, when rule evaluates, then inner statement is not evaluated`() {
        var evaluated = false
        val rule = WooAiSmokeLiveEnvRule(
            environment = validEnvironment(),
            defaultOutputDirectory = File("build/outputs/woo-ai-smoke/live/latest"),
            runLiveEnabled = false,
        )

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
            environment = emptyMap(),
            defaultOutputDirectory = File("build/outputs/woo-ai-smoke/live/latest"),
            runLiveEnabled = true,
        )

        val error = catchThrowable {
            rule.apply(noOpStatement(), Description.EMPTY).evaluate()
        }
        assertThat(error).hasMessageContaining("WOO_SITE_URL")
            .hasMessageContaining("WOO_WPCOM_USERNAME")
            .hasMessageContaining("WOO_WPCOM_PASSWORD")
        assertThat(error.message).doesNotContain("WOO_SITE_ID")
    }

    @Test
    fun `given obsolete site id, when rule evaluates, then credentials are still valid`() {
        val rule = WooAiSmokeLiveEnvRule(
            environment = validEnvironment() + ("WOO_SITE_ID" to "-1"),
            defaultOutputDirectory = File("build/outputs/woo-ai-smoke/live/latest"),
            runLiveEnabled = true,
        )

        rule.apply(noOpStatement(), Description.EMPTY).evaluate()

        assertThat(rule.requireValidCredentials().wpComUsername).isEqualTo("merchant@example.com")
    }

    @Test
    fun `given valid live env, when rule evaluates, then credentials are available`() {
        val rule = WooAiSmokeLiveEnvRule(
            environment = validEnvironment(),
            defaultOutputDirectory = File("build/outputs/woo-ai-smoke/live/latest"),
            runLiveEnabled = true,
        )

        rule.apply(noOpStatement(), Description.EMPTY).evaluate()

        assertThat(rule.requireValidCredentials().wpComPassword).isEqualTo("app password")
    }

    private fun noOpStatement() = object : Statement() {
        override fun evaluate() = Unit
    }

    private fun validEnvironment() = mapOf(
        "WOO_SITE_URL" to "https://store.example",
        "WOO_WPCOM_USERNAME" to "merchant@example.com",
        "WOO_WPCOM_PASSWORD" to "app password",
    )
}
