package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.*
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderCreationFormViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState) {
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
}
