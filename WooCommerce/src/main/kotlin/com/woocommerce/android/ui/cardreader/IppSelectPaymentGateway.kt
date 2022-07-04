package com.woocommerce.android.ui.cardreader

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IppSelectPaymentGateway @Inject constructor() {
    fun isEnabled() = FeatureFlag.IPP_SELECT_PAYMENT_GATEWAY.isEnabled(context = null)
}
