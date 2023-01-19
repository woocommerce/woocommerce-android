package com.woocommerce.android.ui.payments.taptopay

import android.content.Context
import com.woocommerce.android.extensions.isDeviceWithNFC
import com.woocommerce.android.extensions.isGooglePlayServicesAvailable
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.SystemVersionUtils
import javax.inject.Inject

class IsTapToPayAvailable @Inject constructor(private val context: Context) {
    operator fun invoke(countryCode: String) =
        FeatureFlag.IPP_TAP_TO_PAY.isEnabled() &&
            SystemVersionUtils.isAtLeastP() &&
            context.isGooglePlayServicesAvailable() &&
            context.isDeviceWithNFC() &&
            countryCode in countriesWithTapToPaySupport

    companion object {
        private val countriesWithTapToPaySupport = listOf("US")
    }
}
