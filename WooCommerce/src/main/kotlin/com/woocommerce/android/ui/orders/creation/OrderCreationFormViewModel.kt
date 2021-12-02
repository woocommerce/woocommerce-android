package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderCreationFormViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    fun onOrderStatusSelected(status: Order.Status) {
        launch(dispatchers.io) {
            val orderStatus = orderDetailRepository.getOrderStatus(status.value)
            withContext(dispatchers.main) {
                viewState = viewState.copy(orderStatus = orderStatus)
            }
        }
    }

    fun onEditOrderStatusSelected() {
        launch(dispatchers.io) {
            val statusList = orderDetailRepository.getOrderStatusOptions().toTypedArray()
            withContext(dispatchers.main) { displayOrderStatusSelector(statusList) }
        }
    }

    private fun displayOrderStatusSelector(statusList: Array<OrderStatus>) {
        viewState.orderStatus?.run {
            ViewOrderStatusSelector(
                currentStatus = statusKey,
                orderStatusList = statusList
            ).let { triggerEvent(it) }
        }
    }

    @Parcelize
    data class ViewState(
        val orderStatus: OrderStatus? = null
    ) : Parcelable
}
