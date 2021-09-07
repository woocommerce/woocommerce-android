package com.woocommerce.android.ui.orders.cardreader

import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

@ExperimentalCoroutinesApi
class CardReaderPaymentCollectibilityCheckerTest : BaseUnitTest() {
    private val repository: OrderDetailRepository = mock()
    private val checker: CardReaderPaymentCollectibilityChecker = CardReaderPaymentCollectibilityChecker(repository)

    private val generatedOrder = OrderTestUtils.generateTestOrder()

    @Before
    fun setUp() {
        doReturn(false).whenever(repository).hasSubscriptionProducts(any())
    }

    @Test
    fun `when order is paid, then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(datePaid = Date())

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order not paid, then show collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(datePaid = null)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when total amount less than zero, then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(total = BigDecimal(-2))

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when total amount equal to zero, then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(total = BigDecimal(0))

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when total amount greater than zero, then show collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(total = BigDecimal(2))

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has empty payment method, then it is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentMethod = "")

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order in USD then it is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(currency = "USD")

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has no subscriptions items, then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder()
            doReturn(false).whenever(repository).hasSubscriptionProducts(any())

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has subscriptions items, then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder()
            doReturn(true).whenever(repository).hasSubscriptionProducts(any())

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has code payment method, then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentMethod = "cod")

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has non code payment method, then it is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentMethod = "stripe")

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has processing status, then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentStatus = Order.Status.Processing)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has on hold status, then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentStatus = Order.Status.OnHold)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has pending status, then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentStatus = Order.Status.Pending)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has refunded status, then is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentStatus = Order.Status.Refunded)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has custom status, then is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentStatus = Order.Status.Custom("custom"))

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has failed status, then is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentStatus = Order.Status.Failed)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has completed status, then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentStatus = Order.Status.Completed)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has cancelled status, then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(paymentStatus = Order.Status.Cancelled)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    @Test
    // TODO cardreader remove the following test when the backend issue is fixed
    // https://github.com/Automattic/woocommerce-payments/issues/2390
    fun `when order has been refunded, then hide collect button `() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val order = getOrder(refundTotal = 99)

            val isCollectable = checker.isCollectable(order)

            assertThat(isCollectable).isFalse()
        }

    private fun getOrder(
        total: BigDecimal = BigDecimal.ONE,
        currency: String = "USD",
        paymentStatus: Order.Status = Order.Status.Processing,
        paymentMethod: String = "cod",
        paymentMethodTitle: String = "title",
        refundTotal: Int = 0,
        datePaid: Date? = null
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
