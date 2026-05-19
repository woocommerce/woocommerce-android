package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosIsMarkOrderAsPaidEnabled @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository
) {
    operator fun invoke(): Boolean =
        featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_MARK_ORDER_AS_PAID)
}
