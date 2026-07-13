package com.woocommerce.android.detektrules

import com.woocommerce.android.detektrules.store.MixedStoreThemeImportsRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class StoreRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "StoreRules"

    override fun instance(config: Config) = RuleSet(
        ruleSetId,
        listOf(
            MixedStoreThemeImportsRule(config),
        )
    )
}
