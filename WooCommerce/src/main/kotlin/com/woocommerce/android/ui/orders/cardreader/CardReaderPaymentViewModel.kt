package com.woocommerce.android.ui.orders.cardreader

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CapturingPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CollectPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.FailedPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.PaymentSuccessfulState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.ProcessingPaymentState
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T.MAIN
import java.math.BigDecimal
import javax.inject.Inject

private const val ARTIFICIAL_RETRY_DELAY = 500L

@HiltViewModel
class CardReaderPaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    cardReaderManager: CardReaderManager?,
    private val logger: AppLogWrapper,
    private val orderStore: WCOrderStore
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderPaymentDialogArgs by savedState.navArgs()

    // The app shouldn't store the state as payment flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(LoadingDataState)
    val viewStateData: LiveData<ViewState> = viewState

    // TODO remove this, and make the constructor parameter as a non nullable property when
    //  the actual implementation is injected in release builds
    private val cardReaderManager = cardReaderManager!!

    private var paymentFlowJob: Job? = null

    final fun start() {
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
                        collectPaymentFlow(cardReaderManager, order.remoteOrderId, amount, order.currency, "$$amount")
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

    fun retry(orderId: Long, paymentData: PaymentData, amountLabel: String) {
        paymentFlowJob = launch {
            viewState.postValue((LoadingDataState))
            delay(ARTIFICIAL_RETRY_DELAY)
            cardReaderManager.retryCollectPayment(orderId, paymentData).collect { paymentStatus ->
                onPaymentStatusChanged(orderId, paymentStatus, amountLabel)
            }
        }
    }

    private suspend fun collectPaymentFlow(
        cardReaderManager: CardReaderManager,
        orderId: Long,
        amount: BigDecimal,
        currency: String,
        amountLabel: String
    ) {
        cardReaderManager.collectPayment(orderId, amount, currency).collect { paymentStatus ->
            onPaymentStatusChanged(orderId, paymentStatus, amountLabel)
        }
    }

    private fun onPaymentStatusChanged(
        orderId: Long,
        paymentStatus: CardPaymentStatus,
        amountLabel: String
    ) {
        when (paymentStatus) {
            InitializingPayment -> viewState.postValue(LoadingDataState)
            CollectingPayment -> viewState.postValue(CollectPaymentState(amountLabel))
            ProcessingPayment -> viewState.postValue(ProcessingPaymentState(amountLabel))
            CapturingPayment -> viewState.postValue(CapturingPaymentState(amountLabel))
            is PaymentCompleted -> viewState.postValue(PaymentSuccessfulState(amountLabel))
            ShowAdditionalInfo -> {
                // TODO cardreader prompt the user to take certain action eg. Remove card
            }
            WaitingForInput -> {
                // TODO cardreader prompt the user to tap/insert a card
            }
            is PaymentFailed -> emitFailedPaymentState(orderId, paymentStatus, amountLabel)
        }
    }

    private fun emitFailedPaymentState(orderId: Long, error: PaymentFailed, amountLabel: String) {
        WooLog.e(WooLog.T.ORDERS, error.errorMessage)
        val onRetryClicked = error.paymentDataForRetry?.let {
            { retry(orderId, it, amountLabel) }
        } ?: { initPaymentFlow() }
        viewState.postValue(FailedPaymentState(error.type, amountLabel, onRetryClicked))
    }

    private fun loadOrderFromDB() = orderStore.getOrderByIdentifier(arguments.orderIdentifier)

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

        data class PaymentSuccessfulState(override val amountWithCurrencyLabel: String) :
            ViewState(
                headerLabel = R.string.card_reader_payment_completed_payment_header,
                illustration = R.drawable.ic_celebration,
                secondaryActionLabel = R.string.card_reader_payment_send_receipt,
                primaryActionLabel = R.string.card_reader_payment_print_receipt
            )
    }
}
