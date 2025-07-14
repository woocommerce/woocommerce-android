package com.woocommerce.android.ui.orders

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class IsStoreCurrencyMatch @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {

    suspend operator fun invoke(orderCurrency: String): CurrencyMatchResult {
        val storeCurrency = withContext(Dispatchers.IO) {
            wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        }
        return CurrencyMatchResult(
            isMatch = storeCurrency.equals(orderCurrency, ignoreCase = true),
            storeCurrency = storeCurrency
        )
    }
}

data class CurrencyMatchResult(
    val isMatch: Boolean,
    val storeCurrency: String?
)
