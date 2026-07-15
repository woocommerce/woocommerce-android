package com.woocommerce.android.detektrules.store

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFile

class MixedStoreThemeImportsRule(config: Config) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Store files should use either the legacy theme or Store Design System imports, not both.",
        Debt.FIVE_MINS
    )

    override fun visitKtFile(file: KtFile) {
        if (!file.isStoreFile()) return

        super.visitKtFile(file)

        val imports = file.importDirectives.mapNotNull { it.importPath?.pathStr }
        val importsLegacyTheme = imports.any { it.isWithinPackage(LEGACY_THEME_PACKAGE) }
        val importsStoreDesignSystem = imports.any { it.isWithinPackage(STORE_DESIGN_SYSTEM_PACKAGE) }

        if (importsLegacyTheme && importsStoreDesignSystem) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(file),
                    "Use one Store theme root per file and migrate the remaining imports so only the legacy theme " +
                        "or Store Design System root remains."
                )
            )
        }
    }

    private fun KtFile.isStoreFile(): Boolean {
        val packageName = packageFqName.asString()
        return packageName.isWithinPackage(STORE_UI_PACKAGE) &&
            !packageName.isWithinPackage(WOO_POS_PACKAGE)
    }

    private fun String.isWithinPackage(packageRoot: String): Boolean =
        this == packageRoot || startsWith("$packageRoot.")

    companion object {
        private const val STORE_UI_PACKAGE = "com.woocommerce.android.ui"
        private const val WOO_POS_PACKAGE = "com.woocommerce.android.ui.woopos"
        private const val LEGACY_THEME_PACKAGE = "com.woocommerce.android.ui.compose.theme"
        private const val STORE_DESIGN_SYSTEM_PACKAGE = "com.woocommerce.android.ui.compose.designsystem"
    }
}
