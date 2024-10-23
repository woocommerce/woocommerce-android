package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsNonSimpleProductTypesEnabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.POS_NON_SIMPLE_PRODUCT_TYPES.isEnabled()
    }
}
