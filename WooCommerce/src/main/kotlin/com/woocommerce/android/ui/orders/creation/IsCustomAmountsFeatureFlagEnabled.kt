package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsCustomAmountsFeatureFlagEnabled @Inject constructor() {
    operator fun invoke() = FeatureFlag.CUSTOM_AMOUNTS_M1.isEnabled()
}
