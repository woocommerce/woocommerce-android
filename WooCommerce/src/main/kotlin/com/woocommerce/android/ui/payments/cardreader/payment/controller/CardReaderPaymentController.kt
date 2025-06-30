package com.woocommerce.android.ui.payments.cardreader.payment.controller

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.CollectingInteracRefund
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InitializingInteracRefund
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InteracRefundFailure
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InteracRefundSuccess
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.ProcessingInteracRefund
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.CARD_REMOVED_TOO_EARLY
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.CHECK_MOBILE_DEVICE
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.INSERT_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.INSERT_OR_SWIPE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.MULTIPLE_CONTACTLESS_CARDS_DETECTED
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.REMOVE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.RETRY_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.SWIPE_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.TRY_ANOTHER_CARD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.TRY_ANOTHER_READ_METHOD
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPaymentCompleted
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.RefundConfig
import com.woocommerce.android.cardreader.payments.RefundParams
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderInteracRefundErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderInteracRefundableChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.cardreader.payment.InteracRefundFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderInteracRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.reflect.KMutableProperty0

private const val ARTIFICIAL_RETRY_DELAY = 500L
private const val CANADA_FEE_FLAT_IN_CENTS = 15L

@Suppress("LongParameterList", "LargeClass")
class CardReaderPaymentController(
    private val cardReaderManager: CardReaderManager,
    private val orderRepository: OrderDetailRepository,
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefs = AppPrefs,
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker,
    private val interacRefundableChecker: CardReaderInteracRefundableChecker,
    private val tracker: PaymentsFlowTracker,
    private val trackCancelledFlow: CardReaderTrackCanceledFlowAction,
    private val currencyFormatter: CurrencyFormatter,
    private val errorMapper: CardReaderPaymentErrorMapper,
    private val interacRefundErrorMapper: CardReaderInteracRefundErrorMapper,
    private val wooStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers,
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper,
    private val paymentStateProvider: CardReaderPaymentStateProvider,
    private val cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper,
    private val paymentReceiptHelper: PaymentReceiptHelper,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    private val paymentReceiptShare: PaymentReceiptShare,
    private val paymentOrRefund: CardReaderFlowParam.PaymentOrRefund,
    private val cardReaderType: CardReaderType,
    private val isTTPPaymentInProgress: KMutableProperty0<Boolean>,
) {
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _paymentState: MutableStateFlow<CardReaderPaymentOrRefundState> =
        MutableStateFlow(CardReaderPaymentState.LoadingData(::onCancelPaymentFlow))
    val paymentState: StateFlow<CardReaderPaymentOrRefundState> = _paymentState

    private var paymentFlowJob: Job? = null
    private var refundFlowJob: Job? = null
    private var paymentDataForRetry: PaymentData? = null

    private var refetchOrderJob: Job? = null

    private val _event: MutableSharedFlow<CardReaderPaymentEvent> = MutableSharedFlow()
    val event: Flow<CardReaderPaymentEvent> = _event

    private fun triggerEvent(event: CardReaderPaymentEvent) = scope.launch {
        _event.emit(event)
    }

    fun start() {
        scope.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        if (cardReaderManager.readerStatus.value is CardReaderStatus.Connected) {
            startFlowWhenReaderConnected()
        } else {
            exitWithSnackbar(R.string.card_reader_payment_reader_not_connected)
        }

        scope.launch {
            listenToCardReaderBatteryChanges()
        }
    }

    private fun startFlowWhenReaderConnected() {
        val isVMKilledWhenTTPActivityInForeground = paymentFlowJob == null && isTTPPaymentInProgress.get()
        if (isVMKilledWhenTTPActivityInForeground) {
            tracker.trackPaymentFailed("VM killed when TTP activity in foreground")
            _paymentState.value = buildFailedPaymentState(
                PaymentFlowError.BuiltInReader.AppKilledWhileInBackground,
                "",
                {}
            )
        } else {
            when (paymentOrRefund) {
                is CardReaderFlowParam.PaymentOrRefund.Payment -> {
                    if (paymentFlowJob == null) initPaymentFlow(isRetry = false)
                }

                is CardReaderFlowParam.PaymentOrRefund.Refund -> {
                    if (refundFlowJob == null) initRefundFlow(isRetry = false)
                }
            }
        }
    }

    private suspend fun listenToCardReaderBatteryChanges() {
        cardReaderManager.batteryStatus.collect { batteryStatus ->
            if (batteryStatus is CardReaderBatteryStatus.StatusChanged) {
                cardReaderTrackingInfoKeeper.setCardReaderBatteryLevel(batteryStatus.batteryLevel)
            }
        }
    }

    private suspend fun listenForBluetoothCardReaderMessages() {
        cardReaderManager.displayBluetoothCardReaderMessages.collect { message ->
            when (message) {
                is BluetoothCardReaderMessages.CardReaderDisplayMessage -> {
                    updatePaymentState(message.message)
                }

                is BluetoothCardReaderMessages.CardReaderInputMessage -> {
                    /* no-op*/
                }

                is BluetoothCardReaderMessages.CardReaderNoMessage -> {
                    /* no-op*/
                }
            }
        }
    }

    private fun initPaymentFlow(isRetry: Boolean) {
        paymentFlowJob = scope.launch {
            _paymentState.value = CardReaderPaymentState.LoadingData(::onCancelPaymentFlow)
            if (isRetry) {
                delay(ARTIFICIAL_RETRY_DELAY)
            }
            fetchOrder()?.let { order ->
                cardReaderTrackingInfoKeeper.setCurrency(order.currency)

                if (!paymentCollectibilityChecker.isCollectable(order)) {
                    exitWithSnackbar(R.string.card_reader_payment_order_paid_payment_cancelled)
                    return@launch
                }
                launch {
                    isTTPPaymentInProgress.set(cardReaderType == CardReaderType.BUILT_IN)
                    collectPaymentFlow(cardReaderManager, order)
                }
                launch {
                    listenForBluetoothCardReaderMessages()
                }
            } ?: run {
                tracker.trackPaymentFailed("Fetching order failed")
                _paymentState.value = paymentStateProvider.provideNonCancellableFailedPaymentState(
                    cardReaderType = cardReaderType,
                    errorType = PaymentFlowError.FetchingOrderFailed,
                    onRetry = { initPaymentFlow(isRetry = true) },
                )
            }
        }
    }

    private fun initRefundFlow(isRetry: Boolean) {
        refundFlowJob = scope.launch {
            onRefundStatusChanged(InitializingInteracRefund, "")
            if (isRetry) {
                delay(ARTIFICIAL_RETRY_DELAY)
            }
            fetchOrder()?.let { order ->
                if (!interacRefundableChecker.isRefundable(order)) {
                    exitWithSnackbar(R.string.card_reader_interac_refund_order_refunded_refund_cancelled)
                    return@launch
                }
                launch {
                    refundPaymentFlow(cardReaderManager, order)
                }
                launch {
                    listenForBluetoothCardReaderMessages()
                }
            } ?: run {
                tracker.trackInteracPaymentFailed(
                    orderId = paymentOrRefund.orderId,
                    errorMessage = "Fetching order failed"
                )
                _paymentState.value = CardReaderInteracRefundState.InteracRefundFailure.NonCancelable(
                    errorType = InteracRefundFlowError.FetchingOrderFailed,
                    onRetry = { initRefundFlow(isRetry = true) },
                )
            }
        }
    }

    fun retry(orderId: Long, billingEmail: String, paymentData: PaymentData, amountLabel: String) {
        paymentFlowJob = scope.launch {
            _paymentState.value = CardReaderPaymentState.LoadingData(::onCancelPaymentFlow)
            delay(ARTIFICIAL_RETRY_DELAY)
            cardReaderManager.retryCollectPayment(orderId, paymentData).collect { paymentStatus ->
                onPaymentStatusChanged(orderId, billingEmail, paymentStatus, amountLabel)
            }
        }
    }

    private fun retryInteracRefund() {
        initRefundFlow(isRetry = true)
    }

    private suspend fun collectPaymentFlow(cardReaderManager: CardReaderManager, order: Order) {
        val customerEmail = order.billingAddress.email
        val site = selectedSite.get()
        val countryCode = getStoreCountryCode()
        val rawStatementDescriptor = appPrefs.getCardReaderStatementDescriptor(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
        cardReaderManager.collectPayment(
            PaymentInfo(
                paymentDescription = cardReaderPaymentOrderHelper.getPaymentDescription(order),
                statementDescriptor = StatementDescriptor(rawStatementDescriptor),
                orderId = order.id,
                amount = order.total,
                currency = order.currency,
                orderKey = order.orderKey,
                customerEmail = customerEmail.ifEmpty { null },
                isPluginCanSendReceipt = paymentReceiptHelper.isPluginCanSendReceipt(site),
                customerName = "${order.billingAddress.firstName} ${order.billingAddress.lastName}".ifBlank { null },
                storeName = selectedSite.get().name.ifEmpty { null },
                siteUrl = selectedSite.get().url.ifEmpty { null },
                countryCode = countryCode,
                feeAmount = calculateFeeInCents(countryCode),
                channel = determinePaymentChannel(paymentOrRefund)
            )
        ).collect { paymentStatus ->
            onPaymentStatusChanged(
                order.id,
                customerEmail,
                paymentStatus,
                cardReaderPaymentOrderHelper.getAmountLabel(order)
            )
        }
    }

    @Suppress("LongMethod")
    private fun onPaymentStatusChanged(
        orderId: Long,
        billingEmail: String,
        paymentStatus: CardPaymentStatus,
        amountLabel: String
    ) {
        paymentDataForRetry = null
        when (paymentStatus) {
            InitializingPayment -> {
                _paymentState.value =
                    CardReaderPaymentState.LoadingData(::onCancelPaymentFlow)
            }

            CollectingPayment -> {
                _paymentState.value = paymentStateProvider.provideCollectingPaymentState(
                    cardReaderType,
                    amountLabel,
                    ::onCancelPaymentFlow
                )
            }

            ProcessingPayment -> {
                _paymentState.value = paymentStateProvider.provideProcessingPaymentState(
                    cardReaderType,
                    amountLabel,
                    ::onCancelPaymentFlow
                )
            }

            is ProcessingPaymentCompleted -> {
                cardReaderTrackingInfoKeeper.setPaymentMethodType(paymentStatus.paymentMethodType.stringRepresentation)
                when (paymentStatus.paymentMethodType) {
                    // Interac payments done in one step, without capturing. That's why we track success here
                    PaymentMethodType.INTERAC_PRESENT -> tracker.trackInteracPaymentSucceeded()
                    else -> {}
                }
            }

            CapturingPayment -> {
                _paymentState.value = paymentStateProvider.provideCapturingPaymentState(
                    cardReaderType,
                    amountLabel
                )
            }

            is PaymentCompleted -> {
                tracker.trackPaymentSucceeded()
                onPaymentCompleted(paymentStatus, orderId)
            }

            WaitingForInput -> {
                // noop
            }

            is PaymentFailed -> {
                paymentDataForRetry = paymentStatus.paymentDataForRetry
                tracker.trackPaymentFailed(paymentStatus.errorMessage, paymentStatus.type)
                emitFailedPaymentState(orderId, billingEmail, paymentStatus, amountLabel)
            }
        }
    }

    private suspend fun refundPaymentFlow(
        cardReaderManager: CardReaderManager,
        order: Order
    ) {
        val refund: CardReaderFlowParam.PaymentOrRefund.Refund =
            paymentOrRefund as? CardReaderFlowParam.PaymentOrRefund.Refund
                ?: throw IllegalArgumentException("Refund flow started with non-refund payment type")
        val refundAmount = refund.refundAmount
        order.chargeId?.let { chargeId ->

            cardReaderManager.refundInteracPayment(
                RefundParams(
                    chargeId = chargeId,
                    amount = refundAmount,
                    currency = order.currency
                ),
                RefundConfig(
                    enableCustomerCancellation = false
                )
            ).collect { refundStatus ->
                onRefundStatusChanged(
                    refundStatus,
                    currencyFormatter.formatAmountWithCurrency(
                        refundAmount.toDouble(),
                        order.currency
                    )
                )
            }
        } ?: run {
            tracker.trackInteracPaymentFailed(
                orderId = paymentOrRefund.orderId,
                errorMessage = "Charge id is null for the order.",
                errorType = CardInteracRefundStatus.RefundStatusErrorType.NonRetryable,
            )
            emitFailedInteracRefundState(
                currencyFormatter.formatAmountWithCurrency(
                    refundAmount.toDouble(),
                    order.currency
                ),
                InteracRefundFailure(
                    type = CardInteracRefundStatus.RefundStatusErrorType.NonRetryable,
                    errorMessage = "Charge id is null for the order.",
                    refundParams = null
                )
            )
        }
    }

    private fun onRefundStatusChanged(
        refundStatus: CardInteracRefundStatus,
        amountLabel: String
    ) {
        when (refundStatus) {
            InitializingInteracRefund -> {
                _paymentState.value = CardReaderInteracRefundState.LoadingData(::onCancelPaymentFlow)
            }

            CollectingInteracRefund -> {
                _paymentState.value = CardReaderInteracRefundState.CollectingInteracRefund(
                    amountWithCurrencyLabel = amountLabel,
                    onCancel = ::onCancelPaymentFlow
                )
            }

            ProcessingInteracRefund -> {
                _paymentState.value = CardReaderInteracRefundState.ProcessingInteracRefund(amountLabel)
            }

            is InteracRefundSuccess -> {
                _paymentState.value = CardReaderInteracRefundState.InteracRefundSuccessful(amountLabel)
                triggerEvent(CardReaderPaymentEvent.InteracRefundSuccessful)
            }

            is InteracRefundFailure -> {
                tracker.trackInteracPaymentFailed(
                    paymentOrRefund.orderId,
                    refundStatus.errorMessage,
                    refundStatus.type,
                )
                emitFailedInteracRefundState(
                    amountLabel,
                    refundStatus
                )
            }
        }
    }

    private fun onPaymentCompleted(
        paymentStatus: PaymentCompleted,
        orderId: Long,
    ) {
        paymentReceiptHelper.storeReceiptUrl(orderId, paymentStatus.receiptUrl)
        appPrefs.setCardReaderSuccessfulPaymentTime()

        triggerEvent(CardReaderPaymentEvent.PlaySuccessfulPaymentSound)
        showPaymentSuccessfulState()
        reFetchOrder()
    }

    @VisibleForTesting
    fun reFetchOrder() {
        refetchOrderJob = scope.launch {
            fetchOrder() ?: triggerEvent(
                CardReaderPaymentEvent.ShowErrorMessage(
                    R.string.card_reader_refetching_order_failed
                )
            )
            if (_paymentState.value == CardReaderPaymentState.ReFetchingOrder) {
                triggerEvent(CardReaderPaymentEvent.Exit)
            }
        }
    }

    private suspend fun fetchOrder(): Order? {
        return orderRepository.fetchOrderById(paymentOrRefund.orderId)
    }

    private fun emitFailedInteracRefundState(
        amountLabel: String,
        error: InteracRefundFailure
    ) {
        WooLog.e(WooLog.T.CARD_READER, "Refund failed: ${error.errorMessage}")
        cardReaderOnboardingChecker.invalidateCache()
        val onRetryClicked = { retryInteracRefund() }
        val errorType = interacRefundErrorMapper.mapRefundErrorToUiError(error.type)
        _paymentState.value = buildInteracRefundFailedState(errorType, amountLabel, onRetryClicked)
    }

    private fun buildInteracRefundFailedState(
        errorType: InteracRefundFlowError,
        amountLabel: String,
        onRetryClicked: () -> Unit
    ) = when (errorType) {
        is InteracRefundFlowError.ContactSupportError ->
            CardReaderInteracRefundState.InteracRefundFailure.Cancelable(
                errorType = errorType,
                amountWithCurrencyLabel = amountLabel,
                cta = CardReaderPaymentOrRefundState.CallToAction(
                    label = R.string.support_contact,
                    onCallToActionTapped = { onContactSupportClicked() }
                ),
                onCancel = ::onBackPressed
            )

        is InteracRefundFlowError.NonRetryableError ->
            CardReaderInteracRefundState.InteracRefundFailure.Cancelable(
                errorType = errorType,
                amountWithCurrencyLabel = amountLabel,
                onCancel = ::onBackPressed
            )

        else ->
            CardReaderInteracRefundState.InteracRefundFailure.Cancelable(
                errorType = errorType,
                amountWithCurrencyLabel = amountLabel,
                onRetry = onRetryClicked,
                onCancel = ::onBackPressed
            )
    }

    private fun emitFailedPaymentState(
        orderId: Long,
        billingEmail: String,
        error: PaymentFailed,
        amountLabel: String
    ) {
        WooLog.e(WooLog.T.CARD_READER, error.errorMessage)
        cardReaderOnboardingChecker.invalidateCache()
        val onRetryClicked = error.paymentDataForRetry?.let {
            {
                retry(orderId, billingEmail, it, amountLabel)
            }
        } ?: { initPaymentFlow(isRetry = true) }

        val errorType = errorMapper.mapPaymentErrorToUiError(
            error.type,
            cardReaderType == CardReaderType.BUILT_IN
        )
        _paymentState.value = buildFailedPaymentState(errorType, amountLabel, onRetryClicked)
    }

    private fun buildFailedPaymentState(
        errorType: PaymentFlowError,
        amountLabel: String,
        onRetryClicked: () -> Unit
    ): CardReaderPaymentState.PaymentFailed = when (errorType) {
        is PaymentFlowError.ContactSupportError -> paymentStateProvider.provideCancellableFailedPaymentState(
            cardReaderType = cardReaderType,
            errorType = errorType,
            amountWithCurrencyLabel = amountLabel,
            cta = CardReaderPaymentOrRefundState.CallToAction(
                label = R.string.support_contact,
                onCallToActionTapped = { onContactSupportClicked() }
            ),
            onCancel = ::onBackPressed
        )

        is PaymentFlowError.BuiltInReader.NfcDisabled -> paymentStateProvider.provideCancellableFailedPaymentState(
            cardReaderType = cardReaderType,
            errorType = errorType,
            amountWithCurrencyLabel = amountLabel,
            onCancel = ::onBackPressed,
            cta = CardReaderPaymentOrRefundState.CallToAction(
                label = R.string.card_reader_payment_failed_nfc_disabled_enable_nfc,
                onCallToActionTapped = { onEnableNfcClicked() }
            )
        )

        is PaymentFlowError.NonRetryableError -> paymentStateProvider.provideCancellableFailedPaymentState(
            cardReaderType = cardReaderType,
            errorType = errorType,
            amountWithCurrencyLabel = amountLabel,
            onCancel = ::onBackPressed,
        )

        is PaymentFlowError.PurchaseHardwareReaderError -> paymentStateProvider.provideCancellableFailedPaymentState(
            cardReaderType = cardReaderType,
            cta = CardReaderPaymentOrRefundState.CallToAction(
                label = R.string.card_reader_payment_payment_failed_purchase_hardware_reader,
                onCallToActionTapped = { onPurchaseCardReaderClicked() }
            ),
            errorType = errorType,
            amountWithCurrencyLabel = amountLabel,
            onCancel = ::onBackPressed,
        )

        else -> paymentStateProvider.provideCancellableFailedPaymentState(
            cardReaderType = cardReaderType,
            errorType = errorType,
            amountWithCurrencyLabel = amountLabel,
            onRetry = onRetryClicked,
            onCancel = ::onBackPressed,
        )
    }

    private fun showPaymentSuccessfulState() {
        scope.launch {
            val order =
                requireNotNull(orderRepository.getOrderById(paymentOrRefund.orderId)) { "Order URL not available." }
            val amountLabel = cardReaderPaymentOrderHelper.getAmountLabel(order)
            val onPrintReceiptClicked = {
                onPrintReceiptClicked(amountLabel)
            }
            val onSaveUserClicked = {
                onSaveForLaterClicked()
            }
            val onSendReceiptClicked = { onSendReceiptClicked() }

            if (order.billingAddress.email.isBlank()) {
                _paymentState.value = paymentStateProvider.providePaymentSuccessState(
                    cardReaderType = cardReaderType,
                    amountLabel = amountLabel,
                    onPrintReceiptClicked = onPrintReceiptClicked,
                    onSendReceiptClicked = onSendReceiptClicked,
                    onSaveUserClicked = onSaveUserClicked
                )
            } else {
                _paymentState.value = paymentStateProvider.providePaymentSuccessfulReceiptSentAutomaticallyState(
                    cardReaderType = cardReaderType,
                    amountLabel = amountLabel,
                    recipientEmail = order.billingAddress.email,
                    onPrintReceiptClicked = onPrintReceiptClicked,
                    onSaveUserClicked = onSaveUserClicked
                )
            }
        }
    }

    private fun updatePaymentState(cardReaderHint: AdditionalInfoType) {
        when (val state = _paymentState.value) {
            is CardReaderInteracRefundState.CollectingInteracRefund -> {
                _paymentState.value = state.copy(
                    cardReaderHint = cardReaderHint.toHintLabel(true)
                )
            }

            is CardReaderPaymentState.CollectingPayment.BuiltInReaderCollectPaymentState ->
                _paymentState.value = state.copy(
                    cardReaderHint = cardReaderHint.toHintLabel(false)
                )

            is CardReaderPaymentState.CollectingPayment.ExternalReaderCollectPaymentState ->
                _paymentState.value = state.copy(
                    cardReaderHint = cardReaderHint.toHintLabel(false)
                )

            else -> WooLog.e(
                WooLog.T.CARD_READER,
                "Got SDK message when cardReaderPaymentController is in ${_paymentState.value}"
            )
        }
    }

    @StringRes
    private fun AdditionalInfoType.toHintLabel(isInteracRefund: Boolean) =
        when (this) {
            RETRY_CARD -> R.string.card_reader_payment_retry_card_prompt
            INSERT_CARD, INSERT_OR_SWIPE_CARD, SWIPE_CARD ->
                if (isInteracRefund) {
                    R.string.card_reader_interac_refund_refund_payment_hint
                } else {
                    R.string.card_reader_payment_collect_payment_hint
                }

            REMOVE_CARD -> R.string.card_reader_payment_remove_card_prompt
            MULTIPLE_CONTACTLESS_CARDS_DETECTED ->
                R.string.card_reader_payment_multiple_contactless_cards_detected_prompt

            TRY_ANOTHER_READ_METHOD -> R.string.card_reader_payment_try_another_read_method_prompt
            TRY_ANOTHER_CARD -> R.string.card_reader_payment_try_another_card_prompt
            CHECK_MOBILE_DEVICE -> R.string.card_reader_payment_check_mobile_device_prompt
            CARD_REMOVED_TOO_EARLY -> R.string.card_reader_payment_card_removed_too_early
        }

    private fun onSaveForLaterClicked() {
        onCancelPaymentFlow()
    }

    private fun onPrintReceiptClicked(amountWithCurrencyLabel: String) {
        scope.launch {
            _paymentState.value = CardReaderPaymentState.PrintingReceipt(amountWithCurrencyLabel)
            tracker.trackPrintReceiptTapped()
            startPrintingFlow()
        }
    }

    fun onViewCreated() {
        if (_paymentState.value is CardReaderPaymentState.PrintingReceipt) {
            startPrintingFlow()
        }
    }

    private fun startPrintingFlow() {
        scope.launch {
            val receiptResult = paymentReceiptHelper.getReceiptUrl(paymentOrRefund.orderId)
            if (receiptResult.isSuccess) {
                triggerEvent(
                    CardReaderPaymentEvent.PrintReceiptTapped(
                        receiptResult.getOrThrow(),
                        cardReaderPaymentOrderHelper.getReceiptDocumentName(paymentOrRefund.orderId)
                    )
                )
            } else {
                tracker.trackReceiptUrlFetchingFails(
                    errorDescription = receiptResult.exceptionOrNull()?.message ?: "Unknown error",
                )
                triggerEvent(CardReaderPaymentEvent.ShowErrorMessage(R.string.receipt_fetching_error))
                onCancelPaymentFlow()
            }
        }
    }

    private fun onSendReceiptClicked() {
        scope.launch {
            tracker.trackEmailReceiptTapped()
            val paymentStateBeforeLoading = _paymentState.value
            _paymentState.value = CardReaderPaymentState.SharingReceipt
            val receiptResult = paymentReceiptHelper.getReceiptUrl(paymentOrRefund.orderId)

            if (receiptResult.isSuccess) {
                when (
                    val sharingResult =
                        paymentReceiptShare(receiptResult.getOrThrow(), paymentOrRefund.orderId)
                ) {
                    is PaymentReceiptShare.ReceiptShareResult.Error.FileCreation -> {
                        tracker.trackPaymentsReceiptSharingFailed(sharingResult)
                        triggerEvent(
                            CardReaderPaymentEvent.ShowErrorMessage(
                                R.string.card_reader_payment_receipt_can_not_be_stored
                            )
                        )
                    }

                    is PaymentReceiptShare.ReceiptShareResult.Error.FileDownload -> {
                        tracker.trackPaymentsReceiptSharingFailed(sharingResult)
                        triggerEvent(
                            CardReaderPaymentEvent.ShowErrorMessage(
                                R.string.card_reader_payment_receipt_can_not_be_downloaded
                            )
                        )
                    }

                    is PaymentReceiptShare.ReceiptShareResult.Error.Sharing -> {
                        tracker.trackPaymentsReceiptSharingFailed(sharingResult)
                        triggerEvent(
                            CardReaderPaymentEvent.ShowErrorMessage(R.string.card_reader_payment_email_client_not_found)
                        )
                    }

                    PaymentReceiptShare.ReceiptShareResult.Success -> {
                        // no-op
                    }
                }
            } else {
                tracker.trackReceiptUrlFetchingFails(
                    errorDescription = receiptResult.exceptionOrNull()?.message ?: "Unknown error",
                )
                triggerEvent(CardReaderPaymentEvent.ShowErrorMessage(R.string.receipt_fetching_error))
            }

            _paymentState.value = paymentStateBeforeLoading
        }
    }

    fun onPrintResult(result: PrintJobResult) {
        showPaymentSuccessfulState()

        scope.launch {
            when (result) {
                CANCELLED -> tracker.trackPrintReceiptCancelled()
                FAILED -> tracker.trackPrintReceiptFailed()
                STARTED -> tracker.trackPrintReceiptSucceeded()
            }
        }
    }

    fun stop() {
        paymentDataForRetry?.let {
            cardReaderManager.cancelPayment(it)
        }
        scope.cancel()
    }

    fun onBackPressed() {
        onCancelPaymentFlow()
        disconnectFromReaderIfPaymentFailedState()
    }

    private fun onContactSupportClicked() {
        tracker.trackPaymentFailedContactSupportTapped()
        onCancelPaymentFlow()
        triggerEvent(CardReaderPaymentEvent.ContactSupportTapped)
    }

    private fun onEnableNfcClicked() {
        tracker.trackPaymentFailedEnabledNfcTapped()
        onCancelPaymentFlow()
        triggerEvent(CardReaderPaymentEvent.EnableNfcTapped)
    }

    private fun onPurchaseCardReaderClicked() {
        onCancelPaymentFlow()
        val storeCountryCode = wooStore.getStoreCountryCode(selectedSite.get())
        triggerEvent(
            CardReaderPaymentEvent.PurchaseCardReaderTapped(
                "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$storeCountryCode"
            )
        )
    }

    private fun onCancelPaymentFlow() {
        if (refetchOrderJob?.isActive == true) {
            if (_paymentState.value != CardReaderPaymentState.ReFetchingOrder) {
                _paymentState.value = CardReaderPaymentState.ReFetchingOrder
            } else {
                // show "data might be outdated" and exit the flow when the user presses back on FetchingOrder screen
                exitWithSnackbar(R.string.card_reader_refetching_order_failed)
            }
        } else {
            trackCancelledFlow(_paymentState.value)
            triggerEvent(CardReaderPaymentEvent.Exit)
        }
    }

    private fun disconnectFromReaderIfPaymentFailedState() {
        val readerStatus = cardReaderManager.readerStatus.value
        if (readerStatus is CardReaderStatus.Connected) {
            if (ReaderType.isBuiltInReaderType(readerStatus.cardReader.type) &&
                (
                    _paymentState.value is BuiltInReaderFailedPayment ||
                        _paymentState.value is CardReaderInteracRefundState.InteracRefundFailure
                    )
            ) {
                scope.launch { cardReaderManager.disconnectReader() }
            }
        }
    }

    private fun exitWithSnackbar(@StringRes message: Int) {
        triggerEvent(CardReaderPaymentEvent.ShowErrorMessage(message))
        triggerEvent(CardReaderPaymentEvent.Exit)
    }

    private suspend fun getStoreCountryCode(): String {
        return withContext(dispatchers.io) {
            requireNotNull(
                wooStore.getStoreCountryCode(
                    selectedSite.get()
                )
            ) { "Store's country code not found." }
        }
    }

    private fun calculateFeeInCents(countryCode: String) =
        if (countryCode == "CA") {
            CANADA_FEE_FLAT_IN_CENTS
        } else {
            null
        }

    private fun determinePaymentChannel(flow: CardReaderFlowParam.PaymentOrRefund): PaymentInfo.PaymentChannel? =
        when (flow) {
            is CardReaderFlowParam.PaymentOrRefund.Payment -> {
                when (flow.paymentType) {
                    CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.WOO_POS -> PaymentInfo.PaymentChannel.Pos
                    CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.SIMPLE,
                    CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER,
                    CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER_CREATION,
                    CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.TRY_TAP_TO_PAY ->
                        PaymentInfo.PaymentChannel.StoreManager
                }
            }

            is CardReaderFlowParam.PaymentOrRefund.Refund -> null
        }
}
