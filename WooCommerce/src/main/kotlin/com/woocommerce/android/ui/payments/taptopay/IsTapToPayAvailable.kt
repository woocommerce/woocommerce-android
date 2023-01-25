package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.SystemVersionUtilsWrapper
import javax.inject.Inject

class IsTapToPayAvailable @Inject constructor(
    private val deviceFeatures: DeviceFeatures,
    private val systemVersionUtilsWrapper: SystemVersionUtilsWrapper
) {
    operator fun invoke(countryCode: String, isTapToPayEnabled: IsTapToPayEnabled) =
        isTapToPayEnabled() &&
            systemVersionUtilsWrapper.isAtLeastP() &&
            deviceFeatures.isGooglePlayServicesAvailable() &&
            deviceFeatures.isNFCAvailable() &&
            countryCode in countriesWithTapToPaySupport

    companion object {
        private val countriesWithTapToPaySupport = listOf("US")
    }
}
