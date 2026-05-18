package com.woocommerce.android.ui.woopos.scantopay

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent.OrderSuccessfullyPaid.PaymentMethod
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventSender
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCheckoutFromScanToPay
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ScanToPayCollectPaymentSuccess
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ScanToPayPaymentDetectedViaPolling
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ScanToPayPaymentFailed
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosScanToPayViewModelTest {

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Rule
    @JvmField
    val instantTaskRule = InstantTaskExecutorRule()

    private val repository: WooPosScanToPayRepository = mock()
    private val parentToChildrenEventSender: WooPosParentToChildrenEventSender = mock()
    private val tracker: WooPosAnalyticsTracker = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val priceFormat: WooPosFormatPrice = mock()

    private val orderId = 123L

    @Before
    fun setUp() = runTest {
        whenever(resourceProvider.getString(eq(R.string.woopos_scan_to_pay_total), any()))
            .thenReturn("Order total: $42.00")
        whenever(resourceProvider.getString(R.string.woopos_scan_to_pay_error_message))
            .thenReturn("Something went wrong. Please try again.")
        whenever(resourceProvider.getString(R.string.woopos_scan_to_pay_order_note))
            .thenReturn("Customer paid via Scan to Pay")
        whenever(priceFormat(BigDecimal("42.00"))).thenReturn("$42.00")
    }

    private fun createViewModel() = WooPosScanToPayViewModel(
        repository = repository,
        parentToChildrenEventSender = parentToChildrenEventSender,
        analyticsTracker = tracker,
        resourceProvider = resourceProvider,
        priceFormat = priceFormat,
        savedState = SavedStateHandle(mapOf(SCAN_TO_PAY_ROUTE_ORDER_ID_KEY to orderId)),
    )

    @Test
    fun `given promote succeeds and paymentUrl available, when VM initializes, then state ShowingQR`() = runTest {
        // GIVEN
        val cached = Order.getEmptyOrder(Date(), Date()).copy(id = orderId, total = BigDecimal("42.00"))
        val pendingOrder = Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            paymentUrl = "https://example.com/pay/abc",
        )
        whenever(repository.promoteOrderToPending(orderId)).thenReturn(Result.success(Unit))
        whenever(repository.fetchOrderSnapshot(orderId)).thenReturn(pendingOrder)
        whenever(repository.getCachedOrder(orderId)).thenReturn(cached)

        // WHEN
        val viewModel = createViewModel()
        runCurrent()

        // THEN
        val state = viewModel.state.value
        assertThat(state).isInstanceOf(WooPosScanToPayState.ShowingQR::class.java)
        val showing = state as WooPosScanToPayState.ShowingQR
        assertThat(showing.paymentUrl).isEqualTo("https://example.com/pay/abc")
        assertThat(showing.totalText).isEqualTo("Order total: $42.00")
    }

    @Test
    fun `given promote fails, when VM initializes, then state Failed retryable`() = runTest {
        // GIVEN
        whenever(repository.promoteOrderToPending(orderId)).thenReturn(Result.failure(Exception("boom")))

        // WHEN
        val viewModel = createViewModel()
        runCurrent()

        // THEN
        val state = viewModel.state.value as WooPosScanToPayState.Failed
        assertThat(state.retryable).isTrue()
        assertThat(state.message).isEqualTo("Something went wrong. Please try again.")
    }

    @Test
    fun `given paymentUrl blank after retry, when VM initializes, then state Failed retryable`() = runTest {
        // GIVEN
        val blankOrder = Order.getEmptyOrder(Date(), Date()).copy(id = orderId, paymentUrl = "")
        whenever(repository.promoteOrderToPending(orderId)).thenReturn(Result.success(Unit))
        whenever(repository.fetchOrderSnapshot(orderId)).thenReturn(blankOrder)

        // WHEN
        val viewModel = createViewModel()
        advanceTimeBy(2_000)
        runCurrent()

        // THEN
        val state = viewModel.state.value as WooPosScanToPayState.Failed
        assertThat(state.retryable).isTrue()
    }

    @Test
    fun `given QR shown, when polling detects paid order, then analytics tracked, parent event sent and GoBack emitted`() =
        runTest {
            // GIVEN
            val cached = Order.getEmptyOrder(Date(), Date()).copy(id = orderId, total = BigDecimal("42.00"))
            val pendingOrder = Order.getEmptyOrder(Date(), Date()).copy(
                id = orderId,
                paymentUrl = "https://example.com/pay/abc",
                datePaid = null,
                status = Order.Status.Pending,
            )
            val paidOrder = Order.getEmptyOrder(Date(), Date()).copy(id = orderId, datePaid = Date())
            whenever(repository.promoteOrderToPending(orderId)).thenReturn(Result.success(Unit))
            whenever(repository.fetchOrderSnapshot(orderId))
                .thenReturn(pendingOrder) // initial paymentUrl fetch
                .thenReturn(paidOrder) // polling detects payment
            whenever(repository.getCachedOrder(orderId)).thenReturn(cached)
            whenever(repository.addOrderNote(eq(orderId), any())).thenReturn(Result.success(Unit))

            // WHEN
            val viewModel = createViewModel()
            runCurrent()

            viewModel.navigationEvent.test {
                advanceTimeBy(2_500) // poll once
                runCurrent()
                assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
            }

            // THEN
            verify(tracker).track(ScanToPayPaymentDetectedViaPolling)
            verify(tracker).track(ScanToPayCollectPaymentSuccess)
            verify(parentToChildrenEventSender).sendToChildren(
                eq(ParentToChildrenEvent.OrderSuccessfullyPaid(PaymentMethod.SCAN_TO_PAY)),
            )
            verify(repository).addOrderNote(orderId, "Customer paid via Scan to Pay")
        }

    @Test
    fun `when cancel clicked, then BackToCheckoutFromScanToPay tracked and GoBack emitted`() = runTest {
        // GIVEN
        whenever(repository.promoteOrderToPending(orderId)).thenReturn(Result.failure(Exception("boom")))
        val viewModel = createViewModel()
        runCurrent()

        // WHEN / THEN
        viewModel.navigationEvent.test {
            viewModel.onUIEvent(WooPosScanToPayUIEvent.CancelClicked)
            assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
        }
        verify(tracker).track(BackToCheckoutFromScanToPay)
    }

    @Test
    fun `given QR shown, when polling sees Processing status, then payment detected`() = runTest {
        // GIVEN
        val pendingOrder = Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            paymentUrl = "https://example.com/pay/abc",
            status = Order.Status.Pending,
        )
        val processingOrder = Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            datePaid = null,
            status = Order.Status.Processing,
        )
        whenever(repository.promoteOrderToPending(orderId)).thenReturn(Result.success(Unit))
        whenever(repository.fetchOrderSnapshot(orderId)).thenReturn(pendingOrder, processingOrder)
        whenever(repository.getCachedOrder(orderId)).thenReturn(pendingOrder)
        whenever(repository.addOrderNote(eq(orderId), any())).thenReturn(Result.success(Unit))

        // WHEN
        val viewModel = createViewModel()
        runCurrent()

        // THEN
        viewModel.navigationEvent.test {
            assertThat(awaitItem()).isEqualTo(WooPosNavigationEvent.GoBack)
        }
        verify(tracker).track(ScanToPayPaymentDetectedViaPolling)
    }

    @Test
    fun `given QR shown, when polling sees OnHold status, then payment not detected`() = runTest {
        // GIVEN: OnHold means awaiting verification, not paid — we should keep waiting
        val pendingOrder = Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            paymentUrl = "https://example.com/pay/abc",
            status = Order.Status.Pending,
        )
        val onHoldOrder = Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            datePaid = null,
            status = Order.Status.OnHold,
        )
        whenever(repository.promoteOrderToPending(orderId)).thenReturn(Result.success(Unit))
        whenever(repository.fetchOrderSnapshot(orderId)).thenReturn(pendingOrder, onHoldOrder)
        whenever(repository.getCachedOrder(orderId)).thenReturn(pendingOrder)

        // WHEN
        val viewModel = createViewModel()
        runCurrent()
        // Allow at least one poll cycle
        advanceTimeBy(2_500)
        runCurrent()

        // THEN
        verify(tracker, never()).track(ScanToPayPaymentDetectedViaPolling)
        assertThat(viewModel.state.value).isInstanceOf(WooPosScanToPayState.ShowingQR::class.java)
    }

    @Test
    fun `given non-paid order, when polling exhausts MAX_POLL_ATTEMPTS, then ScanToPayPaymentFailed tracked`() = runTest {
        // GIVEN
        val pendingOrder = Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            paymentUrl = "https://example.com/pay/abc",
            status = Order.Status.Pending,
        )
        val nonPaidOrder = Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            datePaid = null,
            status = Order.Status.Pending,
        )
        whenever(repository.promoteOrderToPending(orderId)).thenReturn(Result.success(Unit))
        whenever(repository.fetchOrderSnapshot(orderId)).thenReturn(pendingOrder, nonPaidOrder)
        whenever(repository.getCachedOrder(orderId)).thenReturn(pendingOrder)

        // WHEN
        val viewModel = createViewModel()
        runCurrent()
        // Advance past the 5.5-min ceiling (15 × 2s + 60 × 5s = 330s)
        advanceTimeBy(340_000)
        runCurrent()

        // THEN
        verify(tracker).track(ScanToPayPaymentFailed)
        assertThat(viewModel.state.value).isInstanceOf(WooPosScanToPayState.Failed::class.java)
    }

    @Test
    fun `given Failed state, when RetryClicked, then attempts to prepare again`() = runTest {
        // GIVEN: first attempt fails, second succeeds
        val cached = Order.getEmptyOrder(Date(), Date()).copy(id = orderId, total = BigDecimal("42.00"))
        val pendingOrder = Order.getEmptyOrder(Date(), Date()).copy(
            id = orderId,
            paymentUrl = "https://example.com/pay/abc",
        )
        whenever(repository.promoteOrderToPending(orderId))
            .thenReturn(Result.failure(Exception("boom")))
            .thenReturn(Result.success(Unit))
        whenever(repository.fetchOrderSnapshot(orderId)).thenReturn(pendingOrder)
        whenever(repository.getCachedOrder(orderId)).thenReturn(cached)

        val viewModel = createViewModel()
        runCurrent()
        assertThat(viewModel.state.value).isInstanceOf(WooPosScanToPayState.Failed::class.java)

        // WHEN
        viewModel.onUIEvent(WooPosScanToPayUIEvent.RetryClicked)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value).isInstanceOf(WooPosScanToPayState.ShowingQR::class.java)
    }
}
