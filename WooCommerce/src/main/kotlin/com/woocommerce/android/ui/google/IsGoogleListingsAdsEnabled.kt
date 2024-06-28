package com.woocommerce.android.ui.google

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsGoogleListingsAdsEnabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.GOOGLE_ADS_M1.isEnabled()
    }
}
