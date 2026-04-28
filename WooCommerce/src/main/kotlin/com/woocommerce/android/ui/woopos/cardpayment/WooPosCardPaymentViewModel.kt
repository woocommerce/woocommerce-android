package com.woocommerce.android.ui.woopos.cardpayment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.bookings.BOOKING_PAYMENT_FLOW_FINISHED_KEY
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.WooPosEffectiveReaderStatus
import com.woocommerce.android.ui.woopos.cardreader.WooPosEffectiveReaderStatusProvider
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderPaymentFlow
import com.woocommerce.android.ui.woopos.cashpayment.CashPaymentSource
import com.woocommerce.android.ui.woopos.home.totals.TTPPaymentProgressDelegate
import com.woocommerce.android.ui.woopos.home.totals.WooPosCardReaderPaymentControllerFactory
import com.woocommerce.android.ui.woopos.paymentsuccess.PaymentSuccessSource
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WooPosCardPaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory,
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val networkStatus: WooPosNetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val uiStringParser: UiStringParser,
    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker,
    private val cardPaymentRepository: WooPosCardPaymentRepository,
    private val priceFormat: WooPosFormatPrice,
    private val remoteReaderPaymentFlow: WooPosRemoteReaderPaymentFlow,
    private val effectiveReaderStatusProvider: WooPosEffectiveReaderStatusProvider,
) : ViewModel() {

    private val orderId: Long = requireNotNull(savedState[CARD_PAYMENT_ROUTE_ORDER_ID_KEY])
    private val source: CardPaymentSource = (savedState[CARD_PAYMENT_ROUTE_SOURCE_KEY] as? String)
        ?.let { runCatching { CardPaymentSource.valueOf(it) }.getOrNull() }
        ?: CardPaymentSource.CHECKOUT

    private val _state = MutableStateFlow<WooPosCardPaymentState>(WooPosCardPaymentState.Initiating)
    val state: StateFlow<WooPosCardPaymentState> = _state.asStateFlow()

    val showCashPaymentButton: Boolean = savedState[CARD_PAYMENT_ROUTE_SHOW_CASH_PAYMENT_KEY] ?: false

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private lateinit var orderTotals: WooPosOrderTotalsViewState
    private var order: Order? = null

    private var cardReaderPaymentController: CardReaderPaymentController? = null
    private var paymentListenerJob: Job? = null
    private var controllerEventJob: Job? = null
    private var remotePaymentJob: Job? = null
    private var isTTPPaymentInProgress: Boolean by TTPPaymentProgressDelegate(savedState)
    private var analyticsTrackerJob: Job? = null
    private var activePaymentMode: PaymentMode? = null

    init {
        viewModelScope.launch {
            val loaded = cardPaymentRepository.fetchOrGetOrder(orderId)
            if (loaded == null) {
                _state.value = WooPosCardPaymentState.PaymentFailed(
                    title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
                    subtitle = resourceProvider.getString(R.string.woopos_products_loading_error_message),
                    isDismissButtonVisible = true,
                )
            } else {
                order = loaded
                orderTotals = WooPosOrderTotalsViewState(
                    subtotal = priceFormat(loaded.productsTotal),
                    discount = if (loaded.discountTotal > BigDecimal.ZERO) {
                        "-${priceFormat(loaded.discountTotal)}"
                    } else {
                        null
                    },
                    taxes = priceFormat(loaded.totalTax),
                    total = priceFormat(loaded.total),
                )
                observeReaderStatus()
            }
        }
    }

    private fun observeReaderStatus() {
        viewModelScope.launch {
            effectiveReaderStatusProvider.flow
                .collect { effective ->
                    val currentState = _state.value
                    if (currentState is WooPosCardPaymentState.PaymentInProgress) {
                        return@collect
                    }
                    when (effective) {
                        WooPosEffectiveReaderStatus.RemoteConnected -> {
                            _state.value = buildPreparingState()
                            collectPaymentRemote()
                        }
                        WooPosEffectiveReaderStatus.BluetoothConnected -> {
                            _state.value = buildPreparingState()
                            collectPayment()
                        }
                        // Keep current state during a transient reconnect so an in-flight BT
                        // payment is not cancelled when the reader briefly drops to Reconnecting.
                        WooPosEffectiveReaderStatus.Reconnecting -> Unit
                        WooPosEffectiveReaderStatus.Connecting,
                        WooPosEffectiveReaderStatus.Disconnected -> {
                            _state.value = buildReaderDisconnectedState()
                            cancelPayment()
                        }
                    }
                }
        }
    }

    private fun collectPaymentRemote() {
        if (!networkStatus.isConnected()) {
            _state.value = WooPosCardPaymentState.PaymentFailed(
                title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
                subtitle = resourceProvider.getString(R.string.woopos_no_internet_message),
                actionButtonLabel = resourceProvider.getString(R.string.woo_pos_payment_failed_try_again),
                isDismissButtonVisible = true,
            )
            return
        }
        val order = this.order ?: return

        remotePaymentJob?.cancel()
        activePaymentMode = PaymentMode.REMOTE
        remotePaymentJob = viewModelScope.launch {
            _state.value = WooPosCardPaymentState.PaymentInProgress(
                title = resourceProvider.getString(R.string.woopos_success_totals_payment_processing_title),
                subtitle = resourceProvider.getString(R.string.woopos_success_totals_payment_processing_subtitle),
            )
            when (val result = remoteReaderPaymentFlow.collect(order)) {
                WooPosRemoteReaderPaymentFlow.Result.Completed -> handlePaymentSuccessful()
                is WooPosRemoteReaderPaymentFlow.Result.Failed -> {
                    _state.value = WooPosCardPaymentState.PaymentFailed(
                        title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
                        subtitle = result.message,
                        actionButtonLabel = resourceProvider.getString(R.string.woo_pos_payment_failed_try_again),
                        isDismissButtonVisible = true,
                    )
                }
            }
        }
    }

    private fun collectPayment() {
        if (!networkStatus.isConnected()) {
            _state.value = WooPosCardPaymentState.PaymentFailed(
                title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
                subtitle = resourceProvider.getString(R.string.woopos_no_internet_message),
                actionButtonLabel = resourceProvider.getString(R.string.woo_pos_payment_failed_try_again),
                isDismissButtonVisible = true
            )
            return
        }
        if (cardReaderFacade.readerStatus.value !is Connected) return

        cardReaderPaymentController?.stop()
        activePaymentMode = PaymentMode.BLUETOOTH
        cardReaderPaymentController = cardReaderPaymentControllerFactory.create(
            orderId = orderId,
            paymentType = PaymentOrRefund.Payment.PaymentType.WOO_POS,
            isTTPPaymentInProgress = ::isTTPPaymentInProgress,
            allowCancelledStatus = source == CardPaymentSource.BOOKINGS,
        )
        cardReaderPaymentController?.start()
        listenToPaymentState()
        listenToControllerEvents()
    }

    private fun listenToPaymentState() {
        paymentListenerJob?.cancel()
        paymentListenerJob = viewModelScope.launch {
            cardReaderPaymentController?.paymentState?.collect { paymentState ->
                when (paymentState) {
                    is CardReaderPaymentState.LoadingData -> {
                        _state.value = buildPreparingState()
                    }

                    is CardReaderPaymentState.ProcessingPayment -> {
                        _state.value = WooPosCardPaymentState.Collecting.ReadyForPayment(
                            title = resourceProvider.getString(
                                R.string.woopos_totals_reader_ready_for_payment_title
                            ),
                            subtitle = resourceProvider.getString(
                                R.string.woopos_totals_reader_ready_for_payment_subtitle
                            ),
                            orderTotals = orderTotals,
                        )
                    }

                    is CardReaderPaymentState.PaymentCapturing -> {
                        _state.value = WooPosCardPaymentState.PaymentInProgress(
                            title = resourceProvider.getString(
                                R.string.woopos_success_totals_payment_processing_title
                            ),
                            subtitle = resourceProvider.getString(
                                R.string.woopos_success_totals_payment_processing_subtitle
                            )
                        )
                    }

                    is CardReaderPaymentState.PaymentSuccessful -> {
                        handlePaymentSuccessful()
                    }

                    is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment -> {
                        _state.value = buildPaymentFailedState(paymentState)
                    }

                    CardReaderPaymentState.ReFetchingOrder -> Unit

                    is CardReaderPaymentOrRefundState.CardReaderInteracRefundState,
                    is CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment,
                    is CardReaderPaymentState.PrintingReceipt,
                    CardReaderPaymentState.SharingReceipt -> {
                        throw IllegalArgumentException("Payment state: $paymentState not compatible with POS")
                    }
                }
            }
        }
        analyticsTrackerJob?.cancel()
        analyticsTrackerJob = viewModelScope.launch {
            analyticsTracker.trackPaymentStates(cardReaderPaymentController?.paymentState)
        }
    }

    private fun listenToControllerEvents() {
        controllerEventJob?.cancel()
        controllerEventJob = viewModelScope.launch {
            cardReaderPaymentController?.event?.collect { event ->
                when (event) {
                    is CardReaderPaymentEvent.ShowErrorMessage,
                    is CardReaderPaymentEvent.ShowPaymentErrorMessage -> {
                        val messageRes = when (event) {
                            is CardReaderPaymentEvent.ShowErrorMessage -> event.message
                            is CardReaderPaymentEvent.ShowPaymentErrorMessage -> event.message
                        }
                        _state.value = WooPosCardPaymentState.PaymentFailed(
                            title = resourceProvider.getString(
                                R.string.woopos_success_totals_payment_failed_title
                            ),
                            subtitle = resourceProvider.getString(messageRes),
                            isDismissButtonVisible = true
                        )
                    }

                    is CardReaderPaymentEvent.Exit,
                    is CardReaderPaymentEvent.PlaySuccessfulPaymentSound -> Unit

                    is CardReaderPaymentEvent.InteracRefundSuccessful,
                    is CardReaderPaymentEvent.ContactSupportTapped,
                    is CardReaderPaymentEvent.EnableNfcTapped,
                    is CardReaderPaymentEvent.PurchaseCardReaderTapped,
                    is CardReaderPaymentEvent.PrintReceiptTapped -> {
                        throw IllegalArgumentException(
                            "Payment event: $event not compatible with POS"
                        )
                    }
                }
            }
        }
    }

    private suspend fun handlePaymentSuccessful() {
        val successSource = when (source) {
            CardPaymentSource.CHECKOUT -> PaymentSuccessSource.CARD_CHECKOUT
            CardPaymentSource.BOOKINGS -> PaymentSuccessSource.CARD_BOOKINGS
        }
        _navigationEvent.emit(
            WooPosNavigationEvent.OpenPaymentSuccess(
                orderId = orderId,
                source = successSource,
            )
        )
    }

    private fun buildPaymentFailedState(
        state: CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment
    ): WooPosCardPaymentState.PaymentFailed {
        val isRetryAvailable = state.onRetry != null
        val actionButtonLabel = if (isRetryAvailable) {
            resourceProvider.getString(R.string.woo_pos_payment_failed_try_again)
        } else {
            resourceProvider.getString(R.string.woo_pos_payment_failed_try_another_payment_method)
        }
        return WooPosCardPaymentState.PaymentFailed(
            title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
            subtitle = uiStringParser.asString(state.errorType.message),
            actionButtonLabel = actionButtonLabel,
            isDismissButtonVisible = isRetryAvailable
        )
    }

    private fun buildPreparingState() = WooPosCardPaymentState.Collecting.Preparing(
        title = resourceProvider.getString(R.string.woopos_totals_reader_getting_ready),
        subtitle = resourceProvider.getString(R.string.woopos_totals_reader_preparing_reader_for_payment),
        orderTotals = orderTotals,
    )

    private fun buildReaderDisconnectedState() = WooPosCardPaymentState.Collecting.ReaderDisconnected(
        title = resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_title),
        subtitle = resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_subtitle),
        actionButtonLabel = resourceProvider.getString(
            R.string.woopos_success_totals_error_reader_not_connected_cta_button_label
        ),
        orderTotals = orderTotals,
    )

    fun onScreenResumed() {
        if (_state.value is WooPosCardPaymentState.Collecting) {
            when (effectiveReaderStatusProvider.current()) {
                WooPosEffectiveReaderStatus.RemoteConnected -> collectPaymentRemote()
                WooPosEffectiveReaderStatus.BluetoothConnected -> collectPayment()
                WooPosEffectiveReaderStatus.Connecting,
                WooPosEffectiveReaderStatus.Reconnecting,
                WooPosEffectiveReaderStatus.Disconnected -> Unit
            }
        }
    }

    fun onScreenPaused() {
        if (_state.value is WooPosCardPaymentState.Collecting) {
            cancelPayment()
        }
    }

    fun onRetryClicked() {
        when (activePaymentMode) {
            PaymentMode.REMOTE -> collectPaymentRemote()
            PaymentMode.BLUETOOTH -> retryBluetoothPayment()
            null -> error("Retry clicked but no active payment mode")
        }
    }

    private fun retryBluetoothPayment() {
        val paymentState = cardReaderPaymentController?.paymentState?.value
        check(paymentState != null) {
            "Retry clicked but payment controller is null"
        }
        check(paymentState is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment) {
            "Retry clicked but payment state is not PaymentFailed"
        }
        val onRetry = paymentState.onRetry
        if (onRetry != null) {
            onRetry()
        } else {
            collectPayment()
        }
    }

    fun onBackClicked() {
        if (isPaymentInFlight()) return
        cancelPayment()
        navigateBack()
    }

    fun onDismissClicked() {
        if (isPaymentInFlight()) return
        cancelPayment()
        navigateBack()
    }

    private fun isPaymentInFlight(): Boolean {
        return when (activePaymentMode) {
            PaymentMode.BLUETOOTH -> {
                val paymentState = cardReaderPaymentController?.paymentState?.value
                paymentState is CardReaderPaymentState.ProcessingPayment ||
                    paymentState is CardReaderPaymentState.PaymentCapturing
            }
            PaymentMode.REMOTE -> _state.value is WooPosCardPaymentState.PaymentInProgress
            null -> false
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            when (source) {
                CardPaymentSource.BOOKINGS -> _navigationEvent.emit(
                    WooPosNavigationEvent.NavigateBackToBookingsAfterPayment(
                        BOOKING_PAYMENT_FLOW_FINISHED_KEY,
                        true
                    )
                )
                CardPaymentSource.CHECKOUT -> _navigationEvent.emit(WooPosNavigationEvent.GoBack)
            }
        }
    }

    fun onConnectReaderClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(WooPosNavigationEvent.GoBack)
        }
    }

    fun onCashPaymentClicked() {
        cancelPayment()
        val cashSource = when (source) {
            CardPaymentSource.CHECKOUT -> CashPaymentSource.CHECKOUT
            CardPaymentSource.BOOKINGS -> CashPaymentSource.BOOKINGS
        }
        viewModelScope.launch {
            _navigationEvent.emit(WooPosNavigationEvent.NavigateToCashPayment(orderId, cashSource))
        }
    }

    private fun cancelPayment() {
        paymentListenerJob?.cancel()
        paymentListenerJob = null
        controllerEventJob?.cancel()
        controllerEventJob = null
        analyticsTrackerJob?.cancel()
        analyticsTrackerJob = null
        remotePaymentJob?.cancel()
        remotePaymentJob = null
        cardReaderPaymentController?.onBackPressed()
        cardReaderPaymentController?.stop()
        activePaymentMode = null
    }

    override fun onCleared() {
        cancelPayment()
    }

    private enum class PaymentMode {
        BLUETOOTH,
        REMOTE,
    }
}
