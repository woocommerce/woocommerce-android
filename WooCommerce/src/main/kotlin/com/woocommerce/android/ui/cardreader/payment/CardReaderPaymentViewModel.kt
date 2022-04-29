package com.woocommerce.android.ui.cardreader.payment

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.CollectingInteracRefund
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InitializingInteracRefund
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InteracRefundFailure
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InteracRefundSuccess
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.ProcessingInteracRefund
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType
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
import com.woocommerce.android.cardreader.payments.RefundParams
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.CardReaderTracker
import com.woocommerce.android.ui.cardreader.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.cardreader.onboarding.WCPAY_RECEIPTS_SENDING_SUPPORT_VERSION
import com.woocommerce.android.ui.cardreader.payment.ViewState.CapturingPaymentState
import com.woocommerce.android.ui.cardreader.payment.ViewState.CollectPaymentState
import com.woocommerce.android.ui.cardreader.payment.ViewState.CollectRefundState
import com.woocommerce.android.ui.cardreader.payment.ViewState.FailedPaymentState
import com.woocommerce.android.ui.cardreader.payment.ViewState.FailedRefundState
import com.woocommerce.android.ui.cardreader.payment.ViewState.LoadingDataState
import com.woocommerce.android.ui.cardreader.payment.ViewState.PaymentSuccessfulReceiptSentAutomaticallyState
import com.woocommerce.android.ui.cardreader.payment.ViewState.PaymentSuccessfulState
import com.woocommerce.android.ui.cardreader.payment.ViewState.PrintingReceiptState
import com.woocommerce.android.ui.cardreader.payment.ViewState.ProcessingPaymentState
import com.woocommerce.android.ui.cardreader.payment.ViewState.ProcessingRefundState
import com.woocommerce.android.ui.cardreader.payment.ViewState.ReFetchingOrderState
import com.woocommerce.android.ui.cardreader.payment.ViewState.RefundLoadingDataState
import com.woocommerce.android.ui.cardreader.payment.ViewState.RefundSuccessfulState
import com.woocommerce.android.ui.cardreader.receipt.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.cardreader.receipt.ReceiptEvent.SendReceipt
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

private const val ARTIFICIAL_RETRY_DELAY = 500L
private const val CANADA_FEE_FLAT_IN_CENTS = 15L

@HiltViewModel
class CardReaderPaymentViewModel
@Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderManager: CardReaderManager,
    private val orderRepository: OrderDetailRepository,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker,
    private val interacRefundableChecker: CardReaderInteracRefundableChecker,
    private val tracker: CardReaderTracker,
    private val currencyFormatter: CurrencyFormatter,
    private val errorMapper: CardReaderPaymentErrorMapper,
    private val interacRefundErrorMapper: CardReaderInteracRefundErrorMapper,
    private val wooStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers,
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderPaymentDialogFragmentArgs by savedState.navArgs()

    private val orderId = arguments.paymentOrRefund.orderId

    private val refundAmount: BigDecimal
        get() = when (val param = arguments.paymentOrRefund) {
            is CardReaderFlowParam.PaymentOrRefund.Refund -> param.refundAmount
            else -> throw IllegalStateException("Accessing refund account on $param flow")
        }

    // The app shouldn't store the state as payment flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(LoadingDataState)
    val viewStateData: LiveData<ViewState> = viewState

    private var paymentFlowJob: Job? = null
    private var refundFlowJob: Job? = null
    private var paymentDataForRetry: PaymentData? = null

    private var refetchOrderJob: Job? = null

    fun start() {
        if (cardReaderManager.readerStatus.value is CardReaderStatus.Connected) {
            when (arguments.paymentOrRefund) {
                is CardReaderFlowParam.PaymentOrRefund.Payment -> {
                    if (paymentFlowJob == null) initPaymentFlow(isRetry = false)
                }
                is CardReaderFlowParam.PaymentOrRefund.Refund -> {
                    if (refundFlowJob == null) initRefundFlow(isRetry = false)
                }
            }
        } else {
            exitWithSnackbar(R.string.card_reader_payment_reader_not_connected)
        }
    }

    private suspend fun listenForBluetoothCardReaderMessages() {
        cardReaderManager.displayBluetoothCardReaderMessages.collect { message ->
            when (message) {
                is BluetoothCardReaderMessages.CardReaderDisplayMessage -> {
                    handleAdditionalInfo(message.message)
                }
                is BluetoothCardReaderMessages.CardReaderInputMessage -> { /* no-op*/
                }
                is BluetoothCardReaderMessages.CardReaderNoMessage -> { /* no-op*/
                }
            }.exhaustive
        }
    }

    private fun initPaymentFlow(isRetry: Boolean) {
        paymentFlowJob = launch {
            viewState.postValue((LoadingDataState))
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
                    collectPaymentFlow(cardReaderManager, order)
                }
                launch {
                    listenForBluetoothCardReaderMessages()
                }
            } ?: run {
                tracker.trackPaymentFailed("Fetching order failed")
                viewState.postValue(
                    FailedPaymentState(
                        errorType = PaymentFlowError.FetchingOrderFailed,
                        amountWithCurrencyLabel = null,
                        onPrimaryActionClicked = { initPaymentFlow(isRetry = true) }
                    )
                )
            }
        }
    }

    private fun initRefundFlow(isRetry: Boolean) {
        refundFlowJob = launch {
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
                    orderId = orderId,
                    errorMessage = "Fetching order failed"
                )
                viewState.postValue(
                    FailedRefundState(
                        errorType = InteracRefundFlowError.FetchingOrderFailed,
                        amountWithCurrencyLabel = null,
                        onPrimaryActionClicked = { initRefundFlow(isRetry = true) }
                    )
                )
            }
        }
    }

    fun retry(orderId: Long, billingEmail: String, paymentData: PaymentData, amountLabel: String) {
        paymentFlowJob = launch {
            viewState.postValue((LoadingDataState))
            delay(ARTIFICIAL_RETRY_DELAY)
            cardReaderManager.retryCollectPayment(orderId, paymentData).collect { paymentStatus ->
                onPaymentStatusChanged(orderId, billingEmail, paymentStatus, amountLabel)
            }
        }
    }

    fun retryInteracRefund() {
        initRefundFlow(isRetry = true)
    }

    private suspend fun collectPaymentFlow(cardReaderManager: CardReaderManager, order: Order) {
        val customerEmail = order.billingAddress.email
        val site = selectedSite.get()
        val countryCode = getStoreCountryCode()
        cardReaderManager.collectPayment(
            PaymentInfo(
                paymentDescription = order.getPaymentDescription(),
                statementDescriptor = appPrefsWrapper.getCardReaderStatementDescriptor(
                    localSiteId = site.id,
                    remoteSiteId = site.siteId,
                    selfHostedSiteId = site.selfHostedSiteId
                ),
                orderId = order.id,
                amount = order.total,
                currency = order.currency,
                orderKey = order.orderKey,
                customerEmail = customerEmail.ifEmpty { null },
                isPluginCanSendReceipt = isPluginCanSendReceipt(site),
                customerName = "${order.billingAddress.firstName} ${order.billingAddress.lastName}".ifBlank { null },
                storeName = selectedSite.get().name.ifEmpty { null },
                siteUrl = selectedSite.get().url.ifEmpty { null },
                countryCode = countryCode,
                feeAmount = calculateFeeInCents(countryCode)
            )
        ).collect { paymentStatus ->
            onPaymentStatusChanged(order.id, customerEmail, paymentStatus, order.getAmountLabel())
        }
    }

    private fun onPaymentStatusChanged(
        orderId: Long,
        billingEmail: String,
        paymentStatus: CardPaymentStatus,
        amountLabel: String
    ) {
        paymentDataForRetry = null
        when (paymentStatus) {
            InitializingPayment -> viewState.postValue(LoadingDataState)
            CollectingPayment -> viewState.postValue(CollectPaymentState(amountLabel))
            ProcessingPayment -> viewState.postValue(ProcessingPaymentState(amountLabel))
            is ProcessingPaymentCompleted -> {
                cardReaderTrackingInfoKeeper.setPaymentMethodType(paymentStatus.paymentMethodType.stringRepresentation)
                when (paymentStatus.paymentMethodType) {
                    // Interac payments done in one step, without capturing. That's why we track success here
                    PaymentMethodType.INTERAC_PRESENT -> tracker.trackInteracPaymentSucceeded()
                    else -> {}
                }
            }
            CapturingPayment -> viewState.postValue(CapturingPaymentState(amountLabel))
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
        }.exhaustive
    }

    private suspend fun refundPaymentFlow(
        cardReaderManager: CardReaderManager,
        order: Order
    ) {
        order.chargeId?.let { chargeId ->
            cardReaderManager.refundInteracPayment(
                RefundParams(
                    chargeId = chargeId,
                    amount = refundAmount,
                    currency = order.currency
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
                orderId = orderId,
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
            InitializingInteracRefund -> viewState.postValue(RefundLoadingDataState)
            CollectingInteracRefund -> viewState.postValue(CollectRefundState(amountLabel))
            ProcessingInteracRefund -> viewState.postValue(ProcessingRefundState(amountLabel))
            is InteracRefundSuccess -> {
                viewState.postValue(RefundSuccessfulState(amountLabel))
                triggerEvent(InteracRefundSuccessful)
            }
            is InteracRefundFailure -> {
                tracker.trackInteracPaymentFailed(
                    orderId,
                    refundStatus.errorMessage,
                    refundStatus.type,
                )
                emitFailedInteracRefundState(
                    amountLabel,
                    refundStatus
                )
            }
        }.exhaustive
    }

    private fun onPaymentCompleted(
        paymentStatus: PaymentCompleted,
        orderId: Long,
    ) {
        storeReceiptUrl(orderId, paymentStatus.receiptUrl)
        triggerEvent(PlayChaChing)
        showPaymentSuccessfulState()
        reFetchOrder()
    }

    @VisibleForTesting
    fun reFetchOrder() {
        refetchOrderJob = launch {
            fetchOrder() ?: triggerEvent(ShowSnackbar(R.string.card_reader_refetching_order_failed))
            if (viewState.value == ReFetchingOrderState) {
                triggerEvent(Exit)
            }
        }
    }

    private suspend fun fetchOrder(): Order? {
        return orderRepository.fetchOrderById(orderId)
    }

    private fun emitFailedInteracRefundState(
        amountLabel: String?,
        error: InteracRefundFailure
    ) {
        WooLog.e(WooLog.T.CARD_READER, "Refund failed: ${error.errorMessage}")
        val onRetryClicked = { retryInteracRefund() }
        val errorType = interacRefundErrorMapper.mapRefundErrorToUiError(error.type)
        if (errorType is InteracRefundFlowError.NonRetryableError) {
            viewState.postValue(
                FailedRefundState(
                    errorType,
                    amountLabel,
                    R.string.card_reader_interac_refund_refund_failed_ok,
                    onPrimaryActionClicked = { onBackPressed() }
                )
            )
        } else {
            viewState.postValue(
                FailedRefundState(
                    errorType,
                    amountLabel,
                    onPrimaryActionClicked = onRetryClicked
                )
            )
        }
    }

    private fun emitFailedPaymentState(orderId: Long, billingEmail: String, error: PaymentFailed, amountLabel: String) {
        WooLog.e(WooLog.T.CARD_READER, error.errorMessage)
        val onRetryClicked = error.paymentDataForRetry?.let {
            { retry(orderId, billingEmail, it, amountLabel) }
        } ?: { initPaymentFlow(isRetry = true) }
        val errorType = errorMapper.mapPaymentErrorToUiError(error.type)
        if (errorType is PaymentFlowError.NonRetryableError) {
            viewState.postValue(
                FailedPaymentState(
                    errorType,
                    amountLabel,
                    R.string.card_reader_payment_payment_failed_ok,
                    onPrimaryActionClicked = { onBackPressed() }
                )
            )
        } else {
            viewState.postValue(
                FailedPaymentState(
                    errorType,
                    amountLabel,
                    onPrimaryActionClicked = onRetryClicked
                )
            )
        }
    }

    private fun showPaymentSuccessfulState() {
        launch {
            val order = orderRepository.getOrderById(orderId)
                ?: throw IllegalStateException("Order URL not available.")
            val amountLabel = order.getAmountLabel()
            val receiptUrl = getReceiptUrl(order.id)
            val onPrintReceiptClicked = {
                onPrintReceiptClicked(amountLabel, receiptUrl, order.getReceiptDocumentName())
            }
            val onSaveUserClicked = {
                onSaveForLaterClicked()
            }
            val onSendReceiptClicked = {
                onSendReceiptClicked(receiptUrl, order.billingAddress.email)
            }

            if (order.billingAddress.email.isBlank()) {
                viewState.postValue(
                    PaymentSuccessfulState(
                        amountLabel, onPrintReceiptClicked, onSendReceiptClicked, onSaveUserClicked
                    )
                )
            } else {
                val receiptSentHint = UiStringRes(
                    R.string.card_reader_payment_reader_receipt_sent,
                    listOf(UiStringText(order.billingAddress.email)),
                    true
                )
                viewState.postValue(
                    PaymentSuccessfulReceiptSentAutomaticallyState(
                        amountLabel, receiptSentHint, onPrintReceiptClicked, onSaveUserClicked
                    )
                )
            }
        }
    }

    private fun handleAdditionalInfo(type: AdditionalInfoType) {
        when (val state = viewState.value) {
            is CollectRefundState -> {
                viewState.value = state.copy(
                    hintLabel = type.toHintLabel(true)
                )
            }
            is CollectPaymentState -> {
                viewState.value = state.copy(
                    hintLabel = type.toHintLabel(false)
                )
            }
            else -> WooLog.e(
                WooLog.T.CARD_READER, "Got SDK message when cardReaderPaymentViewModel is in ${viewState.value}"
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
        }

    private fun onSaveForLaterClicked() {
        onBackPressed()
    }

    private fun onPrintReceiptClicked(amountWithCurrencyLabel: String, receiptUrl: String, documentName: String) {
        launch {
            viewState.value = PrintingReceiptState(amountWithCurrencyLabel, receiptUrl, documentName)
            tracker.trackPrintReceiptTapped()
            startPrintingFlow()
        }
    }

    fun onViewCreated() {
        if (viewState.value is PrintingReceiptState) {
            startPrintingFlow()
        }
    }

    private fun startPrintingFlow() {
        launch {
            val order = orderRepository.getOrderById(orderId)
                ?: throw IllegalStateException("Order URL not available.")
            triggerEvent(PrintReceipt(getReceiptUrl(order.id), order.getReceiptDocumentName()))
        }
    }

    private fun onSendReceiptClicked(receiptUrl: String, billingEmail: String) {
        launch {
            tracker.trackEmailReceiptTapped()
            triggerEvent(
                SendReceipt(
                    content = UiStringRes(
                        R.string.card_reader_payment_receipt_email_content,
                        listOf(UiStringText(receiptUrl))
                    ),
                    subject = UiStringRes(
                        R.string.card_reader_payment_receipt_email_subject,
                        listOf(UiStringText(selectedSite.get().name.orEmpty()))
                    ),
                    address = billingEmail
                )
            )
        }
    }

    fun onEmailActivityNotFound() {
        tracker.trackEmailReceiptFailed()
        triggerEvent(ShowSnackbarInDialog(R.string.card_reader_payment_email_client_not_found))
    }

    fun onPrintResult(result: PrintJobResult) {
        showPaymentSuccessfulState()

        when (result) {
            CANCELLED -> tracker.trackPrintReceiptCancelled()
            FAILED -> tracker.trackPrintReceiptFailed()
            STARTED -> tracker.trackPrintReceiptSucceeded()
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun onCleared() {
        super.onCleared()
        paymentDataForRetry?.let {
            cardReaderManager.cancelPayment(it)
        }
    }

    fun onBackPressed() {
        if (refetchOrderJob?.isActive == true) {
            if (viewState.value != ReFetchingOrderState) {
                viewState.value = ReFetchingOrderState
            } else {
                // show "data might be outdated" and exit the flow when the user presses back on FetchingOrder screen
                exitWithSnackbar(R.string.card_reader_refetching_order_failed)
            }
        } else {
            viewState.value?.let { state ->
                trackCancelledFlow(state)
            }
            triggerEvent(Exit)
        }
    }

    private fun trackCancelledFlow(state: ViewState) {
        when (state) {
            is PaymentFlow -> {
                tracker.trackPaymentCancelled(state.nameForTracking)
            }
            is InteracRefundFlow -> {
                tracker.trackInteracRefundCancelled(state.nameForTracking)
            }
            else -> WooLog.e(WooLog.T.CARD_READER, "Invalid state received")
        }
    }

    private fun exitWithSnackbar(@StringRes message: Int) {
        triggerEvent(ShowSnackbar(message))
        triggerEvent(Exit)
    }

    private fun storeReceiptUrl(orderId: Long, receiptUrl: String) {
        selectedSite.get().let {
            appPrefsWrapper.setReceiptUrl(it.id, it.siteId, it.selfHostedSiteId, orderId, receiptUrl)
        }
    }

    private fun getReceiptUrl(orderId: Long): String {
        return selectedSite.get().let {
            appPrefsWrapper.getReceiptUrl(it.id, it.siteId, it.selfHostedSiteId, orderId)
        }
    }

    private fun Order.getPaymentDescription(): String =
        resourceProvider.getString(
            R.string.card_reader_payment_description,
            this.number,
            selectedSite.get().name.orEmpty()
        )

    private fun Order.getAmountLabel(): String = currencyFormatter
        .formatAmountWithCurrency(this.total.toDouble(), this.currency)

    private fun Order.getReceiptDocumentName() = "receipt-order-$id"

    private suspend fun getStoreCountryCode(): String {
        return withContext(dispatchers.io) {
            wooStore.getStoreCountryCode(
                selectedSite.get()
            ) ?: throw IllegalStateException("Store's country code not found.")
        }
    }

    private fun calculateFeeInCents(countryCode: String) =
        if (countryCode == "CA") {
            CANADA_FEE_FLAT_IN_CENTS
        } else {
            null
        }

    private fun isPluginCanSendReceipt(site: SiteModel): Boolean {
        val preferredPlugin = appPrefsWrapper.getCardReaderPreferredPlugin(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
        return if (preferredPlugin == null || preferredPlugin != PluginType.WOOCOMMERCE_PAYMENTS) {
            false
        } else {
            val pluginVersion = appPrefsWrapper.getCardReaderPreferredPluginVersion(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId,
                preferredPlugin,
            ) ?: return false

            pluginVersion.semverCompareTo(WCPAY_RECEIPTS_SENDING_SUPPORT_VERSION) >= 0
        }
    }

    class ShowSnackbarInDialog(@StringRes val message: Int) : Event()

    object PlayChaChing : MultiLiveEvent.Event()

    object InteracRefundSuccessful : MultiLiveEvent.Event()
}
