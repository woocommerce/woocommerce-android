package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsPosProductsFtsEnabled @Inject constructor() {
    operator fun invoke(): Boolean = FeatureFlag.POS_PRODUCTS_FTS.isEnabled()
}
