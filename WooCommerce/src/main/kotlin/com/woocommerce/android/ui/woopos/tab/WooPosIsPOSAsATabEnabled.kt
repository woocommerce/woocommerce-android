package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class WooPosIsPosAsTabEnabled @Inject constructor() {
    operator fun invoke(): Boolean {
        return FeatureFlag.WOO_POS_AS_A_TAB_I1.isEnabled()
    }
}
