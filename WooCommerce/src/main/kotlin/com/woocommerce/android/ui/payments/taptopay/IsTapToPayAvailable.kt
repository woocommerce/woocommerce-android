package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.SystemVersionUtilsWrapper
import javax.inject.Inject

class IsTapToPayAvailable @Inject constructor(
    private val isTapToPayEnabled: IsTapToPayEnabled,
    private val deviceFeatures: DeviceFeatures,
    private val systemVersionUtilsWrapper: SystemVersionUtilsWrapper
) {
    operator fun invoke(countryCode: String) =
        if (!isTapToPayEnabled()) Result.NotAvailable.TapToPayDisabled
        else if (!systemVersionUtilsWrapper.isAtLeastP()) Result.NotAvailable.SystemVersionNotSupported
        else if (!deviceFeatures.isGooglePlayServicesAvailable()) Result.NotAvailable.GooglePlayServicesNotAvailable
        else if (!deviceFeatures.isNFCAvailable()) Result.NotAvailable.NfcNotAvailable
        else if (countryCode !in countriesWithTapToPaySupport) Result.NotAvailable.CountryNotSupported
        else Result.Available

    sealed class Result {
        object Available : Result()
        sealed class NotAvailable : Result() {
            object TapToPayDisabled : NotAvailable()
            object SystemVersionNotSupported : NotAvailable()
            object GooglePlayServicesNotAvailable : NotAvailable()
            object NfcNotAvailable : NotAvailable()
            object CountryNotSupported : NotAvailable()
        }
    }

    companion object {
        private val countriesWithTapToPaySupport = listOf("US")
    }
}
