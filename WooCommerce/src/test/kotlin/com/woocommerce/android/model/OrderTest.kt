package com.woocommerce.android.model

import com.woocommerce.android.ui.orders.OrderTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderTest {
    @Test
    fun `given order has date paid, when check is order paid, then returns true`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(datePaid = mock())

        // WHEN
        val result = order.isOrderPaid

        // THEN
        assertTrue(result)
    }

    @Test
    fun `given order does not have date paid, when check is order paid, then returns false`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(datePaid = null)

        // WHEN
        val result = order.isOrderPaid

        // THEN
        assertFalse(result)
    }

    @Test
    fun `given refunded total equal to total, when check is order fully refunded, then returns true`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            total = BigDecimal(100),
            refundTotal = BigDecimal(100)
        )

        // WHEN
        val result = order.isOrderFullyRefunded

        // THEN
        assertTrue(result)
    }

    @Test
    fun `given refunded total less than total, when check is order fully refunded, then returns false`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            total = BigDecimal(100),
            refundTotal = BigDecimal(50)
        )

        // WHEN
        val result = order.isOrderFullyRefunded

        // THEN
        assertFalse(result)
    }

    @Test
    fun `given items quantity is 0, when check is quantity items possible to refund, then returns 0`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(
                mock {
                    on { quantity }.thenReturn(0F)
                }
            )
        )

        // WHEN
        val result = order.quantityOfItemsWhichPossibleToRefund

        // THEN
        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `given items quantity is 5 and 3, when check is quantity items possible to refund, then returns 8`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(
                mock { on { quantity }.thenReturn(3F) },
                mock { on { quantity }.thenReturn(5F) },
            )
        )

        // WHEN
        val result = order.quantityOfItemsWhichPossibleToRefund

        // THEN
        assertThat(result).isEqualTo(8)
    }

    @Test
    fun `given items quantity is 1 fees 1, when check is quantity items possible to refund, then returns 2`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(
                mock { on { quantity }.thenReturn(1F) },
            ),
            feesLines = listOf(Order.FeeLine.EMPTY)
        )

        // WHEN
        val result = order.quantityOfItemsWhichPossibleToRefund

        // THEN
        assertThat(result).isEqualTo(2)
    }

    @Test
    fun `given order fully refunded, when check is refund available, then returns false`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            total = BigDecimal(100),
            refundTotal = BigDecimal(100)
        )

        // WHEN
        val result = order.isRefundAvailable

        // THEN
        assertFalse(result)
    }

    @Test
    fun `given items quantity is 0, when check is refund available, then returns false`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(mock { on { quantity }.thenReturn(0F) })
        )

        // WHEN
        val result = order.isRefundAvailable

        // THEN
        assertFalse(result)
    }

    @Test
    fun `given date paid is null, when check is refund available, then returns false`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(datePaid = null)

        // WHEN
        val result = order.isRefundAvailable

        // THEN
        assertFalse(result)
    }

    @Test
    fun `given order not fully refunded, when check is refund available, then returns true`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            total = BigDecimal(100),
            refundTotal = BigDecimal(50),
            datePaid = mock()
        )

        // WHEN
        val result = order.isRefundAvailable

        // THEN
        assertTrue(result)
    }

    @Test
    fun `given items quantity is 1, when check is refund available, then returns true`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(mock { on { quantity }.thenReturn(1F) }),
            datePaid = mock()
        )

        // WHEN
        val result = order.isRefundAvailable

        // THEN
        assertTrue(result)
    }

    @Test
    fun `given date paid is null, when check is refund available, then returns true`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(datePaid = mock())

        // WHEN
        val result = order.isRefundAvailable

        // THEN
        assertTrue(result)
    }
}
