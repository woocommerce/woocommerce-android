package com.woocommerce.android.ui.woopos.paymentsuccess

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.bookings.BOOKING_PAYMENT_FLOW_FINISHED_KEY
import com.woocommerce.android.ui.woopos.cardpayment.WooPosCardPaymentAnalyticsTracker
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPaymentSuccessViewModelTest {
    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val analyticsTracker: WooPosCardPaymentAnalyticsTracker = mock()
    private val totalsRepository: WooPosTotalsRepository = mock()
    private val priceFormat: WooPosFormatPrice = mock()
    private val resourceProvider: ResourceProvider = mock()

    private fun createViewModel(
        orderId: Long = 123L,
        source: PaymentSuccessSource = PaymentSuccessSource.CARD_CHECKOUT,
    ): WooPosPaymentSuccessViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                PAYMENT_SUCCESS_ORDER_ID_KEY to orderId,
                PAYMENT_SUCCESS_SOURCE_KEY to source.name,
            )
        )
        return WooPosPaymentSuccessViewModel(
            analyticsTracker = analyticsTracker,
            totalsRepository = totalsRepository,
            priceFormat = priceFormat,
            resourceProvider = resourceProvider,
            savedStateHandle = savedStateHandle,
        )
    }

    @Test
    fun `given card source and order exists, when init, then state has card payment text`() = runTest {
        // GIVEN
        val order = Order.getEmptyOrder(Date(), Date()).copy(total = BigDecimal("50.00"))
        whenever(totalsRepository.getOrderById(123L)).thenReturn(order)
        whenever(priceFormat(BigDecimal("50.00"))).thenReturn("$50.00")
        whenever(resourceProvider.getString(R.string.woopos_totals_success_payment_card, "$50.00"))
            .thenReturn("A card payment of $50.00 was successfully made.")

        // WHEN
        val viewModel = createViewModel(source = PaymentSuccessSource.CARD_CHECKOUT)

        // THEN
        assertThat(viewModel.state.value.orderTotalText)
            .isEqualTo("A card payment of $50.00 was successfully made.")
    }

    @Test
    fun `given cash source and order exists, when init, then state has cash payment text`() = runTest {
        // GIVEN
        val order = Order.getEmptyOrder(Date(), Date()).copy(total = BigDecimal("50.00"))
        whenever(totalsRepository.getOrderById(123L)).thenReturn(order)
        whenever(priceFormat(BigDecimal("50.00"))).thenReturn("$50.00")
        whenever(resourceProvider.getString(R.string.woopos_totals_success_payment_cash, "$50.00"))
            .thenReturn("A cash payment of $50.00 was successfully made.")

        // WHEN
        val viewModel = createViewModel(source = PaymentSuccessSource.CASH_BOOKINGS)

        // THEN
        assertThat(viewModel.state.value.orderTotalText)
            .isEqualTo("A cash payment of $50.00 was successfully made.")
    }

    @Test
    fun `given null order, when init, then state has fallback text`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(123L)).thenReturn(null)
        whenever(resourceProvider.getString(R.string.woopos_payment_successful_label))
            .thenReturn("Payment successful")

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.orderTotalText).isEqualTo("Payment successful")
    }

    @Test
    fun `given order with billing email, when init, then state has receipt message`() = runTest {
        // GIVEN
        val order = Order.getEmptyOrder(Date(), Date()).copy(
            total = BigDecimal("50.00"),
            customer = Order.Customer(
                billingAddress = Address.EMPTY.copy(email = "customer@example.com"),
                shippingAddress = Address.EMPTY
            )
        )
        whenever(totalsRepository.getOrderById(123L)).thenReturn(order)
        whenever(priceFormat(any<BigDecimal>())).thenReturn("$50.00")
        whenever(resourceProvider.getString(any(), any())).thenReturn("test")
        whenever(resourceProvider.getString(R.string.woopos_receipt_sent_to_customer, "customer@example.com"))
            .thenReturn("A receipt has been sent to customer@example.com.")

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.receiptSentMessage)
            .isEqualTo("A receipt has been sent to customer@example.com.")
    }

    @Test
    fun `given cash source and order with billing email, when init, then receipt message is null`() = runTest {
        // GIVEN
        val order = Order.getEmptyOrder(Date(), Date()).copy(
            total = BigDecimal("50.00"),
            customer = Order.Customer(
                billingAddress = Address.EMPTY.copy(email = "customer@example.com"),
                shippingAddress = Address.EMPTY
            )
        )
        whenever(totalsRepository.getOrderById(123L)).thenReturn(order)
        whenever(priceFormat(any<BigDecimal>())).thenReturn("$50.00")
        whenever(resourceProvider.getString(any(), any())).thenReturn("test")

        // WHEN
        val viewModel = createViewModel(source = PaymentSuccessSource.CASH_BOOKINGS)

        // THEN
        assertThat(viewModel.state.value.receiptSentMessage).isNull()
    }

    @Test
    fun `given order without billing email, when init, then receipt message is null`() = runTest {
        // GIVEN
        val order = Order.getEmptyOrder(Date(), Date()).copy(total = BigDecimal("50.00"))
        whenever(totalsRepository.getOrderById(123L)).thenReturn(order)
        whenever(priceFormat(any<BigDecimal>())).thenReturn("$50.00")
        whenever(resourceProvider.getString(any(), any())).thenReturn("test")

        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.state.value.receiptSentMessage).isNull()
    }

    @Test
    fun `given CARD_CHECKOUT source, when onDoneClicked, then GoBack emitted`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel(source = PaymentSuccessSource.CARD_CHECKOUT)

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onDoneClicked()
            assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
        }
    }

    @Test
    fun `given CARD_BOOKINGS source, when onDoneClicked, then NavigateBackToBookingsAfterPayment emitted`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel(source = PaymentSuccessSource.CARD_BOOKINGS)

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onDoneClicked()
            val event = awaitItem() as WooPosNavigationEvent.NavigateBackToBookingsAfterPayment
            assertThat(event.key).isEqualTo(BOOKING_PAYMENT_FLOW_FINISHED_KEY)
            assertThat(event.value).isEqualTo(true)
        }
    }

    @Test
    fun `given CASH_BOOKINGS source, when onDoneClicked, then NavigateBackToBookingsAfterPayment emitted`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel(source = PaymentSuccessSource.CASH_BOOKINGS)

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onDoneClicked()
            val event = awaitItem() as WooPosNavigationEvent.NavigateBackToBookingsAfterPayment
            assertThat(event.key).isEqualTo(BOOKING_PAYMENT_FLOW_FINISHED_KEY)
            assertThat(event.value).isEqualTo(true)
        }
    }

    @Test
    fun `given CARD_BOOKINGS source, when onBackPressed, then NavigateBackToBookingsAfterPayment emitted`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel(source = PaymentSuccessSource.CARD_BOOKINGS)

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onBackPressed()
            val event = awaitItem() as WooPosNavigationEvent.NavigateBackToBookingsAfterPayment
            assertThat(event.key).isEqualTo(BOOKING_PAYMENT_FLOW_FINISHED_KEY)
            assertThat(event.value).isEqualTo(true)
        }
    }

    @Test
    fun `when onEmailReceiptClicked, then trackEmailReceiptTapped is called`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel()

        // WHEN
        viewModel.onEmailReceiptClicked()

        // THEN
        verify(analyticsTracker).trackEmailReceiptTapped()
    }

    @Test
    fun `when onEmailReceiptClicked, then OpenEmailReceipt navigation event emitted`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(any())).thenReturn(null)
        whenever(resourceProvider.getString(any())).thenReturn("test")
        val viewModel = createViewModel(orderId = 42L)

        // WHEN & THEN
        viewModel.navigationEvent.test {
            viewModel.onEmailReceiptClicked()
            val event = awaitItem()
            assertThat(event).isEqualTo(WooPosNavigationEvent.OpenEmailReceipt(42L))
        }
    }
}
