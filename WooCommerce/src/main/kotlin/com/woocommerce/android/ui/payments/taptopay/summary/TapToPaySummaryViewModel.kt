package com.woocommerce.android.ui.payments.taptopay.summary

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class TapToPaySummaryViewModel @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val refundStore: WCRefundStore,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    private val currencyFormatter: CurrencyFormatter,
    wooStore: WooCommerceStore,
    cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: TapToPaySummaryFragmentArgs by savedState.navArgs()

    private val countryConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(
        wooStore.getStoreCountryCode(selectedSite.get())
    ) as CardReaderConfigForSupportedCountry

    private val _viewState = MutableLiveData(
        UiState(messageWithAmount = countryConfig.buildPaymentMessage())
    )
    val viewState: LiveData<UiState> = _viewState

    init {
        analyticsTrackerWrapper.track(AnalyticsEvent.TAP_TO_PAY_SUMMARY_SHOWN)

        handleFlowParam(navArgs.testTapToPayFlow)
    }

    @VisibleForTesting
    internal fun handleFlowParam(flow: TapToPaySummaryFragment.TestTapToPayFlow) =
        when (flow) {
            TapToPaySummaryFragment.TestTapToPayFlow.BeforePayment -> {
                // no-op
            }

            is TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment -> {
                launch {
                    _viewState.value = _viewState.value!!.copy(isProgressVisible = true)
                    triggerEvent(ShowSnackbar(R.string.card_reader_tap_to_pay_explanation_refunding_payment))
                    autoRefundTestPayment(flow.order)
                    _viewState.value = _viewState.value!!.copy(isProgressVisible = false)
                }
                Unit
            }
        }

    fun onTryPaymentClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.TAP_TO_PAY_SUMMARY_TRY_PAYMENT_TAPPED)
        launch {
            _viewState.value = _viewState.value!!.copy(isProgressVisible = true)
            val result = orderCreateEditRepository.createSimplePaymentOrder(
                countryConfig.minimumAllowedChargeAmount,
                customerNote = resourceProvider.getString(R.string.card_reader_tap_to_pay_test_payment_note),
                isTaxable = false,
            )
            result.fold(
                onSuccess = {
                    triggerEvent(StartTryPaymentFlow(it))
                },
                onFailure = {
                    analyticsTrackerWrapper.track(
                        AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                        mapOf(
                            AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_TAP_TO_PAY_SOURCE_TRY_PAYMENT_PROMPT,
                            AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_TTP_TRY_PAYMENT_FLOW,
                        )
                    )
                    triggerEvent(ShowSnackbar(R.string.card_reader_tap_to_pay_explanation_test_payment_error))
                }
            )
            _viewState.value = _viewState.value!!.copy(isProgressVisible = false)
        }
    }

    fun onBackClicked() {
        triggerEvent(Event.Exit)
    }

    fun onLearnMoreClicked() {
        triggerEvent(NavigateTTPAboutScreen(countryConfig))
    }

    private suspend fun autoRefundTestPayment(order: Order) {
        refundStore.createAmountRefund(
            selectedSite.get(),
            order.id,
            calculateRefundAmount(order),
            resourceProvider.getString(R.string.tap_to_pay_refund_reason),
            true,
        ).apply {
            if (!isError) {
                analyticsTrackerWrapper.track(AnalyticsEvent.CARD_PRESENT_TAP_TO_PAY_TEST_PAYMENT_REFUND_SUCCESS)
                showSuccessfulRefundNotification(order.id)
            } else {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.CARD_PRESENT_TAP_TO_PAY_TEST_PAYMENT_REFUND_FAILED,
                    mapOf(AnalyticsTracker.KEY_ERROR_DESC to this.error.message)
                )
                triggerEvent(ShowSnackbar(R.string.card_reader_tap_to_pay_explanation_refund_failed))
                triggerEvent(NavigateToOrderDetails(order.id))
            }
        }
    }

    private fun calculateRefundAmount(order: Order) =
        if (order.feesLines.firstOrNull()?.taxStatus == Order.FeeLine.FeeLineTaxStatus.TAXABLE) {
            order.total
        } else {
            order.total - order.totalTax
        }

    private fun showSuccessfulRefundNotification(orderId: Long) {
        triggerEvent(
            ShowSuccessfulRefundNotification(
                message = R.string.card_reader_tap_to_pay_successful_refund_message,
                actionLabel = R.string.card_reader_tap_to_pay_successful_refund_action_label,
                action = {
                    triggerEvent(NavigateToOrderDetails(orderId))
                }
            )
        )
    }

    private fun CardReaderConfigForSupportedCountry.buildPaymentMessage(): String {
        val amount = currencyFormatter.formatCurrency(
            rawValue = minimumAllowedChargeAmount.toPlainString(),
            currencyCode = currency,
        )
        return resourceProvider.getString(
            R.string.card_reader_tap_to_pay_explanation_try_and_refund_with_amount,
            amount,
        )
    }

    data class UiState(
        val isProgressVisible: Boolean = false,
        val messageWithAmount: String
    )

    data class StartTryPaymentFlow(val order: Order) : Event()

    data class NavigateToOrderDetails(val orderId: Long) : Event()

    data class ShowSuccessfulRefundNotification(
        @StringRes val message: Int,
        @StringRes val actionLabel: Int,
        val action: () -> Unit
    ) : Event()

    data class NavigateTTPAboutScreen(
        val cardReaderConfigForSupportedCountry: CardReaderConfigForSupportedCountry
    ) : Event()
}
