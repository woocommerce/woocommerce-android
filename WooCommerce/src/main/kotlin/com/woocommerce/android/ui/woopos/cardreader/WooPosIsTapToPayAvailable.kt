package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.payments.taptopay.isAvailable
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import javax.inject.Inject

class WooPosIsTapToPayAvailable @Inject constructor(
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus,
    private val featureFlagRepository: FeatureFlagRepository,
) {
    operator fun invoke(): Boolean =
        isFeatureFlagEnabled() && tapToPayAvailabilityStatus().isAvailable

    fun isFeatureFlagEnabled(): Boolean =
        featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TAP_TO_PAY)
}
