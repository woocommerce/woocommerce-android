package com.woocommerce.android.ui.orders.simplepayments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import java.math.BigDecimal
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class TakePaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState) {
    private val navArgs: TakePaymentFragmentArgs by savedState.navArgs()
    private var paymentFlowJob: Job? = null

    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val order: Order
        get() = navArgs.order

    val orderTotal: BigDecimal
        get() = order.total

    fun onCashPaymentClicked() {
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
    fun onCashPaymentConfirmed() {
        if (networkStatus.isConnected()) {
            launch {
                markOrderCompleted()
            }
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        }
    }

    fun onCardPaymentClicked(cardReaderManager: CardReaderManager) {
        if (cardReaderManager.readerStatus.value is CardReaderStatus.Connected && paymentFlowJob == null) {
            triggerEvent(OrderNavigationTarget.StartCardReaderPaymentFlow(order.id))
        } else {
            triggerEvent(OrderNavigationTarget.StartCardReaderConnectFlow(skipOnboarding = true))
        }
    }

    fun onConnectToReaderResultReceived(connected: Boolean) {
        launch {
            // this dummy delay needs to be here since the navigation component hasn't finished the previous
            // transaction when a result is received
            delay(1)
            if (connected) {
                triggerEvent(OrderNavigationTarget.StartCardReaderPaymentFlow(order.id))
            }
        }
    }

    fun onCardReaderPaymentCompleted() {
        viewState = viewState.copy(isProgressShowing = true)

        launch {
            markOrderCompleted()
            withContext(Dispatchers.Main) {
                viewState = viewState.copy(isProgressShowing = false)
                triggerEvent(MultiLiveEvent.Event.Exit)
            }
        }
    }

    private suspend fun markOrderCompleted() {
        val status = withContext(dispatchers.io) {
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), CoreOrderStatus.COMPLETED.value)
                ?: error("Couldn't find a status with key ${CoreOrderStatus.COMPLETED.value}")
        }

        orderStore.updateOrderStatus(
            LocalOrRemoteId.RemoteId(navArgs.order.id),
            selectedSite.get(),
            status
        ).collect { result ->
            when (result) {
                is WCOrderStore.UpdateOrderResult.OptimisticUpdateResult -> {
                    triggerEvent(MultiLiveEvent.Event.Exit)
                }
                is WCOrderStore.UpdateOrderResult.RemoteUpdateResult -> {
                    if (result.event.isError) {
                        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.order_error_update_general))
                    }
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val isProgressShowing: Boolean = false
    ) : Parcelable
}
