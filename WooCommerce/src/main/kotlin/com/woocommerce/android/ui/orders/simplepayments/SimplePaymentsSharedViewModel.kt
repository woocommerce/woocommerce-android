package com.woocommerce.android.ui.orders.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreationRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class SimplePaymentsSharedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val currencyFormatter: CurrencyFormatter,
    private val dispatchers: CoroutineDispatchers,
) : ScopedViewModel(savedState) {
    val currencyCode: String
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""

    val decimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: DEFAULT_DECIMAL_PRECISION

    fun formatAmount(amount: BigDecimal) = currencyFormatter.formatCurrency(amount, currencyCode)

    private suspend fun isAutoDraftSupported(): Boolean {
        val version = withContext(dispatchers.io) {
            wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE)?.version
                ?: "0.0"
        }
        return version.semverCompareTo(OrderCreationRepository.AUTO_DRAFT_SUPPORTED_VERSION) >= 0
    }

    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
    }
}
