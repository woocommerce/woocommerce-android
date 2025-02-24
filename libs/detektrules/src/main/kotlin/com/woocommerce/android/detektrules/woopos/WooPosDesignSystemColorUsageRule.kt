package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile

class WooPosDesignSystemColorUsageRule(config: Config) : Rule(config) {
    private val targetPackagePrefix = "com.woocommerce.android.ui.woopos"
    private val allowedColorSources = listOf("MaterialTheme.colorScheme", "WooPosTheme.colors")
    private val colorArguments = setOf(
        "background",
        "containerColor",
        "contentColor",
        "borderColor",
        "tint",
        "shadowColor",
        "disabledContainerColor",
        "disabledContentColor"
    )

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Use colors from WooPosTheme.colors or MaterialTheme.colorScheme instead of hardcoded colors.",
        Debt.FIVE_MINS
    )

    override fun visitKtFile(file: KtFile) {
        if (file.packageFqName.asString().startsWith(targetPackagePrefix)) {
            super.visitKtFile(file)
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        expression.valueArguments.forEach { argument ->
            val argName = argument.getArgumentName()?.asName?.asString()
            if (argName != null && argName in colorArguments) {
                val argExprText = argument.getArgumentExpression()?.text ?: return@forEach
                checkColorUsage(argExprText, argument)
            }
        }
    }

    private fun checkColorUsage(colorText: String, element: PsiElement) {
        val recognizedPrefixes = listOf(
            "Color(",
            "Color.",
            "WooPosColors.",
            "MaterialTheme.colorScheme.",
            "WooPosTheme.colors."
        )

        if (recognizedPrefixes.none { colorText.startsWith(it) }) {
            return
        }

        if (allowedColorSources.none { colorText.startsWith(it) }) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(element),
                    "Colors should be used from WooPosTheme.colors or MaterialTheme.colorScheme. Found: $colorText"
                )
            )
        }
    }
}
