package com.woocommerce.android.ui.login.storecreation.iap

import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

class WooIapLogWrapper @Inject constructor() : IAPLogWrapper {
    override fun w(tag: String, message: String) {
        WooLog.w(WooLog.T.IAP, message)
    }

    override fun d(tag: String, message: String) {
        WooLog.d(WooLog.T.IAP, message)
    }

    override fun e(tag: String, message: String) {
        WooLog.e(WooLog.T.IAP, message)
    }
}
