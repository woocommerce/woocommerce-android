package com.woocommerce.android.ui.orders.simplepayments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class SimplePaymentsFragmentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) : ScopedViewModel(savedState) {
    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal final var viewState by viewStateLiveData

    private val navArgs: SimplePaymentsFragmentArgs by savedState.navArgs()

    init {
        viewState = viewState.copy(order = navArgs.order)
    }

    val currencyCode: String
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""

    val order: Order
        get() = viewState.order!!

    @Parcelize
    data class ViewState(
        val currentPrice: BigDecimal = BigDecimal.ZERO,
        val order: Order? = null
    ) : Parcelable
}
