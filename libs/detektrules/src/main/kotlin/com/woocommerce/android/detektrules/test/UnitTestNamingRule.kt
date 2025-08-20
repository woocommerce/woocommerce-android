package com.woocommerce.android.detektrules.test

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

class UnitTestNamingRule(config: Config) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Unit test functions should follow the 'given X, when Y, then Z' naming convention using backticks.",
        Debt.FIVE_MINS
    )

    private val validTestNamePattern = Regex(
        "^(given .+, )?when .+, then .+$"
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (!isTestFunction(function) || isAndroidTest(function)) return

        val functionName = function.name ?: return

        if (!functionName.matches(validTestNamePattern)) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(function),
                    "Test function '$functionName' should follow the naming convention: " +
                        "'[given something,] when something happens, then something is expected'. " +
                        "The 'given' part is optional but if present must be lowercase. " +
                        "The 'when' and 'then' parts are mandatory and must be lowercase. " +
                        "Use backticks for the function name."
                )
            )
        }
    }

    private fun isTestFunction(function: KtNamedFunction): Boolean {
        return function.annotationEntries.any { annotation ->
            val annotationName = annotation.shortName?.asString()
            annotationName == "Test"
        }
    }

    private fun isAndroidTest(function: KtNamedFunction): Boolean {
        val filePath = function.containingKtFile.virtualFilePath
        return filePath.contains("androidTest")
    }
}
