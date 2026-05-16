package com.woocommerce.android.aiassistant.headless

import org.junit.Assume.assumeTrue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

class WooAiSmokeLiveEnvRule(
    private val environment: Map<String, String>,
    private val defaultOutputDirectory: File,
) : TestRule {
    private var credentials: WooAiSmokeCredentialConfig? = null

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement = object : Statement() {
        override fun evaluate() {
            when (val result = WooAiSmokeCredentialSource.fromEnvironment(environment, defaultOutputDirectory)) {
                is WooAiSmokeCredentialParseResult.Skipped -> assumeTrue(result.reason, false)
                is WooAiSmokeCredentialParseResult.Invalid -> error(result.message)
                is WooAiSmokeCredentialParseResult.Valid -> {
                    credentials = result.config
                    base.evaluate()
                }
            }
        }
    }

    fun requireValidCredentials(): WooAiSmokeCredentialConfig =
        requireNotNull(credentials) { "Woo AI smoke credentials were not validated." }
}
