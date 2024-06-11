package com.woocommerce.android.ui.payments.taptopay.summary

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
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TapToPaySummaryViewModelTest : BaseUnitTest() {
    private val order: Order = mock {
        on { id }.thenReturn(1L)
        on { total }.thenReturn(BigDecimal.valueOf(0.6))
        on { totalTax }.thenReturn(BigDecimal.valueOf(0.1))
    }
    private val site: SiteModel = mock()
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val refundStore: WCRefundStore = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.tap_to_pay_refund_reason) }.thenReturn("Test Tap To Pay payment auto refund")
        on { getString(R.string.card_reader_tap_to_pay_test_payment_note) }.thenReturn("Test payment")
        on { getString(R.string.card_reader_tap_to_pay_explanation_try_and_refund_with_amount, "$0.50") }.thenReturn(
            "Try a $0.50 payment with your debit or credit card."
        )
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(site)
    }
    private val wooStore: WooCommerceStore = mock {
        on { getStoreCountryCode(site) }.thenReturn("US")
    }
    private val cardReaderConfig = mock<CardReaderConfigForSupportedCountry> {
        on { minimumAllowedChargeAmount }.thenReturn(BigDecimal("0.5"))
        on { currency }.thenReturn("USD")
    }
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider = mock {
        on { provideCountryConfigFor("US") }.thenReturn(cardReaderConfig)
    }
    private val currencyFormatter: CurrencyFormatter = mock {
        on { formatCurrency("0.5", "USD") }.thenReturn("$0.50")
    }

    @Test
    fun `given us cardconfig, when viewmodel init, then message with usd minim amount constructed`() {
        // GIVEN
        whenever(cardReaderConfig.currency).thenReturn("USD")
        whenever(cardReaderConfig.minimumAllowedChargeAmount).thenReturn(BigDecimal("0.5"))
        whenever(currencyFormatter.formatCurrency("0.5", "USD")).thenReturn("$0.50")
        whenever(
            resourceProvider.getString(
                R.string.card_reader_tap_to_pay_explanation_try_and_refund_with_amount,
                "$0.50"
            )
        ).thenReturn("Try a $0.50 payment with your debit or credit card.")
        val viewModel = initViewModel()

        // THEN
        assertThat(viewModel.viewState.value!!.messageWithAmount).isEqualTo(
            "Try a $0.50 payment with your debit or credit card."
        )
    }

    @Test
    fun `give order creation error, when onTryPaymentClicked, then show snackbar`() = testBlocking {
        // GIVEN
        whenever(
            orderCreateEditRepository.createSimplePaymentOrder(
                BigDecimal.valueOf(0.5),
                customerNote = "Test payment",
                isTaxable = false,
            )
        ).thenReturn(
            Result.failure(Exception())
        )
        val viewModel = initViewModel()

        // WHEN
        viewModel.onTryPaymentClicked()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            ShowSnackbar(R.string.card_reader_tap_to_pay_explanation_test_payment_error)
        )
    }

    @Test
    fun `give order creation success, when onTryPaymentClicked, then navigate to simple payment`() =
        testBlocking {
            // GIVEN
            val order = mock<Order>()
            whenever(
                orderCreateEditRepository.createSimplePaymentOrder(
                    BigDecimal.valueOf(0.5),
                    customerNote = "Test payment",
                    isTaxable = false,
                )
            ).thenReturn(
                Result.success(order)
            )
            val viewModel = initViewModel()

            // WHEN
            viewModel.onTryPaymentClicked()

            // THEN
            assertThat((viewModel.event.value as TapToPaySummaryViewModel.StartTryPaymentFlow).order).isEqualTo(order)
        }

    @Test
    fun `when onTryPaymentClicked, then progress is shown and then hidden`() = testBlocking {
        // GIVEN
        whenever(
            orderCreateEditRepository.createSimplePaymentOrder(
                BigDecimal.valueOf(0.5),
                customerNote = "Test payment",
                isTaxable = false,
            )
        ).thenReturn(
            Result.failure(Exception())
        )
        val viewModel = initViewModel()

        val states = viewModel.viewState.captureValues()

        // WHEN
        viewModel.onTryPaymentClicked()

        // THEN
        assertThat(states[0].isProgressVisible).isFalse()
        assertThat(states[1].isProgressVisible).isTrue()
        assertThat(states[2].isProgressVisible).isFalse()
    }

    @Test
    fun `when onTryPaymentClicked, then ttp try payment tracked`() = testBlocking {
        // GIVEN
        whenever(
            orderCreateEditRepository.createSimplePaymentOrder(
                BigDecimal.valueOf(0.5),
                customerNote = "Test payment",
                isTaxable = false,
            )
        ).thenReturn(
            Result.failure(Exception())
        )
        val viewModel = initViewModel()

        // WHEN
        viewModel.onTryPaymentClicked()

        // THEN
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.TAP_TO_PAY_SUMMARY_TRY_PAYMENT_TAPPED
        )
    }

    @Test
    fun `given error creating order, when onTryPaymentClicked, then card reader failed tracked`() = testBlocking {
        // GIVEN
        whenever(
            orderCreateEditRepository.createSimplePaymentOrder(
                BigDecimal.valueOf(0.5),
                customerNote = "Test payment",
                isTaxable = false,
            )
        ).thenReturn(
            Result.failure(Exception())
        )
        val viewModel = initViewModel()

        // WHEN
        viewModel.onTryPaymentClicked()

        // THEN
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.PAYMENTS_FLOW_FAILED,
            mapOf(
                "source" to "tap_to_pay_try_a_payment_prompt",
                "flow" to "tap_to_pay_try_a_payment",
            )
        )
    }

    @Test
    fun `given 30 cents min allowed charged config, when onTryPaymentClicked, then order created with 30 cents value`() =
        testBlocking {
            // GIVEN
            whenever(cardReaderConfig.minimumAllowedChargeAmount).thenReturn(BigDecimal.valueOf(0.3))
            whenever(cardReaderConfig.currency).thenReturn("GDP")
            whenever(currencyFormatter.formatCurrency("0.3", "GDP")).thenReturn("£0.30")
            whenever(
                resourceProvider.getString(
                    R.string.card_reader_tap_to_pay_explanation_try_and_refund_with_amount,
                    "£0.30"
                )
            ).thenReturn("Try a £0.30 payment with your debit or credit card.")
            val viewModel = initViewModel()

            // WHEN
            viewModel.onTryPaymentClicked()

            // THEN
            verify(orderCreateEditRepository).createSimplePaymentOrder(
                BigDecimal.valueOf(0.3),
                customerNote = "Test payment",
                isTaxable = false,
            )
        }

    @Test
    fun `when onBackClicked, then exit emitted`() {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onBackClicked()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(Exit)
    }

    @Test
    fun `when view model started, then view shown tracked`() {
        // WHEN
        initViewModel()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.TAP_TO_PAY_SUMMARY_SHOWN)
    }

    @Test
    fun `given try ttp payment flow and autorefund success, when vm created, then show success snack bar`() =
        testBlocking {
            // GIVEN
            whenever(
                refundStore.createAmountRefund(
                    selectedSite.get(),
                    order.id,
                    BigDecimal(0.5),
                    "Test Tap To Pay payment auto refund",
                    true,
                )
            ).thenReturn(WooResult(mock<WCRefundModel>()))

            // WHEN
            val viewModel = initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))

            // THEN
            val event = viewModel.event.value as TapToPaySummaryViewModel.ShowSuccessfulRefundNotification
            assertThat(event.actionLabel).isEqualTo(R.string.card_reader_tap_to_pay_successful_refund_action_label)
            assertThat(event.message).isEqualTo(R.string.card_reader_tap_to_pay_successful_refund_message)
        }

    @Test
    fun `given try ttp payment flow and autorefund success, when vm created, then refund success event tracked`() =
        testBlocking {
            // GIVEN
            whenever(
                refundStore.createAmountRefund(
                    selectedSite.get(),
                    order.id,
                    BigDecimal(0.5),
                    "Test Tap To Pay payment auto refund",
                    true,
                )
            ).thenReturn(WooResult(mock<WCRefundModel>()))

            // WHEN
            initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CARD_PRESENT_TAP_TO_PAY_TEST_PAYMENT_REFUND_SUCCESS
            )
        }

    @Test
    fun `given try ttp payment flow and autorefund failed, when vm created, then refund failed event tracked`() =
        testBlocking {
            // GIVEN
            val error = WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR)
            whenever(
                refundStore.createAmountRefund(
                    selectedSite.get(),
                    order.id,
                    BigDecimal(0.5),
                    "Test Tap To Pay payment auto refund",
                    true,
                )
            ).thenReturn(WooResult(error))

            // WHEN
            initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CARD_PRESENT_TAP_TO_PAY_TEST_PAYMENT_REFUND_FAILED,
                mapOf(AnalyticsTracker.KEY_ERROR_DESC to error.message)
            )
        }

    @Test
    fun `given try ttp payment flow with taxable status, when vm created, then correct refunded is calculated`() =
        testBlocking {
            // GIVEN
            val feesLine: Order.FeeLine = mock {
                on { taxStatus }.thenReturn(Order.FeeLine.FeeLineTaxStatus.TAXABLE)
            }
            whenever(order.feesLines).thenReturn(listOf(feesLine))
            whenever(order.total).thenReturn(BigDecimal(10))
            val captor = argumentCaptor<BigDecimal>()
            whenever(
                refundStore.createAmountRefund(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(WooResult(mock<WCRefundModel>()))

            // WHEN
            initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))

            // THEN
            verify(refundStore).createAmountRefund(
                any(),
                any(),
                captor.capture(),
                any(),
                any()
            )

            assertThat(captor.firstValue).isEqualTo(BigDecimal(10))
        }

    @Test
    fun `given try ttp payment flow with NONE status, when vm created, then correct refunded is calculated`() =
        testBlocking {
            // GIVEN
            val feesLine: Order.FeeLine = mock {
                on { taxStatus }.thenReturn(Order.FeeLine.FeeLineTaxStatus.NONE)
            }
            whenever(order.feesLines).thenReturn(listOf(feesLine))
            whenever(order.total).thenReturn(BigDecimal(10))
            whenever(order.totalTax).thenReturn(BigDecimal(1))
            val captor = argumentCaptor<BigDecimal>()
            whenever(
                refundStore.createAmountRefund(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(WooResult(mock<WCRefundModel>()))

            // WHEN
            initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))

            // THEN
            verify(refundStore).createAmountRefund(
                any(),
                any(),
                captor.capture(),
                any(),
                any()
            )

            assertThat(captor.firstValue).isEqualTo(BigDecimal(9))
        }

    @Test
    fun `given try ttp payment flow with none status, when vm created, then correct amount refunded`() =
        testBlocking {
            // GIVEN
            val feesLine: Order.FeeLine = mock {
                on { taxStatus }.thenReturn(Order.FeeLine.FeeLineTaxStatus.NONE)
            }
            whenever(order.feesLines).thenReturn(listOf(feesLine))
            whenever(
                refundStore.createAmountRefund(
                    selectedSite.get(),
                    order.id,
                    order.total - order.totalTax,
                    "Test Tap To Pay payment auto refund",
                    true,
                )
            ).thenReturn(WooResult(mock<WCRefundModel>()))

            // WHEN
            val viewModel = initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(
                TapToPaySummaryViewModel.ShowSuccessfulRefundNotification::class.java
            )
        }

    @Test
    fun `given try ttp payment flow and autorefund success, when vm created, then UIState loading true and then false`() =
        testBlocking {
            // GIVEN
            whenever(
                refundStore.createAmountRefund(
                    selectedSite.get(),
                    order.id,
                    BigDecimal(0.5),
                    "Test Tap To Pay payment auto refund",
                    true,
                )
            ).thenAnswer {
                WooResult(mock<WCRefundModel>())
            }

            // WHEN
            val viewModel = initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))
            val states = viewModel.viewState.captureValues()
            viewModel.handleFlowParam(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))

            // THEN
            assertThat(states[0].isProgressVisible).isFalse()
            assertThat(states[1].isProgressVisible).isTrue()
            assertThat(states[2].isProgressVisible).isFalse()
        }

    @Test
    fun `given try ttp payment flow and autorefund success, when snack bar action clicked, then NavigateToOrderDetails event emitted`() =
        testBlocking {
            // GIVEN
            whenever(
                refundStore.createAmountRefund(
                    selectedSite.get(),
                    order.id,
                    BigDecimal(0.5),
                    "Test Tap To Pay payment auto refund",
                    true,
                )
            ).thenReturn(WooResult(mock<WCRefundModel>()))

            // WHEN
            val viewModel = initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))
            ((viewModel.event.value as TapToPaySummaryViewModel.ShowSuccessfulRefundNotification).action).invoke()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(TapToPaySummaryViewModel.NavigateToOrderDetails(1))
        }

    @Test
    fun `given after payment flow and autorefund fails, when vm created, then exit to order details`() =
        testBlocking {
            // GIVEN
            whenever(
                refundStore.createAmountRefund(
                    selectedSite.get(),
                    order.id,
                    BigDecimal(0.5),
                    "Test Tap To Pay payment auto refund",
                    true,
                )
            ).thenReturn(
                WooResult(
                    WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR)
                )
            )

            // WHEN
            val viewModel = initViewModel(TapToPaySummaryFragment.TestTapToPayFlow.AfterPayment(order))

            // THEN
            assertThat(viewModel.event.value).isEqualTo(TapToPaySummaryViewModel.NavigateToOrderDetails(1))
        }

    @Test
    fun `when onLearnMoreClicked, then NavigateToUrlInGenericWebView emitted`() {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onLearnMoreClicked()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            TapToPaySummaryViewModel.NavigateTTPAboutScreen(cardReaderConfig)
        )
    }

    private fun initViewModel(
        flow: TapToPaySummaryFragment.TestTapToPayFlow = TapToPaySummaryFragment.TestTapToPayFlow.BeforePayment
    ) =
        TapToPaySummaryViewModel(
            orderCreateEditRepository,
            analyticsTrackerWrapper,
            refundStore,
            resourceProvider,
            selectedSite,
            currencyFormatter,
            wooStore,
            cardReaderCountryConfigProvider,
            TapToPaySummaryFragmentArgs(flow).toSavedStateHandle()
        )
}
