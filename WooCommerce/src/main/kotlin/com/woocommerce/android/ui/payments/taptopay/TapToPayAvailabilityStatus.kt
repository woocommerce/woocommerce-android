package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.SystemVersionUtilsWrapper
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class TapToPayAvailabilityStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val deviceFeatures: DeviceFeatures,
    private val systemVersionUtilsWrapper: SystemVersionUtilsWrapper,
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
    private val wooStore: WooCommerceStore,
    private val tapToPayDeviceSupportChecker: TapToPayDeviceSupportChecker,
) {
    operator fun invoke(): Result =
        when {
            !systemVersionUtilsWrapper.isAtLeastR() -> Result.NotAvailable.SystemVersionNotSupported
            !deviceFeatures.isGooglePlayServicesAvailable() -> Result.NotAvailable.GooglePlayServicesNotAvailable
            !deviceFeatures.isNFCAvailable() -> Result.NotAvailable.NfcNotAvailable
            !isTppSupportedInCountry() -> Result.NotAvailable.CountryNotSupported

            else -> when (tapToPayDeviceSupportChecker.checkSupport()) {
                TapToPayDeviceSupport.Supported -> Result.Available
                TapToPayDeviceSupport.NotSupported -> Result.NotAvailable.DeviceNotSupported
                TapToPayDeviceSupport.Unknown -> Result.Unknown
            }
        }

    private fun isTppSupportedInCountry(): Boolean {
        val selectedSite = selectedSite.getIfExists() ?: return false
        val countryCode = wooStore.getStoreCountryCode(selectedSite)
        return when (val config = cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode)) {
            is CardReaderConfigForSupportedCountry -> config.supportedReaders.any { it is ReaderType.BuildInReader }
            CardReaderConfigForUnsupportedCountry -> false
        }
    }

    sealed class Result {
        object Available : Result()
        object Unknown : Result()

        sealed class NotAvailable : Result() {
            object SystemVersionNotSupported : NotAvailable()
            object GooglePlayServicesNotAvailable : NotAvailable()
            object NfcNotAvailable : NotAvailable()
            object CountryNotSupported : NotAvailable()
            object DeviceNotSupported : NotAvailable()
        }
    }
}

/**
 * Unknown counts as available: Stripe Terminal is only initialized from the flows these entry points
 * lead to, so hiding them until Stripe answers would keep the answer unknown forever.
 */
val TapToPayAvailabilityStatus.Result.isAvailableOrUnknown
    get() = this !is TapToPayAvailabilityStatus.Result.NotAvailable
