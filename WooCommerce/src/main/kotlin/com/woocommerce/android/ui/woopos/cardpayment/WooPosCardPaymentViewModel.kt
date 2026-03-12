package com.woocommerce.android.ui.woopos.cardpayment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.bookings.BOOKING_PAYMENT_FLOW_FINISHED_KEY
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
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

    private var cardReaderPaymentController: CardReaderPaymentController? = null
    private var paymentListenerJob: Job? = null
    private var controllerEventJob: Job? = null
    private var isTTPPaymentInProgress: Boolean by TTPPaymentProgressDelegate(savedState)
    private var analyticsTrackerJob: Job? = null

    init {
        viewModelScope.launch {
            val totals = loadOrderTotals()
            if (totals != null) {
                orderTotals = totals
                observeCardReaderStatus()
            }
        }
    }

    private suspend fun loadOrderTotals(): WooPosOrderTotalsViewState? {
        val order = cardPaymentRepository.fetchOrGetOrder(orderId)
        if (order == null) {
            _state.value = WooPosCardPaymentState.PaymentFailed(
                title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
                subtitle = resourceProvider.getString(R.string.woopos_products_loading_error_message),
                isDismissButtonVisible = true,
            )
            return null
        }

        return WooPosOrderTotalsViewState(
            subtotal = priceFormat(order.productsTotal),
            discount = if (order.discountTotal > BigDecimal.ZERO) {
                "-${priceFormat(order.discountTotal)}"
            } else {
                null
            },
            taxes = priceFormat(order.totalTax),
            total = priceFormat(order.total),
        )
    }

    private fun observeCardReaderStatus() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect { status ->
                when (status) {
                    is NotConnected, is Connecting -> {
                        val currentState = _state.value
                        if (currentState is WooPosCardPaymentState.PaymentInProgress) {
                            return@collect
                        }
                        _state.value = buildReaderDisconnectedState()
                        cancelPayment()
                    }

                    is Connected -> {
                        val currentState = _state.value
                        if (currentState is WooPosCardPaymentState.PaymentInProgress) {
                            return@collect
                        }
                        _state.value = buildPreparingState()
                        collectPayment()
                    }

                    is CardReaderStatus.Reconnecting -> Unit
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
            collectPayment()
        }
    }

    fun onScreenPaused() {
        if (_state.value is WooPosCardPaymentState.Collecting) {
            cancelPayment()
        }
    }

    fun onRetryClicked() {
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
        val paymentState = cardReaderPaymentController?.paymentState?.value
        if (paymentState is CardReaderPaymentState.ProcessingPayment ||
            paymentState is CardReaderPaymentState.PaymentCapturing
        ) {
            return
        }
        cancelPayment()
        navigateBack()
    }

    fun onDismissClicked() {
        cancelPayment()
        navigateBack()
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
        cardReaderPaymentController?.onBackPressed()
        cardReaderPaymentController?.stop()
    }

    override fun onCleared() {
        cancelPayment()
    }
}
