package com.woocommerce.android.ui.orders.quickorder

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class QuickOrderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) : ScopedViewModel(savedState) {
    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal var viewState by viewStateLiveData

    val currencyCode: String
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""

    val decimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: DEFAULT_DECIMAL_PRECISION

    @Parcelize
    data class ViewState(
        val dummyValue: Int = 0
    ) : Parcelable

    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
    }
}
