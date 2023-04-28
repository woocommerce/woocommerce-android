package com.woocommerce.android.ui.payments.taptopay.summary

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class TapToPaySummaryViewModelTest : BaseUnitTest() {
    private val order: Order = mock {
        on { id }.thenReturn(1L)
    }
    private val site: SiteModel = mock()
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val refundStore: WCRefundStore = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.tap_to_pay_refund_reason) }.thenReturn("Test Tap To Pay payment auto refund")
        on { getString(R.string.card_reader_tap_to_pay_test_payment_note) }.thenReturn("Test payment")
    }
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(site)
    }

    @Test
    fun `give order creation error, when onTryPaymentClicked, then show snackbar`() = testBlocking {
        // GIVEN
        whenever(
            orderCreateEditRepository.createSimplePaymentOrder(
                BigDecimal.valueOf(0.5),
                customerNote = "Test payment"
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
                    customerNote = "Test payment"
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
                customerNote = "Test payment"
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
                customerNote = "Test payment"
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
                customerNote = "Test payment"
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
                    order.total,
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
    fun `given try ttp payment flow and autorefund success, when vm created, then UIState loading true and then false`() =
        testBlocking {
            // GIVEN
            whenever(
                refundStore.createAmountRefund(
                    selectedSite.get(),
                    order.id,
                    order.total,
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
                    order.total,
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
                    order.total,
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

    private fun initViewModel(
        flow: TapToPaySummaryFragment.TestTapToPayFlow = TapToPaySummaryFragment.TestTapToPayFlow.BeforePayment
    ) =
        TapToPaySummaryViewModel(
            orderCreateEditRepository,
            analyticsTrackerWrapper,
            refundStore,
            resourceProvider,
            selectedSite,
            TapToPaySummaryFragmentArgs(flow).initSavedStateHandle()
        )
}
