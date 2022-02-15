package com.woocommerce.android.ui.orders.cardreader

import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import javax.inject.Inject
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore

class CardReaderPaymentCurrencySupportedChecker @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val cardReaderConfigFactory: CardReaderConfigFactory,
) {
    suspend fun isCurrencySupported(currency: String): Boolean {
        return cardReaderConfigFactory.getCardReaderConfigFor(getStoreCountryCode()).currency == currency
    }

    private suspend fun getStoreCountryCode(): String? {
        return withContext(dispatchers.io) {
            wooStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
                WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.")
            }
        }
    }
}
