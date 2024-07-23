package com.woocommerce.android.ui.orders.details.editing

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import javax.inject.Inject

@HiltViewModel
class OrderEditingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val orderEditingRepository: OrderEditingRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<OrderDetailFragmentArgs>()

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    lateinit var order: Order
    private var orderId: Long = -1L

    fun start() {
        runBlocking {
            val orderId = if (orderId == -1L) {
                navArgs.orderId
            } else {
                orderId
            }
            order = requireNotNull(orderDetailRepository.getOrderById(orderId)) {
                "Order $orderId not found in the database."
            }
        }
    }

    fun setOrderId(orderId: Long) {
        this.orderId = orderId
    }

    private fun checkConnectionAndResetState(): Boolean {
        return if (networkStatus.isConnected()) {
            true
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
            false
        }
    }

    fun updateCustomerOrderNote(updatedNote: String) = runWhenUpdateIsPossible {
        orderEditingRepository.updateCustomerOrderNote(
            order.id,
            updatedNote
        ).collectOrderUpdate(AnalyticsTracker.ORDER_EDIT_CUSTOMER_NOTE)
    }

    fun updateShippingAddress(updatedShippingAddress: Address) = runWhenUpdateIsPossible {
        if (viewState.replicateBothAddressesToggleActivated == true) {
            sendReplicateShippingAndBillingAddressesWith(updatedShippingAddress)
        } else {
            orderEditingRepository.updateOrderAddress(
                order.id,
                updatedShippingAddress.toShippingAddressModel()
            )
        }.collectOrderUpdate(AnalyticsTracker.ORDER_EDIT_SHIPPING_ADDRESS)
    }

    fun updateBillingAddress(updatedBillingAddress: Address) = runWhenUpdateIsPossible {
        if (viewState.replicateBothAddressesToggleActivated == true) {
            sendReplicateShippingAndBillingAddressesWith(updatedBillingAddress)
        } else {
            orderEditingRepository.updateOrderAddress(
                order.id,
                updatedBillingAddress.toBillingAddressModel()
            )
        }.collectOrderUpdate(AnalyticsTracker.ORDER_EDIT_BILLING_ADDRESS)
    }

    private suspend fun sendReplicateShippingAndBillingAddressesWith(orderAddress: Address) =
        orderEditingRepository.updateBothOrderAddresses(
            order.id,
            orderAddress.toShippingAddressModel(),
            orderAddress.toBillingAddressModel(
                customEmail = orderAddress.email
                    .takeIf { it.isNotEmpty() }
                    ?: order.billingAddress.email
            )
        )

    fun onReplicateAddressSwitchChanged(enabled: Boolean) {
        viewState = viewState.copy(replicateBothAddressesToggleActivated = enabled)
    }

    private suspend fun Flow<UpdateOrderResult>.collectOrderUpdate(editingSubject: String) {
        collect { result ->
            when (result) {
                is OptimisticUpdateResult -> {
                    withContext(Dispatchers.Main) {
                        triggerEvent(OrderEdited)
                    }
                }
                is RemoteUpdateResult -> {
                    val stat = if (result.event.isError) {
                        withContext(Dispatchers.Main) {
                            triggerEvent(
                                OrderEditFailed(
                                    if (result.event.error.type == WCOrderStore.OrderErrorType.EMPTY_BILLING_EMAIL) {
                                        R.string.order_error_update_empty_mail
                                    } else {
                                        R.string.order_error_update_general
                                    }
                                )
                            )
                        }
                        AnalyticsEvent.ORDER_DETAIL_EDIT_FLOW_FAILED
                    } else {
                        AnalyticsEvent.ORDER_DETAIL_EDIT_FLOW_COMPLETED
                    }
                    AnalyticsTracker.track(
                        stat,
                        mapOf(AnalyticsTracker.KEY_SUBJECT to editingSubject)
                    )
                }
            }
        }
    }

    private inline fun runWhenUpdateIsPossible(
        crossinline action: suspend () -> Unit
    ) = checkConnectionAndResetState().also {
        if (it) launch(dispatchers.io) { action() }
    }

    @Parcelize
    data class ViewState(
        val replicateBothAddressesToggleActivated: Boolean? = null
    ) : Parcelable

    object OrderEdited : MultiLiveEvent.Event()
    data class OrderEditFailed(@StringRes val message: Int) : MultiLiveEvent.Event()
}
