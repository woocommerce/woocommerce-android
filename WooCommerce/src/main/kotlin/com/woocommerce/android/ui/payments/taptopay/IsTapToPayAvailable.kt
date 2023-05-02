package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.SystemVersionUtilsWrapper
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class IsTapToPayAvailable @Inject constructor(
    private val appPrefs: AppPrefs = AppPrefs,
    private val selectedSite: SelectedSite,
    private val deviceFeatures: DeviceFeatures,
    private val systemVersionUtilsWrapper: SystemVersionUtilsWrapper,
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
    private val wooStore: WooCommerceStore,
) {
    operator fun invoke() =
        when {
            !appPrefs.isTapToPayEnabled -> Result.NotAvailable.TapToPayDisabled
            !systemVersionUtilsWrapper.isAtLeastP() -> Result.NotAvailable.SystemVersionNotSupported
            !deviceFeatures.isGooglePlayServicesAvailable() -> Result.NotAvailable.GooglePlayServicesNotAvailable
            !deviceFeatures.isNFCAvailable() -> Result.NotAvailable.NfcNotAvailable
            !isTppSupportedInCountry(wooStore.getStoreCountryCode(selectedSite.get())) ->
                Result.NotAvailable.CountryNotSupported

            else -> Result.Available
        }

    private fun isTppSupportedInCountry(countryCode: String?) =
        when (val config = cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode)) {
            is CardReaderConfigForSupportedCountry -> config.supportedReaders.any { it is ReaderType.BuildInReader }
            CardReaderConfigForUnsupportedCountry -> false
        }

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
}

val IsTapToPayAvailable.Result.isAvailable get() = this is IsTapToPayAvailable.Result.Available
