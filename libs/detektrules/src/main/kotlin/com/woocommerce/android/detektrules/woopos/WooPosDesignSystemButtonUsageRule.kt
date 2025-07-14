package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class WooPosDesignSystemButtonUsageRule(config: Config) : WooPosBaseDetektRule(config) {
    private val disallowedButtonNames = setOf("Button")

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Standard Compose buttons should not be used. Use the WooPos button variants instead.",
        Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression
        val calleeName = when (callee) {
            is KtNameReferenceExpression -> callee.getReferencedName()
            is KtDotQualifiedExpression -> (callee.selectorExpression as? KtNameReferenceExpression)
                ?.getReferencedName()

            else -> null
        }
        if (calleeName == null || !disallowedButtonNames.contains(calleeName)) return

        if (callee is KtDotQualifiedExpression) {
            val receiverText = callee.receiverExpression.text
            if (receiverText.startsWith("androidx.compose.material3")) {
                reportStandardButtonUsage(expression, calleeName)
                return
            }
        } else {
            val ktFile = expression.containingKtFile
            val isStandardButtonImported = ktFile.importDirectives.any { directive ->
                val importPath = directive.importPath?.pathStr
                importPath == "androidx.compose.material3.$calleeName" || importPath == "androidx.compose.material3.*"
            }
            if (isStandardButtonImported) {
                reportStandardButtonUsage(expression, calleeName)
            }
        }
    }

    private fun reportStandardButtonUsage(expression: KtCallExpression, buttonType: String) {
        report(
            CodeSmell(
                issue,
                Entity.from(expression),
                "Do not use the standard Compose $buttonType. Use the appropriate WooPos button variant instead."
            )
        )
    }
}
