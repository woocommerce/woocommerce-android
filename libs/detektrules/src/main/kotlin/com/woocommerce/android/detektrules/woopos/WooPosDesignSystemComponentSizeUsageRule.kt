package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class WooPosDesignSystemComponentSizeUsageRule(config: Config) : WooPosBaseDetektRule(config) {
    private val dpRegex = Regex("\\b\\d+(?:\\.\\d+)?\\.dp\\b")
    private val adaptiveHelperRegex = Regex("toAdaptive\\w*Size\\b")

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Use .toAdaptive*Size() or a WooPos design system size instead of raw dp values.",
        Debt.FIVE_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callName = (expression.calleeExpression as? KtNameReferenceExpression)
            ?.getReferencedName() ?: return
        if (callName !in SIZE_MODIFIER_NAMES) return

        expression.valueArguments.forEach { argument ->
            val argText = argument.getArgumentExpression()?.text ?: return@forEach
            if (adaptiveHelperRegex.containsMatchIn(argText)) return@forEach
            if (argText.startsWith("WooPos")) return@forEach
            dpRegex.findAll(argText).forEach { match ->
                report(
                    CodeSmell(
                        issue,
                        Entity.from(expression),
                        "Use .toAdaptive*Size() or a WooPos design system size " +
                            "instead of a raw dp value. Found: ${match.value} in Modifier.$callName(...)"
                    )
                )
            }
        }
    }

    private companion object {
        val SIZE_MODIFIER_NAMES = setOf(
            "size",
            "height",
            "width",
            "sizeIn",
            "heightIn",
            "widthIn",
            "defaultMinSize",
            "requiredSize",
            "requiredHeight",
            "requiredWidth",
            "requiredSizeIn",
            "requiredHeightIn",
            "requiredWidthIn",
        )
    }
}
