package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

@Suppress("unused")
class WooPosLocalCatalogM1Enabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.WOO_POS_LOCAL_CATALOG_M1.isEnabled()
    }
}
