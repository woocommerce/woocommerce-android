package com.woocommerce.android.ui.blaze

import com.woocommerce.android.util.FeatureFlag

class IsBlazeEnabled {
    operator fun invoke(): Boolean = FeatureFlag.BLAZE.isEnabled()
}
