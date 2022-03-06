package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderCreationShippingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: OrderCreationShippingFragmentArgs by savedStateHandle.navArgs()

    val viewStateData = LiveDataDelegate(
        savedState = savedStateHandle,
        initialValue = ViewState(
            amount = navArgs.currentShippingLine?.total ?: BigDecimal.ZERO,
            name = navArgs.currentShippingLine?.methodTitle,
            isEditFlow = navArgs.currentShippingLine != null
        )
    )
    private var viewState by viewStateData

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
