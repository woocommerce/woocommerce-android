package com.woocommerce.android.util

import com.woocommerce.android.AppPrefsWrapper

class FakeAppPrefs : AppPrefsWrapper() {
    override var orderSummaryMigrated = false
    override var gatewayMigrated = false
}
