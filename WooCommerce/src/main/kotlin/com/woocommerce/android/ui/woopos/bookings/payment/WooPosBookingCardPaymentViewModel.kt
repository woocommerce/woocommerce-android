package com.woocommerce.android.ui.woopos.bookings.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.home.totals.WooPosCardReaderPaymentControllerFactory
import com.woocommerce.android.util.UiStringParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosBookingCardPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cardReaderPaymentControllerFactory: WooPosCardReaderPaymentControllerFactory,
    private val cardReaderFacade: WooPosCardReaderFacade,
    private val uiStringParser: UiStringParser,
) : ViewModel() {

    private val orderId: Long = savedStateHandle[BOOKING_CARD_PAYMENT_ORDER_ID_KEY]!!

    private val _state = MutableStateFlow<BookingCardPaymentState>(BookingCardPaymentState.Loading)
    val state: StateFlow<BookingCardPaymentState> = _state

    private var isTTPPaymentInProgress: Boolean = false

    private var cardReaderPaymentController: CardReaderPaymentController? = null
    private var paymentStateJob: Job? = null

    init {
        checkReaderAndStartPayment()
    }

    fun onConnectReaderClicked() {
        cardReaderFacade.connectToReader()
        waitForReaderConnection()
    }

    fun onRetryClicked() {
        checkReaderAndStartPayment()
    }

    fun onCancelClicked() {
        cardReaderPaymentController?.onBackPressed()
    }

    private fun checkReaderAndStartPayment() {
        if (cardReaderFacade.readerStatus.value is CardReaderStatus.Connected) {
            startPayment()
        } else {
            _state.value = BookingCardPaymentState.ReaderNotConnected
            waitForReaderConnection()
        }
    }

    private fun waitForReaderConnection() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus
                .filter { it is CardReaderStatus.Connected }
                .first()
            startPayment()
        }
    }

    private fun startPayment() {
        _state.value = BookingCardPaymentState.Loading

        cardReaderPaymentController?.stop()
        cardReaderPaymentController = cardReaderPaymentControllerFactory.create(
            orderId = orderId,
            paymentType = PaymentType.WOO_POS,
            isTTPPaymentInProgress = ::isTTPPaymentInProgress,
        )

        cardReaderPaymentController?.start()
        listenToPaymentState()
    }

    private fun listenToPaymentState() {
        paymentStateJob?.cancel()
        paymentStateJob = viewModelScope.launch {
            cardReaderPaymentController?.paymentState?.collect { paymentState ->
                when (paymentState) {
                    is CardReaderPaymentState.LoadingData -> {
                        _state.value = BookingCardPaymentState.Loading
                    }

                    is CardReaderPaymentState.CollectingPayment -> {
                        _state.value = BookingCardPaymentState.CollectingPayment(
                            amountLabel = paymentState.amountWithCurrencyLabel,
                        )
                    }

                    is CardReaderPaymentState.ProcessingPayment,
                    is CardReaderPaymentState.PaymentCapturing -> {
                        val amount = when (paymentState) {
                            is CardReaderPaymentState.ProcessingPayment -> paymentState.amountWithCurrencyLabel
                            is CardReaderPaymentState.PaymentCapturing -> paymentState.amountWithCurrencyLabel
                            else -> ""
                        }
                        _state.value = BookingCardPaymentState.ProcessingPayment(
                            amountLabel = amount,
                        )
                    }

                    is CardReaderPaymentState.PaymentSuccessful -> {
                        _state.value = BookingCardPaymentState.PaymentSuccessful(
                            amountLabel = paymentState.amountWithCurrencyLabel,
                        )
                    }

                    is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment -> {
                        _state.value = BookingCardPaymentState.PaymentFailed(
                            errorMessage = uiStringParser.asString(paymentState.errorType.message),
                        )
                    }

                    is CardReaderPaymentState.ReFetchingOrder -> Unit

                    else -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cardReaderPaymentController?.stop()
    }
}

sealed class BookingCardPaymentState {
    data object Loading : BookingCardPaymentState()

    data object ReaderNotConnected : BookingCardPaymentState()

    data class CollectingPayment(
        val amountLabel: String,
    ) : BookingCardPaymentState()

    data class ProcessingPayment(
        val amountLabel: String,
    ) : BookingCardPaymentState()

    data class PaymentSuccessful(
        val amountLabel: String,
    ) : BookingCardPaymentState()

    data class PaymentFailed(
        val errorMessage: String,
    ) : BookingCardPaymentState()
}
