package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosIsProductsSearchEnabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.POS_PRODUCTS_SEARCH.isEnabled()
    }
}
