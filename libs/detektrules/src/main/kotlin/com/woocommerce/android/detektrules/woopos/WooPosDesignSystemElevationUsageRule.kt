package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class WooPosDesignSystemElevationUsageRule(config: Config) : WooPosBaseDetektRule(config) {
    private val dpRegex = Regex("\\b\\d+(?:\\.\\d+)?\\.dp\\b")

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Use WooPosElevation for elevation values instead of hardcoded dp.",
        Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callName = (expression.calleeExpression as? KtNameReferenceExpression)
            ?.getReferencedName() ?: return
        if (!callName.isElevationCall()) return

        expression.valueArguments.forEach { argument ->
            val argText = argument.getArgumentExpression()?.text ?: return@forEach
            if (argText.startsWith("WooPosElevation")) return@forEach
            dpRegex.findAll(argText).forEach { match ->
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        "Use WooPosElevation instead of a raw dp value. " +
                            "Found: ${match.value} in $callName(...)"
                    )
                )
            }
        }
    }

    private fun String.isElevationCall(): Boolean {
        if (this == SHADOW_CALLEE) return true
        return endsWith("Elevation") || endsWith("elevation")
    }

    private companion object {
        const val SHADOW_CALLEE = "shadow"
    }
}
