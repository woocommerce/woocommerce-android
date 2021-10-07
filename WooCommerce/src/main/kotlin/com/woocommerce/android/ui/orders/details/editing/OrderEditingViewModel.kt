package com.woocommerce.android.ui.orders.details.editing

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderEditingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderEditingRepository: OrderEditingRepository
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<OrderDetailFragmentArgs>()
    private val order: Order

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val orderIdentifier: String
        get() = navArgs.orderId

    val customerOrderNote: String
        get() = order.customerNote

    init {
        order = orderEditingRepository.getOrder(orderIdentifier)
    }

    fun updateCustomerOrderNote(updatedCustomerOrderNote: String) {
        launch(dispatchers.io) {
            if (!orderEditingRepository.updateCustomerOrderNote(order, updatedCustomerOrderNote)) {
                withContext(Dispatchers.Main) {
                    viewState = viewState.copy(orderEditingFailed = true)
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val orderEditingFailed: Boolean? = null
    ) : Parcelable
}
