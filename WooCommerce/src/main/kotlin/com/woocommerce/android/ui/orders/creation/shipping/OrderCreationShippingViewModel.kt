package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

private const val DEFAULT_DECIMAL_PRECISION = 2

@HiltViewModel
class OrderCreationShippingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: OrderCreationShippingFragmentArgs by savedStateHandle.navArgs()

    val viewStateData = LiveDataDelegate(
        savedStateHandle, ViewState(
            amount = navArgs.currentShippingLine?.total ?: BigDecimal.ZERO,
            name = navArgs.currentShippingLine?.methodTitle,
            isEditFlow = navArgs.currentShippingLine != null
        )
    )
    private var viewState by viewStateData

    val currencyDecimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: DEFAULT_DECIMAL_PRECISION

    fun onAmountEdited(amount: BigDecimal) {
        viewState = viewState.copy(amount = amount)
    }

    fun onNameEdited(name: String) {
        viewState = viewState.copy(name = name)
    }

    fun onDoneButtonClicked() {
        triggerEvent(UpdateShipping(viewState.amount, viewState.name.orEmpty()))
    }

    fun onRemoveShippingClicked() {
        triggerEvent(RemoveShipping)
    }

    @Parcelize
    data class ViewState(
        val amount: BigDecimal,
        val name: String?,
        val isEditFlow: Boolean
    ) : Parcelable

    data class UpdateShipping(val amount: BigDecimal, val name: String) : MultiLiveEvent.Event()
    object RemoveShipping : MultiLiveEvent.Event()
}
