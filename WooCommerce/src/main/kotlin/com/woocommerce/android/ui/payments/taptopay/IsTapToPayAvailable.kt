package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsTapToPayAvailable @Inject constructor() {
    operator fun invoke() = FeatureFlag.IPP_TAP_TO_PAY.isEnabled()
}
