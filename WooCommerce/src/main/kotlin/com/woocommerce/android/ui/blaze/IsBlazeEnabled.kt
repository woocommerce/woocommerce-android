package com.woocommerce.android.ui.blaze

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsBlazeEnabled @Inject constructor() {
    operator fun invoke(): Boolean = FeatureFlag.BLAZE.isEnabled()
}
