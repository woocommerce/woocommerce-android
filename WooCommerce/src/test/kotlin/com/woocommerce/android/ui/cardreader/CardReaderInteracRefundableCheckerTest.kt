package com.woocommerce.android.ui.orders.cardreader

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.cardreader.payment.CardReaderInteracRefundableChecker
import com.woocommerce.android.ui.cardreader.payment.CardReaderPaymentCurrencySupportedChecker
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@ExperimentalCoroutinesApi
class CardReaderInteracRefundableCheckerTest : BaseUnitTest() {
    private val repository: OrderDetailRepository = mock()
    private val cardReaderPaymentCurrencySupportedChecker: CardReaderPaymentCurrencySupportedChecker = mock()
    private val checker: CardReaderInteracRefundableChecker = CardReaderInteracRefundableChecker(
        repository,
        cardReaderPaymentCurrencySupportedChecker,
    )

    private val generatedOrder = OrderTestUtils.generateTestOrder()

    @Before
    fun setUp() {
//        doReturn(false).whenever(repository).hasSubscriptionProducts(any())
        runBlockingTest {
            whenever(cardReaderPaymentCurrencySupportedChecker.isCurrencySupported(any())).thenReturn(true)
        }
    }

    @Test
    fun `when order is paid, then order is refundable`() =
        testBlocking {
            val order = getOrder(
                paymentStatus = Order.Status.Completed,
                datePaid = Date()
            )

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order not paid, then order is not refundable`() =
        testBlocking {
            val order = getOrder(datePaid = null)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when total amount less than zero, then order is not refundable`() =
        testBlocking {
            val order = getOrder(total = BigDecimal(-2))

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when total amount equal to zero, then order is not refundable`() =
        testBlocking {
            val order = getOrder(total = BigDecimal(0))

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when total amount greater than zero, then order is refundable`() =
        testBlocking {
            val order = getOrder(
                datePaid = Date(),
                total = BigDecimal(2)
            )
            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order has empty payment method, then order is refundable`() =
        testBlocking {
            val order = getOrder(paymentMethod = "")

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order in supported country then it is refundable`() =
        testBlocking {
            val order = getOrder(currency = "USD")

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order in not supported country then it is not refundable`() =
        testBlocking {
            val order = getOrder(currency = "INR")
            whenever(
                cardReaderPaymentCurrencySupportedChecker.isCurrencySupported("INR")
            ).thenReturn(false)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has no subscriptions items, then is refundable`() =
        testBlocking {
            val order = getOrder()
            doReturn(false).whenever(repository).hasSubscriptionProducts(any())

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order has subscriptions items, then order is not refundable`() =
        testBlocking {
            val order = getOrder()
            doReturn(true).whenever(repository).hasSubscriptionProducts(any())

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has code payment method, then is refundable`() =
        testBlocking {
            val order = getOrder(paymentMethod = "cod")

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order has non code payment method, then it is not refundable`() =
        testBlocking {
            val order = getOrder(paymentMethod = "stripe")

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has processing status, then is not refundable`() =
        testBlocking {
            val order = getOrder(paymentStatus = Order.Status.Processing)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has on hold status, then is not refundable`() =
        testBlocking {
            val order = getOrder(paymentStatus = Order.Status.OnHold)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has pending status, then is not refundable`() =
        testBlocking {
            val order = getOrder(paymentStatus = Order.Status.Pending)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has refunded status, then is not refundable`() =
        testBlocking {
            val order = getOrder(paymentStatus = Order.Status.Refunded)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has custom status, then is not refundable`() =
        testBlocking {
            val order = getOrder(paymentStatus = Order.Status.Custom("custom"))

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has failed status, then is not refundable`() =
        testBlocking {
            val order = getOrder(paymentStatus = Order.Status.Failed)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has completed status, then is refundable`() =
        testBlocking {
            val order = getOrder(paymentStatus = Order.Status.Completed)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order has cancelled status, then is not refundable`() =
        testBlocking {
            val order = getOrder(paymentStatus = Order.Status.Cancelled)

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isFalse()
        }

    @Test
    fun `when order has been partially refunded, then is refundable`() =
        testBlocking {
            val order = getOrder(
                total = BigDecimal("50"),
                refundTotal = 99
            )

            val isRefundable = checker.isRefundable(order)

            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order has "woocommerce_payments" payment method, then it is refundable`() =
        testBlocking {
            // GIVEN
            val order = getOrder(paymentMethod = "woocommerce_payments")

            // WHEN
            val isRefundable = checker.isRefundable(order)

            // THEN
            assertThat(isRefundable).isTrue()
        }

    @Test
    fun `when order has "wc-booking-gateway" payment method, then it is refundable`() =
        testBlocking {
            // GIVEN
            val order = getOrder(paymentMethod = "wc-booking-gateway")

            // WHEN
            val isRefundable = checker.isRefundable(order)

            // THEN
            assertThat(isRefundable).isTrue()
        }

    /**
     * This test is a clone of the previous test (except of the name) and was added just for the documentation purposes.
     */
    @Test
    fun `given bookings order requires confirmation, when checking refundability, then it is refundable`() =
        testBlocking {
            // GIVEN
            val order = getOrder(paymentMethod = "wc-booking-gateway")

            // WHEN
            val isRefundable = checker.isRefundable(order)

            // THEN
            assertThat(isRefundable).isTrue()
        }

    private fun getOrder(
        total: BigDecimal = BigDecimal.ONE,
        currency: String = "USD",
        paymentStatus: Order.Status = Order.Status.Completed,
        paymentMethod: String = "cod",
        paymentMethodTitle: String = "title",
        refundTotal: Int = 0,
        datePaid: Date? = Date()
    ): Order {
        return generatedOrder.copy(
            total = total,
            currency = currency,
            paymentMethod = paymentMethod,
            paymentMethodTitle = paymentMethodTitle,
            datePaid = datePaid,
            status = paymentStatus,
            refundTotal = BigDecimal(refundTotal)
        )
    }
}
