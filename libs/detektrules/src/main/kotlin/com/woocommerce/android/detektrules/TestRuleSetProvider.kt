package com.woocommerce.android.detektrules

import com.woocommerce.android.detektrules.test.UnitTestNamingRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class TestRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "TestRules"

    override fun instance(config: Config) = RuleSet(
        ruleSetId,
        listOf(
            UnitTestNamingRule(config),
        )
    )
}