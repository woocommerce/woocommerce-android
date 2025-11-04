package com.woocommerce.android.ui.woopos.util.format

import android.util.Log
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
    @Volatile
    private var cachedCurrencyCode: String? = null

    suspend operator fun invoke(price: BigDecimal?): String {
        val tag = "WooPOS-Price"

        val fetchStart = System.currentTimeMillis()
        val currencyCode = getCurrencyCode()
        Log.d(tag, "getCurrencyCode() took ${System.currentTimeMillis() - fetchStart}ms")

        val formatStart = System.currentTimeMillis()
        val result = PriceUtils.formatCurrency(price, currencyCode, currencyFormatter)
        Log.d(tag, "formatCurrency() took ${System.currentTimeMillis() - formatStart}ms")

        return result
    }

//    suspend operator fun invoke(price: BigDecimal?): String {
//        val tag = "WooPOS-Price"
//
//        val fetchStart = System.currentTimeMillis()
//        val currencyCode = withContext(Dispatchers.IO) {
//            wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
//        }
//        val fetchDuration = System.currentTimeMillis() - fetchStart
//        Log.d(tag, "fetch currencyCode took ${fetchDuration}ms (code=$currencyCode)")
//
//        val formatStart = System.currentTimeMillis()
//        val result = PriceUtils.formatCurrency(price, currencyCode, currencyFormatter)
//        val formatDuration = System.currentTimeMillis() - formatStart
//        Log.d(tag, "PriceUtils.formatCurrency took ${formatDuration}ms")
//
//        return result
//    }

    private suspend fun getCurrencyCode(): String? {
        // fast path
        cachedCurrencyCode?.let { return it }

        // slow path
        val code = withContext(Dispatchers.IO) {
            wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        }
        cachedCurrencyCode = code
        return code
    }
}
