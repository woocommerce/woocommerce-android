package com.woocommerce.android.ui.woopos.util.format

import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import java.math.BigDecimal
import javax.inject.Inject

class WooPosFormatPrice @Inject constructor(
    private val currencyFormatter: CurrencyFormatter,
    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency,
) {
    suspend operator fun invoke(price: BigDecimal?): String {
        return PriceUtils.formatCurrency(price, getCachedStoreCurrency(), currencyFormatter)
    }
}
