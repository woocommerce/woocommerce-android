package com.woocommerce.android.ui.woopos.featureflags

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosPosSettingsEnabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.WOO_POS_SETTINGS.isEnabled()
    }
}
