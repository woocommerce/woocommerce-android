package com.woocommerce.android.ui.orders.cardreader

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.CollectPaymentState
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentViewModel.ViewState.LoadingDataState
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelFragmentArgs
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingPackageSelectorViewModel.ViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T

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

    init {
        start()
    }

    final fun start() {
        launch {
            val order = orderStore.getOrderByIdentifier(arguments.orderIdentifier)
            logger.d(T.MAIN, "CardReader: Order: $order")
        }
    }

    fun foo() {
        logger.d(T.MAIN, "Testing foo()")
    }

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
