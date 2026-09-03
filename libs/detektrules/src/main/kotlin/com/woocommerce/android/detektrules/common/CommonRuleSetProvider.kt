package com.woocommerce.android.detektrules.common

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class CommonRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "CommonRules"

    override fun instance(config: Config) = RuleSet(
        ruleSetId,
        listOf(
            StringifyLambdaBearingObjectRule(config),
        )
    )
}
