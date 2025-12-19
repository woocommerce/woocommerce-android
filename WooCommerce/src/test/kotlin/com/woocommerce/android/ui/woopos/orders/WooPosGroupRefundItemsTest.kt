package com.woocommerce.android.ui.woopos.orders

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import java.math.BigDecimal
import kotlin.test.Test

class WooPosGroupRefundItemsTest {
    private lateinit var sut: WooPosGroupRefundItems

    @Before
    fun setup() {
        sut = WooPosGroupRefundItems()
    }

    private fun createRefundableItem(
        orderItemId: Long,
        productId: Long = 100L,
        variationId: Long = 0L,
        name: String = "Test Product",
        unitPrice: BigDecimal = BigDecimal("20.00"),
        unitTax: BigDecimal = BigDecimal("2.00"),
        rowIndex: Int = 0
    ) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = productId,
        variationId = variationId,
        name = name,
        unitPrice = unitPrice,
        unitTax = unitTax,
        formattedUnitPrice = "$${unitPrice}",
        formattedUnitTax = "$${unitTax}",
        rowIndex = rowIndex
    )

    @Test
    fun `given empty list, when invoke called, then returns empty list`() {
        // GIVEN
        val refundableItems = emptyList<WooPosRefundableItem>()

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given single item, when invoke called, then returns single refund item with quantity 1`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L)
        )

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(1)
    }

    @Test
    fun `given multiple items with same orderItemId, when invoke called, then groups them into single refund item with correct quantity`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, rowIndex = 0),
            createRefundableItem(orderItemId = 123L, rowIndex = 1),
            createRefundableItem(orderItemId = 123L, rowIndex = 2)
        )

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(3)
    }

    @Test
    fun `given items with different orderItemIds, when invoke called, then creates separate refund items`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L),
            createRefundableItem(orderItemId = 456L),
            createRefundableItem(orderItemId = 789L)
        )

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(3)
        assertThat(result.map { it.itemId }).containsExactlyInAnyOrder(123L, 456L, 789L)
        result.forEach { item ->
            assertThat(item.quantity).isEqualTo(1)
        }
    }

    @Test
    fun `given mixed orderItemIds, when invoke called, then groups correctly by orderItemId`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, rowIndex = 0),
            createRefundableItem(orderItemId = 456L, rowIndex = 0),
            createRefundableItem(orderItemId = 123L, rowIndex = 1),
            createRefundableItem(orderItemId = 789L, rowIndex = 0),
            createRefundableItem(orderItemId = 456L, rowIndex = 1)
        )

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(3)

        val item123 = result.find { it.itemId == 123L }
        assertThat(item123).isNotNull
        assertThat(item123!!.quantity).isEqualTo(2)

        val item456 = result.find { it.itemId == 456L }
        assertThat(item456).isNotNull
        assertThat(item456!!.quantity).isEqualTo(2)

        val item789 = result.find { it.itemId == 789L }
        assertThat(item789).isNotNull
        assertThat(item789!!.quantity).isEqualTo(1)
    }

    @Test
    fun `given 5 items with same orderItemId, when invoke called, then returns single item with quantity 5`() {
        // GIVEN
        val refundableItems = (0..4).map { index ->
            createRefundableItem(orderItemId = 999L, rowIndex = index)
        }

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(999L)
        assertThat(result[0].quantity).isEqualTo(5)
    }

    @Test
    fun `given items with different products but same orderItemId, when invoke called, then groups by orderItemId only`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, productId = 100L, rowIndex = 0),
            createRefundableItem(orderItemId = 123L, productId = 100L, rowIndex = 1)
        )

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(2)
    }

    @Test
    fun `given same product with different orderItemIds, when invoke called, then creates separate refund items`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, productId = 100L),
            createRefundableItem(orderItemId = 456L, productId = 100L)
        )

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result.map { it.itemId }).containsExactlyInAnyOrder(123L, 456L)
        result.forEach { item ->
            assertThat(item.quantity).isEqualTo(1)
        }
    }

    @Test
    fun `given large number of items with same orderItemId, when invoke called, then groups all items correctly`() {
        // GIVEN
        val refundableItems = (0..99).map { index ->
            createRefundableItem(orderItemId = 1L, rowIndex = index)
        }

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(1L)
        assertThat(result[0].quantity).isEqualTo(100)
    }

    @Test
    fun `given items with varying properties but same orderItemId, when invoke called, then groups by orderItemId regardless of other properties`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(
                orderItemId = 123L,
                productId = 100L,
                variationId = 10L,
                name = "Product A",
                unitPrice = BigDecimal("10.00"),
                rowIndex = 0
            ),
            createRefundableItem(
                orderItemId = 123L,
                productId = 200L,
                variationId = 20L,
                name = "Product B",
                unitPrice = BigDecimal("20.00"),
                rowIndex = 1
            )
        )

        // WHEN
        val result = sut(refundableItems)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(2)
    }
}
