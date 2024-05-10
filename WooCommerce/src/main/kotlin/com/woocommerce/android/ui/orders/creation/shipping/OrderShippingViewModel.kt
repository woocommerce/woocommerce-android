package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.extensions.capitalize
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderShippingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {

    private val navArgs: OrderShippingFragmentArgs by savedState.navArgs()
    val viewState: MutableStateFlow<ViewState>

    init {
        val state = if (navArgs.currentShippingLine == null) {
            ViewState.ShippingState(
                method = null,
                name = null,
                amount = BigDecimal.ZERO,
                isEditFlow = false,
                isSaveChangesEnabled = false
            )
        } else {
            ViewState.Loading
        }
        viewState = MutableStateFlow(state)
        navArgs.currentShippingLine?.let { shippingLine: Order.ShippingLine ->
            launch {
                viewState.value = ViewState.ShippingState(
                    method = getShippingMethod(shippingLine),
                    name = shippingLine.methodTitle,
                    amount = shippingLine.total,
                    isEditFlow = true,
                    isSaveChangesEnabled = false
                )
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun getShippingMethod(shippingLine: Order.ShippingLine): Order.ShippingMethod? {
        return if (shippingLine.methodId == null) {
            null
        } else {
            delay(1000)
            Order.ShippingMethod(
                id = shippingLine.methodId,
                title = shippingLine.methodId.capitalize(),
                total = BigDecimal.ZERO,
                tax = BigDecimal.ZERO
            )
        }
    }

    fun onNameChanged(name: String) {
        (viewState.value as? ViewState.ShippingState)?.let {
            viewState.value = it.copy(
                name = name,
                isSaveChangesEnabled = isSaveChangesEnabled(newName = name)
            )
        }
    }

    fun onAmountChanged(amount: BigDecimal) {
        (viewState.value as? ViewState.ShippingState)?.let {
            viewState.value = it.copy(
                amount = amount,
                isSaveChangesEnabled = isSaveChangesEnabled(newAmount = amount)
            )
        }
    }

    private fun isSaveChangesEnabled(
        newName: String? = null,
        newAmount: BigDecimal? = null,
        newMethodId: String? = null
    ): Boolean {
        val state = viewState.value as? ViewState.ShippingState ?: return false
        val name = newName ?: state.name
        val amount = newAmount ?: state.amount
        val methodId = newMethodId ?: state.method?.id
        val canEditValues = navArgs.currentShippingLine?.let { shippingLine: Order.ShippingLine ->
            shippingLine.methodId != methodId ||
                shippingLine.methodTitle != name ||
                shippingLine.total != amount
        } ?: true
        return canEditValues
    }

    fun onMethodSelected() {
        /*TODO*/
    }

    fun onSaveChanges() {
        val state = viewState.value as? ViewState.ShippingState ?: return
        val id = navArgs.currentShippingLine?.itemId
        val name = if (state.name.isNullOrEmpty()) {
            resourceProvider.getString(R.string.order_creation_add_shipping_name_hint)
        } else {
            state.name
        }
        val event = UpdateShipping(
            ShippingUpdateResult(
                id = id,
                amount = state.amount,
                name = name,
                methodId = state.method?.id
            )
        )
        triggerEvent(event)
    }

    fun onRemove() {
        navArgs.currentShippingLine?.let { shippingLine: Order.ShippingLine ->
            triggerEvent(RemoveShipping(shippingLine.itemId))
        }
    }

    sealed class ViewState {
        data object Loading : ViewState()
        data class ShippingState(
            val method: Order.ShippingMethod?,
            val name: String?,
            val amount: BigDecimal,
            val isEditFlow: Boolean,
            val isSaveChangesEnabled: Boolean
        ) : ViewState()
    }
}

@Parcelize
data class ShippingUpdateResult(
    val id: Long?,
    val amount: BigDecimal,
    val name: String,
    val methodId: String?
) : Parcelable

data class UpdateShipping(val shippingUpdate: ShippingUpdateResult) : MultiLiveEvent.Event()
data class RemoveShipping(val id: Long) : MultiLiveEvent.Event()
