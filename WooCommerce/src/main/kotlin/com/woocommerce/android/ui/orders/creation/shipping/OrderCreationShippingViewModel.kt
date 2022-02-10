package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.simplepayments.SimplePaymentsSharedViewModel
import com.woocommerce.android.ui.orders.simplepayments.SimplePaymentsSharedViewModel.Companion
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

private const val DEFAULT_DECIMAL_PRECISION = 2

@HiltViewModel
class OrderCreationShippingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) : ScopedViewModel(savedStateHandle) {
    val viewStateData = LiveDataDelegate(savedStateHandle, ViewState())
    private var viewState by viewStateData

    val currencyDecimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: DEFAULT_DECIMAL_PRECISION

    @Parcelize
    data class ViewState(
        val amount: Double? = null,
        val name: String? = null
    ) : Parcelable
}
