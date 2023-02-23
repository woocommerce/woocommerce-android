package com.woocommerce.android.ui.payments.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tracker.OrderDurationRecorder
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class SimplePaymentsSharedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
) : ScopedViewModel(savedState) {
    val currencyCode: String
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""

    val decimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: DEFAULT_DECIMAL_PRECISION

    init {
        // Reset order duration recorder to ensure we don't track Simple Payments flow
        OrderDurationRecorder.reset()
    }

    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
    }
}
