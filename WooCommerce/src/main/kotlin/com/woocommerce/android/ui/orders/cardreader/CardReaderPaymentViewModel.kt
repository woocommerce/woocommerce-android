package com.woocommerce.android.ui.orders.cardreader

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.CARD_READ_TIMED_OUT
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.GENERIC_ERROR
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.NO_NETWORK
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.PAYMENT_DECLINED
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.ShowAdditionalInfo
import com.woocommerce.android.cardreader.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.PaymentData
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.CardReaderPaymentEvent.PrintReceipt
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.CardReaderPaymentEvent.SendReceipt
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CapturingPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CollectPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.FailedPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.FetchingOrderState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.PaymentSuccessfulState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.ProcessingPaymentState
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
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
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T.MAIN
import java.math.BigDecimal
import javax.inject.Inject

private const val ARTIFICIAL_RETRY_DELAY = 500L

@HiltViewModel
class CardReaderPaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderManager: CardReaderManager,
    private val dispatchers: CoroutineDispatchers,
    private val logger: AppLogWrapper,
    private val orderStore: WCOrderStore,
    private val orderRepository: OrderDetailRepository,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderPaymentDialogArgs by savedState.navArgs()

    // TODO cardreader if payment succeeds we need to save the state as otherwise the payment might be collected twice
    // The app shouldn't store the state as payment flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(LoadingDataState)
    val viewStateData: LiveData<ViewState> = viewState

    private var paymentFlowJob: Job? = null
    @VisibleForTesting var fetchOrderJob: Job? = null

    fun start() {
        // TODO cardreader Check if the payment was already processed and cancel this flow
        // TODO cardreader Make sure a reader is connected
        if (paymentFlowJob == null) {
            initPaymentFlow()
        }
    }

    private fun initPaymentFlow() {
        paymentFlowJob = launch {
            try {
                loadOrderFromDB()?.let { order ->
                    order.total.toBigDecimalOrNull()?.let { amount ->
                        // TODO cardreader don't hardcode currency symbol ($)
                        collectPaymentFlow(
                            cardReaderManager,
                            order.getPaymentDescription(),
                            order.remoteOrderId,
                            amount,
                            order.currency,
                            order.billingEmail,
                            "$$amount"
                        )
                    } ?: throw IllegalStateException("Converting order.total to BigDecimal failed")
                } ?: throw IllegalStateException("Null order is not expected at this point")
            } catch (e: IllegalStateException) {
                logger.e(MAIN, e.stackTraceToString())
                viewState.postValue(
                    FailedPaymentState(
                        errorType = GENERIC_ERROR,
                        amountWithCurrencyLabel = null,
                        onPrimaryActionClicked = { initPaymentFlow() }
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

    private suspend fun collectPaymentFlow(
        cardReaderManager: CardReaderManager,
        paymentDescription: String,
        orderId: Long,
        amount: BigDecimal,
        currency: String,
        billingEmail: String,
        amountLabel: String
    ) {
        cardReaderManager.collectPayment(paymentDescription, orderId, amount, currency, billingEmail.ifEmpty { null })
            .collect { paymentStatus ->
                onPaymentStatusChanged(orderId, billingEmail, paymentStatus, amountLabel)
            }
    }

    private fun onPaymentStatusChanged(
        orderId: Long,
        billingEmail: String,
        paymentStatus: CardPaymentStatus,
        amountLabel: String
    ) {
        when (paymentStatus) {
            InitializingPayment -> viewState.postValue(LoadingDataState)
            CollectingPayment -> viewState.postValue(CollectPaymentState(amountLabel))
            ProcessingPayment -> viewState.postValue(ProcessingPaymentState(amountLabel))
            CapturingPayment -> viewState.postValue(CapturingPaymentState(amountLabel))
            // TODO cardreader store receipt data into a persistent storage
            is PaymentCompleted -> onPaymentCompleted(paymentStatus, billingEmail, orderId, amountLabel)
            ShowAdditionalInfo -> {
                // TODO cardreader prompt the user to take certain action eg. Remove card
            }
            WaitingForInput -> {
                // TODO cardreader prompt the user to tap/insert a card
            }
            is PaymentFailed -> emitFailedPaymentState(orderId, billingEmail, paymentStatus, amountLabel)
        }
    }

    private fun onPaymentCompleted(
        paymentStatus: PaymentCompleted,
        billingEmail: String,
        orderId: Long,
        amountLabel: String
    ) {
        viewState.postValue(PaymentSuccessfulState(
            amountLabel,
            // TODO cardreader this breaks equals of PaymentSuccessfulState - consider if it is ok
            { onPrintReceiptClicked(paymentStatus.receiptUrl, "receipt-order-$orderId") },
            { onSendReceiptClicked(paymentStatus.receiptUrl, billingEmail) }
        ))
        reFetchOrder()
    }

    @VisibleForTesting
    fun reFetchOrder() {
        fetchOrderJob = launch {
            orderRepository.fetchOrder(arguments.orderIdentifier)
                ?: triggerEvent(Event.ShowSnackbar(R.string.card_reader_fetching_order_failed))
            if (viewState.value == FetchingOrderState) {
                triggerEvent(Exit)
            }
        }
    }

    private fun emitFailedPaymentState(orderId: Long, billingEmail: String, error: PaymentFailed, amountLabel: String) {
        WooLog.e(WooLog.T.ORDERS, error.errorMessage)
        val onRetryClicked = error.paymentDataForRetry?.let {
            { retry(orderId, billingEmail, it, amountLabel) }
        } ?: { initPaymentFlow() }
        viewState.postValue(FailedPaymentState(error.type, amountLabel, onRetryClicked))
    }

    private fun onPrintReceiptClicked(receiptUrl: String, documentName: String) {
        launch {
            // TODO cardreader show a progress dialog as url loading might take some time
            triggerEvent(PrintReceipt(receiptUrl, documentName))
        }
    }

    private fun onSendReceiptClicked(receiptUrl: String, billingEmail: String) {
        launch {
            triggerEvent(SendReceipt(
                content = UiStringRes(
                    R.string.card_reader_payment_receipt_email_content,
                    listOf(UiStringText(receiptUrl))
                ),
                subject = UiStringRes(R.string.card_reader_payment_receipt_email_subject),
                address = billingEmail
            ))
        }
    }

    fun onEmailActivityNotFound() {
        triggerEvent(ShowSnackbar(R.string.card_reader_payment_email_client_not_found))
    }

    // TODO cardreader cancel payment intent in vm.onCleared if payment not completed with success
    override fun onCleared() {
        super.onCleared()
        orderRepository.onCleanup()
    }

    private suspend fun loadOrderFromDB() =
        withContext(dispatchers.io) { orderStore.getOrderByIdentifier(arguments.orderIdentifier) }

    private fun WCOrderModel.getPaymentDescription(): String =
        resourceProvider.getString(R.string.card_reader_payment_description, this.id, selectedSite.get().name.orEmpty())

    sealed class CardReaderPaymentEvent : Event() {
        data class PrintReceipt(val receiptUrl: String, val documentName: String) : CardReaderPaymentEvent()
        data class SendReceipt(val content: UiString, val subject: UiString, val address: String) :
            CardReaderPaymentEvent()
    }

    fun onBackPressed() {
        return if (fetchOrderJob?.isActive == true) {
            viewState.value = FetchingOrderState
        } else {
            triggerEvent(Exit)
        }
    }

    sealed class ViewState(
        @StringRes val hintLabel: Int? = null,
        @StringRes val headerLabel: Int? = null,
        @StringRes val paymentStateLabel: Int? = null,
        @DrawableRes val illustration: Int? = null,
        // TODO cardreader add tests
        val isProgressVisible: Boolean = false,
        val primaryActionLabel: Int? = null,
        val secondaryActionLabel: Int? = null
    ) {
        open val onPrimaryActionClicked: (() -> Unit)? = null
        open val onSecondaryActionClicked: (() -> Unit)? = null
        open val amountWithCurrencyLabel: String? = null

        object LoadingDataState : ViewState(
            headerLabel = R.string.card_reader_payment_collect_payment_loading_header,
            hintLabel = R.string.card_reader_payment_collect_payment_loading_hint,
            paymentStateLabel = R.string.card_reader_payment_collect_payment_loading_payment_state,
            isProgressVisible = true
        )

        // TODO cardreader Update FailedPaymentState
        data class FailedPaymentState(
            val errorType: CardPaymentStatusErrorType,
            override val amountWithCurrencyLabel: String?,
            override val onPrimaryActionClicked: (() -> Unit)
        ) : ViewState(
            headerLabel = R.string.card_reader_payment_payment_failed_header,
            paymentStateLabel = when (errorType) {
                NO_NETWORK -> R.string.card_reader_payment_failed_no_network_state
                PAYMENT_DECLINED -> R.string.card_reader_payment_failed_card_declined_state
                CARD_READ_TIMED_OUT,
                GENERIC_ERROR -> R.string.card_reader_payment_failed_unexpected_error_state
            },
            primaryActionLabel = R.string.retry,
            // TODO cardreader optimize all newly added vector drawables
            illustration = R.drawable.img_products_error
        )

        data class CollectPaymentState(override val amountWithCurrencyLabel: String) : ViewState(
            hintLabel = R.string.card_reader_payment_collect_payment_hint,
            headerLabel = R.string.card_reader_payment_collect_payment_header,
            paymentStateLabel = R.string.card_reader_payment_collect_payment_state,
            illustration = R.drawable.ic_card_reader
        )

        data class ProcessingPaymentState(override val amountWithCurrencyLabel: String) :
            ViewState(
                hintLabel = R.string.card_reader_payment_processing_payment_hint,
                headerLabel = R.string.card_reader_payment_processing_payment_header,
                paymentStateLabel = R.string.card_reader_payment_processing_payment_state,
                illustration = R.drawable.ic_card_reader
            )

        data class CapturingPaymentState(override val amountWithCurrencyLabel: String) :
            ViewState(
                hintLabel = R.string.card_reader_payment_capturing_payment_hint,
                headerLabel = R.string.card_reader_payment_capturing_payment_header,
                paymentStateLabel = R.string.card_reader_payment_capturing_payment_state,
                illustration = R.drawable.ic_card_reader
            )

        data class PaymentSuccessfulState(
            override val amountWithCurrencyLabel: String,
            override val onPrimaryActionClicked: (() -> Unit),
            override val onSecondaryActionClicked: (() -> Unit)
        ) :
            ViewState(
                headerLabel = R.string.card_reader_payment_completed_payment_header,
                illustration = R.drawable.ic_celebration,
                primaryActionLabel = R.string.card_reader_payment_print_receipt,
                secondaryActionLabel = R.string.card_reader_payment_send_receipt
            )

        object FetchingOrderState : ViewState(
            headerLabel = R.string.card_reader_payment_fetch_order_loading_header,
            hintLabel = R.string.card_reader_payment_fetch_order_loading_hint,
            paymentStateLabel = R.string.card_reader_payment_fetch_order_loading_payment_state,
            isProgressVisible = true
        )
    }
}
