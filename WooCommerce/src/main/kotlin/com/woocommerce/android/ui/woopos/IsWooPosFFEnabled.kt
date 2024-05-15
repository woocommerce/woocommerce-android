package com.woocommerce.android.ui.woopos

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsWooPosFFEnabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.WOO_POS.isEnabled()
    }
}