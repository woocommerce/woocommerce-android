package com.woocommerce.android.ui.orders.creation.shipping

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderShippingMethodsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getShippingMethodsWithOtherValue: GetShippingMethodsWithOtherValue,
    private val refreshShippingMethods: RefreshShippingMethods
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: OrderShippingMethodsFragmentArgs by savedState.navArgs()
    val viewState: MutableStateFlow<ViewState>

    init {
        viewState = MutableStateFlow(ViewState.Loading)
        launch {
            getShippingMethods()
        }
        launch {
            refreshShippingMethods().onFailure {
                if ((viewState.value is ViewState.ShippingMethodsState).not()) {
                    viewState.value = ViewState.Error
                }
            }
        }
    }

    fun retry() {
        launch {
            viewState.value = ViewState.Loading
            refreshShippingMethods().onFailure { viewState.value = ViewState.Error }
        }
    }

    fun refresh() {
        launch {
            val refreshingState = when (val state = viewState.value) {
                is ViewState.ShippingMethodsState -> state.copy(isRefreshing = true)
                else -> ViewState.Loading
            }
            viewState.value = refreshingState
            refreshShippingMethods().onFailure { viewState.value = ViewState.Error }
        }
    }

    private suspend fun getShippingMethods() {
        getShippingMethodsWithOtherValue()
            .filter { fetchedShippingMethods ->
                fetchedShippingMethods.isEmpty().not()
            }
            .collect { fetchedShippingMethods ->
                var methodsUIList = fetchedShippingMethods.map { ShippingMethodUI(it) }

                methodsUIList = navArgs.selectedMethodId?.let { selectedId ->
                    updateSelection(selectedId, fetchedShippingMethods.map { ShippingMethodUI(it) })
                } ?: methodsUIList

                viewState.value = ViewState.ShippingMethodsState(methods = methodsUIList, isRefreshing = false)
            }
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
            val isRefreshing: Boolean,
            val methods: List<ShippingMethodUI>
        ) : ViewState()
    }

    data class ShippingMethodUI(
        val method: ShippingMethod,
        val isSelected: Boolean = false
    )
}

data class ShippingMethodSelected(val selected: ShippingMethod) : MultiLiveEvent.Event()
