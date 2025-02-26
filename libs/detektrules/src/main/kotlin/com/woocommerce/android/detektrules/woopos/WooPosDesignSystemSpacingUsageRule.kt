package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class WooPosDesignSystemSpacingUsageRule(config: Config) : Rule(config) {
    private val targetPackagePrefix = "com.woocommerce.android.ui.woopos"
    private val dpRegex = Regex("\\d+\\.dp")
    private val dpWordRegex = Regex("\\b\\d+\\.dp\\b")

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Use spacing from the WooPosSpacing design system.",
        Debt.FIVE_MINS
    )

    override fun visitKtFile(file: KtFile) {
        if (!file.packageFqName.asString().startsWith(targetPackagePrefix)) return
        super.visitKtFile(file)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callName = (expression.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return

        when (callName) {
            "padding" -> checkPaddingArguments(expression)
            "Spacer" -> checkSpacerArguments(expression)
        }
    }

    private fun checkPaddingArguments(expression: KtCallExpression) {
        expression.valueArguments.forEach { argument ->
            val argText = argument.getArgumentExpression()?.text ?: return@forEach
            if (!argText.startsWith("WooPosSpacing") && argText.matches(dpRegex)) {
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        "Use WooPosSpacing for padding/margins instead of hardcoded values. Found: $argText"
                    )
                )
            }
        }
    }

    private fun checkSpacerArguments(expression: KtCallExpression) {
        expression.valueArguments.forEach { argument ->
            val argText = argument.getArgumentExpression()?.text ?: return@forEach
            dpWordRegex.findAll(argText).forEach { match ->
                if (!argText.contains("WooPosSpacing")) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expression),
                            "Use WooPosSpacing for Spacer dimensions instead of hardcoded values. Found: ${match.value}"
                        )
                    )
                }
            }
        }
    }
}
