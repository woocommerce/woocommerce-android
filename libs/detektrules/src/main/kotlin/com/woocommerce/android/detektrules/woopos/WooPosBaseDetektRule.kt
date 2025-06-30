package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtFile

abstract class WooPosBaseDetektRule(config: Config) : Rule(config) {
    private val targetPackagePrefix = "com.woocommerce.android.ui.woopos"

    override fun visitKtFile(file: KtFile) {
        if (file.packageFqName.asString().startsWith(targetPackagePrefix)) {
            super.visitKtFile(file)
        }
    }
}
