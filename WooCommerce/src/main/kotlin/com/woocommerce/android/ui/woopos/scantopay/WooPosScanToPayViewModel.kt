package com.woocommerce.android.ui.woopos.scantopay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
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
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val resourceProvider: ResourceProvider,
    private val priceFormat: WooPosFormatPrice,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val orderId: Long = requireNotNull(savedState[SCAN_TO_PAY_ROUTE_ORDER_ID_KEY])

    // `replay = 1` so the init-time `GoBack` emitted from a restored `PaymentDetected` state still
    // reaches a collector that subscribes after the VM has been constructed (e.g. on process
    // restart with restored saved state). Sibling POS payment VMs don't need replay because they
    // don't emit navigation events from init.
    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>(replay = 1)
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _state = savedState.getStateFlow<WooPosScanToPayState>(
        scope = viewModelScope,
        initialValue = WooPosScanToPayState.Loading,
        key = STATE_KEY,
    )
    val state: StateFlow<WooPosScanToPayState> = _state

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            when (_state.value) {
                WooPosScanToPayState.Loading -> prepareAndShowQr()
                is WooPosScanToPayState.ShowingQR -> startPolling()
                WooPosScanToPayState.PaymentDetected -> _navigationEvent.emit(WooPosNavigationEvent.GoBack)
                is WooPosScanToPayState.Failed -> Unit
            }
        }
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
        if (_state.value is WooPosScanToPayState.PaymentDetected) return
        viewModelScope.launch {
            analyticsTracker.track(BackToCheckoutFromScanToPay)
            pollingJob?.cancel()
            _navigationEvent.emit(WooPosNavigationEvent.GoBack)
        }
    }

    private suspend fun prepareAndShowQr() {
        val promote = repository.promoteOrderToPending(orderId)
        if (promote.isFailure) {
            failAndTrack()
            return
        }

        val paymentUrl = readPaymentUrlWithRetry()
        if (paymentUrl.isNullOrBlank()) {
            failAndTrack()
            return
        }

        val totalText = repository.getCachedOrder(orderId)?.total?.let {
            resourceProvider.getString(R.string.woopos_scan_to_pay_total, priceFormat(it))
        }.orEmpty()

        _state.value = WooPosScanToPayState.ShowingQR(paymentUrl = paymentUrl, totalText = totalText)
        startPolling()
    }

    private suspend fun readPaymentUrlWithRetry(): String? {
        repeat(MAX_PAYMENT_URL_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(PAYMENT_URL_RETRY_DELAY_MS)
            repository.fetchOrderSnapshot(orderId)?.paymentUrl?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private suspend fun failAndTrack() {
        analyticsTracker.track(ScanToPayPaymentFailed)
        _state.value = failedState()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            for (attempt in 0 until MAX_POLL_ATTEMPTS) {
                val intervalMs = if (attempt < FAST_POLL_ATTEMPTS) FAST_POLL_INTERVAL_MS else SLOW_POLL_INTERVAL_MS
                delay(intervalMs)

                val snapshot = repository.fetchOrderSnapshot(orderId) ?: continue
                if (snapshot.isPaidOnline()) {
                    onPaymentDetected()
                    return@launch
                }
            }
            failAndTrack()
        }
    }

    // Offline gateways such as "Pay in Person" (cod) still move the order to processing, which makes
    // WooCommerce stamp `datePaid` even though no money has been collected. Only an online gateway
    // means the customer actually paid through the QR code.
    private fun Order.isPaidOnline(): Boolean = isOrderPaid && !isCashPayment

    private suspend fun onPaymentDetected() {
        _state.value = WooPosScanToPayState.PaymentDetected
        analyticsTracker.track(ScanToPayPaymentDetectedViaPolling)
        analyticsTracker.track(ScanToPayCollectPaymentSuccess)
        childrenToParentEventSender.sendToParent(
            ChildToParentEvent.OrderSuccessfullyPaid(PaymentMethod.SCAN_TO_PAY)
        )
        _navigationEvent.emit(WooPosNavigationEvent.GoBack)
        viewModelScope.launch {
            repository.addOrderNote(orderId, resourceProvider.getString(R.string.woopos_scan_to_pay_order_note))
        }
    }

    private fun failedState() = WooPosScanToPayState.Failed(
        message = resourceProvider.getString(R.string.woopos_scan_to_pay_error_message),
    )

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val STATE_KEY = "woo_pos_scan_to_pay_state"
        const val PAYMENT_URL_RETRY_DELAY_MS = 1_000L
        const val MAX_PAYMENT_URL_ATTEMPTS = 3
        const val FAST_POLL_INTERVAL_MS = 2_000L
        const val SLOW_POLL_INTERVAL_MS = 5_000L
        const val FAST_POLL_ATTEMPTS = 15
        const val MAX_POLL_ATTEMPTS = 75
    }
}
