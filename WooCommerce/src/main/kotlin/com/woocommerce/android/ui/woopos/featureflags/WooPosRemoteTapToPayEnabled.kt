package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosRemoteTapToPayEnabled @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository
) {
    operator fun invoke(): Boolean = featureFlagRepository.isEnabled(FeatureFlag.REMOTE_TAP_TO_PAY)
}
