package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsTaxRateSelectorEnabled @Inject constructor()  {
    operator fun invoke(): Boolean = FeatureFlag.ORDER_CREATION_TAX_RATE_SELECTOR.isEnabled()
}
