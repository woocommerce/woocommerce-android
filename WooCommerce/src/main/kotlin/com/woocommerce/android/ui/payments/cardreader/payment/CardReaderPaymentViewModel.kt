package com.woocommerce.android.ui.payments.cardreader.payment

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.connection.event.BluetoothCardReaderMessages
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.CollectingInteracRefund
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InitializingInteracRefund
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InteracRefundFailure
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InteracRefundSuccess
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.ProcessingInteracRefund
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.AdditionalInfoType.CARD_REMOVED_TOO_EARLY
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
import com.woocommerce.android.cardreader.payments.RefundConfig
import com.woocommerce.android.cardreader.payments.RefundParams
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderCollectPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderFailedPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.CollectRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderCollectPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.FailedRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.LoadingDataState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.PrintingReceiptState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ProcessingRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ReFetchingOrderState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.RefundLoadingDataState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.RefundSuccessfulState
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.CANCELLED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.FAILED
import com.woocommerce.android.util.PrintHtmlHelper.PrintJobResult.STARTED
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

private const val ARTIFICIAL_RETRY_DELAY = 500L
private const val CANADA_FEE_FLAT_IN_CENTS = 15L
private const val KEY_TTP_PAYMENT_IN_PROGRESS = "ttp_payment_in_progress"

@HiltViewModel
@Suppress("LargeClass")
class CardReaderPaymentViewModel
@Inject constructor(
    savedState: SavedStateHandle,
    private val cardReaderManager: CardReaderManager,
    private val orderRepository: OrderDetailRepository,
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefs = AppPrefs,
    private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker,
    private val interacRefundableChecker: CardReaderInteracRefundableChecker,
    private val tracker: PaymentsFlowTracker,
    private val currencyFormatter: CurrencyFormatter,
    private val errorMapper: CardReaderPaymentErrorMapper,
    private val interacRefundErrorMapper: CardReaderInteracRefundErrorMapper,
    private val wooStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers,
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper,
    private val cardReaderPaymentReaderTypeStateProvider: CardReaderPaymentReaderTypeStateProvider,
    private val cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper,
    private val paymentReceiptHelper: PaymentReceiptHelper,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    private val cardReaderConfigProvider: CardReaderCountryConfigProvider,
    private val paymentReceiptShare: PaymentReceiptShare,
) : ScopedViewModel(savedState) {
    private val arguments: CardReaderPaymentDialogFragmentArgs by savedState.navArgs()

    private var isTTPPaymentInProgress: Boolean
        get() = savedState.get<Boolean>(KEY_TTP_PAYMENT_IN_PROGRESS) == true
        set(value) {
            savedState[KEY_TTP_PAYMENT_IN_PROGRESS] = value
        }

    private val orderId = arguments.paymentOrRefund.orderId

    private val refundAmount: BigDecimal
        get() = when (val param = arguments.paymentOrRefund) {
            is CardReaderFlowParam.PaymentOrRefund.Refund -> param.refundAmount
            else -> throw IllegalStateException("Accessing refund amount on $param flow")
        }

    // The app shouldn't store the state as payment flow gets canceled when the vm dies
    private val viewState = MutableLiveData<ViewState>(LoadingDataState(::onCancelPaymentFlow))
    val viewStateData: LiveData<ViewState> = viewState

    private var paymentFlowJob: Job? = null
    private var refundFlowJob: Job? = null
    private var paymentDataForRetry: PaymentData? = null

    private var refetchOrderJob: Job? = null

    private val CardReaderFlowParam.PaymentOrRefund.isPOS: Boolean
        get() = this is CardReaderFlowParam.PaymentOrRefund.Payment &&
            this.paymentType == CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.WOO_POS

    fun start() {
        if (cardReaderManager.readerStatus.value is CardReaderStatus.Connected) {
            startFlowWhenReaderConnected()
        } else {
            exitWithSnackbar(R.string.card_reader_payment_reader_not_connected)
        }

        viewModelScope.launch {
            listenToCardReaderBatteryChanges()
        }
    }

    private fun startFlowWhenReaderConnected() {
        val isVMKilledWhenTTPActivityInForeground = paymentFlowJob == null && isTTPPaymentInProgress
        if (isVMKilledWhenTTPActivityInForeground) {
            tracker.trackPaymentFailed("VM killed when TTP activity in foreground")
            viewState.postValue(
                buildFailedPaymentState(
                    PaymentFlowError.BuiltInReader.AppKilledWhileInBackground,
                    "",
                    {}
                )
            )
        } else {
            when (arguments.paymentOrRefund) {
                is CardReaderFlowParam.PaymentOrRefund.Payment -> {
                    if (paymentFlowJob == null) initPaymentFlow(isRetry = false)
                }

                is CardReaderFlowParam.PaymentOrRefund.Refund -> {
                    if (refundFlowJob == null) initRefundFlow(isRetry = false)
                }
            }
        }
    }

    private suspend fun listenToCardReaderBatteryChanges() {
        cardReaderManager.batteryStatus.collect { batteryStatus ->
            if (batteryStatus is CardReaderBatteryStatus.StatusChanged) {
                cardReaderTrackingInfoKeeper.setCardReaderBatteryLevel(batteryStatus.batteryLevel)
            }
        }
    }

    private suspend fun listenForBluetoothCardReaderMessages() {
        cardReaderManager.displayBluetoothCardReaderMessages.collect { message ->
            when (message) {
                is BluetoothCardReaderMessages.CardReaderDisplayMessage -> {
                    handleAdditionalInfo(message.message)
                }

                is BluetoothCardReaderMessages.CardReaderInputMessage -> {
                    /* no-op*/
                }

                is BluetoothCardReaderMessages.CardReaderNoMessage -> {
                    /* no-op*/
                }
            }
        }
    }

    private fun initPaymentFlow(isRetry: Boolean) {
        paymentFlowJob = launch {
            viewState.postValue((LoadingDataState(::onCancelPaymentFlow)))
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
                    isTTPPaymentInProgress = arguments.cardReaderType == CardReaderType.BUILT_IN
                    collectPaymentFlow(cardReaderManager, order)
                }
                launch {
                    listenForBluetoothCardReaderMessages()
                }
            } ?: run {
                tracker.trackPaymentFailed("Fetching order failed")
                viewState.postValue(
                    cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                        cardReaderType = arguments.cardReaderType,
                        errorType = PaymentFlowError.FetchingOrderFailed,
                        amountLabel = null,
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
            viewState.postValue(LoadingDataState(::onCancelPaymentFlow))
            delay(ARTIFICIAL_RETRY_DELAY)
            cardReaderManager.retryCollectPayment(orderId, paymentData).collect { paymentStatus ->
                onPaymentStatusChanged(orderId, billingEmail, paymentStatus, amountLabel)
            }
        }
    }

    private fun retryInteracRefund() {
        initRefundFlow(isRetry = true)
    }

    private suspend fun collectPaymentFlow(cardReaderManager: CardReaderManager, order: Order) {
        val customerEmail = order.billingAddress.email
        val site = selectedSite.get()
        val countryCode = getStoreCountryCode()
        val rawStatementDescriptor = appPrefs.getCardReaderStatementDescriptor(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
        cardReaderManager.collectPayment(
            PaymentInfo(
                paymentDescription = cardReaderPaymentOrderHelper.getPaymentDescription(order),
                statementDescriptor = StatementDescriptor(rawStatementDescriptor),
                orderId = order.id,
                amount = order.total,
                currency = order.currency,
                orderKey = order.orderKey,
                customerEmail = customerEmail.ifEmpty { null },
                isPluginCanSendReceipt = paymentReceiptHelper.isPluginCanSendReceipt(site),
                customerName = "${order.billingAddress.firstName} ${order.billingAddress.lastName}".ifBlank { null },
                storeName = selectedSite.get().name.ifEmpty { null },
                siteUrl = selectedSite.get().url.ifEmpty { null },
                countryCode = countryCode,
                feeAmount = calculateFeeInCents(countryCode)
            )
        ).collect { paymentStatus ->
            onPaymentStatusChanged(
                order.id,
                customerEmail,
                paymentStatus,
                cardReaderPaymentOrderHelper.getAmountLabel(order)
            )
        }
    }

    private suspend fun onPaymentStatusChanged(
        orderId: Long,
        billingEmail: String,
        paymentStatus: CardPaymentStatus,
        amountLabel: String
    ) {
        paymentDataForRetry = null
        when (paymentStatus) {
            InitializingPayment -> viewState.postValue(LoadingDataState(::onCancelPaymentFlow))
            CollectingPayment -> viewState.postValue(
                cardReaderPaymentReaderTypeStateProvider.provideCollectPaymentState(
                    arguments.cardReaderType,
                    amountLabel,
                    ::onCancelPaymentFlow
                )
            )

            ProcessingPayment -> viewState.postValue(
                cardReaderPaymentReaderTypeStateProvider.provideProcessingPaymentState(
                    arguments.cardReaderType,
                    amountLabel,
                    ::onCancelPaymentFlow
                )
            )

            is ProcessingPaymentCompleted -> {
                cardReaderTrackingInfoKeeper.setPaymentMethodType(paymentStatus.paymentMethodType.stringRepresentation)
                when (paymentStatus.paymentMethodType) {
                    // Interac payments done in one step, without capturing. That's why we track success here
                    PaymentMethodType.INTERAC_PRESENT -> tracker.trackInteracPaymentSucceeded()
                    else -> {}
                }
            }

            CapturingPayment -> viewState.postValue(
                cardReaderPaymentReaderTypeStateProvider.provideCapturingPaymentState(
                    arguments.cardReaderType,
                    amountLabel,
                )
            )

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
        }
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
                ),
                RefundConfig(
                    enableCustomerCancellation = false
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
            InitializingInteracRefund -> viewState.postValue(RefundLoadingDataState(::onCancelPaymentFlow))
            CollectingInteracRefund -> viewState.postValue(
                CollectRefundState(
                    amountLabel,
                    onSecondaryActionClicked = ::onCancelPaymentFlow
                )
            )

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
        }
    }

    private fun onPaymentCompleted(
        paymentStatus: PaymentCompleted,
        orderId: Long,
    ) {
        paymentReceiptHelper.storeReceiptUrl(orderId, paymentStatus.receiptUrl)
        appPrefs.setCardReaderSuccessfulPaymentTime()
        if (arguments.paymentOrRefund.isPOS) {
            launch {
                orderRepository.fetchOrderById(orderId)
                triggerEvent(Exit)
            }
        } else {
            triggerEvent(PlayChaChing)
            showPaymentSuccessfulState()
            reFetchOrder()
        }
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
        cardReaderOnboardingChecker.invalidateCache()
        val onRetryClicked = { retryInteracRefund() }
        val errorType = interacRefundErrorMapper.mapRefundErrorToUiError(error.type)
        viewState.postValue(buildInteracRefundFailedState(errorType, amountLabel, onRetryClicked))
    }

    private fun buildInteracRefundFailedState(
        errorType: InteracRefundFlowError,
        amountLabel: String?,
        onRetryClicked: () -> Unit
    ) = when (errorType) {
        is InteracRefundFlowError.ContactSupportError ->
            FailedRefundState(
                errorType,
                amountLabel,
                primaryLabel = R.string.support_contact,
                onPrimaryActionClicked = { onContactSupportClicked() },
                secondaryLabel = R.string.cancel,
                onSecondaryActionClicked = { onBackPressed() }
            )

        is InteracRefundFlowError.NonRetryableError ->
            FailedRefundState(
                errorType,
                amountLabel,
                R.string.card_reader_interac_refund_refund_failed_ok,
                onPrimaryActionClicked = { onBackPressed() }
            )

        else ->
            FailedRefundState(
                errorType,
                amountLabel,
                onPrimaryActionClicked = onRetryClicked,
                secondaryLabel = R.string.cancel,
                onSecondaryActionClicked = { onBackPressed() }
            )
    }

    private suspend fun emitFailedPaymentState(
        orderId: Long,
        billingEmail: String,
        error: PaymentFailed,
        amountLabel: String
    ) {
        WooLog.e(WooLog.T.CARD_READER, error.errorMessage)
        cardReaderOnboardingChecker.invalidateCache()
        val onRetryClicked = error.paymentDataForRetry?.let {
            {
                retry(orderId, billingEmail, it, amountLabel)
            }
        } ?: { initPaymentFlow(isRetry = true) }
        val config = cardReaderConfigProvider.provideCountryConfigFor(getStoreCountryCode())

        require(config is CardReaderConfigForSupportedCountry) {
            "State mismatch: received unsupported country config"
        }

        val errorType = errorMapper.mapPaymentErrorToUiError(
            error.type,
            config,
            arguments.cardReaderType == CardReaderType.BUILT_IN
        )
        viewState.postValue(buildFailedPaymentState(errorType, amountLabel, onRetryClicked))
    }

    private fun buildFailedPaymentState(errorType: PaymentFlowError, amountLabel: String, onRetryClicked: () -> Unit) =
        when (errorType) {
            is PaymentFlowError.ContactSupportError ->
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = arguments.cardReaderType,
                    errorType = errorType,
                    amountLabel = amountLabel,
                    primaryLabel = R.string.support_contact,
                    onPrimaryActionClicked = { onContactSupportClicked() },
                    secondaryLabel = R.string.cancel,
                    onSecondaryActionClicked = { onBackPressed() }
                )

            is PaymentFlowError.BuiltInReader.NfcDisabled ->
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = arguments.cardReaderType,
                    errorType = errorType,
                    amountLabel = amountLabel,
                    primaryLabel = R.string.card_reader_payment_failed_nfc_disabled_enable_nfc,
                    onPrimaryActionClicked = { onEnableNfcClicked() },
                    secondaryLabel = R.string.cancel,
                    onSecondaryActionClicked = { onBackPressed() }
                )

            is PaymentFlowError.NonRetryableError ->
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = arguments.cardReaderType,
                    errorType = errorType,
                    amountLabel = amountLabel,
                    primaryLabel = R.string.card_reader_payment_payment_failed_ok,
                    onPrimaryActionClicked = { onBackPressed() }
                )

            is PaymentFlowError.PurchaseHardwareReaderError ->
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = arguments.cardReaderType,
                    errorType = errorType,
                    amountLabel = amountLabel,
                    primaryLabel = R.string.card_reader_payment_payment_failed_purchase_hardware_reader,
                    onPrimaryActionClicked = { onPurchaseCardReaderClicked() },
                    secondaryLabel = R.string.cancel,
                    onSecondaryActionClicked = { onBackPressed() }
                )

            else ->
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = arguments.cardReaderType,
                    errorType = errorType,
                    amountLabel = amountLabel,
                    onPrimaryActionClicked = onRetryClicked,
                    secondaryLabel = R.string.cancel,
                    onSecondaryActionClicked = { onBackPressed() }
                )
        }

    private fun showPaymentSuccessfulState() {
        launch {
            val order = requireNotNull(orderRepository.getOrderById(orderId)) { "Order URL not available." }
            val amountLabel = cardReaderPaymentOrderHelper.getAmountLabel(order)
            val onPrintReceiptClicked = {
                onPrintReceiptClicked(amountLabel)
            }
            val onSaveUserClicked = {
                onSaveForLaterClicked()
            }
            val onSendReceiptClicked = { onSendReceiptClicked() }

            if (order.billingAddress.email.isBlank()) {
                viewState.postValue(
                    cardReaderPaymentReaderTypeStateProvider.providePaymentSuccessState(
                        cardReaderType = arguments.cardReaderType,
                        amountLabel,
                        onPrintReceiptClicked,
                        onSendReceiptClicked,
                        onSaveUserClicked
                    )
                )
            } else {
                val receiptSentHint = UiStringRes(
                    R.string.card_reader_payment_reader_receipt_sent,
                    listOf(UiStringText(order.billingAddress.email)),
                    true
                )
                viewState.postValue(
                    cardReaderPaymentReaderTypeStateProvider.providePaymentSuccessfulReceiptSentAutomaticallyState(
                        cardReaderType = arguments.cardReaderType,
                        amountLabel,
                        receiptSentHint,
                        onPrintReceiptClicked,
                        onSaveUserClicked
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

            is ExternalReaderCollectPaymentState ->
                viewState.value = state.copy(
                    hintLabel = type.toHintLabel(false)
                )

            is BuiltInReaderCollectPaymentState ->
                viewState.value = state.copy(
                    hintLabel = type.toHintLabel(false)
                )

            else -> WooLog.e(
                WooLog.T.CARD_READER,
                "Got SDK message when cardReaderPaymentViewModel is in ${viewState.value}"
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
            CARD_REMOVED_TOO_EARLY -> R.string.card_reader_payment_card_removed_too_early
        }

    private fun onSaveForLaterClicked() {
        onCancelPaymentFlow()
    }

    private fun onPrintReceiptClicked(amountWithCurrencyLabel: String) {
        launch {
            viewState.value = PrintingReceiptState(amountWithCurrencyLabel)
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
            val receiptResult = paymentReceiptHelper.getReceiptUrl(orderId)
            if (receiptResult.isSuccess) {
                triggerEvent(
                    PrintReceipt(
                        receiptResult.getOrThrow(),
                        cardReaderPaymentOrderHelper.getReceiptDocumentName(orderId)
                    )
                )
            } else {
                tracker.trackReceiptUrlFetchingFails(
                    errorDescription = receiptResult.exceptionOrNull()?.message ?: "Unknown error",
                )
                triggerEvent(ShowSnackbar(R.string.receipt_fetching_error))
            }
        }
    }

    private fun onSendReceiptClicked() {
        launch {
            tracker.trackEmailReceiptTapped()
            val stateBeforeLoading = viewState.value!!
            viewState.postValue(ViewState.SharingReceiptState)
            val receiptResult = paymentReceiptHelper.getReceiptUrl(orderId)

            if (receiptResult.isSuccess) {
                when (val sharingResult = paymentReceiptShare(receiptResult.getOrThrow(), orderId)) {
                    is PaymentReceiptShare.ReceiptShareResult.Error.FileCreation -> {
                        tracker.trackPaymentsReceiptSharingFailed(sharingResult)
                        triggerEvent(ShowSnackbar(R.string.card_reader_payment_receipt_can_not_be_stored))
                    }
                    is PaymentReceiptShare.ReceiptShareResult.Error.FileDownload -> {
                        tracker.trackPaymentsReceiptSharingFailed(sharingResult)
                        triggerEvent(ShowSnackbar(R.string.card_reader_payment_receipt_can_not_be_downloaded))
                    }
                    is PaymentReceiptShare.ReceiptShareResult.Error.Sharing -> {
                        tracker.trackPaymentsReceiptSharingFailed(sharingResult)
                        triggerEvent(ShowSnackbar(R.string.card_reader_payment_email_client_not_found))
                    }
                    PaymentReceiptShare.ReceiptShareResult.Success -> {
                        // no-op
                    }
                }
            } else {
                tracker.trackReceiptUrlFetchingFails(
                    errorDescription = receiptResult.exceptionOrNull()?.message ?: "Unknown error",
                )
                triggerEvent(ShowSnackbar(R.string.receipt_fetching_error))
            }

            viewState.postValue(stateBeforeLoading)
        }
    }

    fun onPrintResult(result: PrintJobResult) {
        showPaymentSuccessfulState()

        launch {
            when (result) {
                CANCELLED -> tracker.trackPrintReceiptCancelled()
                FAILED -> tracker.trackPrintReceiptFailed()
                STARTED -> tracker.trackPrintReceiptSucceeded()
            }
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
        onCancelPaymentFlow()
        disconnectFromReaderIfPaymentFailedState()
    }

    private fun onContactSupportClicked() {
        tracker.trackPaymentFailedContactSupportTapped()
        onCancelPaymentFlow()
        triggerEvent(ContactSupport)
    }

    private fun onEnableNfcClicked() {
        tracker.trackPaymentFailedEnabledNfcTapped()
        onCancelPaymentFlow()
        triggerEvent(EnableNfc)
    }

    private fun onPurchaseCardReaderClicked() {
        onCancelPaymentFlow()
        val storeCountryCode = wooStore.getStoreCountryCode(selectedSite.get())
        triggerEvent(
            PurchaseCardReader(
                "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$storeCountryCode"
            )
        )
    }

    private fun onCancelPaymentFlow() {
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

    private fun disconnectFromReaderIfPaymentFailedState() {
        val readerStatus = cardReaderManager.readerStatus.value
        if (readerStatus is CardReaderStatus.Connected) {
            if (ReaderType.isBuiltInReaderType(readerStatus.cardReader.type) &&
                (viewState.value is BuiltInReaderFailedPaymentState || viewState.value is FailedRefundState)
            ) {
                launch { cardReaderManager.disconnectReader() }
            }
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

    private suspend fun getStoreCountryCode(): String {
        return withContext(dispatchers.io) {
            requireNotNull(
                wooStore.getStoreCountryCode(
                    selectedSite.get()
                )
            ) { "Store's country code not found." }
        }
    }

    private fun calculateFeeInCents(countryCode: String) =
        if (countryCode == "CA") {
            CANADA_FEE_FLAT_IN_CENTS
        } else {
            null
        }
}
