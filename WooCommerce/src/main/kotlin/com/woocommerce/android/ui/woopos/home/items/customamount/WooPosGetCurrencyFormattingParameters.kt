package com.woocommerce.android.ui.woopos.home.items.customamount

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

data class WooPosCurrencyFormattingParameters(
    val currencySymbol: String,
    val currencyPosition: CurrencyPosition,
    val decimalSeparator: String,
    val numberOfDecimals: Int,
)

class WooPosGetCurrencyFormattingParameters @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) {
    private var cached: WooPosCurrencyFormattingParameters? = null

    suspend operator fun invoke(): WooPosCurrencyFormattingParameters = cached ?: load().also { cached = it }

    private suspend fun load(): WooPosCurrencyFormattingParameters = withContext(Dispatchers.IO) {
        val site = selectedSite.get()
        val siteSettings = wooCommerceStore.getSiteSettings(site)
        val currencyCode = siteSettings?.currencyCode
        val currencySymbol = wooCommerceStore.getSiteCurrency(site, currencyCode).orEmpty()
        WooPosCurrencyFormattingParameters(
            currencySymbol = currencySymbol,
            currencyPosition = siteSettings?.currencyPosition ?: CurrencyPosition.LEFT,
            decimalSeparator = siteSettings?.currencyDecimalSeparator ?: ".",
            numberOfDecimals = siteSettings?.currencyDecimalNumber ?: DEFAULT_NUMBER_OF_DECIMALS,
        )
    }

    private companion object {
        const val DEFAULT_NUMBER_OF_DECIMALS = 2
    }
}
