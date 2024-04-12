package com.woocommerce.android.ui.payments.cashpayments

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class OtherPaymentMethodsEnabled @Inject constructor() {
    operator fun invoke() = FeatureFlag.OTHER_PAYMENT_METHODS.isEnabled()
}
