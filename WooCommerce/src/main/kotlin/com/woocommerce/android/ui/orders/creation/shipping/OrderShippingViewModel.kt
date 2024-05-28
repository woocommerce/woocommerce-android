package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderShippingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val getShippingMethodById: GetShippingMethodById,
    private val tracker: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {

    private val navArgs: OrderShippingFragmentArgs by savedState.navArgs()
    val viewState: MutableStateFlow<ViewState>

    val isEditFlow = navArgs.currentShippingLine != null

    init {
        val state = if (navArgs.currentShippingLine == null) {
            ViewState.ShippingState(
                method = null,
                name = null,
                amount = BigDecimal.ZERO,
                isSaveChangesEnabled = false
            )
        } else {
            ViewState.Loading
        }
        viewState = MutableStateFlow(state)
        navArgs.currentShippingLine?.let { shippingLine: Order.ShippingLine ->
            launch {
                viewState.value = ViewState.ShippingState(
                    method = getShippingMethodById(shippingLine.methodId),
                    name = shippingLine.methodTitle,
                    amount = shippingLine.total,
                    isSaveChangesEnabled = false
                )
            }
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

    fun onSelectMethod() {
        triggerEvent(SelectShippingMethod((viewState.value as? ViewState.ShippingState)?.method?.id))
    }

    fun onMethodSelected(selected: ShippingMethod) {
        (viewState.value as? ViewState.ShippingState)?.let {
            tracker.track(
                AnalyticsEvent.ORDER_SHIPPING_METHOD_SELECTED,
                mapOf(AnalyticsTracker.KEY_SHIPPING_METHOD to selected.id)
            )
            viewState.value = it.copy(
                method = selected,
                isSaveChangesEnabled = isSaveChangesEnabled(newMethodId = selected.id)
            )
        }
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
            val method: ShippingMethod?,
            val name: String?,
            val amount: BigDecimal,
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

fun ShippingUpdateResult.getMethodIdOrDefault() = if (this.methodId.isNullOrEmpty()) {
    " " // Default should be N/A Id (""), but passing "" fails, so we add a space
} else {
    this.methodId
}

data class UpdateShipping(val shippingUpdate: ShippingUpdateResult) : MultiLiveEvent.Event()
data class RemoveShipping(val id: Long) : MultiLiveEvent.Event()
data class SelectShippingMethod(val currentMethodId: String?) : MultiLiveEvent.Event()
