package com.woocommerce.android.ui.woopos

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsWooPosEnabled @Inject constructor() {
    operator fun invoke() = FeatureFlag.WOO_POS.isEnabled()
}
