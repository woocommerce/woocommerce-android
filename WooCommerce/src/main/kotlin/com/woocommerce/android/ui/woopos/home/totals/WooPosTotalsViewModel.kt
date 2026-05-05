package com.woocommerce.android.ui.woopos.home.totals

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connecting
import com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Reconnecting
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.woopos.cardreader.BuiltInReaderDiscoveryFailedException
import com.woocommerce.android.ui.woopos.cardreader.MissingFineLocationPermissionException
import com.woocommerce.android.ui.woopos.cardreader.WooPosBuiltInReaderConnector
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.cardreader.WooPosIsTapToPayAvailable
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToCashPayment
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent.ToEmailReceipt
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.OnNewTransactionStarted
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.OrderSuccessfullyPaidByCard
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.ToastMessageDisplayed
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosOrderCreatedData
import com.woocommerce.android.ui.woopos.home.WooPosOrderCreatedData.CouponInfo
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.PaymentFailed
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.PaymentInProgress
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState.Totals
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIncrementalSyncReason
import com.woocommerce.android.ui.woopos.localcatalog.WooPosPerformLocalCatalogIncrementalSync
import com.woocommerce.android.ui.woopos.util.WooPosNetworkStatus
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.UiStringParser
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
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
    private val totalsAnalyticsTracker: WooPosTotalsAnalyticsTracker,
    private val wooPosLogWrapper: WooPosLogWrapper,
    private val performIncrementalSyncUseCase: WooPosPerformLocalCatalogIncrementalSync,
    private val isTapToPayAvailable: WooPosIsTapToPayAvailable,
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus,
    private val paymentsFlowTracker: PaymentsFlowTracker,
    private val builtInReaderConnector: WooPosBuiltInReaderConnector,
    savedState: SavedStateHandle,
) : ViewModel() {

    private companion object {
        private const val EMPTY_ORDER_ID = -1L
        private const val KEY_STATE = "woo_pos_totals_data_state"
        private const val TAP_TO_PAY_SOURCE = "woo_pos_checkout"
        private val InitialState = WooPosTotalsViewState.Loading
    }

    private val uiState: MutableStateFlow<WooPosTotalsViewState> =
        savedState.getStateFlow(
            scope = viewModelScope,
            initialValue = InitialState,
            key = "woo_pos_totals_view_state"
        )

    val state: StateFlow<WooPosTotalsViewState> = uiState

    private val _screenEvents = MutableSharedFlow<WooPosTotalsScreenEvent>(extraBufferCapacity = 1)
    val screenEvents: SharedFlow<WooPosTotalsScreenEvent> = _screenEvents.asSharedFlow()

    private var createDraftOrderJob: Job? = null
    private var newOrder: Order? = null
    private var deletedVariationFromApiError: Long? = null

    private val dataState: MutableStateFlow<TotalsDataState> = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = TotalsDataState(),
        key = KEY_STATE,
    )

    private var isTTPPaymentInProgress: Boolean by TTPPaymentProgressDelegate(savedState)

    private var cardReaderPaymentController: CardReaderPaymentController? = null

    private var isTapToPayPayment: Boolean = false

    private fun createCardReaderPaymentController(
        orderId: Long,
        cardReaderType: CardReaderType = CardReaderType.EXTERNAL,
    ) {
        cardReaderPaymentController = cardReaderPaymentControllerFactory.create(
            orderId = orderId,
            paymentType = PaymentOrRefund.Payment.PaymentType.WOO_POS,
            isTTPPaymentInProgress = ::isTTPPaymentInProgress,
            cardReaderType = cardReaderType,
        )
    }

    init {
        listenUpEvents()
        observeCardReaderStatus()
        (tapToPayAvailabilityStatus() as? TapToPayAvailabilityStatus.Result.NotAvailable)?.let {
            paymentsFlowTracker.trackTapToPayNotAvailableReason(it, TAP_TO_PAY_SOURCE)
        }
    }

    private fun observeCardReaderStatus() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.combine(
                dataState
            ) { status, data -> Pair(status, data) }.collect { (status, data) ->
                if (isTapToPayPayment) return@collect
                when (status) {
                    is NotConnected, is Connecting -> {
                        val state = uiState.value
                        if (state !is WooPosTotalsViewState.Checkout) return@collect
                        uiState.value = state.copy(readerStatus = buildTotalsReaderNotConnectedError())
                        cancelPaymentAction()
                    }

                    Reconnecting -> {
                        // We start payment right away so this state not worth handling
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
        if (isTapToPayPayment) {
            isTTPPaymentInProgress = false
        }
        isTapToPayPayment = false
    }

    private fun cancelCreateOrderDraftAction() {
        createDraftOrderJob?.cancel()
    }

    fun onUIEvent(event: WooPosTotalsUIEvent) {
        when (event) {
            is WooPosTotalsUIEvent.OnNewTransactionClicked -> viewModelScope.launch {
                childrenToParentEventSender.sendToParent(OnNewTransactionStarted)
                totalsAnalyticsTracker.trackCreateNewOrderTapped()
            }

            is WooPosTotalsUIEvent.RetryOrderCreationClicked -> {
                createOrderDraft(dataState.value.itemClickedDataList)
            }

            WooPosTotalsUIEvent.OnStartReceiptFlowClicked -> handleEmailReceiptClicked()

            WooPosTotalsUIEvent.OnCashPaymentClicked -> handleCashPaymentClicked()

            WooPosTotalsUIEvent.OnTapToPayClicked,
            is WooPosTotalsUIEvent.OnFineLocationPermissionResult,
            is WooPosTotalsUIEvent.OnAllPaymentMethodsVisibilityChanged -> handleNewPaymentMethodEvent(event)

            WooPosTotalsUIEvent.GoBackToCheckoutAfterFailedPayment -> handleGoBackToCheckoutClickedWhenPaymentFailed()

            WooPosTotalsUIEvent.RetryFailedTransactionClicked -> handleRetryFailedTransactionClicked()

            WooPosTotalsUIEvent.ConnectReaderClicked -> {
                viewModelScope.launch {
                    childrenToParentEventSender.sendToParent(ChildToParentEvent.ShowCardReaderConnectionDialog)
                }
            }

            WooPosTotalsUIEvent.OnBackClicked -> handleBackPress()

            WooPosTotalsUIEvent.GoBackToCheckoutAfterFailedCouponValidation -> handleEditOrderClicked()

            WooPosTotalsUIEvent.OnRemoveCouponsClicked -> handleRemoveCouponsClicked()

            WooPosTotalsUIEvent.GoBackToOrderEditAfterProductNotFound -> handleEditOrderClickedAfterProductNotFound()

            WooPosTotalsUIEvent.OnRemoveProductsClicked -> handleRemoveProductsClicked()
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

    private fun handleCashPaymentClicked() = viewModelScope.launch {
        totalsAnalyticsTracker.trackCashPaymentTapped()
        childrenToParentEventSender.sendToParent(
            ToCashPayment(dataState.value.orderId)
        )
    }

    private fun handleNewPaymentMethodEvent(event: WooPosTotalsUIEvent) {
        when (event) {
            WooPosTotalsUIEvent.OnTapToPayClicked -> startTapToPayPayment()
            is WooPosTotalsUIEvent.OnFineLocationPermissionResult -> onFineLocationPermissionResult(event.granted)
            is WooPosTotalsUIEvent.OnAllPaymentMethodsVisibilityChanged -> {
                val checkout = uiState.value as? WooPosTotalsViewState.Checkout ?: return
                uiState.value = checkout.copy(isAllPaymentMethodsDialogVisible = event.isVisible)
            }
            else -> Unit
        }
    }

    private fun startTapToPayPayment() {
        if (isTapToPayPayment) return
        viewModelScope.launch {
            totalsAnalyticsTracker.trackTapToPayEntryPointTapped()

            val orderId = dataState.value.orderId
            if (orderId == EMPTY_ORDER_ID) {
                wooPosLogWrapper.e("Tap to Pay tapped before order draft was created")
                return@launch
            }
            if (!networkStatus.isConnected()) {
                childrenToParentEventSender.sendToParent(
                    ToastMessageDisplayed(resourceProvider.getString(R.string.woopos_no_internet_message))
                )
                return@launch
            }

            attemptTapToPayConnect(orderId)
        }
    }

    private suspend fun attemptTapToPayConnect(orderId: Long) {
        isTapToPayPayment = true
        setTapToPayInProgress(true)
        builtInReaderConnector.connect().fold(
            onSuccess = {
                createCardReaderPaymentController(orderId, CardReaderType.BUILT_IN)
                cardReaderPaymentController?.start()
                listenToPaymentState()
            },
            onFailure = { error ->
                wooPosLogWrapper.e("Tap to Pay connection failed", error)
                isTapToPayPayment = false
                setTapToPayInProgress(false)
                when (error) {
                    is MissingFineLocationPermissionException ->
                        _screenEvents.tryEmit(WooPosTotalsScreenEvent.RequestFineLocationPermission)

                    is BuiltInReaderDiscoveryFailedException -> {
                        val detail = error.message?.takeIf { it.isNotBlank() }
                        val message = if (detail != null) {
                            resourceProvider.getString(
                                R.string.woopos_tap_to_pay_payment_failed_with_reason_message,
                                detail,
                            )
                        } else {
                            resourceProvider.getString(R.string.woopos_tap_to_pay_payment_failed_message)
                        }
                        childrenToParentEventSender.sendToParent(ToastMessageDisplayed(message))
                    }

                    else -> childrenToParentEventSender.sendToParent(
                        ToastMessageDisplayed(
                            resourceProvider.getString(R.string.woopos_tap_to_pay_payment_failed_message)
                        )
                    )
                }
            }
        )
    }

    private fun setTapToPayInProgress(inProgress: Boolean) {
        val checkout = uiState.value as? WooPosTotalsViewState.Checkout ?: return
        if (checkout.isTapToPayInProgress != inProgress) {
            uiState.value = checkout.copy(isTapToPayInProgress = inProgress)
        }
    }

    private fun onFineLocationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                val orderId = dataState.value.orderId
                if (orderId != EMPTY_ORDER_ID) {
                    attemptTapToPayConnect(orderId)
                }
            } else {
                childrenToParentEventSender.sendToParent(
                    ToastMessageDisplayed(
                        resourceProvider.getString(R.string.woopos_tap_to_pay_missing_location_permission_message)
                    )
                )
            }
        }
    }

    private fun handleGoBackToCheckoutClickedWhenPaymentFailed() {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(ChildToParentEvent.GoBackToCheckoutAfterFailedPayment)
            if (lastFailedPaymentWasTapToPay()) {
                returnToCheckoutAfterTapToPayFailed()
            } else {
                retryPaymentCollectionFromScratch()
            }
        }
    }

    private fun lastFailedPaymentWasTapToPay(): Boolean =
        cardReaderPaymentController?.paymentState?.value is
            CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment

    private suspend fun returnToCheckoutAfterTapToPayFailed() {
        cardReaderPaymentController?.stop()
        cardReaderPaymentController = null
        isTapToPayPayment = false
        isTTPPaymentInProgress = false
        childrenToParentEventSender.sendToParent(
            ChildToParentEvent.ReturnedFromCardReaderPaymentToCheckout
        )
        val order = totalsRepository.getOrderById(dataState.value.orderId)
        checkNotNull(order)
        uiState.value = buildWooPosTotalsViewState(order)
    }

    private fun handleRetryFailedTransactionClicked() {
        viewModelScope.launch {
            val paymentState = cardReaderPaymentController?.paymentState?.value
            when (paymentState) {
                is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment -> {
                    when {
                        paymentState.onRetry != null -> paymentState.onRetry!!()
                        else -> {
                            childrenToParentEventSender.sendToParent(
                                ChildToParentEvent.ReturnedFromCardReaderPaymentToCheckout
                            )
                            retryPaymentCollectionFromScratch()
                        }
                    }
                }

                is CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment -> {
                    val sdkRetry = paymentState.onRetry
                    if (sdkRetry != null) {
                        sdkRetry.invoke()
                    } else {
                        returnToCheckoutAfterTapToPayFailed()
                    }
                }

                else -> error("Retry failed transaction clicked but payment state is not PaymentFailed")
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

                else -> {
                    cancelCreateOrderDraftAction()
                    childrenToParentEventSender.sendToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
                }
            }
        }
    }

    private fun handleEditOrderClicked() {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
        }
    }

    private fun handleEditOrderClickedAfterProductNotFound() {
        viewModelScope.launch {
            totalsAnalyticsTracker.trackCheckoutOutdatedItemDetectedEditOrderTapped()
            childrenToParentEventSender.sendToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
        }
    }

    private fun handleRemoveCouponsClicked() {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(ChildToParentEvent.RemoveCouponsClicked)
        }
    }

    private fun handleRemoveProductsClicked() {
        viewModelScope.launch {
            totalsAnalyticsTracker.trackCheckoutOutdatedItemDetectedRemoveTapped()
            val itemIdsToRemove: List<Long> = deletedVariationFromApiError?.let { missingVariation ->
                listOf(missingVariation)
            } ?: getProductsIdsMissingFromOrder(requireNotNull(newOrder))

            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.RemoveProductsClicked(itemIdsToRemove)
            )
            deletedVariationFromApiError = null
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
                        onCartDataReceived(event.itemClickedDataList)
                        totalsAnalyticsTracker.incrementCheckoutButtonTaps()
                    }

                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> {
                        cancelPaymentAction()
                        cancelCreateOrderDraftAction()
                        uiState.value = InitialState
                    }

                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> {
                        if (event.paymentMethod == PaymentMethod.CASH) {
                            // Cancel payment intent if order is marked completed by cash
                            cancelPaymentAction()
                        }
                        showSuccessfulPaymentState(event.paymentMethod)
                        performIncrementalSyncUseCase.execute(WooPosIncrementalSyncReason.AFTER_SUCCESSFUL_PAYMENT)
                    }

                    is ParentToChildrenEvent.CouponsRemoved -> {
                        onCartDataReceived(event.cartDataList)
                    }

                    is ParentToChildrenEvent.ProductsRemoved -> {
                        if (event.cartDataList.isNotEmpty()) {
                            onCartDataReceived(event.cartDataList)
                        } else {
                            childrenToParentEventSender.sendToParent(
                                ChildToParentEvent.BackFromCheckoutToCartClicked
                            )
                        }
                    }

                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected,
                    is ParentToChildrenEvent.ItemClickedInItemsList,
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery,
                    ParentToChildrenEvent.SearchEvent.Finished,
                    is ParentToChildrenEvent.OrderCreated,
                    ParentToChildrenEvent.SearchEvent.Started,
                    ParentToChildrenEvent.RemoveCouponsClicked,
                    ParentToChildrenEvent.RefreshProductList,
                    ParentToChildrenEvent.CouponsValidationFailed,
                    is ParentToChildrenEvent.BarcodeEvent,
                    is ParentToChildrenEvent.RemoveProductsClicked,
                    is ParentToChildrenEvent.MissingVariationEvent,
                    is ParentToChildrenEvent.SettingsEvent -> Unit
                }
            }
        }
    }

    private fun onCartDataReceived(newCartData: List<WooPosItemsViewModel.ItemClickedData>) {
        deletedVariationFromApiError = null
        dataState.value = dataState.value.copy(itemClickedDataList = newCartData)
        createOrderDraft(dataState.value.itemClickedDataList)
    }

    private fun listenToPaymentState() {
        viewModelScope.launch {
            cardReaderPaymentController?.paymentState?.collect { paymentState ->
                when (paymentState) {
                    is CardReaderPaymentState.ProcessingPayment -> {
                        if (isTapToPayPayment) return@collect
                        handleProcessingPaymentState(paymentState)
                    }

                    is CardReaderPaymentState.LoadingData -> {
                        if (isTapToPayPayment) return@collect
                        handleReaderLoadingPaymentState()
                    }

                    is CardReaderPaymentState.PaymentCapturing -> {
                        if (isTapToPayPayment) return@collect
                        handleCapturingPaymentState()
                    }

                    is CardReaderPaymentState.PaymentSuccessful -> {
                        if (isTapToPayPayment) {
                            isTTPPaymentInProgress = false
                        }
                        isTapToPayPayment = false
                        setTapToPayInProgress(false)
                        childrenToParentEventSender.sendToParent(OrderSuccessfullyPaidByCard)
                    }

                    is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment -> {
                        uiState.value = buildPaymentFailedState(paymentState)
                        childrenToParentEventSender.sendToParent(ChildToParentEvent.PaymentFailed)
                    }

                    is CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment -> {
                        wooPosLogWrapper.e("Tap to Pay payment failed: ${paymentState.errorType}")
                        isTTPPaymentInProgress = false
                        isTapToPayPayment = false
                        uiState.value = buildBuiltInPaymentFailedState(paymentState)
                        childrenToParentEventSender.sendToParent(ChildToParentEvent.PaymentFailed)
                    }

                    CardReaderPaymentState.ReFetchingOrder -> Unit

                    is CardReaderPaymentOrRefundState.CardReaderInteracRefundState,
                    is CardReaderPaymentState.PrintingReceipt,
                    CardReaderPaymentState.SharingReceipt -> {
                        throw IllegalArgumentException("Payment state: $paymentState not compatible with POS")
                    }
                }
            }
        }
        viewModelScope.launch { totalsAnalyticsTracker.trackPaymentStates(cardReaderPaymentController?.paymentState) }
    }

    private suspend fun handleCapturingPaymentState() {
        val state = uiState.value
        if (state is WooPosTotalsViewState.Checkout) {
            uiState.value = state.copy(totals = Totals.Hidden)
            @Suppress("MagicNumber")
            delay(384)
        }
        uiState.value = buildPaymentInProgressState()
        childrenToParentEventSender.sendToParent(ChildToParentEvent.PaymentInProgress)
        childrenToParentEventSender.sendToParent(
            NavigationEvent.ReturnHomeFromCashWhenCardPaymentStarted
        )
    }

    private suspend fun handleProcessingPaymentState(paymentState: CardReaderPaymentState.ProcessingPayment) {
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
                readerStatus = WooPosTotalsViewState.ReaderStatus.Preparing(
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

    private fun buildBuiltInPaymentFailedState(
        state: CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment
    ): PaymentFailed {
        val isRetryAvailable = state.onRetry != null
        val retryButtonLabel = if (isRetryAvailable) {
            resourceProvider.getString(R.string.woo_pos_payment_failed_try_again)
        } else {
            resourceProvider.getString(R.string.woo_pos_payment_failed_try_another_payment_method)
        }
        return PaymentFailed(
            title = resourceProvider.getString(R.string.woopos_success_totals_payment_failed_title),
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
        createDraftOrderJob?.cancel()
        createDraftOrderJob = viewModelScope.launch {
            uiState.value = WooPosTotalsViewState.Loading

            totalsRepository.createOrderFromCartItems(itemClickedDataList = itemClickedDataList)
                .fold(
                    onSuccess = { order -> handleCreatedOrder(order) },
                    onFailure = { error ->
                        onCreateOrderDraftFails(error)
                    }
                )
            createDraftOrderJob = null
        }
    }

    private suspend fun onCreateOrderDraftFails(exception: Throwable) {
        wooPosLogWrapper.e("Order creation failed - $exception")
        val wooError = (exception as? WooException)?.error

        when {
            wooError != null && wooError.type == WooErrorType.INVALID_COUPON -> {
                uiState.value = WooPosTotalsViewState.InvalidCouponError(
                    message = resourceProvider.getString(R.string.woopos_totals_invalid_coupon_error),
                    reason = wooError.message ?: ""
                )
                childrenToParentEventSender.sendToParent(ChildToParentEvent.CouponsValidationFailed)
            }
            wooError != null && wooError.type == WooErrorType.INVALID_VARIATION_ID -> {
                /**
                 * When a variation gets deleted, the server might react in three ways.
                 * 1. Returns an error with `variation_id` in the error data, indicating which variation is invalid.
                 * 2. Returns an error without error data - versions of Woo before the variation_id was added.
                 * 3. Doesn't return an error but the variation is missing from the order - happens on all versions of
                 * Woo and depends on timing of cache. The server knows roughly for 24 hours that a variation was
                 * deleted, but after this period it can't differentiate between deleted variation and variation
                 * that never existed, so it returns a successfully created order but the variation is ignored (this
                 * is consistent behavior with permanently deleted products).
                 */
                if (wooError.errorData?.has("variation_id") == true) {
                    wooError.errorData?.optLong("variation_id", 0L)?.takeIf { it > 0L }?.let {
                        deletedVariationFromApiError = it
                        childrenToParentEventSender.sendToParent(ChildToParentEvent.MissingVariationEvent(it))
                    }
                }

                totalsAnalyticsTracker.trackCheckoutOutdatedItemDetectedScreenShown()
                uiState.value = WooPosTotalsViewState.ProductNotFoundError(
                    message = resourceProvider.getString(R.string.woopos_totals_product_not_found_error),
                    reason = resourceProvider.getString(R.string.woopos_totals_product_not_found_reason),
                    isRemoveProductSupported = deletedVariationFromApiError != null
                )
            }
            else -> {
                uiState.value = WooPosTotalsViewState.Error(
                    resourceProvider.getString(R.string.woopos_totals_order_creation_error)
                )
            }
        }
        totalsAnalyticsTracker.trackOrderCreationFailed(exception)
    }

    private suspend fun handleCreatedOrder(order: Order) {
        totalsAnalyticsTracker.trackOrderCreationSuccess()
        notifyCartAboutOrderCreation(order)
        val notFoundProductIds = getProductsIdsMissingFromOrder(order)

        if (notFoundProductIds.isNotEmpty()) {
            newOrder = order
            uiState.value = WooPosTotalsViewState.ProductNotFoundError(
                message = resourceProvider.getString(R.string.woopos_totals_product_not_found_error),
                reason = resourceProvider.getString(R.string.woopos_totals_product_not_found_reason),
                isRemoveProductSupported = true,
            )
            totalsAnalyticsTracker.trackCheckoutOutdatedItemDetectedScreenShown()
            return
        }
        readyToCollectPayment(order)
    }

    private suspend fun readyToCollectPayment(order: Order) {
        dataState.value = dataState.value.copy(
            orderId = order.id,
            orderTotal = order.total
        )
        uiState.value = buildWooPosTotalsViewState(order)
        collectPayment()
    }

    private fun getProductsIdsMissingFromOrder(order: Order): List<Long> {
        val productsInCart = dataState.value.itemClickedDataList
            .filterIsInstance<WooPosItemsViewModel.ItemClickedData.Product>()

        val orderProductIds: Set<Long> = order.items
            .map { it.productId }
            .toSet()

        val orderVariationIds: Set<Long> = order.items
            .mapNotNull { it.variationId.takeIf { id -> id > 0 } }
            .toSet()

        return productsInCart.filter { product ->
            when (product) {
                is WooPosItemsViewModel.ItemClickedData.Product.Simple ->
                    product.id !in orderProductIds
                is WooPosItemsViewModel.ItemClickedData.Product.Variation ->
                    product.id !in orderVariationIds
            }
        }.map { it.id }
    }

    private fun notifyCartAboutOrderCreation(order: Order) {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.OrderCreated(
                    WooPosOrderCreatedData(
                        updatedProducts = mapItemLines(order),
                        updatedCoupons = mapCouponLines(order)
                    )
                )
            )
        }
    }

    private fun mapItemLines(order: Order) = order.items.map {
        val basePrice = if (order.pricesIncludeTax) {
            it.subtotal + it.subtotalTax
        } else {
            it.subtotal
        }
        when {
            it.variationId == 0L -> {
                WooPosOrderCreatedData.ProductInfo.Simple(
                    id = it.productId,
                    name = it.name,
                    finalPrice = it.price,
                    basePrice = basePrice,
                    quantity = it.quantity
                )
            }

            else -> {
                WooPosOrderCreatedData.ProductInfo.Variation(
                    id = it.productId,
                    name = it.name,
                    finalPrice = it.price,
                    quantity = it.quantity,
                    basePrice = basePrice,
                    variationId = it.variationId
                )
            }
        }
    }

    private fun mapCouponLines(order: Order) = order.couponLines
        .mapNotNull { coupon ->
            coupon.takeIf { it.id != null && !it.discount.isNullOrEmpty() }?.let {
                try {
                    CouponInfo(
                        id = requireNotNull(it.id),
                        code = it.code,
                        discountAmount = BigDecimal(it.discount)
                    )
                } catch (e: NumberFormatException) {
                    wooPosLogWrapper.e(
                        "Parsing coupon failed, discount: ${it.discount}, code: ${it.code}, id: ${it.id}, $e"
                    )
                    null
                }
            } ?: null.also {
                wooPosLogWrapper.e(
                    "Coupon info is null or empty: ${coupon.code}, coupon id: ${coupon.id}"
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
                orderDiscountText = if (discountAmount > BigDecimal.ZERO) {
                    "-${priceFormat(discountAmount)}"
                } else {
                    null
                },
                orderSubtotalText = priceFormat(subtotalAmount),
                orderTaxText = priceFormat(taxAmount),
                orderTotalText = priceFormat(totalAmount),
            ),
            readerStatus = readerStatus,
            isTapToPayAvailable = isTapToPayAvailable(),
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
