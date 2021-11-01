package com.woocommerce.android.ui.orders.details.editing

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
    private val orderEditingRepository: OrderEditingRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState) {
    private val navArgs by savedState.navArgs<OrderDetailFragmentArgs>()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val orderIdentifier: String
        get() = navArgs.orderId

    internal lateinit var order: Order

    fun start() {
        order = orderEditingRepository.getOrder(orderIdentifier)
    }

    private fun resetViewState() {
        viewState = viewState.copy(
            orderEdited = false,
            orderEditingFailed = false
        )
    }

    private fun checkConnectionAndResetState(): Boolean {
        return if (networkStatus.isConnected()) {
            resetViewState()
            true
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
            false
        }
    }

    fun updateCustomerOrderNote(updatedNote: String) = runWhenUpdateIsPossible {
        orderEditingRepository.updateCustomerOrderNote(
            order.localId, updatedNote
        ).collectOrderUpdate()
    }

    fun updateShippingAddress(updatedShippingAddress: Address) = runWhenUpdateIsPossible {
        if (viewState.replicateBothAddressesToggleActivated == true) {
            sendReplicateShippingAndBillingAddressesWith(updatedShippingAddress)
        } else {
            orderEditingRepository.updateOrderAddress(
                order.localId,
                updatedShippingAddress.toShippingAddressModel()
            )
        }.collectOrderUpdate()
    }

    fun updateBillingAddress(updatedBillingAddress: Address) = runWhenUpdateIsPossible {
        if (viewState.replicateBothAddressesToggleActivated == true) {
            sendReplicateShippingAndBillingAddressesWith(updatedBillingAddress)
        } else {
            orderEditingRepository.updateOrderAddress(
                order.localId,
                updatedBillingAddress.toBillingAddressModel()
            )
        }.collectOrderUpdate()
    }

    private suspend fun sendReplicateShippingAndBillingAddressesWith(orderAddress: Address) =
        orderEditingRepository.updateBothOrderAddresses(
            order.localId,
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

    private suspend fun Flow<UpdateOrderResult>.collectOrderUpdate() {
        collect { result ->
            when (result) {
                is OptimisticUpdateResult -> {
                    withContext(Dispatchers.Main) {
                        triggerEvent(OrderEdited)
                    }
                }
                is RemoteUpdateResult -> {
                    val stat = if (result.event.isError) {
                        AnalyticsTracker.Stat.ORDER_DETAIL_EDIT_FLOW_FAILED
                    } else {
                        AnalyticsTracker.Stat.ORDER_DETAIL_EDIT_FLOW_COMPLETED
                    }
                    AnalyticsTracker.track(
                        stat,
                        mapOf(
                            AnalyticsTracker.KEY_SUBJECT to AnalyticsTracker.ORDER_EDIT_CUSTOMER_NOTE
                        )
                    )
                    if (result.event.isError) {
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
                    }
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
