package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosHistoricalOrdersM1Enabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.WOO_POS_HISTORICAL_ORDERS_M1.isEnabled()
    }
}
