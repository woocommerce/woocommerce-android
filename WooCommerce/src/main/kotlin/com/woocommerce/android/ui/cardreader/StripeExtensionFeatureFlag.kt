package com.woocommerce.android.ui.cardreader

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class StripeExtensionFeatureFlag @Inject constructor() {
    fun isEnabled() = FeatureFlag.PAYMENTS_STRIPE_EXTENSION.isEnabled(null)
}
