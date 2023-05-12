package com.woocommerce.android.ui.payments.methodselection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PAYMENT_CARD_READER_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TIME_ELAPSED_SINCE_ADD_NEW_ORDER_IN_MILLIS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CARD_READER_TYPE_BUILT_IN
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CARD_READER_TYPE_EXTERNAL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_CASH
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tracker.OrderDurationRecorder
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.SIMPLE
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.TRY_TAP_TO_PAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Refund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.BUILT_IN
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.EXTERNAL
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodViewState.Loading
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodViewState.Success
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus.Result.NotAvailable
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class SelectPaymentMethodViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val orderMapper: OrderMapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val cardPaymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker,
    private val learnMoreUrlProvider: LearnMoreUrlProvider,
    private val cardReaderTracker: CardReaderTracker,
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus,
    private val appPrefs: AppPrefs = AppPrefs,
    private val isScanToPayAvailable: IsScanToPayAvailable,
) : ScopedViewModel(savedState) {
    private val navArgs: SelectPaymentMethodFragmentArgs by savedState.navArgs()

    private val _viewState = MutableLiveData<SelectPaymentMethodViewState>(Loading)
    val viewStateData: LiveData<SelectPaymentMethodViewState> = _viewState

    private lateinit var order: Order
    private lateinit var orderTotal: String
    private lateinit var cardReaderPaymentFlowParam: Payment

    init {
        launch {
            checkStatus()
        }
    }

    private suspend fun checkStatus() {
        when (val param = navArgs.cardReaderFlowParam) {
            is CardReadersHub -> triggerEvent(NavigateToCardReaderHubFlow(param))
            is PaymentOrRefund -> {
                when (param) {
                    is Payment -> {
                        // stay on screen
                        cardReaderPaymentFlowParam = param
                        showPaymentState(param)
                    }

                    is Refund -> triggerEvent(NavigateToCardReaderRefundFlow(param, EXTERNAL))
                }
            }
        }.exhaustive
    }

    private suspend fun showPaymentState(param: PaymentOrRefund) {
        order = orderStore.getOrderByIdAndSite(param.orderId, selectedSite.get())!!.let {
            orderMapper.toAppModel(it)
        }
        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""
        orderTotal = currencyFormatter.formatCurrency(order.total, currencyCode)
        _viewState.value = buildSuccessState(
            currencyCode = currencyCode,
            isPaymentCollectableWithCardReader = cardPaymentCollectibilityChecker.isCollectable(order),
            isPaymentCollectableWithTapToPay = isTapToPayAvailable()
        )
    }

    private fun buildSuccessState(
        currencyCode: String,
        isPaymentCollectableWithCardReader: Boolean,
        isPaymentCollectableWithTapToPay: Boolean,
    ) = Success(
        paymentUrl = order.paymentUrl,
        orderTotal = currencyFormatter.formatCurrency(order.total, currencyCode),
        isPaymentCollectableWithExternalCardReader = isPaymentCollectableWithCardReader,
        isPaymentCollectableWithTapToPay = isPaymentCollectableWithCardReader && isPaymentCollectableWithTapToPay,
        isScanToPayAvailable = isScanToPayAvailable(),
        learMoreIpp = SelectPaymentMethodViewState.LearMoreIpp(
            label = UiStringRes(
                R.string.card_reader_connect_learn_more,
                containsHtml = true
            ),
            onClick = ::onLearnMoreIppClicked
        )
    )

    private fun isTapToPayAvailable(): Boolean {
        val result = tapToPayAvailabilityStatus()
        return if (result is NotAvailable) {
            cardReaderTracker.trackTapToPayNotAvailableReason(result)
            false
        } else {
            true
        }
    }

    fun onCashPaymentClicked() {
        trackPaymentMethodSelection(VALUE_SIMPLE_PAYMENTS_COLLECT_CASH)
        val messageIdForPaymentType = when (cardReaderPaymentFlowParam.paymentType) {
            SIMPLE, TRY_TAP_TO_PAY -> R.string.simple_payments_cash_dlg_message
            ORDER -> R.string.existing_order_cash_dlg_message
        }
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = R.string.simple_payments_cash_dlg_title,
                messageId = messageIdForPaymentType,
                positiveButtonId = R.string.simple_payments_cash_dlg_button,
                positiveBtnAction = { _, _ ->
                    onCashPaymentConfirmed()
                },
                negativeButtonId = R.string.cancel
            )
        )
    }

    /**
     * User has confirmed the cash payment, so mark it as completed
     */
    private fun onCashPaymentConfirmed() {
        if (networkStatus.isConnected()) {
            launch {
                trackPaymentMethodCompletion(
                    VALUE_SIMPLE_PAYMENTS_COLLECT_CASH,
                    extraField = AnalyticsTracker.KEY_AMOUNT to orderTotal
                )
                updateOrderStatus(Order.Status.Completed.value).handleOrderUpdateResultBeforeExit()
            }
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        }
    }

    fun onSharePaymentUrlClicked() {
        trackPaymentMethodSelection(VALUE_SIMPLE_PAYMENTS_COLLECT_LINK)
        triggerEvent(SharePaymentUrl(selectedSite.get().name, order.paymentUrl))
    }

    fun onSharePaymentUrlCompleted() {
        trackPaymentMethodCompletion(VALUE_SIMPLE_PAYMENTS_COLLECT_LINK)
        launch {
            updateOrderStatus(Order.Status.Pending.value).handleOrderUpdateResultBeforeExit()
        }
    }

    fun onBtReaderClicked() {
        OrderDurationRecorder.recordCardPaymentStarted()
        trackPaymentMethodSelection(VALUE_SIMPLE_PAYMENTS_COLLECT_CARD, VALUE_CARD_READER_TYPE_EXTERNAL)
        triggerEvent(NavigateToCardReaderPaymentFlow(cardReaderPaymentFlowParam, EXTERNAL))
    }

    fun onTapToPayClicked() {
        trackPaymentMethodSelection(VALUE_SIMPLE_PAYMENTS_COLLECT_CARD, VALUE_CARD_READER_TYPE_BUILT_IN)
        appPrefs.setTTPWasUsedAtLeastOnce()
        triggerEvent(NavigateToCardReaderPaymentFlow(cardReaderPaymentFlowParam, BUILT_IN))
    }

    fun onConnectToReaderResultReceived(connected: Boolean) {
        if (!connected) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                    cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
                )
            )
        }
    }

    fun onCardReaderPaymentCompleted() {
        launch {
            // this function is called even when the payment fails - in other words, it tells us
            // the card reader flow completed but not necessarily successfully -, so we check the
            // status of the order to determine whether payment succeeded
            val status = orderStore.getOrderByIdAndSite(cardReaderPaymentFlowParam.orderId, selectedSite.get())?.status
            if (status == CoreOrderStatus.COMPLETED.value) {
                trackPaymentMethodCompletion(
                    VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                    extraField = AnalyticsTracker.KEY_AMOUNT to orderTotal
                )
                delay(DELAY_MS)
                exitFlow()
            } else {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                        cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
                    )
                )
            }
        }
    }

    fun onScanToPayClicked() {
        trackPaymentMethodSelection(AnalyticsTracker.VALUE_SCAN_TO_PAY_PAYMENT_FLOW)
        launch {
            updateOrderStatus(Order.Status.Pending.value).collect { result ->
                _viewState.value = Loading
                when (result) {
                    is WCOrderStore.UpdateOrderResult.RemoteUpdateResult -> {
                        if (result.event.isError) {
                            handleUpdateOrderStatusError()
                        } else {
                            triggerEvent(SharePaymentUrlViaQr(order.paymentUrl))
                        }
                        showPaymentState(cardReaderPaymentFlowParam)
                    }

                    is WCOrderStore.UpdateOrderResult.OptimisticUpdateResult -> {
                        // we need to make sure that remote call has been successful
                        // otherwise the order is not collectable
                    }
                }
            }
        }
    }

    fun onBackPressed() {
        // Simple payments flow is not canceled if we going back from this fragment
        if (cardReaderPaymentFlowParam.paymentType == ORDER) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.PAYMENTS_FLOW_CANCELED,
                mapOf(AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW)
            )
        }
    }

    fun onScanToPayCompleted() {
        trackPaymentMethodCompletion(AnalyticsTracker.VALUE_SCAN_TO_PAY_PAYMENT_FLOW)
        launch {
            delay(DELAY_MS)
            exitFlow()
        }
    }

    private fun trackPaymentMethodSelection(paymentMethodType: String, cardReaderType: String? = null) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
            mutableMapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to paymentMethodType,
                AnalyticsTracker.KEY_ORDER_ID to order.id,
                cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
            ).also { mutableMap ->
                cardReaderType?.let { mutableMap[KEY_PAYMENT_CARD_READER_TYPE] = it }
                OrderDurationRecorder.millisecondsSinceOrderAddNew().getOrNull()?.let { timeElapsed ->
                    mutableMap[KEY_TIME_ELAPSED_SINCE_ADD_NEW_ORDER_IN_MILLIS] = timeElapsed.toString()
                }
            }
        )
    }

    private fun trackPaymentMethodCompletion(paymentMethodType: String, extraField: Pair<String, String>? = null) {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
            mutableMapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to paymentMethodType,
                AnalyticsTracker.KEY_ORDER_ID to order.id,
                cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
            ).apply {
                extraField?.let { put(it.first, it.second) }
            }
        )
    }

    private suspend fun updateOrderStatus(statusKey: String): Flow<WCOrderStore.UpdateOrderResult> {
        val statusModel = withContext(dispatchers.io) {
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), statusKey)
                ?: WCOrderStatusModel(statusKey = statusKey).apply {
                    label = statusKey
                }
        }

        return orderStore.updateOrderStatus(
            cardReaderPaymentFlowParam.orderId,
            selectedSite.get(),
            statusModel
        )
    }

    private suspend fun Flow<WCOrderStore.UpdateOrderResult>.handleOrderUpdateResultBeforeExit() {
        collect { result ->
            when (result) {
                is WCOrderStore.UpdateOrderResult.OptimisticUpdateResult -> exitFlow()
                is WCOrderStore.UpdateOrderResult.RemoteUpdateResult -> {
                    if (result.event.isError) {
                        handleUpdateOrderStatusError()
                    }
                }
            }
        }
    }

    private fun handleUpdateOrderStatusError() {
        triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.order_error_update_general))
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PAYMENTS_FLOW_FAILED,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to
                    AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_PAYMENT_METHOD,
                cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
            )
        )
    }

    private fun exitFlow() {
        triggerEvent(
            when (cardReaderPaymentFlowParam.paymentType) {
                SIMPLE -> NavigateBackToHub(CardReadersHub())
                TRY_TAP_TO_PAY -> NavigateToTapToPaySummary(order)
                ORDER -> NavigateBackToOrderList
            }
        )
    }

    private fun Payment.toAnalyticsFlowParams() =
        AnalyticsTracker.KEY_FLOW to when (paymentType) {
            SIMPLE -> AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW
            ORDER -> AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW
            TRY_TAP_TO_PAY -> AnalyticsTracker.VALUE_TTP_TRY_PAYMENT_FLOW
        }

    private fun onLearnMoreIppClicked() {
        cardReaderTracker.trackIPPLearnMoreClicked(LEARN_MORE_SOURCE)
        triggerEvent(
            OpenGenericWebView(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
                )
            )
        )
    }

    companion object {
        private const val DELAY_MS = 1L
        const val UTM_CAMPAIGN = "feature_announcement_card"
        const val UTM_SOURCE = "payment_method"
        const val UTM_CONTENT = "upsell_card_readers"
        const val LEARN_MORE_SOURCE = "payment_methods"
    }
}

class IsScanToPayAvailable @Inject constructor() {
    operator fun invoke() = FeatureFlag.IPP_SCAN_TO_PAY.isEnabled()
}
