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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCOrderStore
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

    private fun resetViewState() {
        viewState = viewState.copy(
            orderEdited = false,
            orderEditingFailed = false
        )
    }

    fun updateCustomerOrderNote(updatedCustomerOrderNote: String) {
        resetViewState()

        launch(dispatchers.io) {
            orderEditingRepository.updateCustomerOrderNote(order, updatedCustomerOrderNote).collect { result ->
                when (result) {
                    is WCOrderStore.UpdateOrderResult.OptimisticUpdateResult -> {
                        withContext(Dispatchers.Main) {
                            viewState = viewState.copy(orderEdited = true)
                        }
                    }
                    is WCOrderStore.UpdateOrderResult.RemoteUpdateResult -> {
                        if (result.event.isError) {
                            withContext(Dispatchers.Main) {
                                viewState = viewState.copy(orderEditingFailed = true)
                            }
                        }
                    }
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val orderEdited: Boolean? = null,
        val orderEditingFailed: Boolean? = null
    ) : Parcelable
}
