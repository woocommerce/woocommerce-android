package com.woocommerce.android.ui.woopos

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosFeatureFlagEnabled @Inject constructor() {
    fun isEnabled() = FeatureFlag.WOO_POS.isEnabled()
}
