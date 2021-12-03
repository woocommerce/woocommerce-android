package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
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
    fun onEditOrderStatusSelected(currentStatus: Order.Status) {
        launch(dispatchers.io) {
            orderDetailRepository.getOrderStatusOptions().toTypedArray().let { statusList ->
                orderDetailRepository
                    .getOrderStatus(currentStatus.value)
                    .runWithContext(dispatchers.main) { displayOrderStatusSelector(it, statusList) }
            }
        }
    }

    private fun displayOrderStatusSelector(currentStatus: OrderStatus, statusList: Array<OrderStatus>) {
        ViewOrderStatusSelector(
            currentStatus = currentStatus.statusKey,
            orderStatusList = statusList
        ).let { triggerEvent(it) }
    }
}
