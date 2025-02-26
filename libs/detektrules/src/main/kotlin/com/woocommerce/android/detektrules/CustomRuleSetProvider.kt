package com.woocommerce.android.detektrules

import com.woocommerce.android.detektrules.woopos.WooPosDesignSystemButtonUsageRule
import com.woocommerce.android.detektrules.woopos.WooPosDesignSystemColorUsageRule
import com.woocommerce.android.detektrules.woopos.WooPosDesignSystemCornerRadiusUsageRule
import com.woocommerce.android.detektrules.woopos.WooPosDesignSystemSpacingUsageRule
import com.woocommerce.android.detektrules.woopos.WooPosDesignSystemTextUsageRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class CustomRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "WooPosRules"

    override fun instance(config: Config) = RuleSet(
        ruleSetId,
        listOf(
            WooPosDesignSystemSpacingUsageRule(config),
            WooPosDesignSystemCornerRadiusUsageRule(config),
            WooPosDesignSystemColorUsageRule(config),
            WooPosDesignSystemTextUsageRule(config),
            WooPosDesignSystemButtonUsageRule(config),
        )
    )
}
