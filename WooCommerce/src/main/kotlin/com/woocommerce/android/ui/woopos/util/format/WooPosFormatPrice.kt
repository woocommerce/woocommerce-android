package com.woocommerce.android.ui.woopos.util.format

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

class WooPosFormatPrice @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
) {
    suspend operator fun invoke(price: BigDecimal?): String {
        val currencyCode = withContext(Dispatchers.IO) {
            wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        }
        return PriceUtils.formatCurrency(price, currencyCode, currencyFormatter)
    }
}
