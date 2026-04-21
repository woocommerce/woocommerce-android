package com.woocommerce.android.ui.woopos.cashpayment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.bookings.BOOKING_PAYMENT_FLOW_FINISHED_KEY
import com.woocommerce.android.ui.woopos.paymentsuccess.PaymentSuccessSource
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CashCollectPaymentSuccess
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CashPaymentFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CashPaymentTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCashPaymentViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private val repository: WooPosCashPaymentRepository = mock()
    private val priceFormat: WooPosFormatPrice = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val tracker: WooPosAnalyticsTracker = mock()
    private val trackerData: WooPosAnalyticsTrackingDataKeeper = WooPosAnalyticsTrackingDataKeeper()

    private lateinit var viewModel: WooPosCashPaymentViewModel

    @Before
    fun setUp() = runTest {
        val orderId = 123L
        val mockOrder = mock<Order> {
            on { total }.thenReturn(BigDecimal("100.00"))
        }

        whenever(repository.getOrderById(orderId)).thenReturn(mockOrder)
        whenever(repository.getCurrencySymbol()).thenReturn("$")
        whenever(repository.getCurrencySymbolPosition()).thenReturn(CurrencyPosition.LEFT)
        whenever(repository.getDecimalSeparator()).thenReturn(".")
        whenever(repository.getNumberOfDecimals()).thenReturn(2)
        whenever(resourceProvider.getString(R.string.woopos_cash_payment_total, "100.00"))
            .thenReturn("Total: $100.00")
        whenever(priceFormat(BigDecimal("100.00"))).thenReturn("100.00")

        whenever(resourceProvider.getString(R.string.woopos_complete_cash_order_button))
            .thenReturn("Complete Order")
        whenever(resourceProvider.getString(R.string.woopos_cash_payment_change_due, "20.00"))
            .thenReturn("Change Due: $20.00")

        val savedStateHandle = SavedStateHandle(mapOf("orderId" to orderId))

        viewModel = WooPosCashPaymentViewModel(
            repository = repository,
            priceFormat = priceFormat,
            resourceProvider = resourceProvider,
            analyticsTracker = tracker,
            analyticsData = trackerData,
            savedState = savedStateHandle,
        )
    }

    @Test
    fun `given order exists, when ViewModel initializes, then state is Collecting`() = runTest {
        // WHEN
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isInstanceOf(WooPosCashPaymentState.Collecting::class.java)
        val collectingState = state as WooPosCashPaymentState.Collecting
        assertThat(collectingState.total).isEqualTo(BigDecimal("100.00"))
        assertThat(collectingState.totalText).isEqualTo("Total: $100.00")
        assertThat(collectingState.currencySymbol).isEqualTo("$")
        assertThat(collectingState.currencyPosition).isEqualTo(CurrencyPosition.LEFT)
        assertThat(collectingState.decimalSeparator).isEqualTo(".")
        assertThat(collectingState.numberOfDecimals).isEqualTo(2)
    }

    @Test
    fun `when ViewModel initializes, then entered amount prefilled with total value and button is enabled`() = runTest {
        // WHEN
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isInstanceOf(WooPosCashPaymentState.Collecting::class.java)
        val collectingState = state as WooPosCashPaymentState.Collecting
        assertThat(collectingState.enteredAmount).isEqualTo(BigDecimal("100.00"))
        assertThat(collectingState.button.status).isEqualTo(WooPosCashPaymentState.Collecting.Button.Status.ENABLED)
    }

    @Test
    fun `given valid amount,when onUIEvent AmountChanged , then button is enabled and changeDue is updated`() = runTest {
        // GIVEN
        val enteredAmount = BigDecimal("120.00")
        val changeDue = BigDecimal("20.00")
        whenever(resourceProvider.getString(R.string.woopos_cash_payment_change_due, "20.00"))
            .thenReturn("Change Due: $20.00")
        whenever(priceFormat(changeDue)).thenReturn("20.00")

        // WHEN
        viewModel.onUIEvent(WooPosCashPaymentUIEvent.AmountChanged(enteredAmount))
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isInstanceOf(WooPosCashPaymentState.Collecting::class.java)
        val collectingState = state as WooPosCashPaymentState.Collecting
        assertThat(collectingState.enteredAmount).isEqualTo(enteredAmount)
        assertThat(collectingState.changeDueText).isEqualTo("Change Due: $20.00")
        assertThat(collectingState.changeDue).isEqualTo(changeDue)
        assertThat(collectingState.button.status).isEqualTo(WooPosCashPaymentState.Collecting.Button.Status.ENABLED)
    }

    @Test
    fun `given invalid amount less than total, when onUIEvent AmountChanged, then button is disabled`() = runTest {
        // GIVEN
        val enteredAmount = BigDecimal("30.00")

        // WHEN
        viewModel.onUIEvent(
            WooPosCashPaymentUIEvent.AmountChanged(enteredAmount)
        )
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isInstanceOf(WooPosCashPaymentState.Collecting::class.java)
        val collectingState = state as WooPosCashPaymentState.Collecting
        assertThat(collectingState.enteredAmount).isEqualTo(enteredAmount)
        assertThat(collectingState.changeDueText).isEqualTo("")
        assertThat(collectingState.changeDue).isNull()
        assertThat(collectingState.button.status).isEqualTo(WooPosCashPaymentState.Collecting.Button.Status.DISABLED)
    }

    @Test
    fun `given changeDue is set, when onUIEvent CompleteOrderClicked, then changeDue is passed to completeOrder`() = runTest {
        // GIVEN
        val enteredAmount = BigDecimal("120.00")
        val changeDue = BigDecimal("20.00")
        whenever(resourceProvider.getString(R.string.woopos_cash_payment_change_due, "20.00"))
            .thenReturn("Change Due: $20.00")
        whenever(priceFormat(changeDue)).thenReturn("20.00")
        whenever(repository.completeOrder(any(), any())).thenReturn(Result.success(Unit))

        // WHEN
        viewModel.onUIEvent(WooPosCashPaymentUIEvent.AmountChanged(enteredAmount))
        viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked)
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isEqualTo(WooPosCashPaymentState.Complete)
        verify(repository).completeOrder(any(), eq(changeDue.toString()))
    }

    @Test
    fun `given repository completes order successfully, when onUIEvent CompleteOrderClicked, then state is Complete`() = runTest {
        // GIVEN
        whenever(repository.completeOrder(any(), any())).thenReturn(Result.success(Unit))

        // WHEN
        viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked)
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isEqualTo(WooPosCashPaymentState.Complete)
        verify(repository).completeOrder(any(), any())
    }

    @Test
    fun `given repository fails to complete order, when onUIEvent CompleteOrderClicked, then error message is set and button is enabled`() = runTest {
        // GIVEN
        val errorMessage = givenRepoFailsToCompleteOrder()

        // WHEN
        viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked)
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isInstanceOf(WooPosCashPaymentState.Collecting::class.java)
        val collectingState = state as WooPosCashPaymentState.Collecting
        assertThat(collectingState.errorMessage).isEqualTo(errorMessage)
        assertThat(collectingState.button.status).isEqualTo(WooPosCashPaymentState.Collecting.Button.Status.ENABLED)
        verify(repository).completeOrder(any(), any())
    }

    @Test
    fun `given repository fails to complete order, when onUIEvent CompleteOrderClicked, then event is tracked`() = runTest {
        // GIVEN
        givenRepoFailsToCompleteOrder()

        // WHEN
        viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked)

        // THEN
        verify(tracker).track(CashPaymentFailed)
    }

    @Test
    fun `when Complete button tapped, then should track event`() = runTest {
        // GIVEN
        whenever(repository.completeOrder(any(), any())).thenReturn(Result.success(Unit))

        // WHEN
        viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked)

        // THEN
        verify(tracker).track(CashPaymentTapped)
    }

    @Test
    fun `when state is Complete, then should track event`() = runTest {
        // GIVEN
        whenever(repository.completeOrder(any(), any())).thenReturn(Result.success(Unit))

        // WHEN
        viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked)
        val state = viewModel.state.first()

        // THEN
        assertThat(state).isEqualTo(WooPosCashPaymentState.Complete)
        verify(tracker).track(CashCollectPaymentSuccess)
    }

    @Test
    fun `given source is CHECKOUT, when order completed successfully, then navigation event is OpenHomeFromCashPaymentAfterSuccessfulPayment`() = runTest {
        // GIVEN
        whenever(repository.completeOrder(any(), any())).thenReturn(Result.success(Unit))

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked)

            val event = awaitItem()
            assertThat(event).isEqualTo(WooPosNavigationEvent.OpenHomeFromCashPaymentAfterSuccessfulPayment)
        }
    }

    @Test
    fun `given source is BOOKINGS, when order completed successfully, then navigation event is OpenPaymentSuccess`() = runTest {
        // GIVEN
        whenever(repository.completeOrder(any(), any())).thenReturn(Result.success(Unit))
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CASH_ROUTE_ORDER_ID_KEY to 123L,
                CASH_ROUTE_SOURCE_KEY to CashPaymentSource.BOOKINGS.name,
            )
        )
        viewModel = WooPosCashPaymentViewModel(
            repository = repository,
            priceFormat = priceFormat,
            resourceProvider = resourceProvider,
            analyticsTracker = tracker,
            analyticsData = trackerData,
            savedState = savedStateHandle,
        )

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onUIEvent(WooPosCashPaymentUIEvent.CompleteOrderClicked)

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.OpenPaymentSuccess::class.java)
            val successEvent = event as WooPosNavigationEvent.OpenPaymentSuccess
            assertThat(successEvent.orderId).isEqualTo(123L)
            assertThat(successEvent.source).isEqualTo(PaymentSuccessSource.CASH_BOOKINGS)
        }
    }

    @Test
    fun `given source is BOOKINGS, when onBackClicked, then NavigateBackToBookingsAfterPayment emitted`() = runTest {
        // GIVEN
        val savedStateHandle = SavedStateHandle(
            mapOf(
                CASH_ROUTE_ORDER_ID_KEY to 123L,
                CASH_ROUTE_SOURCE_KEY to CashPaymentSource.BOOKINGS.name,
            )
        )
        viewModel = WooPosCashPaymentViewModel(
            repository = repository,
            priceFormat = priceFormat,
            resourceProvider = resourceProvider,
            analyticsTracker = tracker,
            analyticsData = trackerData,
            savedState = savedStateHandle,
        )

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onBackClicked()

            val event = awaitItem()
            assertThat(event).isInstanceOf(WooPosNavigationEvent.NavigateBackToBookingsAfterPayment::class.java)
            val navEvent = event as WooPosNavigationEvent.NavigateBackToBookingsAfterPayment
            assertThat(navEvent.key).isEqualTo(BOOKING_PAYMENT_FLOW_FINISHED_KEY)
            assertThat(navEvent.value).isEqualTo(true)
        }
    }

    @Test
    fun `given source is CHECKOUT, when onBackClicked, then GoBack emitted`() = runTest {
        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onBackClicked()

            val event = awaitItem()
            assertThat(event).isEqualTo(WooPosNavigationEvent.GoBack)
        }
    }

    private suspend fun givenRepoFailsToCompleteOrder(): String {
        val errorMessage = "Something went wrong"
        whenever(repository.completeOrder(any(), any())).thenReturn(Result.failure(Exception()))
        whenever(resourceProvider.getString(R.string.woopos_cash_payment_error_message))
            .thenReturn(errorMessage)
        return errorMessage
    }
}
