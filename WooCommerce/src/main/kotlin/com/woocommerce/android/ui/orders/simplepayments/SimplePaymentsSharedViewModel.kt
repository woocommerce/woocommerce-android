package com.woocommerce.android.ui.orders.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SimplePaymentsSharedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedState) {
    val currencyCode: String
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""

    val decimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: DEFAULT_DECIMAL_PRECISION

    fun formatAmount(amount: BigDecimal) = currencyFormatter.formatCurrency(amount, currencyCode)

    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
    }
}
