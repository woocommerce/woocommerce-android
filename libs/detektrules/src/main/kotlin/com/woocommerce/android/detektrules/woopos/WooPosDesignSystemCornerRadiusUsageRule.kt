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

class WooPosDesignSystemCornerRadiusUsageRule(config: Config) : Rule(config) {
    private val targetPackagePrefix = "com.woocommerce.android.ui.woopos"
    private val cornerRadiusFile = "WooPosCornerRadius"

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Use corner radius values from $cornerRadiusFile instead of hardcoded values.",
        Debt.FIVE_MINS
    )

    override fun visitKtFile(file: KtFile) {
        if (file.packageFqName.asString().startsWith(targetPackagePrefix)) {
            super.visitKtFile(file)
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val calleeExpression = expression.calleeExpression as? KtNameReferenceExpression
        val callName = calleeExpression?.getReferencedName()

        if (callName == "RoundedCornerShape") {
            expression.valueArguments.forEach { argument ->
                val argumentExpression = argument.getArgumentExpression()
                val argumentText = argumentExpression?.text ?: return

                if (!argumentText.startsWith(cornerRadiusFile) && argumentText.matches(Regex("\\d+\\.dp"))) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expression),
                            "Corner radius should use WooPosCornerRadius instead of hardcoded values." +
                                "Found: $argumentText"
                        )
                    )
                }
            }
        }
    }
}
