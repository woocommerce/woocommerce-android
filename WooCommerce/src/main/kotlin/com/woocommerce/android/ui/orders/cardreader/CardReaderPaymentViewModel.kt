package com.woocommerce.android.ui.orders.cardreader

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.ShowAdditionalInfo
import com.woocommerce.android.cardreader.CardPaymentStatus.UnexpectedError
import com.woocommerce.android.cardreader.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CapturingPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CollectPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.FailedPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.PaymentSuccessfulState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.ProcessingPaymentState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T
import java.math.BigDecimal

class CardReaderPaymentViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val logger: AppLogWrapper,
    private val orderStore: WCOrderStore
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: CardReaderPaymentDialogArgs by savedState.navArgs()

    // The app shouldn't store the state as payment flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(LoadingDataState)
    val viewStateData: LiveData<ViewState> = viewState

    final fun start(cardReaderManager: CardReaderManager) {
        // TODO cardreader Check if the payment was already processed and cancel this flow
        // TODO cardreader Make sure a reader is connected
        initPaymentFlow(cardReaderManager)
    }

    private fun initPaymentFlow(cardReaderManager: CardReaderManager) {
        launch {
            try {
                loadOrderFromDB()?.let { order ->
                    order.total.toBigDecimalOrNull()?.let { amount ->
                        // TODO cardreader don't hardcode currency symbol ($)
                        collectPaymentFlow(cardReaderManager, amount, order.currency, "$$amount")
                    } ?: throw IllegalStateException("Converting order.total to BigDecimal failed")
                } ?: throw IllegalStateException("Null order is not expected at this point")
            } catch (e: IllegalStateException) {
                logger.e(T.MAIN, e.stackTraceToString())
                viewState.postValue(FailedPaymentState)
            }
        }
    }

    private suspend fun collectPaymentFlow(
        cardReaderManager: CardReaderManager,
        amount: BigDecimal,
        currency: String,
        amountLabel: String
    ) {
        cardReaderManager.collectPayment(amount, currency).collect { paymentStatus ->
            when (paymentStatus) {
                InitializingPayment -> viewState.postValue(LoadingDataState)
                CollectingPayment -> viewState.postValue(CollectPaymentState(amountLabel))
                ProcessingPayment -> viewState.postValue(ProcessingPaymentState(amountLabel))
                CapturingPayment -> viewState.postValue(CapturingPaymentState(amountLabel))
                PaymentCompleted -> viewState.postValue(PaymentSuccessfulState(amountLabel))
                ShowAdditionalInfo -> {
                    // TODO cardreader prompt the user to take certain action eg. Remove card
                }
                WaitingForInput -> {
                    // TODO cardreader prompt the user to tap/insert a card
                }
                CapturingPaymentFailed,
                CollectingPaymentFailed,
                InitializingPaymentFailed,
                ProcessingPaymentFailed -> viewState.postValue(FailedPaymentState)
                is UnexpectedError -> {
                    logger.e(T.MAIN, paymentStatus.errorCause)
                    viewState.postValue(FailedPaymentState)
                }
            }
        }
    }

    private fun loadOrderFromDB() = orderStore.getOrderByIdentifier(arguments.orderIdentifier)

    sealed class ViewState(
        @StringRes val hintLabel: Int? = null,
        @StringRes val headerLabel: Int? = null,
        @StringRes val paymentStateLabel: Int? = null,
        @DrawableRes val illustration: Int? = null,
        // TODO cardreader add tests
        val isProgressVisible: Boolean = false,
        val printReceiptLabel: Int? = null,
        val sendReceiptLabel: Int? = null
    ) {
        @StringRes open val amountWithCurrencyLabel: String? = ""

        object LoadingDataState : ViewState(isProgressVisible = true)

        // TODO cardreader Update FailedPaymentState
        object FailedPaymentState : ViewState()

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
                hintLabel = R.string.card_reader_payment_collect_payment_hint,
                headerLabel = R.string.card_reader_payment_capturing_payment_header,
                paymentStateLabel = R.string.card_reader_payment_capturing_payment_state,
                illustration = R.drawable.ic_card_reader
            )

        data class PaymentSuccessfulState(override val amountWithCurrencyLabel: String) :
            ViewState(
                headerLabel = R.string.card_reader_payment_completed_payment_header,
                illustration = R.drawable.ic_celebration,
                sendReceiptLabel = R.string.card_reader_payment_send_receipt,
                printReceiptLabel = R.string.card_reader_payment_print_receipt
            )
    }

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<CardReaderPaymentViewModel>
}
