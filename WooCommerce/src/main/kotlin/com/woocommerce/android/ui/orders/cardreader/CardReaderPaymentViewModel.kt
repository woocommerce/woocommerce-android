package com.woocommerce.android.ui.orders.cardreader

import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
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
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType.INSERT_CARD
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType.INSERT_OR_SWIPE_CARD
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType.MULTIPLE_CONTACTLESS_CARDS_DETECTED
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType.REMOVE_CARD
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType.RETRY_CARD
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType.SWIPE_CARD
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType.TRY_ANOTHER_CARD
import com.woocommerce.android.cardreader.CardPaymentStatus.AdditionalInfoType.TRY_ANOTHER_READ_METHOD
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.ShowAdditionalInfo
import com.woocommerce.android.cardreader.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.PaymentData
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CapturingPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CollectPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.FailedPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.PaymentSuccessfulState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.ProcessingPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.ReFetchingOrderState
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.PrintReceipt
import com.woocommerce.android.ui.orders.cardreader.ReceiptEvent.SendReceipt
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
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
                collectPaymentFlow(cardReaderManager, order)
            } ?: run {
                tracker.track(
                    AnalyticsTracker.Stat.CARD_PRESENT_COLLECT_PAYMENT_FAILED,
                    this@CardReaderPaymentViewModel.javaClass.simpleName,
                    null,
                    "Fetching order failed"
                )
                viewState.postValue(
                    FailedPaymentState(
                        errorType = PaymentFlowError.FETCHING_ORDER_FAILED,
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
        cardReaderManager.collectPayment(
            PaymentInfo(
                paymentDescription = order.getPaymentDescription(),
                orderId = order.remoteId,
                amount = order.total,
                currency = order.currency,
                orderKey = order.orderKey,
                customerEmail = customerEmail.ifEmpty { null },
                customerName = "${order.billingAddress.firstName} ${order.billingAddress.lastName}".ifBlank { null },
                storeName = selectedSite.get().name.ifEmpty { null },
                siteUrl = selectedSite.get().url.ifEmpty { null },
            )
        ).collect { paymentStatus ->
            onPaymentStatusChanged(order.remoteId, customerEmail, paymentStatus, order.getAmountLabel())
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
            is ShowAdditionalInfo -> {
                handleAdditionalInfo(paymentStatus.type)
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
        return orderRepository.fetchOrder(arguments.orderIdentifier)
    }

    private fun emitFailedPaymentState(orderId: Long, billingEmail: String, error: PaymentFailed, amountLabel: String) {
        WooLog.e(WooLog.T.CARD_READER, error.errorMessage)
        val onRetryClicked = error.paymentDataForRetry?.let {
            { retry(orderId, billingEmail, it, amountLabel) }
        } ?: { initPaymentFlow(isRetry = true) }
        when (val errorType = error.type.mapToUiError()) {
            PaymentFlowError.AMOUNT_TOO_SMALL -> {
                val onBackPressed: () -> Unit = { onBackPressed() }
                viewState.postValue(
                    FailedPaymentState(
                        errorType, amountLabel, R.string.card_reader_payment_payment_failed_ok, onBackPressed
                    )
                )
            }
            else -> {
                viewState.postValue(
                    FailedPaymentState(errorType, amountLabel, onPrimaryActionClicked = onRetryClicked)
                )
            }
        }.exhaustive
    }

    private fun showPaymentSuccessfulState() {
        launch {
            val order = orderRepository.getOrder(arguments.orderIdentifier)
                ?: throw IllegalStateException("Order URL not available.")
            val amountLabel = order.getAmountLabel()
            val receiptUrl = getReceiptUrl(order.remoteId)

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
            when (type) {
                RETRY_CARD -> R.string.card_reader_payment_retry_card_prompt
                INSERT_CARD -> null // noop - collect payment screen is currently shown
                INSERT_OR_SWIPE_CARD -> null // noop - collect payment screen is currently shown
                SWIPE_CARD -> null // noop - collect payment screen is currently shown
                REMOVE_CARD -> null // noop - processing payment screen always shows "remove card" message
                MULTIPLE_CONTACTLESS_CARDS_DETECTED ->
                    R.string.card_reader_payment_multiple_contactless_cards_detected_prompt
                TRY_ANOTHER_READ_METHOD -> R.string.card_reader_payment_try_another_read_method_prompt
                TRY_ANOTHER_CARD -> R.string.card_reader_payment_try_another_card_prompt
            }?.let { hint ->
                viewState.value = collectPaymentState.copy(hintLabel = hint)
            }
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
        val order = orderRepository.getOrder(arguments.orderIdentifier)
            ?: throw IllegalStateException("Order URL not available.")
        triggerEvent(PrintReceipt(getReceiptUrl(order.remoteId), order.getReceiptDocumentName()))
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
        orderRepository.onCleanup()
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
            triggerEvent(Exit)
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

    private fun Order.getReceiptDocumentName() = "receipt-order-$remoteId"

    class ShowSnackbarInDialog(@StringRes val message: Int) : Event()

    object PlayChaChing : MultiLiveEvent.Event()

    sealed class ViewState(
        @StringRes open val hintLabel: Int? = null,
        @StringRes open val headerLabel: Int? = null,
        @StringRes val paymentStateLabel: Int? = null,
        @DimenRes val paymentStateLabelTopMargin: Int = R.dimen.major_275,
        @DrawableRes val illustration: Int? = null,
        // TODO cardreader add tests
        open val isProgressVisible: Boolean = false,
        val primaryActionLabel: Int? = null,
        val secondaryActionLabel: Int? = null,
        val tertiaryActionLabel: Int? = null,
    ) {
        open val onPrimaryActionClicked: (() -> Unit)? = null
        open val onSecondaryActionClicked: (() -> Unit)? = null
        open val onTertiaryActionClicked: (() -> Unit)? = null
        open val amountWithCurrencyLabel: String? = null

        object LoadingDataState : ViewState(
            headerLabel = R.string.card_reader_payment_collect_payment_loading_header,
            hintLabel = R.string.card_reader_payment_collect_payment_loading_hint,
            paymentStateLabel = R.string.card_reader_payment_collect_payment_loading_payment_state,
            isProgressVisible = true
        )

        data class FailedPaymentState(
            private val errorType: PaymentFlowError,
            override val amountWithCurrencyLabel: String?,
            private val primaryLabel: Int? = R.string.try_again,
            override val onPrimaryActionClicked: (() -> Unit)
        ) : ViewState(
            headerLabel = R.string.card_reader_payment_payment_failed_header,
            paymentStateLabel = errorType.message,
            paymentStateLabelTopMargin = R.dimen.major_100,
            primaryActionLabel = primaryLabel,
            illustration = R.drawable.img_products_error
        )

        data class CollectPaymentState(
            override val amountWithCurrencyLabel: String,
            override val hintLabel: Int = R.string.card_reader_payment_collect_payment_hint,
            override val headerLabel: Int = R.string.card_reader_payment_collect_payment_header,
        ) : ViewState(
            paymentStateLabel = R.string.card_reader_payment_collect_payment_state,
            illustration = R.drawable.img_card_reader_available
        )

        data class ProcessingPaymentState(override val amountWithCurrencyLabel: String) :
            ViewState(
                hintLabel = R.string.card_reader_payment_processing_payment_hint,
                headerLabel = R.string.card_reader_payment_processing_payment_header,
                paymentStateLabel = R.string.card_reader_payment_processing_payment_state,
                illustration = R.drawable.img_card_reader_available
            )

        data class CapturingPaymentState(override val amountWithCurrencyLabel: String) :
            ViewState(
                hintLabel = R.string.card_reader_payment_capturing_payment_hint,
                headerLabel = R.string.card_reader_payment_capturing_payment_header,
                paymentStateLabel = R.string.card_reader_payment_capturing_payment_state,
                illustration = R.drawable.img_card_reader_available
            )

        data class PaymentSuccessfulState(
            override val amountWithCurrencyLabel: String,
            override val onPrimaryActionClicked: (() -> Unit),
            override val onSecondaryActionClicked: (() -> Unit),
            override val onTertiaryActionClicked: (() -> Unit)
        ) : ViewState(
            headerLabel = R.string.card_reader_payment_completed_payment_header,
            illustration = R.drawable.img_celebration,
            primaryActionLabel = R.string.card_reader_payment_print_receipt,
            secondaryActionLabel = R.string.card_reader_payment_send_receipt,
            tertiaryActionLabel = R.string.card_reader_payment_save_for_later,
        )

        data class PrintingReceiptState(
            override val amountWithCurrencyLabel: String,
            val receiptUrl: String,
            val documentName: String
        ) : ViewState(
            headerLabel = R.string.card_reader_payment_completed_payment_header,
            illustration = null,
            primaryActionLabel = null,
            secondaryActionLabel = null,
        ) {
            override val isProgressVisible = true
        }

        object ReFetchingOrderState : ViewState(
            headerLabel = R.string.card_reader_payment_fetch_order_loading_header,
            hintLabel = R.string.card_reader_payment_fetch_order_loading_hint,
            paymentStateLabel = R.string.card_reader_payment_fetch_order_loading_payment_state,
            isProgressVisible = true
        )
    }

    enum class PaymentFlowError(val message: Int) {
        FETCHING_ORDER_FAILED(R.string.order_error_fetch_generic),
        NO_NETWORK(R.string.card_reader_payment_failed_no_network_state),
        SERVER_ERROR(R.string.card_reader_payment_failed_server_error_state),
        PAYMENT_DECLINED(R.string.card_reader_payment_failed_card_declined_state),
        GENERIC_ERROR(R.string.card_reader_payment_failed_unexpected_error_state),
        AMOUNT_TOO_SMALL(R.string.card_reader_payment_failed_amount_too_small),
    }

    private fun CardPaymentStatusErrorType.mapToUiError(): PaymentFlowError =
        when (this) {
            CardPaymentStatusErrorType.NO_NETWORK -> PaymentFlowError.NO_NETWORK
            CardPaymentStatusErrorType.PAYMENT_DECLINED -> PaymentFlowError.PAYMENT_DECLINED
            CardPaymentStatusErrorType.CARD_READ_TIMED_OUT,
            CardPaymentStatusErrorType.GENERIC_ERROR -> PaymentFlowError.GENERIC_ERROR
            CardPaymentStatusErrorType.SERVER_ERROR -> PaymentFlowError.SERVER_ERROR
            CardPaymentStatusErrorType.AMOUNT_TOO_SMALL -> PaymentFlowError.AMOUNT_TOO_SMALL
        }
}
