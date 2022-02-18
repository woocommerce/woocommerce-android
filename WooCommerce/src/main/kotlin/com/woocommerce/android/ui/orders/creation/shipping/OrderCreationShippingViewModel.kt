package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
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
    val viewStateData = LiveDataDelegate(savedStateHandle, ViewState())
    private var viewState by viewStateData

    private lateinit var currentOrder: Order

    val currencyDecimals: Int
        get() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: DEFAULT_DECIMAL_PRECISION

    fun setCurrentOrder(order: Order) {
        currentOrder = order
        if (order.shippingLines.any { it.methodId.isNotEmpty() } && viewState.isEmpty) {
            // Set the current shipping line if the user didn't make any edits yet
            val shippingLine = order.shippingLines.first { it.methodId.isNotEmpty() }
            viewState = viewState.copy(amount = shippingLine.total, name = shippingLine.methodTitle)
        }
    }

    fun onAmountEdited(amount: BigDecimal) {
        viewState = viewState.copy(amount = amount)
    }

    fun onNameEdited(name: String) {
        viewState = viewState.copy(name = name)
    }

    fun onDoneButtonClicked() {
        triggerEvent(UpdateShipping(viewState.amount, viewState.name.orEmpty()))
    }

    private val ViewState.isEmpty
        get() = amount.isEqualTo(BigDecimal.ZERO) && name == null

    @Parcelize
    data class ViewState(
        val amount: BigDecimal = BigDecimal.ZERO,
        val name: String? = null
    ) : Parcelable

    data class UpdateShipping(val amount: BigDecimal, val name: String) : MultiLiveEvent.Event()
}
