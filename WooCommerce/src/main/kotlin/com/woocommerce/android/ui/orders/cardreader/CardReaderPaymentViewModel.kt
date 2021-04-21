package com.woocommerce.android.ui.orders.cardreader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.FailedPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
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
                        collectPaymentFlow(cardReaderManager, amount, order.currency)
                    } ?: IllegalStateException("Converting order.total to BigDecimal failed")
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
        currency: String
    ) {
        cardReaderManager.collectPayment(amount, currency).collect { paymentStatus ->
            triggerEvent(ShowSnackbar(R.string.generic_string, arrayOf(paymentStatus.javaClass.simpleName)))
        }
    }

    private fun loadOrderFromDB() = orderStore.getOrderByIdentifier(arguments.orderIdentifier)

    sealed class ViewState(
        val hintLabel: Int?,
        val headerLabel: Int?,
        val paymentStateLabel: Int?,
        val illustration: Int?
    ) {
        abstract val amountWithCurrencyLabel: String?

        // TODO cardreader Update LoadingDataState
        object LoadingDataState: ViewState(
            hintLabel = null,
            headerLabel = null,
            paymentStateLabel = null,
            illustration = null
        ) {
            override val amountWithCurrencyLabel = null
        }

        data class CollectPaymentState(override val amountWithCurrencyLabel: String) : ViewState(
            hintLabel = R.string.card_reader_payment_collect_payment_hint,
            headerLabel = R.string.card_reader_payment_collect_payment_header,
            paymentStateLabel = R.string.card_reader_payment_collect_payment_state,
            illustration = R.drawable.common_full_open_on_phone
        )

        data class ProcessingPaymentState(override val amountWithCurrencyLabel: String) :
            ViewState(
                hintLabel = R.string.card_reader_payment_processing_payment_hint,
                headerLabel = R.string.card_reader_payment_processing_payment_header,
                paymentStateLabel = R.string.card_reader_payment_processing_payment_state,
                illustration = R.drawable.common_full_open_on_phone
            )

        data class CapturingPaymentState(override val amountWithCurrencyLabel: String) :
            ViewState(
                hintLabel = R.string.card_reader_payment_collect_payment_hint,
                headerLabel = R.string.card_reader_payment_capturing_payment_header,
                paymentStateLabel = R.string.card_reader_payment_capturing_payment_state,
                illustration = R.drawable.common_full_open_on_phone
            )

        data class PaymentSuccessfulState(override val amountWithCurrencyLabel: String) :
            ViewState(
                hintLabel = null,
                headerLabel = R.string.card_reader_payment_completed_payment_header,
                paymentStateLabel = null,
                illustration = R.drawable.common_full_open_on_phone
            )
    }

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<CardReaderPaymentViewModel>
}
