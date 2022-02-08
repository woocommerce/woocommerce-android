package com.woocommerce.android.ui.orders.cardreader

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_EMAIL_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_EMAIL_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_PRINT_CANCELED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_PRINT_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_PRINT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.RECEIPT_PRINT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
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
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.SendReceipt
import com.woocommerce.android.ui.orders.cardreader.ViewState.CapturingPaymentState
import com.woocommerce.android.ui.orders.cardreader.ViewState.CollectPaymentState
import com.woocommerce.android.ui.orders.cardreader.ViewState.FailedPaymentState
import com.woocommerce.android.ui.orders.cardreader.ViewState.LoadingDataState
import com.woocommerce.android.ui.orders.cardreader.ViewState.PaymentSuccessfulState
import com.woocommerce.android.ui.orders.cardreader.ViewState.ProcessingPaymentState
import com.woocommerce.android.ui.orders.cardreader.ViewState.ReFetchingOrderState
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.*
import javax.inject.Inject

private const val ARTIFICIAL_RETRY_DELAY = 500L

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
    private val tracker: AnalyticsTrackerWrapper,
    private val currencyFormatter: CurrencyFormatter,
    private val errorMapper: CardReaderPaymentErrorMapper,
    private val wooStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderPaymentDialogFragmentArgs by savedState.navArgs()

    // The app shouldn't store the state as payment flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(LoadingDataState)
    val viewStateData: LiveData<ViewState> = viewState

    private var paymentFlowJob: Job? = null
    private var paymentDataForRetry: PaymentData? = null

    private var refetchOrderJob: Job? = null

    fun start() {
        if (cardReaderManager.readerStatus.value is CardReaderStatus.Connected && paymentFlowJob == null) {
            initPaymentFlow(isRetry = false)
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
                tracker.track(
                    AnalyticsTracker.Stat.CARD_PRESENT_COLLECT_PAYMENT_FAILED,
                    this@CardReaderPaymentViewModel.javaClass.simpleName,
                    null,
                    "Fetching order failed"
                )
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

    fun retry(orderId: Long, billingEmail: String, paymentData: PaymentData, amountLabel: String) {
        paymentFlowJob = launch {
            viewState.postValue((LoadingDataState))
            delay(ARTIFICIAL_RETRY_DELAY)
            cardReaderManager.retryCollectPayment(orderId, paymentData).collect { paymentStatus ->
                onPaymentStatusChanged(orderId, billingEmail, paymentStatus, amountLabel)
            }
        }
    }

    private suspend fun collectPaymentFlow(cardReaderManager: CardReaderManager, order: Order) {
        val customerEmail = order.billingAddress.email
        val site = selectedSite.get()
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
                customerName = "${order.billingAddress.firstName} ${order.billingAddress.lastName}".ifBlank { null },
                storeName = selectedSite.get().name.ifEmpty { null },
                siteUrl = selectedSite.get().url.ifEmpty { null },
                countryCode = getStoreCountryCode(),
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
            CapturingPayment -> viewState.postValue(CapturingPaymentState(amountLabel))
            is PaymentCompleted -> {
                tracker.track(AnalyticsTracker.Stat.CARD_PRESENT_COLLECT_PAYMENT_SUCCESS)
                onPaymentCompleted(paymentStatus, orderId)
            }
            WaitingForInput -> {
                // noop
            }
            is PaymentFailed -> {
                paymentDataForRetry = paymentStatus.paymentDataForRetry
                tracker.track(
                    AnalyticsTracker.Stat.CARD_PRESENT_COLLECT_PAYMENT_FAILED,
                    this@CardReaderPaymentViewModel.javaClass.simpleName,
                    paymentStatus.type.toString(),
                    paymentStatus.errorMessage
                )
                emitFailedPaymentState(orderId, billingEmail, paymentStatus, amountLabel)
            }
        }
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
        return orderRepository.fetchOrderById(arguments.orderId)
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
            val order = orderRepository.getOrderById(arguments.orderId)
                ?: throw IllegalStateException("Order URL not available.")
            val amountLabel = order.getAmountLabel()
            val receiptUrl = getReceiptUrl(order.id)

            viewState.postValue(
                PaymentSuccessfulState(
                    order.getAmountLabel(),
                    { onPrintReceiptClicked(amountLabel, receiptUrl, order.getReceiptDocumentName()) },
                    { onSendReceiptClicked(receiptUrl, order.billingAddress.email) },
                    { onSaveForLaterClicked() }
                )
            )
        }
    }

    private fun handleAdditionalInfo(type: AdditionalInfoType) {
        (viewState.value as? CollectPaymentState)?.let { collectPaymentState ->
            viewState.value = collectPaymentState.copy(
                hintLabel = when (type) {
                    RETRY_CARD -> R.string.card_reader_payment_retry_card_prompt
                    INSERT_CARD, INSERT_OR_SWIPE_CARD, SWIPE_CARD -> R.string.card_reader_payment_collect_payment_hint
                    REMOVE_CARD -> R.string.card_reader_payment_remove_card_prompt
                    MULTIPLE_CONTACTLESS_CARDS_DETECTED ->
                        R.string.card_reader_payment_multiple_contactless_cards_detected_prompt
                    TRY_ANOTHER_READ_METHOD -> R.string.card_reader_payment_try_another_read_method_prompt
                    TRY_ANOTHER_CARD -> R.string.card_reader_payment_try_another_card_prompt
                    CHECK_MOBILE_DEVICE -> R.string.card_reader_payment_check_mobile_device_prompt
                }
            )
        } ?: run {
            WooLog.e(WooLog.T.CARD_READER, "Got SDK message when cardReaderPaymentViewModel is in ${viewState.value}")
        }
    }

    private fun onSaveForLaterClicked() {
        onBackPressed()
    }

    private fun onPrintReceiptClicked(amountWithCurrencyLabel: String, receiptUrl: String, documentName: String) {
        launch {
            viewState.value = ViewState.PrintingReceiptState(amountWithCurrencyLabel, receiptUrl, documentName)
            tracker.track(RECEIPT_PRINT_TAPPED)
            startPrintingFlow()
        }
    }

    fun onViewCreated() {
        if (viewState.value is ViewState.PrintingReceiptState) {
            startPrintingFlow()
        }
    }

    private fun startPrintingFlow() {
        launch {
            val order = orderRepository.getOrderById(arguments.orderId)
                ?: throw IllegalStateException("Order URL not available.")
            triggerEvent(PrintReceipt(getReceiptUrl(order.id), order.getReceiptDocumentName()))
        }
    }

    private fun onSendReceiptClicked(receiptUrl: String, billingEmail: String) {
        launch {
            tracker.track(RECEIPT_EMAIL_TAPPED)
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
        tracker.track(RECEIPT_EMAIL_FAILED)
        triggerEvent(ShowSnackbarInDialog(R.string.card_reader_payment_email_client_not_found))
    }

    fun onPrintResult(result: PrintJobResult) {
        showPaymentSuccessfulState()

        tracker.track(
            when (result) {
                CANCELLED -> RECEIPT_PRINT_CANCELED
                FAILED -> RECEIPT_PRINT_FAILED
                STARTED -> RECEIPT_PRINT_SUCCESS
            }
        )
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
            viewState.value?.let { paymentState ->
                if (isStateEligibleForTracking(paymentState)) {
                    tracker.track(
                        AnalyticsTracker.Stat.CARD_PRESENT_COLLECT_PAYMENT_CANCELLED,
                        this@CardReaderPaymentViewModel.javaClass.simpleName,
                        null,
                        "User manually cancelled the payment during state ${getCurrentPaymentState()}"
                    )
                }
            }
            triggerEvent(Exit)
        }
    }

    private fun isStateEligibleForTracking(paymentState: ViewState) =
        paymentState is LoadingDataState ||
            paymentState is CollectPaymentState ||
            paymentState is ProcessingPaymentState ||
            paymentState is CapturingPaymentState

    private fun getCurrentPaymentState(): String? {
        return when (viewState.value) {
            is LoadingDataState -> "Loading"
            is CapturingPaymentState -> "Capturing"
            is CollectPaymentState -> "Collecting"
            is ProcessingPaymentState -> "Processing"
            else -> {
                WooLog.e(WooLog.T.CARD_READER, "Invalid payment state received")
                null
            }
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
                ?: throw IllegalStateException("Receipt URL not available.")
        }
    }

    private fun Order.getPaymentDescription(): String =
        resourceProvider.getString(
            R.string.card_reader_payment_description,
            this.number,
            selectedSite.get().name.orEmpty()
        )

    private fun Order.getAmountLabel(): String = currencyFormatter
        .formatAmountWithCurrency(this.currency, this.total.toDouble())

    private fun Order.getReceiptDocumentName() = "receipt-order-$id"

    private suspend fun getStoreCountryCode(): String {
        return withContext(dispatchers.io) {
            wooStore.getStoreCountryCode(
                selectedSite.get()
            ) ?: throw IllegalStateException("Store's country code not found.")
        }
    }

    class ShowSnackbarInDialog(@StringRes val message: Int) : Event()

    object PlayChaChing : MultiLiveEvent.Event()
}
