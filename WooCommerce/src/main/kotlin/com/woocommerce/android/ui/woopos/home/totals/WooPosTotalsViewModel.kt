package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsCouponsEnabled
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToCashPayment
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NewTransactionClicked
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.OrderSuccessfullyPaidByCard
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.ToastMessageDisplayed
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.PaymentFailed
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.PaymentInProgress
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.Totals
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WooPosTotalsViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val totalsRepository: WooPosTotalsRepository,
    private val priceFormat: WooPosFormatPrice,
    private val networkStatus: WooPosNetworkStatus,
    private val cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory,
    private val uiStringParser: UiStringParser,
    private val isCouponsEnabled: WooPosIsCouponsEnabled,
    private val totalsAnalyticsTracker: WooPosTotalsAnalyticsTracker,
    savedState: SavedStateHandle,
) : ViewModel() {

    private companion object {
        private const val EMPTY_ORDER_ID = -1L
        private const val KEY_STATE = "woo_pos_totals_data_state"
        private val InitialState = WooPosTotalsViewState.Loading
    }

    private val uiState: MutableStateFlow<WooPosTotalsViewState> =
        savedState.getStateFlow(
            scope = viewModelScope,
            initialValue = InitialState,
            key = "woo_pos_totals_view_state"
        )

    val state: StateFlow<WooPosTotalsViewState> = uiState

    private val dataState: MutableStateFlow<TotalsDataState> = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = TotalsDataState(),
        key = KEY_STATE,
    )

    private var isTTPPaymentInProgress: Boolean by TTPPaymentProgressDelegate(savedState)

    private var cardReaderPaymentController: CardReaderPaymentController? = null

    private fun createCardReaderPaymentController(orderId: Long) {
        cardReaderPaymentController = cardReaderPaymentControllerFactory.create(
            orderId = orderId,
            paymentType = PaymentOrRefund.Payment.PaymentType.WOO_POS,
            isTTPPaymentInProgress = ::isTTPPaymentInProgress,
        )
    }

    init {
        listenUpEvents()
        observeCardReaderStatus()
    }

    private fun observeCardReaderStatus() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.combine(
                dataState
            ) { status, data -> Pair(status, data) }.collect { (status, data) ->
                when (status) {
                    is NotConnected, is Connecting -> {
                        val state = uiState.value
                        if (state !is WooPosTotalsViewState.Checkout) return@collect
                        uiState.value = state.copy(readerStatus = buildTotalsReaderNotConnectedError())
                        cancelPaymentAction()
                    }

                    is Connected -> {
                        val state = uiState.value
                        if (state !is WooPosTotalsViewState.Checkout) return@collect
                        uiState.value = state.copy(readerStatus = buildPreparingReaderStatusState())
                        if (data.orderId != EMPTY_ORDER_ID) {
                            collectPayment()
                        }
                    }
                }
            }
        }
    }

    private fun buildPreparingReaderStatusState() = WooPosTotalsViewState.ReaderStatus.Preparing(
        title = resourceProvider.getString(R.string.woopos_totals_reader_getting_ready),
        subtitle = resourceProvider.getString(R.string.woopos_totals_reader_checking_order)
    )

    private fun cancelPaymentAction() {
        cardReaderPaymentController?.onBackPressed()
        cardReaderPaymentController?.stop()
    }

    fun onUIEvent(event: WooPosTotalsUIEvent) {
        when (event) {
            is WooPosTotalsUIEvent.OnNewTransactionClicked -> viewModelScope.launch {
                childrenToParentEventSender.sendToParent(NewTransactionClicked)
                totalsAnalyticsTracker.trackCreateNewOrderTapped()
            }

            is WooPosTotalsUIEvent.RetryOrderCreationClicked -> {
                createOrderDraft(dataState.value.itemClickedDataList)
            }

            WooPosTotalsUIEvent.OnStartReceiptFlowClicked -> handleEmailReceiptClicked()

            WooPosTotalsUIEvent.OnCashPaymentClicked -> informParentAboutNavigatingToCashPayment()

            WooPosTotalsUIEvent.GoBackToCheckoutAfterFailedPayment -> handleGoBackToCheckoutClickedWhenPaymentFailed()

            WooPosTotalsUIEvent.RetryFailedTransactionClicked -> handleRetryFailedTransactionClicked()

            WooPosTotalsUIEvent.ConnectReaderClicked -> cardReaderFacade.connectToReader()

            WooPosTotalsUIEvent.OnBackClicked -> handleBackPress()
        }
    }

    private fun handleEmailReceiptClicked() {
        viewModelScope.launch {
            totalsAnalyticsTracker.trackEmailReceiptTapped()
            childrenToParentEventSender.sendToParent(
                ToEmailReceipt(dataState.value.orderId)
            )
        }
    }

    private fun informParentAboutNavigatingToCashPayment() = viewModelScope.launch {
        childrenToParentEventSender.sendToParent(
            ToCashPayment(dataState.value.orderId)
        )
    }

    private fun handleGoBackToCheckoutClickedWhenPaymentFailed() {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(ChildToParentEvent.GoBackToCheckoutAfterFailedPayment)
            retryPaymentCollectionFromScratch()
        }
    }

    private fun handleRetryFailedTransactionClicked() {
        viewModelScope.launch {
            val paymentState = cardReaderPaymentController?.paymentState?.value
            check(paymentState != null) {
                "Retry failed transaction clicked but payment controller is null"
            }
            check(paymentState is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment) {
                "Retry failed transaction clicked but payment state is not PaymentFailed"
            }
            when {
                paymentState.onRetry != null -> paymentState.onRetry!!()
                else -> {
                    childrenToParentEventSender.sendToParent(ChildToParentEvent.ReturnedFromCardReaderPaymentToCheckout)
                    retryPaymentCollectionFromScratch()
                }
            }
        }
    }

    private fun handleBackPress() {
        viewModelScope.launch {
            when (state.value) {
                is PaymentFailed, is PaymentInProgress -> {
                    val paymentState = cardReaderPaymentController?.paymentState?.value
                    if (paymentState is CardReaderPaymentState.ProcessingPayment ||
                        paymentState is CardReaderPaymentState.PaymentCapturing
                    ) {
                        return@launch
                    }

                    childrenToParentEventSender.sendToParent(ChildToParentEvent.ReturnedFromCardReaderPaymentToCheckout)
                    retryPaymentCollectionFromScratch()
                }

                else -> childrenToParentEventSender.sendToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
            }
        }
    }

    private suspend fun retryPaymentCollectionFromScratch() {
        cancelPaymentAction()
        val order = totalsRepository.getOrderById(dataState.value.orderId)
        checkNotNull(order)
        uiState.value = buildWooPosTotalsViewState(order)
        collectPayment()
    }

    private fun collectPayment() {
        if (!networkStatus.isConnected()) {
            viewModelScope.launch {
                childrenToParentEventSender.sendToParent(
                    ToastMessageDisplayed(
                        message = resourceProvider.getString(R.string.woopos_no_internet_message)
                    )
                )
            }
        } else {
            val orderId = dataState.value.orderId
            check(orderId != EMPTY_ORDER_ID)
            if (
                cardReaderFacade.readerStatus.value is Connected &&
                dataState.value.orderTotal?.compareTo(BigDecimal.ZERO) == 1
            ) {
                val state = uiState.value
                check(state is WooPosTotalsViewState.Checkout)
                check(uiState.value is WooPosTotalsViewState.Checkout)
                createCardReaderPaymentController(dataState.value.orderId)
                cardReaderPaymentController?.start()
                listenToPaymentState()
            }
        }
    }

    private fun listenUpEvents() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.CheckoutClicked -> {
                        dataState.value = dataState.value.copy(itemClickedDataList = event.itemClickedDataList)
                        createOrderDraft(dataState.value.itemClickedDataList)
                        totalsAnalyticsTracker.incrementCheckoutButtonTaps()
                    }

                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        cancelPaymentAction()
                        uiState.value = InitialState
                    }

                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> {
                        if (event.paymentMethod == PaymentMethod.CASH) {
                            // Cancel payment intent if order is marked completed by cash
                            cancelPaymentAction()
                        }
                        showSuccessfulPaymentState(event.paymentMethod)
                    }

                    is ParentToChildrenEvent.ItemClickedInProductSelector,
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery,
                    ParentToChildrenEvent.SearchEvent.Finished,
                    ParentToChildrenEvent.SearchEvent.Started -> Unit
                }
            }
        }
    }

    private fun listenToPaymentState() {
        viewModelScope.launch {
            cardReaderPaymentController?.paymentState?.collect { paymentState ->
                when (paymentState) {
                    is CardReaderPaymentState.CollectingPayment -> handleCollectingPaymentState(paymentState)

                    is CardReaderPaymentState.LoadingData -> handleReaderLoadingPaymentState()

                    is CardReaderPaymentState.PaymentCapturing,
                    is CardReaderPaymentState.ProcessingPayment -> {
                        handleProcessingOrCapturingPaymentState()
                    }

                    is CardReaderPaymentState.PaymentSuccessful -> {
                        childrenToParentEventSender.sendToParent(OrderSuccessfullyPaidByCard)
                    }

                    is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment -> {
                        uiState.value = buildPaymentFailedState(paymentState)
                        childrenToParentEventSender.sendToParent(ChildToParentEvent.PaymentFailed)
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
        viewModelScope.launch { totalsAnalyticsTracker.trackPaymentStates(cardReaderPaymentController?.paymentState) }
    }

    private suspend fun handleProcessingOrCapturingPaymentState() {
        val state = uiState.value
        if (state is WooPosTotalsViewState.Checkout) {
            uiState.value = state.copy(totals = Totals.Hidden)
            // allow the UI to show "shrinking" exit animation of totals grid before showing
            // the "payment in progress" state.
            @Suppress("MagicNumber")
            delay(384)
        }
        uiState.value = buildPaymentInProgressState()
        childrenToParentEventSender.sendToParent(ChildToParentEvent.PaymentInProgress)
        childrenToParentEventSender.sendToParent(
            NavigationEvent.ReturnHomeFromCashWhenCardPaymentStarted
        )
    }

    private suspend fun handleCollectingPaymentState(paymentState: CardReaderPaymentState.CollectingPayment) {
        val totalsState = uiState.value
        if (totalsState is WooPosTotalsViewState.Checkout) {
            uiState.value = totalsState.copy(
                readerStatus = WooPosTotalsViewState.ReaderStatus.ReadyForPayment(
                    title = resourceProvider.getString(R.string.woopos_totals_reader_ready_for_payment_title),
                    subtitle = resourceProvider.getString(
                        paymentState.cardReaderHint ?: R.string.woopos_totals_reader_ready_for_payment_subtitle
                    )
                )
            )
        } else {
            val order = totalsRepository.getOrderById(dataState.value.orderId)
            checkNotNull(order)
            uiState.value = buildWooPosTotalsViewState(order)
            childrenToParentEventSender.sendToParent(ChildToParentEvent.PaymentCollecting)
        }
    }

    private suspend fun handleReaderLoadingPaymentState() {
        val totalsState = uiState.value
        if (totalsState is WooPosTotalsViewState.Checkout) {
            uiState.value = totalsState.copy(
                readerStatus =
                WooPosTotalsViewState.ReaderStatus.Preparing(
                    title = resourceProvider.getString(R.string.woopos_totals_reader_getting_ready),
                    subtitle = resourceProvider.getString(R.string.woopos_totals_reader_preparing_reader_for_payment)
                )
            )
        } else {
            val order = totalsRepository.getOrderById(dataState.value.orderId)
            checkNotNull(order)
            uiState.value = buildWooPosTotalsViewState(order)
            childrenToParentEventSender.sendToParent(ChildToParentEvent.PaymentCollecting)
        }
    }

    private fun buildPaymentFailedState(
        state: CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment
    ): PaymentFailed {
        val isRetryAvailable = state.onRetry != null
        val retryButtonLabel = if (isRetryAvailable) {
            resourceProvider.getString(R.string.woo_pos_payment_failed_try_again)
        } else {
            resourceProvider.getString(R.string.woo_pos_payment_failed_try_another_payment_method)
        }
        return PaymentFailed(
            title = resourceProvider.getString(
                R.string.woopos_success_totals_payment_failed_title
            ),
            subtitle = uiStringParser.asString(state.errorType.message),
            retryPaymentButtonLabel = retryButtonLabel,
            isReturnToCheckoutButtonVisible = isRetryAvailable
        )
    }

    private fun buildPaymentInProgressState(): PaymentInProgress {
        return PaymentInProgress(
            title = resourceProvider.getString(
                R.string.woopos_success_totals_payment_processing_title
            ),
            subtitle = resourceProvider.getString(R.string.woopos_success_totals_payment_processing_subtitle)
        )
    }

    override fun onCleared() {
        cardReaderPaymentController?.stop()
    }

    private fun createOrderDraft(itemClickedDataList: List<WooPosItemsViewModel.ItemClickedData>) {
        viewModelScope.launch {
            uiState.value = WooPosTotalsViewState.Loading

            totalsRepository.createOrderFromCartItems(itemClickedDataList = itemClickedDataList)
                .fold(
                    onSuccess = { order ->
                        dataState.value = dataState.value.copy(
                            orderId = order.id,
                            orderTotal = order.total
                        )
                        uiState.value = buildWooPosTotalsViewState(order)
                        totalsAnalyticsTracker.trackOrderCreationSuccess()
                        collectPayment()
                    },
                    onFailure = { error ->
                        WooLog.e(T.POS, "Order creation failed - $error")
                        uiState.value = WooPosTotalsViewState.Error(
                            resourceProvider.getString(R.string.woopos_totals_order_creation_error)
                        )
                        totalsAnalyticsTracker.trackOrderCreationFailed(error)
                    }
                )
        }
    }

    private fun showSuccessfulPaymentState(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            val dataState = dataState.value
            checkNotNull(dataState.orderTotal)
            val template = when (paymentMethod) {
                PaymentMethod.CARD -> R.string.woopos_totals_success_payment_card
                PaymentMethod.CASH -> R.string.woopos_totals_success_payment_cash
            }
            val orderTotalText = resourceProvider.getString(
                template,
                priceFormat(dataState.orderTotal)
            )
            uiState.value = WooPosTotalsViewState.PaymentSuccess(
                orderTotalText = orderTotalText
            )
        }
    }

    private suspend fun buildWooPosTotalsViewState(order: Order): WooPosTotalsViewState.Checkout {
        val discountAmount = order.discountTotal
        val subtotalAmount = order.productsTotal
        val taxAmount = order.totalTax
        val totalAmount = order.total
        val readerStatus = if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            WooPosTotalsViewState.ReaderStatus.Unavailable
        } else {
            when (cardReaderFacade.readerStatus.value) {
                is Connected -> buildPreparingReaderStatusState()
                else -> buildTotalsReaderNotConnectedError()
            }
        }
        return WooPosTotalsViewState.Checkout(
            totals = Totals.Visible(
                orderDiscountText =
                if (isCouponsEnabled() && discountAmount > BigDecimal.ZERO) priceFormat(discountAmount) else null,
                orderSubtotalText = priceFormat(subtotalAmount),
                orderTaxText = priceFormat(taxAmount),
                orderTotalText = priceFormat(totalAmount),
            ),
            readerStatus = readerStatus,
        )
    }

    private fun buildTotalsReaderNotConnectedError(): WooPosTotalsViewState.ReaderStatus.Disconnected =
        WooPosTotalsViewState.ReaderStatus.Disconnected(
            title = resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_title),
            subtitle = resourceProvider.getString(R.string.woopos_success_totals_error_reader_not_connected_subtitle),
            actionButtonLabel = resourceProvider.getString(
                R.string.woopos_success_totals_error_reader_not_connected_cta_button_label
            ),
        )

    @Parcelize
    private data class TotalsDataState(
        val orderId: Long = EMPTY_ORDER_ID,
        val orderTotal: BigDecimal? = null,
        val itemClickedDataList: List<WooPosItemsViewModel.ItemClickedData> = emptyList()
    ) : Parcelable
}
