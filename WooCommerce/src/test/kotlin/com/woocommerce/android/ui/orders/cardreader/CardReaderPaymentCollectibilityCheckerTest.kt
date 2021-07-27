package com.woocommerce.android.ui.orders.cardreader

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

class CardReaderPaymentCollectibilityCheckerTest : BaseUnitTest() {
    private val repository: OrderDetailRepository = mock()
    private val checker: CardReaderPaymentCollectibilityChecker = CardReaderPaymentCollectibilityChecker(repository)

    private val generatedOrder = OrderTestUtils.generateTestOrder()

    @Before
    fun setUp() {
        doReturn(false).whenever(repository).hasSubscriptionProducts(any())
    }

    @Test
    fun `when order is paid then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(datePaid = null, paymentMethodTitle = "")

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order not paid then show collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(datePaid = Date())

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has empty payment method then it is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentMethod = "")

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order in USD then it is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(currency = "USD")

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has no subscriptions items then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder()
            doReturn(false).whenever(repository).hasSubscriptionProducts(any())

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has subscriptions items then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder()
            doReturn(true).whenever(repository).hasSubscriptionProducts(any())

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has code payment method then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentMethod = "cod")

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has non code payment method then it is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentMethod = "stripe")

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has processing status then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentStatus = Order.Status.Processing)

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has on hold status then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentStatus = Order.Status.OnHold)

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has pending status then is collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentStatus = Order.Status.Pending)

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isTrue()
        }

    @Test
    fun `when order has refunded status then is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentStatus = Order.Status.Refunded)

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has custom status then is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentStatus = Order.Status.Custom("custom"))

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has failed status then is not collectable`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentStatus = Order.Status.Failed)

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has completed status then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentStatus = Order.Status.Completed)

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    @Test
    fun `when order has cancelled status then hide collect button`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(paymentStatus = Order.Status.Cancelled)

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    @Test
    // TODO cardreader remove the following test when the backend issue is fixed
    // https://github.com/Automattic/woocommerce-payments/issues/2390
    fun `when order has been refunded, then hide collect button `() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val order = getOrder(refundTotal = 99)

            // WHEN
            val isCollectable = checker.isCollectable(order)

            // THEN
            assertThat(isCollectable).isFalse()
        }

    private fun getOrder(
        currency: String = "USD",
        paymentStatus: Order.Status = Order.Status.Processing,
        paymentMethod: String = "cod",
        paymentMethodTitle: String = "title",
        refundTotal: Int = 0,
        datePaid: Date? = null
    ): Order {
        return generatedOrder.copy(
            currency = currency,
            paymentMethod = paymentMethod,
            paymentMethodTitle = paymentMethodTitle,
            datePaid = datePaid,
            status = paymentStatus,
            refundTotal = BigDecimal(refundTotal)
        )
    }
}
