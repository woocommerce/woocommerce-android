package com.woocommerce.android.ui.orders.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_CASH
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class TakePaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
) : ScopedViewModel(savedState) {
    private val navArgs: TakePaymentFragmentArgs by savedState.navArgs()

    val order: Order
        get() = navArgs.order

    val orderTotal: BigDecimal
        get() = order.total

    fun onCashPaymentClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_COLLECT,
            mapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_CASH
            )
        )
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = R.string.simple_payments_cash_dlg_title,
                messageId = R.string.simple_payments_cash_dlg_message,
                positiveButtonId = R.string.simple_payments_cash_dlg_button,
                positiveBtnAction = { _, _ ->
                    onCashPaymentConfirmed()
                },
                negativeButtonId = R.string.cancel
            )
        )
    }

    /**
     * User has confirmed the cash payment, so mark it as completed
     */
    private fun onCashPaymentConfirmed() {
        if (networkStatus.isConnected()) {
            launch {
                AnalyticsTracker.track(
                    AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_COMPLETED,
                    mapOf(
                        AnalyticsTracker.KEY_AMOUNT to orderTotal.toString(),
                        AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_CASH
                    )
                )
                updateOrderStatus(Order.Status.Completed.value)
            }
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        }
    }

    fun onSharePaymentUrlClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_COLLECT,
            mapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_LINK
            )
        )
        triggerEvent(SharePaymentUrl(selectedSite.get().name, order.paymentUrl))
    }

    fun onSharePaymentUrlCompleted() {
        AnalyticsTracker.track(
            AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_COMPLETED,
            mapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_LINK
            )
        )
        launch {
            updateOrderStatus(Order.Status.Pending.value)
        }
    }

    fun onCardPaymentClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_COLLECT,
            mapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_CARD
            )
        )
        triggerEvent(OrderNavigationTarget.StartCardReaderPaymentFlow(order.id))
    }

    fun onConnectToReaderResultReceived(connected: Boolean) {
        if (!connected) {
            AnalyticsTracker.track(
                AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_FAILED,
                mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD)
            )
        }
    }

    fun onCardReaderPaymentCompleted() {
        launch {
            // this function is called even when the payment fails - in other words, it tells us
            // the card reader flow completed but not necessarily successfully -, so we check the
            // status of the order to determine whether payment succeeded
            val status = orderStore.getOrderByIdAndSite(navArgs.order.id, selectedSite.get())?.status
            if (status == CoreOrderStatus.COMPLETED.value) {
                AnalyticsTracker.track(
                    AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_COMPLETED,
                    mapOf(
                        AnalyticsTracker.KEY_AMOUNT to orderTotal.toString(),
                        AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_CARD
                    )
                )
            } else {
                AnalyticsTracker.track(
                    AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_SOURCE to
                            AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD
                    )
                )
            }

            delay(DELAY_MS)
            triggerEvent(MultiLiveEvent.Event.Exit)
        }
    }

    private suspend fun updateOrderStatus(statusKey: String) {
        val statusModel = withContext(dispatchers.io) {
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), statusKey)
                ?: error("Couldn't find a status with key $statusKey")
        }

        orderStore.updateOrderStatus(
            navArgs.order.id,
            selectedSite.get(),
            statusModel
        ).collect { result ->
            when (result) {
                is WCOrderStore.UpdateOrderResult.OptimisticUpdateResult -> {
                    triggerEvent(MultiLiveEvent.Event.Exit)
                }
                is WCOrderStore.UpdateOrderResult.RemoteUpdateResult -> {
                    if (result.event.isError) {
                        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.order_error_update_general))
                        AnalyticsTracker.track(
                            AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_FAILED,
                            mapOf(
                                AnalyticsTracker.KEY_SOURCE to
                                    AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD
                            )
                        )
                    }
                }
            }
        }
    }

    data class SharePaymentUrl(
        val storeName: String,
        val paymentUrl: String
    ) : MultiLiveEvent.Event()

    companion object {
        private const val DELAY_MS = 1L
    }
}
