package com.woocommerce.android.ui.woopos.cardpayment

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosIsCardPaymentEnabledForCountry @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val countryConfigProvider: CardReaderCountryConfigProvider,
) {
    suspend operator fun invoke(): Boolean = withContext(Dispatchers.IO) {
        val site = selectedSite.getOrNull() ?: return@withContext false
        val countryCode = wooCommerceStore.getStoreCountryCode(site) ?: return@withContext false
        countryConfigProvider.provideCountryConfigFor(countryCode).isPosCardPaymentEnabled
    }
}
