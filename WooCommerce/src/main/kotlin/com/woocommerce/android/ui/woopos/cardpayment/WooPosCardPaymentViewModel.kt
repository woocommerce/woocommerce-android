package com.woocommerce.android.ui.woopos.cardpayment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.totals.WooPosCardReaderPaymentControllerFactory
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
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
import javax.inject.Inject

@HiltViewModel
class WooPosCardPaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory,
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val networkStatus: WooPosNetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val uiStringParser: UiStringParser,
    private val priceFormat: WooPosFormatPrice,
    private val totalsRepository: WooPosTotalsRepository,
    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker,
) : ViewModel() {

    private val orderId: Long = requireNotNull(savedState[CARD_PAYMENT_ROUTE_ORDER_ID_KEY])
    val source: CardPaymentSource = CardPaymentSource.valueOf(
        savedState[CARD_PAYMENT_ROUTE_SOURCE_KEY] ?: CardPaymentSource.CHECKOUT.name
    )

    private val _state = MutableStateFlow<WooPosCardPaymentState>(WooPosCardPaymentState.Initiating)
    val state: StateFlow<WooPosCardPaymentState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<WooPosNavigationEvent>()
    val navigationEvent: SharedFlow<WooPosNavigationEvent> = _navigationEvent.asSharedFlow()

    private var cardReaderPaymentController: CardReaderPaymentController? = null
    private var paymentListenerJob: Job? = null
    private var controllerEventJob: Job? = null
    private var isTTPPaymentInProgress: Boolean = false

    init {
        observeCardReaderStatus()
    }

    private fun observeCardReaderStatus() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect { status ->
                when (status) {
                    is NotConnected, is Connecting -> {
                        val currentState = _state.value
                        if (currentState is WooPosCardPaymentState.PaymentSuccess ||
                            currentState is WooPosCardPaymentState.PaymentInProgress
                        ) {
                            return@collect
                        }
                        _state.value = buildReaderDisconnectedState()
                        cancelPayment()
                    }

                    is Connected -> {
                        val currentState = _state.value
                        if (currentState is WooPosCardPaymentState.PaymentSuccess ||
                            currentState is WooPosCardPaymentState.PaymentInProgress
                        ) {
                            return@collect
                        }
                        _state.value = buildPreparingState()
                        collectPayment()
                    }
                }
            }
        }
    }

    private fun collectPayment() {
        if (!networkStatus.isConnected()) {
            _state.value = WooPosCardPaymentState.PaymentFailed(
                title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
                subtitle = resourceProvider.getString(R.string.woopos_no_internet_message),
                retryButtonLabel = resourceProvider.getString(R.string.woo_pos_payment_failed_try_again),
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

                    is CardReaderPaymentState.CollectingPayment -> {
                        _state.value = WooPosCardPaymentState.Collecting.ReadyForPayment(
                            title = resourceProvider.getString(
                                R.string.woopos_totals_reader_ready_for_payment_title
                            ),
                            subtitle = resourceProvider.getString(
                                paymentState.cardReaderHint
                                    ?: R.string.woopos_totals_reader_ready_for_payment_subtitle
                            )
                        )
                    }

                    is CardReaderPaymentState.ProcessingPayment,
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

                    else -> Unit
                }
            }
        }
        viewModelScope.launch { analyticsTracker.trackPaymentStates(cardReaderPaymentController?.paymentState) }
    }

    private fun listenToControllerEvents() {
        controllerEventJob?.cancel()
        controllerEventJob = viewModelScope.launch {
            cardReaderPaymentController?.event?.collect { event ->
                when (event) {
                    is CardReaderPaymentEvent.ShowErrorMessage -> {
                        _state.value = WooPosCardPaymentState.PaymentFailed(
                            title = resourceProvider.getString(
                                R.string.woopos_success_totals_payment_failed_title
                            ),
                            subtitle = resourceProvider.getString(event.message),
                            retryButtonLabel = resourceProvider.getString(
                                R.string.woo_pos_payment_failed_try_another_payment_method
                            ),
                            isDismissButtonVisible = false
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    private suspend fun handlePaymentSuccessful() {
        val order = totalsRepository.getOrderById(orderId)
        val orderTotalText = if (order != null) {
            resourceProvider.getString(
                R.string.woopos_totals_success_payment_card,
                priceFormat(order.total)
            )
        } else {
            resourceProvider.getString(R.string.woopos_payment_successful_label)
        }
        _state.value = WooPosCardPaymentState.PaymentSuccess(orderTotalText = orderTotalText)
    }

    private fun buildPaymentFailedState(
        state: CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment
    ): WooPosCardPaymentState.PaymentFailed {
        val isRetryAvailable = state.onRetry != null
        val retryButtonLabel = if (isRetryAvailable) {
            resourceProvider.getString(R.string.woo_pos_payment_failed_try_again)
        } else {
            resourceProvider.getString(R.string.woo_pos_payment_failed_try_another_payment_method)
        }
        return WooPosCardPaymentState.PaymentFailed(
            title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
            subtitle = uiStringParser.asString(state.errorType.message),
            retryButtonLabel = retryButtonLabel,
            isDismissButtonVisible = isRetryAvailable
        )
    }

    private fun buildPreparingState() = WooPosCardPaymentState.Collecting.Preparing(
        title = resourceProvider.getString(R.string.woopos_totals_reader_getting_ready),
        subtitle = resourceProvider.getString(R.string.woopos_totals_reader_preparing_reader_for_payment)
    )

    private fun buildReaderDisconnectedState() = WooPosCardPaymentState.Collecting.ReaderDisconnected(
        title = resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_title),
        subtitle = resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_subtitle),
        actionButtonLabel = resourceProvider.getString(
            R.string.woopos_success_totals_error_reader_not_connected_cta_button_label
        ),
    )

    fun onRetryClicked() {
        val paymentState = cardReaderPaymentController?.paymentState?.value
        if (paymentState is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment &&
            paymentState.onRetry != null
        ) {
            paymentState.onRetry!!()
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
        viewModelScope.launch {
            _navigationEvent.emit(WooPosNavigationEvent.GoBack)
        }
    }

    fun onDismissClicked() {
        cancelPayment()
        viewModelScope.launch {
            _navigationEvent.emit(WooPosNavigationEvent.GoBack)
        }
    }

    fun onConnectReaderClicked() {
        cardReaderFacade.connectToReader()
    }

    fun onDoneClicked() {
        viewModelScope.launch {
            if (source == CardPaymentSource.BOOKINGS) {
                _navigationEvent.emit(
                    WooPosNavigationEvent.GoBackWithResult(
                        BOOKING_CARD_PAYMENT_SUCCESS_KEY,
                        true
                    )
                )
            } else {
                _navigationEvent.emit(WooPosNavigationEvent.GoBack)
            }
        }
    }

    fun onEmailReceiptClicked() {
        viewModelScope.launch {
            analyticsTracker.trackEmailReceiptTapped()
            _navigationEvent.emit(WooPosNavigationEvent.OpenEmailReceipt(orderId))
        }
    }

    private fun cancelPayment() {
        paymentListenerJob?.cancel()
        paymentListenerJob = null
        controllerEventJob?.cancel()
        controllerEventJob = null
        cardReaderPaymentController?.onBackPressed()
        cardReaderPaymentController?.stop()
    }

    override fun onCleared() {
        cardReaderPaymentController?.stop()
    }
}
