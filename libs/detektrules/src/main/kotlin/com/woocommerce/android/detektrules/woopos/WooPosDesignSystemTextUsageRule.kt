package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class WooPosDesignSystemTextUsageRule(config: Config) : Rule(config) {
    private val allowedPackagePrefix = "com.woocommerce.android.ui.woopos.common.composeui.component"

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Standard Compose Text should not be used. Use WooPosText instead.",
        Debt.FIVE_MINS
    )

    override fun visitKtFile(file: KtFile) {
        if (file.packageFqName.asString().startsWith(allowedPackagePrefix)) {
            return
        }
        super.visitKtFile(file)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression
        val textName = when (callee) {
            is KtNameReferenceExpression -> callee.getReferencedName()
            is KtDotQualifiedExpression -> {
                (callee.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
            }

            else -> null
        }
        if (textName != "Text") return

        if (callee is KtDotQualifiedExpression) {
            val receiverText = callee.receiverExpression.text
            if (receiverText == "androidx.compose.material3") {
                reportStandardTextUsage(expression)
                return
            }
        } else {
            val ktFile = expression.containingKtFile
            val isStandardTextImported = ktFile.importDirectives.any { directive ->
                val importPath = directive.importPath?.pathStr
                importPath == "androidx.compose.material3.Text" || importPath == "androidx.compose.material3.*"
            }
            if (isStandardTextImported) {
                reportStandardTextUsage(expression)
            }
        }
    }

    private fun reportStandardTextUsage(expression: KtCallExpression) {
        report(
            CodeSmell(
                issue,
                Entity.from(expression),
                "Do not use the standard Compose Text. Use WooPosText instead."
            )
        )
    }
}
