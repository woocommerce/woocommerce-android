package com.woocommerce.android.ui.payments.cardreader.payment

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentEvent
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentStateProvider
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderTrackCanceledFlowAction
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

private const val KEY_TTP_PAYMENT_IN_PROGRESS = "ttp_payment_in_progress"

@HiltViewModel
class CardReaderPaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    cardReaderManager: CardReaderManager,
    orderRepository: OrderDetailRepository,
    selectedSite: SelectedSite,
    appPrefs: AppPrefs = AppPrefs,
    paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker,
    interacRefundableChecker: CardReaderInteracRefundableChecker,
    tracker: PaymentsFlowTracker,
    trackCancelledFlow: CardReaderTrackCanceledFlowAction,
    currencyFormatter: CurrencyFormatter,
    errorMapper: CardReaderPaymentErrorMapper,
    interacRefundErrorMapper: CardReaderInteracRefundErrorMapper,
    wooStore: WooCommerceStore,
    dispatchers: CoroutineDispatchers,
    cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper,
    paymentStateProvider: CardReaderPaymentStateProvider,
    cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper,
    paymentReceiptHelper: PaymentReceiptHelper,
    cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    cardReaderConfigProvider: CardReaderCountryConfigProvider,
    paymentReceiptShare: PaymentReceiptShare,
    paymentStateMapper: CardReaderPaymentStateToViewStateMapper,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderPaymentDialogFragmentArgs by savedState.navArgs()

    private var isTTPPaymentInProgress: Boolean
        get() = savedState.get<Boolean>(KEY_TTP_PAYMENT_IN_PROGRESS) == true
        set(value) {
            savedState[KEY_TTP_PAYMENT_IN_PROGRESS] = value
        }

    private val paymentController = CardReaderPaymentController(
        cardReaderManager = cardReaderManager,
        orderRepository = orderRepository,
        selectedSite = selectedSite,
        appPrefs = appPrefs,
        paymentCollectibilityChecker = paymentCollectibilityChecker,
        interacRefundableChecker = interacRefundableChecker,
        tracker = tracker,
        trackCancelledFlow = trackCancelledFlow,
        currencyFormatter = currencyFormatter,
        errorMapper = errorMapper,
        interacRefundErrorMapper = interacRefundErrorMapper,
        wooStore = wooStore,
        dispatchers = dispatchers,
        cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
        paymentStateProvider = paymentStateProvider,
        cardReaderPaymentOrderHelper = cardReaderPaymentOrderHelper,
        paymentReceiptHelper = paymentReceiptHelper,
        cardReaderOnboardingChecker = cardReaderOnboardingChecker,
        cardReaderConfigProvider = cardReaderConfigProvider,
        paymentReceiptShare = paymentReceiptShare,
        paymentOrRefund = arguments.paymentOrRefund,
        cardReaderType = arguments.cardReaderType,
        isTTPPaymentInProgress = ::isTTPPaymentInProgress,
    )

    val viewStateData: LiveData<ViewState> =
        paymentController.paymentState.map(paymentStateMapper()).asLiveData(coroutineContext)

    override val event: LiveData<MultiLiveEvent.Event> =
        paymentController.event.asLiveData(coroutineContext).map {
            when (it) {
                CardReaderPaymentEvent.ContactSupportTapped -> ContactSupport
                CardReaderPaymentEvent.EnableNfcTapped -> EnableNfc
                CardReaderPaymentEvent.Exit -> MultiLiveEvent.Event.Exit
                CardReaderPaymentEvent.InteracRefundSuccessful -> InteracRefundSuccessful
                CardReaderPaymentEvent.PlaySuccessfulPaymentSound -> PlayChaChing
                is CardReaderPaymentEvent.PrintReceiptTapped -> PrintReceipt(
                    it.receiptUrl,
                    it.documentName
                )
                is CardReaderPaymentEvent.PurchaseCardReaderTapped -> PurchaseCardReader(it.url)
                is CardReaderPaymentEvent.ShowPaymentErrorMessage -> ShowSnackbarInDialog(it.message)
                is CardReaderPaymentEvent.ShowErrorMessage -> MultiLiveEvent.Event.ShowSnackbar(it.message)
            }
        }

    fun start() = paymentController.start()

    fun retry(orderId: Long, billingEmail: String, paymentData: PaymentData, amountLabel: String) =
        paymentController.retry(orderId, billingEmail, paymentData, amountLabel)

    fun onViewCreated() = paymentController.onViewCreated()

    fun onPrintResult(result: PrintJobResult) = paymentController.onPrintResult(result)

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun onCleared() = paymentController.stop()

    fun onBackPressed() = paymentController.onBackPressed()
}
