package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.*
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreationFormViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val orderCreationRepository: OrderCreationRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    fun onEditOrderStatusClicked(currentStatus: OrderStatus) {
        launch(dispatchers.io) {
            orderDetailRepository
                .getOrderStatusOptions().toTypedArray()
                .runWithContext(dispatchers.main) {
                    displayOrderStatusSelector(currentStatus, it)
                }
        }
    }

    private fun displayOrderStatusSelector(currentStatus: OrderStatus, statusList: Array<OrderStatus>) {
        ViewOrderStatusSelector(
            currentStatus = currentStatus.statusKey,
            orderStatusList = statusList
        ).let { triggerEvent(it) }
    }

    fun onCustomerClicked() {
        triggerEvent(EditCustomer)
    }

    fun onCustomerNoteClicked() {
        triggerEvent(EditCustomerNote)
    }

    fun onAddSimpleProductsClicked() {
        triggerEvent(AddProduct)
    }

    fun onProductClicked(item: Order.Item) {
        triggerEvent(ShowProductDetails(item))
    }

    fun onCreateOrderClicked(order: Order) {
        viewModelScope.launch {
            viewState = viewState.copy(isProgressDialogShown = true)
            orderCreationRepository.createOrder(order).fold(
                onSuccess = {
                    // TODO Navigate to order details
                },
                onFailure = {
                    viewState = viewState.copy(isProgressDialogShown = false)
                    // TODO triggerEvent(ShowSnackbar)
                }
            )
        }
    }
}

@Parcelize
data class ViewState(
    val isProgressDialogShown: Boolean = false
): Parcelable
