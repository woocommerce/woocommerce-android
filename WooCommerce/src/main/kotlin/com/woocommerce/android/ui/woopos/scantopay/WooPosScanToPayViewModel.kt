package com.woocommerce.android.ui.woopos.scantopay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventSender
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCheckoutFromScanToPay
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ScanToPayCollectPaymentSuccess
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ScanToPayPaymentDetectedViaPolling
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ScanToPayPaymentFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosScanToPayViewModel @Inject constructor(
    private val repository: WooPosScanToPayRepository,
    private val parentToChildrenEventSender: WooPosParentToChildrenEventSender,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val priceFormat: WooPosFormatPrice,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val orderId: Long = requireNotNull(savedState[SCAN_TO_PAY_ROUTE_ORDER_ID_KEY])

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _state = savedState.getStateFlow<WooPosScanToPayState>(
        scope = viewModelScope,
        initialValue = WooPosScanToPayState.Loading,
        key = STATE_KEY,
    )
    val state: StateFlow<WooPosScanToPayState> = _state

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch { prepareAndShowQr() }
    }

    fun onUIEvent(event: WooPosScanToPayUIEvent) {
        when (event) {
            WooPosScanToPayUIEvent.RetryClicked -> viewModelScope.launch {
                _state.value = WooPosScanToPayState.Loading
                prepareAndShowQr()
            }
            WooPosScanToPayUIEvent.CancelClicked -> onBackClicked()
        }
    }

    fun onBackClicked() {
        viewModelScope.launch {
            analyticsTracker.track(BackToCheckoutFromScanToPay)
            pollingJob?.cancel()
            _navigationEvent.emit(WooPosNavigationEvent.GoBack)
        }
    }

    private suspend fun prepareAndShowQr() {
        val promote = repository.promoteOrderToPending(orderId)
        if (promote.isFailure) {
            _state.value = failedState(retryable = true)
            return
        }

        val paymentUrl = readPaymentUrlWithRetry()
        if (paymentUrl.isNullOrBlank()) {
            _state.value = failedState(retryable = true)
            return
        }

        val totalText = repository.getCachedOrder(orderId)?.total?.let {
            resourceProvider.getString(R.string.woopos_scan_to_pay_total, priceFormat(it))
        }.orEmpty()

        _state.value = WooPosScanToPayState.ShowingQR(paymentUrl = paymentUrl, totalText = totalText)
        startPolling()
    }

    private suspend fun readPaymentUrlWithRetry(): String? {
        repository.fetchOrderSnapshot(orderId)?.paymentUrl?.takeIf { it.isNotBlank() }?.let { return it }
        delay(POST_PROMOTION_DELAY_MS)
        return repository.fetchOrderSnapshot(orderId)?.paymentUrl?.takeIf { it.isNotBlank() }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            for (attempt in 0 until MAX_POLL_ATTEMPTS) {
                val intervalMs = if (attempt < FAST_POLL_ATTEMPTS) FAST_POLL_INTERVAL_MS else SLOW_POLL_INTERVAL_MS
                delay(intervalMs)

                val snapshot = repository.fetchOrderSnapshot(orderId) ?: continue
                if (snapshot.isPaid()) {
                    onPaymentDetected()
                    return@launch
                }
            }
            analyticsTracker.track(ScanToPayPaymentFailed)
            _state.value = failedState(retryable = true)
        }
    }

    private suspend fun onPaymentDetected() {
        analyticsTracker.track(ScanToPayPaymentDetectedViaPolling)
        _state.value = WooPosScanToPayState.PaymentDetected
        viewModelScope.launch {
            repository.addOrderNote(orderId, resourceProvider.getString(R.string.woopos_scan_to_pay_order_note))
        }
        analyticsTracker.track(ScanToPayCollectPaymentSuccess)
        parentToChildrenEventSender.sendToChildren(
            ParentToChildrenEvent.OrderSuccessfullyPaid(PaymentMethod.SCAN_TO_PAY),
        )
        _navigationEvent.emit(WooPosNavigationEvent.GoBack)
    }

    private fun failedState(retryable: Boolean) = WooPosScanToPayState.Failed(
        message = resourceProvider.getString(R.string.woopos_scan_to_pay_error_message),
        retryable = retryable,
    )

    private fun Order.isPaid(): Boolean = datePaid != null || status in PAID_STATUSES

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val STATE_KEY = "woo_pos_scan_to_pay_state"
        const val POST_PROMOTION_DELAY_MS = 1_000L
        const val FAST_POLL_INTERVAL_MS = 2_000L
        const val SLOW_POLL_INTERVAL_MS = 5_000L
        const val FAST_POLL_ATTEMPTS = 15
        const val MAX_POLL_ATTEMPTS = 75

        val PAID_STATUSES: Set<Order.Status> = setOf(
            Order.Status.Processing,
            Order.Status.Completed,
        )
    }
}
