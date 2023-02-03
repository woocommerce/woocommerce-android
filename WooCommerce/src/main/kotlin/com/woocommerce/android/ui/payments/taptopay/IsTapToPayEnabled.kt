package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsTapToPayEnabled @Inject constructor() {
    operator fun invoke(): Boolean = FeatureFlag.IPP_TAP_TO_PAY.isEnabled()
}
