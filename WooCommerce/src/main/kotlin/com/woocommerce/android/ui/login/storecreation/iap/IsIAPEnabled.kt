package com.woocommerce.android.ui.login.storecreation.iap

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class IsIAPEnabled @Inject constructor() {
    operator fun invoke(): Boolean = FeatureFlag.IAP_FOR_STORE_CREATION.isEnabled()
}
