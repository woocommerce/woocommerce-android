package com.woocommerce.android.ui.orders.creation.shipping

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderShippingMethodsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: OrderShippingMethodsFragmentArgs by savedState.navArgs()
    val viewState: MutableStateFlow<ViewState>

    init {
        viewState = MutableStateFlow(ViewState.Loading)
        launch {
            getShippingMethods()
        }
    }

    fun retry() {
        launch {
            getShippingMethods()
        }
    }

    private suspend fun getShippingMethods() {
        viewState.value = ViewState.Loading
        delay(1000)
        val fetchedShippingMethods = listOf(
            ShippingMethod(
                id = "flat_rate",
                title = "Flat Rate"
            ),
            ShippingMethod(
                id = "free_shipping",
                title = "Free shipping"
            ),
            ShippingMethod(
                id = "local_pickup",
                title = "Local pickup"
            ),
            ShippingMethod(
                id = "other",
                title = "Other"
            )
        )

        var methodsUIList = fetchedShippingMethods.map { ShippingMethodUI(it) }

        methodsUIList = navArgs.selectedMethodId?.let { selectedId ->
            updateSelection(selectedId, fetchedShippingMethods.map { ShippingMethodUI(it) })
        } ?: methodsUIList

        viewState.value = ViewState.ShippingMethodsState(methods = methodsUIList)
    }

    fun onMethodSelected(selected: ShippingMethodUI) {
        val currentState = viewState.value as? ViewState.ShippingMethodsState ?: return
        viewState.value = currentState.copy(methods = updateSelection(selected.method.id, currentState.methods))
        triggerEvent(ShippingMethodSelected(selected.method))
    }

    private fun updateSelection(selectedId: String, methods: List<ShippingMethodUI>): List<ShippingMethodUI> {
        return methods.map { shippingMethod ->
            if (shippingMethod.method.id == selectedId) {
                shippingMethod.copy(isSelected = true)
            } else {
                shippingMethod.copy(isSelected = false)
            }
        }
    }

    sealed class ViewState {
        data object Error : ViewState()
        data object Loading : ViewState()
        data class ShippingMethodsState(
            val methods: List<ShippingMethodUI>
        ) : ViewState()
    }

    data class ShippingMethodUI(
        val method: ShippingMethod,
        val isSelected: Boolean = false
    )
}

data class ShippingMethodSelected(val selected: ShippingMethod) : MultiLiveEvent.Event()
