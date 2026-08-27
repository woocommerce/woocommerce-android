package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.util.CurrencyFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class WooPosBuildRefundContentTest {

    private val currencyFormatter: CurrencyFormatter = mock {
        whenever(it.formatCurrency(any<BigDecimal>(), any<String>(), any<Boolean>())).thenReturn("$0.00")
    }

    private val sut = WooPosBuildRefundContent(currencyFormatter)

    @Test
    fun `given no preserved selection, when content is built, then every row is selected`() {
        // GIVEN
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 1L, rowIndex = 1),
            feeRow(orderItemId = 99L),
        )

        // WHEN
        val content = build(items, preservedSelection = null)

        // THEN
        assertThat(content.selectedItemIds).isEqualTo(items.map { it.uniqueId }.toSet())
        assertThat(content.allItemsSelected).isTrue()
        assertThat(content.itemsCount).isEqualTo(3)
    }

    @Test
    fun `given one unit of a shrunken line was selected, when content is built, then one unit is selected`() {
        // GIVEN — the cashier kept only the last unit of a three-unit line, and the store refunded
        // one unit elsewhere, so the reload renumbers the two survivors from zero.
        val snapshot = WooPosRefundSelectionSnapshot(
            unitCountsByOrderItemId = mapOf(1L to 1),
            selectedLumpSumIds = emptySet(),
        )
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 1L, rowIndex = 1),
        )

        // WHEN
        val content = build(items, preservedSelection = snapshot)

        // THEN
        assertThat(content.selectedItemIds).containsExactly("1_0")
        assertThat(content.itemsCount).isEqualTo(1)
        assertThat(content.allItemsSelected).isFalse()
    }

    @Test
    fun `given more units were selected than are left, when content is built, then the count is clamped`() {
        // GIVEN
        val snapshot = WooPosRefundSelectionSnapshot(
            unitCountsByOrderItemId = mapOf(1L to 3),
            selectedLumpSumIds = emptySet(),
        )
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 1L, rowIndex = 1),
        )

        // WHEN
        val content = build(items, preservedSelection = snapshot)

        // THEN
        assertThat(content.selectedItemIds).containsExactlyInAnyOrder("1_0", "1_1")
        assertThat(content.itemsCount).isEqualTo(2)
        assertThat(content.allItemsSelected).isTrue()
    }

    @Test
    fun `given a selection across two lines and one shrank, when content is built, then the other line is intact`() {
        // GIVEN — two of line 1 and one of line 2 were selected; line 1 lost a unit elsewhere
        val snapshot = WooPosRefundSelectionSnapshot(
            unitCountsByOrderItemId = mapOf(1L to 2, 2L to 1),
            selectedLumpSumIds = emptySet(),
        )
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 2L, rowIndex = 0),
            productRow(orderItemId = 2L, rowIndex = 1),
        )

        // WHEN
        val content = build(items, preservedSelection = snapshot)

        // THEN
        assertThat(content.selectedItemIds).containsExactlyInAnyOrder("1_0", "2_0")
        assertThat(content.itemsCount).isEqualTo(2)
    }

    @Test
    fun `given a selected lump sum, when content is built, then only that fee row is selected`() {
        // GIVEN
        val snapshot = WooPosRefundSelectionSnapshot(
            unitCountsByOrderItemId = emptyMap(),
            selectedLumpSumIds = setOf(99L),
        )
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            feeRow(orderItemId = 98L),
            feeRow(orderItemId = 99L),
        )

        // WHEN
        val content = build(items, preservedSelection = snapshot)

        // THEN
        assertThat(content.selectedItemIds).containsExactly("fee_99")
        assertThat(content.itemsCount).isEqualTo(1)
    }

    @Test
    fun `given a fee id equal to a line item id, when content is built, then only the fee row is selected`() {
        // GIVEN — the two id spaces are kept apart, so a selected fee never selects a line item
        val snapshot = WooPosRefundSelectionSnapshot(
            unitCountsByOrderItemId = emptyMap(),
            selectedLumpSumIds = setOf(42L),
        )
        val items = listOf(
            productRow(orderItemId = 42L, rowIndex = 0),
            feeRow(orderItemId = 42L),
        )

        // WHEN
        val content = build(items, preservedSelection = snapshot)

        // THEN
        assertThat(content.selectedItemIds).containsExactly("fee_42")
    }

    @Test
    fun `given a line item id equal to a fee id, when content is built, then only the line item is selected`() {
        // GIVEN
        val snapshot = WooPosRefundSelectionSnapshot(
            unitCountsByOrderItemId = mapOf(42L to 1),
            selectedLumpSumIds = emptySet(),
        )
        val items = listOf(
            productRow(orderItemId = 42L, rowIndex = 0),
            feeRow(orderItemId = 42L),
        )

        // WHEN
        val content = build(items, preservedSelection = snapshot)

        // THEN
        assertThat(content.selectedItemIds).containsExactly("42_0")
    }

    @Test
    fun `given a snapshot with unit counts and lump sums, when content is built, then both are restored`() {
        // GIVEN — every other case has one half of the snapshot empty, so the two restore rules
        // never run in the same call. A real reload always carries both.
        val snapshot = WooPosRefundSelectionSnapshot(
            unitCountsByOrderItemId = mapOf(1L to 1, 2L to 2),
            selectedLumpSumIds = setOf(98L, 99L),
        )
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 1L, rowIndex = 1),
            productRow(orderItemId = 2L, rowIndex = 0),
            productRow(orderItemId = 3L, rowIndex = 0),
            feeRow(orderItemId = 98L),
        )

        // WHEN — line 1 shrank to two rows, line 2 to one, line 3 was never selected, and fee 99
        // was refunded elsewhere so its row is gone
        val content = build(items, preservedSelection = snapshot)

        // THEN
        assertThat(content.selectedItemIds).containsExactlyInAnyOrder("1_0", "2_0", "fee_98")
        assertThat(content.itemsCount).isEqualTo(3)
        assertThat(content.allItemsSelected).isFalse()
    }

    @Test
    fun `given nothing of the selection is left, when content is built, then the selection is empty`() {
        // GIVEN — the selected line was fully refunded elsewhere. The cashier picks again rather
        // than being handed items they never chose.
        val snapshot = WooPosRefundSelectionSnapshot(
            unitCountsByOrderItemId = mapOf(1L to 2),
            selectedLumpSumIds = emptySet(),
        )
        val items = listOf(productRow(orderItemId = 2L, rowIndex = 0))

        // WHEN
        val content = build(items, preservedSelection = snapshot)

        // THEN
        assertThat(content.selectedItemIds).isEmpty()
        assertThat(content.itemsCount).isEqualTo(0)
        assertThat(content.allItemsSelected).isFalse()
    }

    private fun build(
        items: List<WooPosRefundableItem>,
        preservedSelection: WooPosRefundSelectionSnapshot?,
    ) = sut(
        order = OrderTestUtils.generateTestOrder(),
        refundableItems = items,
        paymentMethod = "Card",
        preservedSelection = preservedSelection,
    )

    private fun productRow(orderItemId: Long, rowIndex: Int) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = 10L,
        variationId = 0L,
        name = "Product",
        unitPrice = BigDecimal("20.00"),
        unitTax = BigDecimal("2.00"),
        formattedUnitPrice = "$20.00",
        formattedUnitTax = "$2.00",
        rowIndex = rowIndex,
        isLumpSum = false,
    )

    private fun feeRow(orderItemId: Long) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = 0L,
        variationId = 0L,
        name = "Fee",
        unitPrice = BigDecimal("10.00"),
        unitTax = BigDecimal("1.50"),
        formattedUnitPrice = "$10.00",
        formattedUnitTax = "$1.50",
        rowIndex = 0,
        isLumpSum = true,
    )
}
