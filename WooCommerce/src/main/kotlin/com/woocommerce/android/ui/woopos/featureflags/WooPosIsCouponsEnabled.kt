package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosIsCouponsEnabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.POS_COUPONS.isEnabled()
    }
}
