package com.woocommerce.android.ui.payments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_CAMPAIGN_NAME
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_UPSELL_CARD_READERS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_CARD
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_CASH
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_COLLECT_LINK
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.compose.component.banner.BannerDisplayEligibilityChecker
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel.TakePaymentViewState.Loading
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.ORDER
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType.SIMPLE
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Refund
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val bannerDisplayEligibilityChecker: BannerDisplayEligibilityChecker,
) : ScopedViewModel(savedState) {
    private val navArgs: SelectPaymentMethodFragmentArgs by savedState.navArgs()
    val shouldShowUpsellCardReaderDismissDialog: MutableLiveData<Boolean> = MutableLiveData(false)

    private val viewState = MutableLiveData<TakePaymentViewState>(Loading)
    val viewStateData: LiveData<TakePaymentViewState> = viewState

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
                        order = orderStore.getOrderByIdAndSite(param.orderId, selectedSite.get())!!.let {
                            orderMapper.toAppModel(it)
                        }
                        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode ?: ""
                        orderTotal = currencyFormatter.formatCurrency(order.total, currencyCode)
                        viewState.value = TakePaymentViewState.Success(
                            paymentUrl = order.paymentUrl,
                            orderTotal = currencyFormatter.formatCurrency(order.total, currencyCode),
                            isPaymentCollectableWithCardReader = cardPaymentCollectibilityChecker.isCollectable(order)
                        )
                    }
                    is Refund -> triggerEvent(NavigateToCardReaderRefundFlow(param))
                }
            }
        }.exhaustive
    }

    fun onCashPaymentClicked() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
            mapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_CASH,
                cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
            )
        )
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = R.string.simple_payments_cash_dlg_title,
                messageId = R.string.simple_payments_cash_dlg_message,
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
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                    mapOf(
                        AnalyticsTracker.KEY_AMOUNT to orderTotal,
                        AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_CASH,
                        cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
                    )
                )
                updateOrderStatus(Order.Status.Completed.value)
            }
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        }
    }

    fun onSharePaymentUrlClicked() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
            mapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
            )
        )
        triggerEvent(SharePaymentUrl(selectedSite.get().name, order.paymentUrl))
    }

    fun onSharePaymentUrlCompleted() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
            mapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_LINK,
                cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
            )
        )
        launch {
            updateOrderStatus(Order.Status.Pending.value)
        }
    }

    fun onCardPaymentClicked() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.PAYMENTS_FLOW_COLLECT,
            mapOf(
                AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
            )
        )
        triggerEvent(NavigateToCardReaderPaymentFlow(cardReaderPaymentFlowParam))
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
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.PAYMENTS_FLOW_COMPLETED,
                    mapOf(
                        AnalyticsTracker.KEY_AMOUNT to orderTotal,
                        AnalyticsTracker.KEY_PAYMENT_METHOD to VALUE_SIMPLE_PAYMENTS_COLLECT_CARD,
                        cardReaderPaymentFlowParam.toAnalyticsFlowParams(),
                    )
                )
                delay(DELAY_MS)
                triggerEvent(MultiLiveEvent.Event.Exit)
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

    fun onBackPressed() {
        // Simple payments flow is not canceled if we going back from this fragment
        if (cardReaderPaymentFlowParam.paymentType == ORDER) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.PAYMENTS_FLOW_CANCELED,
                mapOf(AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW)
            )
        }
    }

    private suspend fun updateOrderStatus(statusKey: String) {
        val statusModel = withContext(dispatchers.io) {
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), statusKey)
                ?: error("Couldn't find a status with key $statusKey")
        }

        orderStore.updateOrderStatus(
            cardReaderPaymentFlowParam.orderId,
            selectedSite.get(),
            statusModel
        ).collect { result ->
            when (result) {
                is WCOrderStore.UpdateOrderResult.OptimisticUpdateResult -> {
                    triggerEvent(MultiLiveEvent.Event.Exit)
                }
                is WCOrderStore.UpdateOrderResult.RemoteUpdateResult -> {
                    if (result.event.isError) {
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
                }
            }
        }
    }

    private fun Payment.toAnalyticsFlowParams() =
        AnalyticsTracker.KEY_FLOW to when (paymentType) {
            SIMPLE -> AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW
            ORDER -> AnalyticsTracker.VALUE_ORDER_PAYMENTS_FLOW
        }

    private suspend fun getStoreCountryCode(): String? {
        return withContext(dispatchers.io) {
            wooCommerceStore.getStoreCountryCode(selectedSite.get()) ?: null.also {
                WooLog.e(WooLog.T.CARD_READER, "Store's country code not found.")
            }
        }
    }

    fun onCtaClicked() {
        launch {
            triggerEvent(OpenPurchaseCardReaderLink(bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl()))
        }
    }

    fun onDismissClicked() {
        shouldShowUpsellCardReaderDismissDialog.value = true
        triggerEvent(DismissCardReaderUpsellBanner)
    }

    fun onRemindLaterClicked(currentTimeInMillis: Long) {
        shouldShowUpsellCardReaderDismissDialog.value = false
        bannerDisplayEligibilityChecker.onRemindLaterClicked(currentTimeInMillis)
        triggerEvent(DismissCardReaderUpsellBannerViaRemindMeLater)
    }

    fun onDontShowAgainClicked() {
        shouldShowUpsellCardReaderDismissDialog.value = false
        bannerDisplayEligibilityChecker.onDontShowAgainClicked()
        triggerEvent(DismissCardReaderUpsellBannerViaDontShowAgain)
    }

    fun canShowCardReaderUpsellBanner(currentTimeInMillis: Long): Boolean {
        return bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(currentTimeInMillis).also { trackable ->
            if (trackable) {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.FEATURE_CARD_SHOWN,
                    mapOf(
                        KEY_BANNER_SOURCE to KEY_BANNER_PAYMENTS,
                        KEY_BANNER_CAMPAIGN_NAME to KEY_BANNER_UPSELL_CARD_READERS
                    )
                )
            }
        }
    }

    sealed class TakePaymentViewState {
        object Loading : TakePaymentViewState()
        data class Success(
            val paymentUrl: String,
            val orderTotal: String,
            val isPaymentCollectableWithCardReader: Boolean,
        ) : TakePaymentViewState()
    }

    object DismissCardReaderUpsellBanner : MultiLiveEvent.Event()
    object DismissCardReaderUpsellBannerViaRemindMeLater : MultiLiveEvent.Event()
    object DismissCardReaderUpsellBannerViaDontShowAgain : MultiLiveEvent.Event()
    data class OpenPurchaseCardReaderLink(val url: String) : MultiLiveEvent.Event()

    data class SharePaymentUrl(
        val storeName: String,
        val paymentUrl: String
    ) : MultiLiveEvent.Event()

    data class NavigateToCardReaderHubFlow(
        val cardReaderFlowParam: CardReadersHub
    ) : MultiLiveEvent.Event()

    data class NavigateToCardReaderPaymentFlow(
        val cardReaderFlowParam: Payment
    ) : MultiLiveEvent.Event()

    data class NavigateToCardReaderRefundFlow(
        val cardReaderFlowParam: Refund
    ) : MultiLiveEvent.Event()

    companion object {
        private const val DELAY_MS = 1L
    }
}
