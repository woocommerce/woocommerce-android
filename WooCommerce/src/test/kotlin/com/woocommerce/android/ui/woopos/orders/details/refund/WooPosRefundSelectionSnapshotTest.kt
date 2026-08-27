package com.woocommerce.android.ui.woopos.orders.details.refund

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class WooPosRefundSelectionSnapshotTest {

    @Test
    fun `given a selection over units and a fee, when a snapshot is taken, then counts and ids match`() {
        // GIVEN
        val items = listOf(
            productRow(orderItemId = 1L, rowIndex = 0),
            productRow(orderItemId = 1L, rowIndex = 1),
            productRow(orderItemId = 2L, rowIndex = 0),
            feeRow(orderItemId = 98L),
            feeRow(orderItemId = 99L),
        )

        // WHEN — the cashier kept the second unit of line 1, all of line 2, and one of the two fees
        val snapshot = WooPosRefundSelectionSnapshot.of(items, selectedIds(items, 1 to 1, 2 to 0, 99 to null))

        // THEN
        assertThat(snapshot.unitCountsByOrderItemId).isEqualTo(mapOf(1L to 1, 2L to 1))
        assertThat(snapshot.selectedLumpSumIds).containsExactly(99L)
    }

    @Test
    fun `given an empty selection, when a snapshot is taken, then it holds nothing`() {
        // GIVEN
        val items = listOf(productRow(orderItemId = 1L, rowIndex = 0), feeRow(orderItemId = 99L))

        // WHEN
        val snapshot = WooPosRefundSelectionSnapshot.of(items, emptySet())

        // THEN
        assertThat(snapshot.unitCountsByOrderItemId).isEmpty()
        assertThat(snapshot.selectedLumpSumIds).isEmpty()
    }

    @Test
    fun `given a selected id that is not among the rows, when a snapshot is taken, then it is ignored`() {
        // GIVEN — the selection and the rows come from different loads
        val items = listOf(productRow(orderItemId = 1L, rowIndex = 0))

        // WHEN
        val snapshot = WooPosRefundSelectionSnapshot.of(items, setOf("7_0", "fee_99"))

        // THEN
        assertThat(snapshot.unitCountsByOrderItemId).isEmpty()
        assertThat(snapshot.selectedLumpSumIds).isEmpty()
    }

    /**
     * Builds the id set from the rows themselves, so these tests do not restate the id format.
     * Each pair is an `orderItemId` plus the `rowIndex` to select, or null for a lump sum.
     */
    private fun selectedIds(
        items: List<WooPosRefundableItem>,
        vararg picks: Pair<Int, Int?>,
    ): Set<String> = picks.mapTo(mutableSetOf()) { (orderItemId, rowIndex) ->
        items.first {
            it.orderItemId == orderItemId.toLong() && it.isLumpSum == (rowIndex == null) &&
                (rowIndex == null || it.rowIndex == rowIndex)
        }.uniqueId
    }

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
